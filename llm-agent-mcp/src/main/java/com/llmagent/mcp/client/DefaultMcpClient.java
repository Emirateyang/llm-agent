package com.llmagent.mcp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.llmagent.mcp.client.logging.DefaultMcpLogMessageHandler;
import com.llmagent.mcp.client.logging.McpLogMessageHandler;
import com.llmagent.mcp.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static com.llmagent.mcp.spec.McpSchema.FIRST_PROTOCOL_VERSION;
import static com.llmagent.mcp.spec.McpSchema.LATEST_PROTOCOL_VERSION;
import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.ValidationUtil.ensureNotNull;

/**
 * The default Model Context Protocol (MCP) client implementation that provides asynchronous
 * communication with MCP servers using Project Reactor's Mono and Flux types.
 *
 * <p>
 * The client follows a lifecycle:
 * <ol>
 * <li>Initialization - Establishes connection and negotiates capabilities
 * <li>Normal Operation - Handles requests and notifications
 * <li>Graceful Shutdown - Ensures clean connection termination
 * </ol>
 * <p>
 */
public class DefaultMcpClient implements McpClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMcpClient.class);

    private final McpClientTransport transport;
    private final McpSchema.Implementation clientInfo;
    private List<String> protocolVersions = List.of(FIRST_PROTOCOL_VERSION, LATEST_PROTOCOL_VERSION);
    private final McpSchema.ClientCapabilities clientCapabilities;

    private final Duration requestTimeout;
    private final Duration initializationTimeout;

    private final String toolTimeoutMsg;
    private final Duration reconnectInterval;
    private final McpLogMessageHandler logHandler;

    // -----------
    // tools
    // -----------
    private static final TypeReference<McpSchema.CallToolResult> CALL_TOOL_RESULT_TYPE_REF = new TypeReference<>() {};
    private static final TypeReference<McpSchema.ListToolsResult> LIST_TOOLS_RESULT_TYPE_REF = new TypeReference<>() {};

    // -----------
    // Resources
    // -----------
    private static final TypeReference<McpSchema.ListResourcesResult> LIST_RESOURCES_RESULT_TYPE_REF = new TypeReference<>() {};
    private static final TypeReference<McpSchema.ReadResourceResult> READ_RESOURCE_RESULT_TYPE_REF = new TypeReference<>() {};
    private static final TypeReference<McpSchema.ListResourceTemplatesResult> LIST_RESOURCE_TEMPLATES_RESULT_TYPE_REF = new TypeReference<>() {};

    // -----------
    // Prompts
    // -----------
    private static final TypeReference<McpSchema.ListPromptsResult> LIST_PROMPTS_RESULT_TYPE_REF = new TypeReference<>() {};
    private static final TypeReference<McpSchema.GetPromptResult> GET_PROMPT_RESULT_TYPE_REF = new TypeReference<>() {};

    /**
     * Server
     */
    private McpSchema.ServerCapabilities serverCapabilities;
    private String serverInstructions;
    private McpSchema.Implementation serverInfo;

    /**
     * The MCP session implementation that manages bidirectional JSON-RPC communication between clients and servers.
     */
    private final McpClientSession mcpSession;

    /**
     * Roots define the boundaries of where servers can operate within the filesystem,
     * allowing them to understand which directories and files they have access to.
     * Servers can request the list of roots from supporting clients and receive notifications when that list changes.
     */
    private final ConcurrentHashMap<String, McpSchema.Root> roots;

    protected final Sinks.One<McpSchema.InitializeResult> initializedSink = Sinks.one();
    private AtomicBoolean initialized = new AtomicBoolean(false);

    public DefaultMcpClient(Builder builder) {

        transport = ensureNotNull(builder.transport, "Transport");
        requestTimeout = ensureNotNull(builder.requestTimeout, "Request timeout");

        initializationTimeout = getOrDefault(builder.initializationTimeout, Duration.ofSeconds(60));
        clientCapabilities = getOrDefault(builder.clientCapabilities,
                McpSchema.ClientCapabilities.builder().roots(true).build());
        clientInfo = getOrDefault(builder.clientInfo, new McpSchema.Implementation("llm-agent-mcp", "1.0"));

        logHandler = getOrDefault(builder.logHandler, new DefaultMcpLogMessageHandler());
        reconnectInterval = getOrDefault(builder.reconnectInterval, Duration.ofSeconds(5));
        toolTimeoutMsg = getOrDefault(builder.toolTimeoutMsg, "There was a timeout executing the tool");

        this.roots = new ConcurrentHashMap<>();

        // request handler
        Map<String, RequestHandler<?>> requestHandlers = new HashMap<>();
        // Roots List Request Handler
        if (this.clientCapabilities.roots() != null) {
            requestHandlers.put(McpSchema.METHOD_ROOTS_LIST, rootsListRequestHandler());
        }

        // Notification Handlers
        Map<String, NotificationHandler> notificationHandlers = new HashMap<>();

        // Tools Change Notification
        List<Function<List<McpSchema.Tool>, Mono<Void>>> toolsChangeConsumers = new ArrayList<>();
        toolsChangeConsumers
                .add((notification) -> Mono.fromRunnable(() -> logger.debug("Tools changed: {}", notification)));
        // TODO add tool change consumers from builder
        notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED,
                asyncToolsChangeNotificationHandler(toolsChangeConsumers));

        // Resources Change Notification
        List<Function<List<McpSchema.Resource>, Mono<Void>>> resourcesChangeConsumers = new ArrayList<>();
        resourcesChangeConsumers
                .add((notification) -> Mono.fromRunnable(() -> logger.debug("Resources changed: {}", notification)));
        // TODO add resource change consumers from builder
        notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED,
                asyncResourcesChangeNotificationHandler(resourcesChangeConsumers));

        // Prompts Change Notification
        List<Function<List<McpSchema.Prompt>, Mono<Void>>> promptsChangeConsumers = new ArrayList<>();
        promptsChangeConsumers
                .add((notification) -> Mono.fromRunnable(() -> logger.debug("Prompts changed: {}", notification)));
        // TODO add prompt change consumers from builder
        notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED,
                asyncPromptsChangeNotificationHandler(promptsChangeConsumers));

        // Utility Logging Notification
        notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_MESSAGE, asyncLoggingNotificationHandler(logHandler));

        this.mcpSession = new McpClientSession(requestTimeout, transport, requestHandlers, notificationHandlers);
    }

    @Override
    public Mono<McpSchema.InitializeResult> initialize() {
        String latestVersion = this.protocolVersions.get(this.protocolVersions.size() - 1);
        McpSchema.InitializeRequest initializeRequest = new McpSchema.InitializeRequest(
                latestVersion, this.clientCapabilities, this.clientInfo);
        Mono<McpSchema.InitializeResult> result = this.mcpSession.sendRequest(McpSchema.METHOD_INITIALIZE,
                initializeRequest, new TypeReference<>() {});

        return result.flatMap(initializeResult -> {

            this.serverCapabilities = initializeResult.capabilities();
            this.serverInstructions = initializeResult.instructions();
            this.serverInfo = initializeResult.serverInfo();

            logger.info("Server response with Protocol: {}, Capabilities: {}, Info: {} and Instructions {}",
                    initializeResult.protocolVersion(), initializeResult.capabilities(), initializeResult.serverInfo(),
                    initializeResult.instructions());

            if (!this.protocolVersions.contains(initializeResult.protocolVersion())) {
                return Mono.error(new McpException(
                        "Unsupported protocol version from the server: " + initializeResult.protocolVersion()));
            }

            return this.mcpSession.sendNotification(McpSchema.METHOD_NOTIFICATION_INITIALIZED, null).doOnSuccess(v -> {
                this.initialized.set(true);
                this.initializedSink.tryEmitValue(initializeResult);
            }).thenReturn(initializeResult);
        });
    }

    private NotificationHandler asyncResourcesChangeNotificationHandler(
            List<Function<List<McpSchema.Resource>, Mono<Void>>> resourcesChangeConsumers) {
        return params -> listResources().flatMap(listResourcesResult -> Flux.fromIterable(resourcesChangeConsumers)
                .flatMap(consumer -> consumer.apply(listResourcesResult.resources()))
                .onErrorResume(error -> {
                    logger.error("Error handling resources list change notification", error);
                    return Mono.empty();
                }).then());
    }

    private NotificationHandler asyncPromptsChangeNotificationHandler(
            List<Function<List<McpSchema.Prompt>, Mono<Void>>> promptsChangeConsumers) {
        return params -> listPrompts().flatMap(listPromptsResult -> Flux.fromIterable(promptsChangeConsumers)
                .flatMap(consumer -> consumer.apply(listPromptsResult.prompts()))
                .onErrorResume(error -> {
                    logger.error("Error handling prompts list change notification", error);
                    return Mono.empty();
                }).then());
    }

    private NotificationHandler asyncLoggingNotificationHandler(McpLogMessageHandler logMessageHandler) {
        // params -> { ... } 对应 NotificationHandler 的 handle(Object params) 方法
        return params -> {
            McpSchema.LoggingMessageNotification message = transport.unmarshal(params,
                    new TypeReference<>() {});
            return Mono.fromRunnable(() -> {
                try {
                    logMessageHandler.handleLogMessage(message);
                } catch (Exception e) {
                    // 注意：fromRunnable 中的异常不会自动传播，需要手动处理或使用其他操作符如 doOnError
                    logger.error("Error handling log message synchronously", e);
                }
            });
        };
    }

    @Override
    public Mono<Void> closeGracefully() {
        return this.mcpSession.closeGracefully();
    }

    /**
     * Obtains a list of tools from the MCP server.
     */
    @Override
    public Mono<McpSchema.ListToolsResult> listTools() {
        return this.listTools(null);
    }

    /**
     * Retrieves a paginated list of tools provided by the server.
     * @param cursor Optional pagination cursor from a previous list request
     */
    public Mono<McpSchema.ListToolsResult> listTools(String cursor) {
        return this.withInitializationCheck("listing tools", initializedResult -> {
            if (this.serverCapabilities.tools() == null) {
                return Mono.error(new McpException("Server does not provide tools capability"));
            }
            return this.mcpSession.sendRequest(McpSchema.METHOD_TOOLS_LIST, new McpSchema.PaginatedRequest(cursor),
                    LIST_TOOLS_RESULT_TYPE_REF);
        });
    }

    @Override
    public Mono<McpSchema.CallToolResult> executeTool(McpSchema.CallToolRequest request) {
        return this.withInitializationCheck("calling tools", initializedResult -> {
            if (this.serverCapabilities.tools() == null) {
                return Mono.error(new McpException("Server does not provide tools capability"));
            }
            return this.mcpSession.sendRequest(McpSchema.METHOD_TOOLS_CALL, request, CALL_TOOL_RESULT_TYPE_REF);
        });
    }

    @Override
    public Mono<McpSchema.ListResourcesResult> listResources() {
        return this.listResources(null);
    }

    /**
     * Retrieves a paginated list of resources provided by the server. Resources represent
     * any kind of UTF-8 encoded data that an MCP server makes available to clients, such
     * as database records, API responses, log files, and more.
     * @return A Mono that completes with the list of resources result.
     * @see McpSchema.ListResourcesResult
     * @see #readResource(McpSchema.Resource)
     */
    public Mono<McpSchema.ListResourcesResult> listResources(String cursor) {
        return this.withInitializationCheck("listing resources", initializedResult -> {
            if (this.serverCapabilities.resources() == null) {
                return Mono.error(new McpException("Server does not provide the resources capability"));
            }
            return this.mcpSession.sendRequest(McpSchema.METHOD_RESOURCES_LIST, new McpSchema.PaginatedRequest(cursor),
                    LIST_RESOURCES_RESULT_TYPE_REF);
        });
    }

    @Override
    public Mono<McpSchema.ListResourceTemplatesResult> listResourceTemplates() {
        return this.listResourceTemplates(null);
    }

    /**
     * Retrieves a paginated list of resource templates provided by the server. Resource
     * templates allow servers to expose parameterized resources using URI templates,
     * enabling dynamic resource access based on variable parameters.
     * @param cursor Optional pagination cursor from a previous list request.
     * @return A Mono that completes with the list of resource templates result.
     * @see McpSchema.ListResourceTemplatesResult
     */
    public Mono<McpSchema.ListResourceTemplatesResult> listResourceTemplates(String cursor) {
        return this.withInitializationCheck("listing resource templates", initializedResult -> {
            if (this.serverCapabilities.resources() == null) {
                return Mono.error(new McpException("Server does not provide the resources capability"));
            }
            return this.mcpSession.sendRequest(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST,
                    new McpSchema.PaginatedRequest(cursor), LIST_RESOURCE_TEMPLATES_RESULT_TYPE_REF);
        });
    }

    @Override
    public Mono<McpSchema.ReadResourceResult> readResource(McpSchema.Resource resource) {
        return this.readResource(new McpSchema.ReadResourceRequest(resource.uri()));
    }

    /**
     * Reads the content of a specific resource identified by the provided request. This
     * method fetches the actual data that the resource represents.
     * @param readResourceRequest The request containing the URI of the resource to read
     * @return A Mono that completes with the resource content.
     * @see McpSchema.ReadResourceRequest
     * @see McpSchema.ReadResourceResult
     */
    public Mono<McpSchema.ReadResourceResult> readResource(McpSchema.ReadResourceRequest readResourceRequest) {
        return this.withInitializationCheck("reading resources", initializedResult -> {
            if (this.serverCapabilities.resources() == null) {
                return Mono.error(new McpException("Server does not provide the resources capability"));
            }
            return this.mcpSession.sendRequest(McpSchema.METHOD_RESOURCES_READ, readResourceRequest,
                    READ_RESOURCE_RESULT_TYPE_REF);
        });
    }

    @Override
    public Mono<McpSchema.ListPromptsResult> listPrompts() {
        return this.listPrompts(null);
    }

    /**
     * Retrieves a paginated list of prompts provided by the server.
     * @param cursor Optional pagination cursor from a previous list request
     * @return A Mono that completes with the list of prompts result.
     * @see McpSchema.ListPromptsResult
     */
    public Mono<McpSchema.ListPromptsResult> listPrompts(String cursor) {
        return this.withInitializationCheck("listing prompts", initializedResult ->
                this.mcpSession.sendRequest(McpSchema.METHOD_PROMPT_LIST, new McpSchema.PaginatedRequest(cursor),
                        LIST_PROMPTS_RESULT_TYPE_REF));
    }

    @Override
    public Mono<McpSchema.GetPromptResult> getPrompt(McpSchema.GetPromptRequest getPromptRequest) {
        return this.withInitializationCheck("getting prompts", initializedResult ->
                this.mcpSession.sendRequest(McpSchema.METHOD_PROMPT_GET, getPromptRequest, GET_PROMPT_RESULT_TYPE_REF));
    }

    @Override
    public void checkHealth() {
        this.transport.checkHealth();
        this.withInitializationCheck("pinging the server", initializedResult ->
                this.mcpSession.sendRequest(McpSchema.METHOD_PING, null, new TypeReference<>() {}));
    }

    @Override
    public void close() throws Exception {
        this.mcpSession.close();
    }

    private RequestHandler<McpSchema.ListRootsResult> rootsListRequestHandler() {
        return params -> {
            @SuppressWarnings("unused")
            McpSchema.PaginatedRequest request = transport.unmarshal(params, new TypeReference<>() {});
            List<McpSchema.Root> roots = this.roots.values().stream().toList();
            return Mono.just(new McpSchema.ListRootsResult(roots));
        };
    }

    private NotificationHandler asyncToolsChangeNotificationHandler(
            List<Function<List<McpSchema.Tool>, Mono<Void>>> toolsChangeConsumers) {
        return params -> this.listTools()
                .flatMap(listToolsResult -> Flux.fromIterable(toolsChangeConsumers)
                        .flatMap(consumer -> consumer.apply(listToolsResult.tools()))
                        .onErrorResume(error -> {
                            logger.error("Error handling tools list change notification", error);
                            return Mono.empty();
                        }).then());
    }

    /**
     * Utility method to handle the common pattern of checking initialization before executing an operation.
     * @param actionName The action to perform if the client is initialized
     * @param operation The operation to execute if the client is initialized
     * @return A Mono that completes with the result of the operation
     */
    private <T> Mono<T> withInitializationCheck(String actionName,
                                                Function<McpSchema.InitializeResult, Mono<T>> operation) {
        return this.initializedSink.asMono()
                .timeout(this.initializationTimeout)
                .onErrorResume(TimeoutException.class,
                        ex -> Mono.error(new McpException("Client must be initialized before " + actionName)))
                .flatMap(operation);
    }

    public static class Builder {

        private String toolTimeoutMsg;
        private McpClientTransport transport;
        private McpSchema.Implementation clientInfo;
        private String protocolVersion;
        private Duration toolExecTimeout;
        private Duration resourcesTimeout;
        private Duration pingTimeout;
        private Duration promptsTimeout;
        private Duration requestTimeout;
        private Duration initializationTimeout;
        private McpLogMessageHandler logHandler;
        private Duration reconnectInterval;
        private McpSchema.ClientCapabilities clientCapabilities;

        public Builder transport(McpClientTransport transport) {
            this.transport = transport;
            return this;
        }

        /**
         * Sets the client info will use to identify itself to the
         * MCP server in the initialization message.
         */
        public Builder clientInfo(McpSchema.Implementation clientInfo) {
            this.clientInfo = clientInfo;
            return this;
        }

        /**
         * Sets the protocol version that the client will advertise in the
         * initialization message. The default value right now is
         * "2024-11-05", but will change over time in later langchain4j
         * versions.
         */
        public Builder protocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        /**
         * Sets the timeout for tool execution.
         * This value applies to each tool execution individually.
         * The default value is 60 seconds.
         * A value of zero means no timeout.
         */
        public Builder toolExecTimeout(Duration toolExecTimeout) {
            this.toolExecTimeout = toolExecTimeout;
            return this;
        }

        /**
         * Sets the timeout for resource-related operations (listing resources as well as reading the contents of a resource).
         * The default value is 60 seconds.
         * A value of zero means no timeout.
         */
        public Builder resourcesTimeout(Duration resourcesTimeout) {
            this.resourcesTimeout = resourcesTimeout;
            return this;
        }

        /**
         * Sets the timeout for prompt-related operations (listing prompts as well as rendering the contents of a prompt).
         * The default value is 60 seconds.
         * A value of zero means no timeout.
         */
        public Builder promptsTimeout(Duration promptsTimeout) {
            this.promptsTimeout = promptsTimeout;
            return this;
        }

        /**
         * Sets the error message to return when a tool execution times out.
         * The default value is "There was a timeout executing the tool".
         */
        public Builder toolTimeoutMsg(String toolTimeoutMsg) {
            this.toolTimeoutMsg = toolTimeoutMsg;
            return this;
        }

        /**
         * Sets the timeout for request
         */
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        /**
         * Sets the timeout for initialization
         */
        public Builder initializationTimeout(Duration initializationTimeout) {
            this.initializationTimeout = initializationTimeout;
            return this;
        }

        /**
         * Sets the log message handler for the client.
         */
        public Builder logHandler(McpLogMessageHandler logHandler) {
            this.logHandler = logHandler;
            return this;
        }

        /**
         * The timeout to apply when waiting for a ping response.
         * Currently, this is only used in the health check - if the
         * server does not send a pong within this timeframe, the health
         * check will fail. The timeout is 10 seconds.
         */
        public Builder pingTimeout(Duration pingTimeout) {
            this.pingTimeout = pingTimeout;
            return this;
        }

        /**
         * The delay before attempting to reconnect after a failed connection.
         * The default is 5 seconds.
         */
        public Builder reconnectInterval(Duration reconnectInterval) {
            this.reconnectInterval = reconnectInterval;
            return this;
        }

        /**
         * Sets the capabilities of client
         */
        public Builder clientCapabilities(McpSchema.ClientCapabilities clientCapabilities) {
            this.clientCapabilities = clientCapabilities;
            return this;
        }

        public DefaultMcpClient build() {
            return new DefaultMcpClient(this);
        }
    }
}
