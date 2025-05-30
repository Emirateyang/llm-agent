package com.llmagent.mcp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.llmagent.mcp.spec.*;
import com.llmagent.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.llmagent.util.ValidationUtil.ensureNotNull;

public class McpClientSession implements McpSession {

    private static final Logger logger = LoggerFactory.getLogger(McpClientSession.class);

    private final Duration requestTimeout;

    /** Transport layer implementation for message exchange */
    private final McpClientTransport transport;

    /** Map of request handlers keyed by method name */
    private final ConcurrentHashMap<String, RequestHandler<?>> requestHandlers = new ConcurrentHashMap<>();

    /** Map of notification handlers keyed by method name */
    private final ConcurrentHashMap<String, NotificationHandler> notificationHandlers = new ConcurrentHashMap<>();

    /** Map of pending responses keyed by request ID */
    private final ConcurrentHashMap<Object, MonoSink<McpSchema.JSONRPCResponse>> pendingResponses = new ConcurrentHashMap<>();

    /** Atomic counter for generating unique request IDs */
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final Disposable connection;

    /**
     * Creates a new McpClientSession with the specified configuration and handlers.
     * @param requestTimeout Duration to wait for responses
     * @param transport Transport implementation for message exchange
     * @param requestHandlers Map of method names to request handlers
     * @param notificationHandlers Map of method names to notification handlers
     */
    public McpClientSession(Duration requestTimeout, McpClientTransport transport,
                            Map<String, RequestHandler<?>> requestHandlers,
                            Map<String, NotificationHandler> notificationHandlers) {

        ensureNotNull(requestTimeout, "The requestTimeout can not be null");
        ensureNotNull(transport, "The transport can not be null");
        ensureNotNull(requestHandlers, "The requestHandlers can not be null");
        ensureNotNull(notificationHandlers, "The notificationHandlers can not be null");

        this.requestTimeout = requestTimeout;
        this.transport = transport;
        this.requestHandlers.putAll(requestHandlers);
        this.notificationHandlers.putAll(notificationHandlers);

        this.connection = this.transport.connect(mono -> mono.doOnNext(message -> {
            if (message instanceof McpSchema.JSONRPCResponse response) {
                logger.debug("Received Response: {}", response);
                var sink = pendingResponses.remove(response.id());
                if (sink == null) {
                    logger.warn("Unexpected response for unknown id {}", response.id());
                } else {
                    sink.success(response);
                }
            } else if (message instanceof McpSchema.JSONRPCRequest request) {
                logger.debug("Received request: {}", request);
                handleIncomingRequest(request).subscribe(response -> transport.sendMessage(response).subscribe(),
                        error -> {
                            var errorResponse = new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(),
                                    null, new McpSchema.JSONRPCResponse.JSONRPCError(
                                    McpSchema.ErrorCodes.INTERNAL_ERROR, error.getMessage(), null));
                            transport.sendMessage(errorResponse).subscribe();
                        });
            } else if (message instanceof McpSchema.JSONRPCNotification notification) {
                logger.debug("Received notification: {}", notification);
                handleIncomingNotification(notification).subscribe(null,
                        error -> logger.error("Error handling notification: {}", error.getMessage()));
            }
		})).subscribe();
	}

    /**
     * Sends a JSON-RPC request and returns the response.
     */
    @Override
    public <T> Mono<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
        String requestId = this.generateRequestId();

        return Mono.<McpSchema.JSONRPCResponse>create(sink -> {
            this.pendingResponses.put(requestId, sink);
            McpSchema.JSONRPCRequest rpcRequest =
                    new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION, requestId, method, requestParams);
            this.transport.sendMessage(rpcRequest)
                    .subscribe(v -> {
                    }, error -> {
                        this.pendingResponses.remove(requestId);
                        sink.error(error);
                    });
        }).timeout(this.requestTimeout).handle((jsonRpcResponse, sink) -> {
            if (jsonRpcResponse.error() != null) {
                logger.error("Error handling request: {}", jsonRpcResponse.error());
                sink.error(new McpException(jsonRpcResponse.error()));
            } else {
                if (typeRef.getType().equals(Void.class)) {
                    sink.complete();
                } else {
                    sink.next(this.transport.unmarshal(jsonRpcResponse.result(), typeRef));
                }
            }
        });
    }

    /**
     * Sends a JSON-RPC notification.
     */
    @Override
    public Mono<Void> sendNotification(String method, Object params) {
        McpSchema.JSONRPCNotification notification =
                new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION, method, params);
        return this.transport.sendMessage(notification);
    }

    /**
     * Closes the session gracefully, allowing pending operations to complete.
     */
    @Override
    public Mono<Void> closeGracefully() {
        return Mono.defer(() -> {
            this.connection.dispose();
            return transport.closeGracefully();
        });
    }

    /**
     * Closes the session immediately
     */
    @Override
    public void close() throws Exception {
        this.connection.dispose();
        transport.close();
    }

    /**
     * Handles an incoming JSON-RPC request by routing it to the appropriate handler.
     * @param request The incoming JSON-RPC request
     * @return A Mono containing the JSON-RPC response
     */
    private Mono<McpSchema.JSONRPCResponse> handleIncomingRequest(McpSchema.JSONRPCRequest request) {
        return Mono.defer(() -> {
            var handler = this.requestHandlers.get(request.method());
            if (handler == null) {
                MethodNotFoundError error = getMethodNotFoundError(request.method());
                return Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), null,
                        new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND,
                                error.message(), error.data())));
            }

            return handler.handle(request.params())
                    .map(result -> new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), result, null))
                    .onErrorResume(error ->
                            Mono.just(
                                new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), null,
                                new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
                                error.getMessage(), null))));
        });
    }

    /**
     * Handles an incoming JSON-RPC notification by routing it to the appropriate handler.
     * @param notification The incoming JSON-RPC notification
     * @return A Mono that completes when the notification is processed
     */
    private Mono<Void> handleIncomingNotification(McpSchema.JSONRPCNotification notification) {
        return Mono.defer(() -> {
            var handler = notificationHandlers.get(notification.method());
            if (handler == null) {
                logger.error("No handler registered for notification method: {}", notification.method());
                return Mono.empty();
            }
            return handler.handle(notification.params());
        });
    }

    record MethodNotFoundError(String method, String message, Object data) {}
    public static MethodNotFoundError getMethodNotFoundError(String method) {
        switch (method) {
            case McpSchema.METHOD_ROOTS_LIST:
                return new MethodNotFoundError(method, "Roots not supported",
                        Map.of("reason", "Client does not have roots capability"));
            default:
                return new MethodNotFoundError(method, "Method not found: " + method, null);
        }
    }

    /**
     * Generates a unique request ID in a non-blocking way.
     * Combines a session-specific prefix with an atomic counter to ensure uniqueness.
     * @return A unique request ID string
     */
    private String generateRequestId() {
        return UUIDUtil.randomUUID().substring(0, 12) + "-" + this.requestCounter.getAndIncrement();
    }
}
