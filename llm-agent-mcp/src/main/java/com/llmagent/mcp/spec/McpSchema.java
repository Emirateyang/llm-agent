package com.llmagent.mcp.spec;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmagent.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Based on the <a href="http://www.jsonrpc.org/specification">JSON-RPC 2.0 specification</a> and
 * the <a href="https://github.com/modelcontextprotocol/modelcontextprotocol/blob/main/schema/2025-03-26/schema.ts">Model Context Protocol Schema</a>.
 */
public final class McpSchema {

	private static final Logger logger = LoggerFactory.getLogger(McpSchema.class);

	private McpSchema() {
	}

	public static final String LATEST_PROTOCOL_VERSION = "2025-03-26";
	public static final String FIRST_PROTOCOL_VERSION = "2024-11-05";

	public static final String JSONRPC_VERSION = "2.0";

	// ---------------------------
	// Method Names
	// ---------------------------

	// Lifecycle Methods
	public static final String METHOD_INITIALIZE = "initialize";

	public static final String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";

	public static final String METHOD_PING = "ping";

	// Tool Methods
	public static final String METHOD_TOOLS_LIST = "tools/list";

	public static final String METHOD_TOOLS_CALL = "tools/call";

	public static final String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

	// Resources Methods
	public static final String METHOD_RESOURCES_LIST = "resources/list";

	public static final String METHOD_RESOURCES_READ = "resources/read";

	public static final String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";

	public static final String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";

	public static final String METHOD_RESOURCES_SUBSCRIBE = "resources/subscribe";

	public static final String METHOD_RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";

	// Prompt Methods
	public static final String METHOD_PROMPT_LIST = "prompts/list";

	public static final String METHOD_PROMPT_GET = "prompts/get";

	public static final String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

	public static final String METHOD_COMPLETION_COMPLETE = "completion/complete";

	// Logging Methods
	public static final String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";

	public static final String METHOD_NOTIFICATION_MESSAGE = "notifications/message";

	// Roots Methods
	public static final String METHOD_ROOTS_LIST = "roots/list";

	public static final String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

	// Sampling Methods
	public static final String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	// ---------------------------
	// JSON-RPC Error Codes
	// ---------------------------
	/**
	 * Standard error codes used in MCP JSON-RPC responses.
	 */
	public static final class ErrorCodes {

		/**
		 * Invalid JSON was received by the server.
		 */
		public static final int PARSE_ERROR = -32700;

		/**
		 * The JSON sent is not a valid Request object.
		 */
		public static final int INVALID_REQUEST = -32600;

		/**
		 * The method does not exist / is not available.
		 */
		public static final int METHOD_NOT_FOUND = -32601;

		/**
		 * Invalid method parameter(s).
		 */
		public static final int INVALID_PARAMS = -32602;

		/**
		 * Internal JSON-RPC error.
		 */
		public static final int INTERNAL_ERROR = -32603;

	}

	public sealed interface Request
			permits InitializeRequest, CallToolRequest, CreateMessageRequest, CompleteRequest, GetPromptRequest {
	}

	private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<>() {
	};

	/**
	 * Deserializes a JSON string into a JSONRPCMessage object.
	 * @param objectMapper The ObjectMapper instance to use for deserialization
	 * @param jsonText The JSON string to deserialize
	 * @return A JSONRPCMessage instance using either the {@link JSONRPCRequest},
	 * {@link JSONRPCNotification}, or {@link JSONRPCResponse} classes.
	 * @throws IOException If there's an error during deserialization
	 * @throws IllegalArgumentException If the JSON structure doesn't match any known
	 * message type
	 */
	public static JSONRPCMessage deserializeJsonRpcMessage(ObjectMapper objectMapper, String jsonText)
			throws IOException {

		logger.debug("Received JSON message: {}", jsonText);

		var map = objectMapper.readValue(jsonText, MAP_TYPE_REF);

		// Determine message type based on specific JSON structure
		if (map.containsKey("method") && map.containsKey("id")) {
			return objectMapper.convertValue(map, JSONRPCRequest.class);
		}
		else if (map.containsKey("method") && !map.containsKey("id")) {
			return objectMapper.convertValue(map, JSONRPCNotification.class);
		}
		else if (map.containsKey("result") || map.containsKey("error")) {
			return objectMapper.convertValue(map, JSONRPCResponse.class);
		}

		throw new IllegalArgumentException("Cannot deserialize JSONRPCMessage: " + jsonText);
	}

	// ---------------------------
	// JSON-RPC Message Types
	// ---------------------------

	public interface MethodAndParams {
		@JsonProperty("method")
		String method();

		@JsonProperty("params")
		Object params();
	}

	public sealed interface JSONRPCMessage permits JSONRPCRequest, JSONRPCNotification, JSONRPCResponse {
		String jsonrpc();
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record JSONRPCRequest (
			@JsonProperty("jsonrpc") String jsonrpc,
			@JsonProperty("id") Object id,
			String method,
			Object params) implements JSONRPCMessage, MethodAndParams {
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record JSONRPCNotification (
			@JsonProperty("jsonrpc") String jsonrpc,
			String method,
			Object params) implements JSONRPCMessage, MethodAndParams {
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record JSONRPCResponse (
			@JsonProperty("jsonrpc") String jsonrpc,
			@JsonProperty("id") Object id,
			@JsonProperty("result") Object result,
			@JsonProperty("error") JSONRPCError error) implements JSONRPCMessage {

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public record JSONRPCError (
			@JsonProperty("code") int code,
			@JsonProperty("message") String message,
			@JsonProperty("data") Object data) {
		}
	}

	// ---------------------------
	// Initialization
	// ---------------------------
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record InitializeRequest (
		@JsonProperty("protocolVersion") String protocolVersion,
		@JsonProperty("capabilities") ClientCapabilities capabilities,
		@JsonProperty("clientInfo") Implementation clientInfo) implements Request {		
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record InitializeResult (
		@JsonProperty("protocolVersion") String protocolVersion,
		@JsonProperty("capabilities") ServerCapabilities capabilities,
		@JsonProperty("serverInfo") Implementation serverInfo,
		@JsonProperty("instructions") String instructions) {
	}

	/**
	 * Clients can implement additional features to enrich connected MCP servers with
	 * additional capabilities. These capabilities can be used to extend the functionality
	 * of the server, or to provide additional information to the server about the
	 * client's capabilities.
	 *
	 * @param experimental WIP
	 * @param roots define the boundaries of where servers can operate within the
	 * filesystem, allowing them to understand which directories and files they have
	 * access to.
	 * @param sampling Provides a standardized way for servers to request LLM sampling
	 * (“completions” or “generations”) from language models via clients.
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ClientCapabilities( 
		@JsonProperty("experimental") Map<String, Object> experimental,
		@JsonProperty("roots") RootCapabilities roots,
		@JsonProperty("sampling") Sampling sampling) {

		/**
		 * Roots define the boundaries of where servers can operate within the filesystem,
		 * allowing them to understand which directories and files they have access to.
		 * Servers can request the list of roots from supporting clients and
		 * receive notifications when that list changes.
		 *
		 * @param listChanged Whether the client would send notification about roots
		 * 		  has changed since the last time the server checked.
		 */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)	
		public record RootCapabilities(
			@JsonProperty("listChanged") Boolean listChanged) {
		}

		/**
		 * Provides a standardized way for servers to request LLM
	 	 * sampling ("completions" or "generations") from language
		 * models via clients. This flow allows clients to maintain
		 * control over model access, selection, and permissions
		 * while enabling servers to leverage AI capabilities—with
		 * no server API keys necessary. Servers can request text or
		 * image-based interactions and optionally include context
		 * from MCP servers in their prompts.
		 */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)			
		public record Sampling() {
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private Map<String, Object> experimental;
			private RootCapabilities roots;
			private Sampling sampling;

			public Builder experimental(Map<String, Object> experimental) {
				this.experimental = experimental;
				return this;
			}

			public Builder roots(Boolean listChanged) {
				this.roots = new RootCapabilities(listChanged);
				return this;
			}

			public Builder sampling() {
				this.sampling = new Sampling();
				return this;
			}

			public ClientCapabilities build() {
				return new ClientCapabilities(experimental, roots, sampling);
			}
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ServerCapabilities (
	    @JsonProperty("completions") Completions completions,
		@JsonProperty("experimental") Map<String, Object> experimental,
		@JsonProperty("logging") Logging logging,
		@JsonProperty("prompts") Prompts prompts,
		@JsonProperty("resources") Resources resources,
		@JsonProperty("tools") Tools tools) {

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public record Completions() {
		}
			
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public record Logging() {
		}
	
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public record Prompts (
			@JsonProperty("listChanged") Boolean listChanged) {
		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public record Resources (
			@JsonProperty("subscribe") Boolean subscribe,
			@JsonProperty("listChanged") Boolean listChanged) {
		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public record Tools(
			@JsonProperty("listChanged") Boolean listChanged) {
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private Completions completions;
			private Map<String, Object> experimental;
			private Logging logging = new Logging();
			private Prompts prompts;
			private Resources resources;
			private Tools tools;

			public Builder completions(Completions completions) {
				this.completions = completions;
				return this;
			}

			public Builder experimental(Map<String, Object> experimental) {
				this.experimental = experimental;
				return this;
			}

			public Builder logging() {
				this.logging = new Logging();
				return this;
			}

			public Builder prompts(Boolean listChanged) {
				this.prompts = new Prompts(listChanged);
				return this;
			}

			public Builder resources(Boolean subscribe, Boolean listChanged) {
				this.resources = new Resources(subscribe, listChanged);
				return this;
			}

			public Builder tools(Boolean listChanged) {
				this.tools = new Tools(listChanged);
				return this;
			}

			public ServerCapabilities build() {
				return new ServerCapabilities(completions, experimental, logging, prompts, resources, tools);
			}
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Implementation (
		@JsonProperty("name") String name,
		@JsonProperty("version") String version) {
	}

	// Existing Enums and Base Types (from previous implementation)
	public enum Role {
		@JsonProperty("user") USER,
		@JsonProperty("assistant") ASSISTANT
	}

	// ---------------------------
	// Resource Interfaces
	// ---------------------------
	/**
	 * Base for objects that include optional annotations for the client. The client can
	 * use annotations to inform how objects are used or displayed
	 */
	public interface Annotated {
		Annotations annotations();
	}

	/**
	 * Optional annotations for the client. The client can use annotations to inform how
	 * objects are used or displayed.
	 *
	 * @param audience Describes who the intended customer of this object or data is. It
	 * can include multiple entries to indicate content useful for multiple audiences
	 * (e.g., `["user", "assistant"]`).
	 * @param priority Describes how important this data is for operating the server. A
	 * value of 1 means "most important," and indicates that the data is effectively
	 * required, while 0 means "least important," and indicates that the data is entirely
	 * optional. It is a number between 0 and 1.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Annotations (
		@JsonProperty("audience") List<Role> audience,
		@JsonProperty("priority") Double priority) {
	}

	/**
	 * A known resource that the server is capable of reading.
	 *
	 * @param uri the URI of the resource.
	 * @param name A human-readable name for this resource. This can be used by clients to
	 * populate UI elements.
	 * @param description A description of what this resource represents. This can be used
	 * by clients to improve the LLM's understanding of available resources. It can be
	 * thought of like a "hint" to the model.
	 * @param mimeType The MIME type of this resource, if known.
	 * @param annotations Optional annotations for the client. The client can use
	 * annotations to inform how objects are used or displayed.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Resource (
		@JsonProperty("uri") String uri,
		@JsonProperty("name") String name,
		@JsonProperty("description") String description,
		@JsonProperty("mimeType") String mimeType,
		@JsonProperty("annotations") Annotations annotations) implements Annotated {
	}

	/**
	 * Resource templates allow servers to expose parameterized resources using URI
	 * templates.
	 *
	 * @param uriTemplate A URI template that can be used to generate URIs for this
	 * resource.
	 * @param name A human-readable name for this resource. This can be used by clients to
	 * populate UI elements.
	 * @param description A description of what this resource represents. This can be used
	 * by clients to improve the LLM's understanding of available resources. It can be
	 * thought of like a "hint" to the model.
	 * @param mimeType The MIME type of this resource, if known.
	 * @param annotations Optional annotations for the client. The client can use
	 * annotations to inform how objects are used or displayed.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6570">RFC 6570</a>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ResourceTemplate (
		@JsonProperty("uriTemplate") String uriTemplate,
		@JsonProperty("name") String name,
		@JsonProperty("description") String description,
		@JsonProperty("mimeType") String mimeType,
		@JsonProperty("annotations") Annotations annotations) implements Annotated {
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ListResourcesResult (
		@JsonProperty("resources") List<Resource> resources,
		@JsonProperty("nextCursor") String nextCursor) {
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ListResourceTemplatesResult(
		@JsonProperty("resourceTemplates") List<ResourceTemplate> resourceTemplates,
		@JsonProperty("nextCursor") String nextCursor) {
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ReadResourceRequest (
		@JsonProperty("uri") String uri) {
	} 

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ReadResourceResult (
		@JsonProperty("contents") List<ResourceContents> contents) {
	}

	/**
	 * Sent from the client to request resources/updated notifications from the server
	 * whenever a particular resource changes.
	 *
	 * @param uri the URI of the resource to subscribe to. The URI can use any protocol;
	 * it is up to the server how to interpret it.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record SubscribeRequest (
		@JsonProperty("uri") String uri) {
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record UnsubscribeRequest (
		@JsonProperty("uri") String uri) {
	}

	/**
	 * The contents of a specific resource or sub-resource.
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, include = As.PROPERTY)
	@JsonSubTypes({ @JsonSubTypes.Type(value = TextResourceContents.class, name = "text"),
			@JsonSubTypes.Type(value = BlobResourceContents.class, name = "blob") })
	public sealed interface ResourceContents permits TextResourceContents, BlobResourceContents {
		/**
		 * The URI of this resource.
		 * @return the URI of this resource.
		 */
		String uri();

		/**
		 * The MIME type of this resource.
		 * @return the MIME type of this resource.
		 */
		String mimeType();
	}

	/**
	 * Text contents of a resource.
	 *
	 * @param uri the URI of this resource.
	 * @param mimeType the MIME type of this resource.
	 * @param text the text of the resource. This must only be set if the resource can
	 * actually be represented as text (not binary data).
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TextResourceContents (
		@JsonProperty("uri") String uri,
		@JsonProperty("mimeType") String mimeType,
		@JsonProperty("text") String text) implements ResourceContents {
	}

	/**
	 * Binary contents of a resource.
	 *
	 * @param uri the URI of this resource.
	 * @param mimeType the MIME type of this resource.
	 * @param blob a base64-encoded string representing the binary data of the resource.
	 * This must only be set if the resource can actually be represented as binary data
	 * (not text).
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record BlobResourceContents (
		@JsonProperty("uri") String uri,
		@JsonProperty("mimeType") String mimeType,
		@JsonProperty("blob") String blob) implements ResourceContents {
	}

	// ---------------------------
	// Prompt Interfaces
	// ---------------------------
	/**
	 * A prompt or prompt template that the server offers.
	 *
	 * @param name The name of the prompt or prompt template.
	 * @param description An optional description of what this prompt provides.
	 * @param arguments A list of arguments to use for templating the prompt.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Prompt (
		@JsonProperty("name") String name,
		@JsonProperty("description") String description,
		@JsonProperty("arguments") List<PromptArgument> arguments) {
	}

	/**
	 * Describes an argument that a prompt can accept.
	 *
	 * @param name The name of the argument.
	 * @param description A human-readable description of the argument.
	 * @param required Whether this argument must be provided.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record PromptArgument(
		@JsonProperty("name") String name,
		@JsonProperty("description") String description,
		@JsonProperty("required") Boolean required) {
	}

	/**
	 * Describes a message returned as part of a prompt.
	 * <p>
	 * This is similar to `SamplingMessage`, but also supports the embedding of resources
	 * from the MCP server.
	 * </p>
	 * @param role The sender or recipient of messages and data in a conversation.
	 * @param content The content of the message of type {@link Content}.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record PromptMessage (
		@JsonProperty("role") Role role,
		@JsonProperty("content") Content content) {
	}

	/**
	 * The server's response to a prompts/list request from the client.
	 *
	 * @param prompts A list of prompts that the server provides.
	 * @param nextCursor An optional cursor for pagination. If present, indicates there
	 * are more prompts available.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ListPromptsResult (
		@JsonProperty("prompts") List<Prompt> prompts,
		@JsonProperty("nextCursor") String nextCursor) {
	}

	/**
	 * Used by the client to get a prompt provided by the server.
	 *
	 * @param name The name of the prompt or prompt template.
	 * @param arguments Arguments to use for templating the prompt.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GetPromptRequest (
		@JsonProperty("name") String name,
		@JsonProperty("arguments") Map<String, Object> arguments) implements Request {
	}

	/**
	 * The server's response to a prompts/get request from the client.
	 *
	 * @param description An optional description for the prompt.
	 * @param messages A list of messages to display as part of the prompt.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GetPromptResult(
		@JsonProperty("description") String description,
		@JsonProperty("messages") List<PromptMessage> messages) {
	}

	// ---------------------------
	// Tool Interfaces
	// ---------------------------
	/**
	 * The server's response to a tools/list request from the client.
	 *
	 * @param tools A list of tools that the server provides.
	 * @param nextCursor An optional cursor for pagination. If present, indicates there
	 * are more tools available.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ListToolsResult (
		@JsonProperty("tools") List<Tool> tools,
		@JsonProperty("nextCursor") String nextCursor) {
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record JsonSchema(
		@JsonProperty("type") String type,
		@JsonProperty("properties") Map<String, Object> properties,
		@JsonProperty("required") List<String> required,
		@JsonProperty("additionalProperties") Boolean additionalProperties) {
	}

	/**
	 * Represents a tool that the server provides. Tools enable servers to expose
	 * executable functionality to the system. Through these tools, you can interact with
	 * external systems, perform computations, and take actions in the real world.
	 *
	 * @param name A unique identifier for the tool. This name is used when calling the
	 * tool.
	 * @param description A human-readable description of what the tool does. This can be
	 * used by clients to improve the LLM's understanding of available tools.
	 * @param inputSchema A JSON Schema object that describes the expected structure of
	 * the arguments when calling this tool. This allows clients to validate tool
	 * arguments before sending them to the server.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Tool(
		@JsonProperty("name") String name,
		@JsonProperty("description") String description,
		@JsonProperty("inputSchema") JsonSchema inputSchema) {
	
		public Tool(String name, String description, String schema) {
			this(name, description, parseSchema(schema));
		}
			
	}

	private static JsonSchema parseSchema(String schema) {
		try {
			return OBJECT_MAPPER.readValue(schema, JsonSchema.class);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Invalid schema: " + schema, e);
		}
	}

	/**
	 * Used by the client to call a tool provided by the server.
	 *
	 * @param name The name of the tool to call. This must match a tool name from
	 * tools/list.
	 * @param arguments Arguments to pass to the tool. These must conform to the tool's
	 * input schema.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CallToolRequest (
		@JsonProperty("name") String name,
		@JsonProperty("arguments") Map<String, Object> arguments) implements Request {

		public CallToolRequest(String name, String jsonArguments) {
			this(name, parseJsonArguments(jsonArguments));			
		}

		private static Map<String, Object> parseJsonArguments(String jsonArguments) {
			try {
				return OBJECT_MAPPER.readValue(jsonArguments, MAP_TYPE_REF);
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Invalid arguments: " + jsonArguments, e);
			}
		}
	}

	/**
	 * The server's response to a tools/call request from the client.
	 *
	 * @param content A list of content items representing the tool's output. Each item can be text, an image,
	 *                or an embedded resource.
	 * @param isError If true, indicates that the tool execution failed and the content contains error information.
	 *                If false or absent, indicates successful execution.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CallToolResult( 
		@JsonProperty("content") List<Content> content,
		@JsonProperty("isError") Boolean isError) {

		/**
		 * Creates a new instance of {@link CallToolResult} with a string containing the
		 * tool result.
		 *
		 * @param content The content of the tool result. This will be mapped to a one-sized list
		 * 				  with a {@link TextContent} element.
		 * @param isError If true, indicates that the tool execution failed and the content contains error information.
		 *                If false or absent, indicates successful execution.
		 */
		public CallToolResult(String content, Boolean isError) {
			this(List.of(new TextContent(content)), isError);
		}

		/**
		 * Creates a builder for {@link CallToolResult}.
		 * @return a new builder instance
		 */
		public static Builder builder() {
			return new Builder();
		}

		/**
		 * Builder for {@link CallToolResult}.
		 */
		public static class Builder {
			private List<Content> content = new ArrayList<>();
			private Boolean isError;

			/**
			 * Sets the content list for the tool result.
			 * @param content the content list
			 * @return this builder
			 */
			public Builder content(List<Content> content) {
				ValidationUtil.ensureNotEmpty(content, "content must not be null");
				this.content = content;
				return this;
			}

			/**
			 * Sets the text content for the tool result.
			 * @param textContent the text content
			 * @return this builder
			 */
			public Builder textContent(List<String> textContent) {
				ValidationUtil.ensureNotEmpty(textContent, "textContent must not be null");
				textContent.stream()
					.map(TextContent::new)
					.forEach(this.content::add);
				return this;
			}

			/**
			 * Adds a content item to the tool result.
			 * @param contentItem the content item to add
			 * @return this builder
			 */
			public Builder addContent(Content contentItem) {
				ValidationUtil.ensureNotNull(contentItem, "contentItem must not be null");
				if (this.content == null) {
					this.content = new ArrayList<>();
				}
				this.content.add(contentItem);
				return this;
			}

			/**
			 * Adds a text content item to the tool result.
			 * @param text the text content
			 * @return this builder
			 */
			public Builder addTextContent(String text) {
				ValidationUtil.ensureNotNull(text, "text must not be null");
				return addContent(new TextContent(text));
			}

			/**
			 * Sets whether the tool execution resulted in an error.
			 * @param isError true if the tool execution failed, false otherwise
			 * @return this builder
			 */
			public Builder isError(Boolean isError) {
				ValidationUtil.ensureNotNull(isError, "isError must not be null");
				this.isError = isError;
				return this;
			}

			/**
			 * Builds a new {@link CallToolResult} instance.
			 * @return a new CallToolResult instance
			 */
			public CallToolResult build() {
				return new CallToolResult(content, isError);
			}
		}

	} 

	// ---------------------------
	// Sampling Interfaces
	// ---------------------------
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ModelPreferences(
	@JsonProperty("hints") List<ModelHint> hints,
	@JsonProperty("costPriority") Double costPriority,
	@JsonProperty("speedPriority") Double speedPriority,
	@JsonProperty("intelligencePriority") Double intelligencePriority) {
	
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private List<ModelHint> hints;
		private Double costPriority;
		private Double speedPriority;
		private Double intelligencePriority;

		public Builder hints(List<ModelHint> hints) {
			this.hints = hints;
			return this;
		}

		public Builder addHint(String name) {
			if (this.hints == null) {
				this.hints = new ArrayList<>();
			}
			this.hints.add(new ModelHint(name));
			return this;
		}

		public Builder costPriority(Double costPriority) {
			this.costPriority = costPriority;
			return this;
		}

		public Builder speedPriority(Double speedPriority) {
			this.speedPriority = speedPriority;
			return this;
		}

		public Builder intelligencePriority(Double intelligencePriority) {
			this.intelligencePriority = intelligencePriority;
			return this;
		}

		public ModelPreferences build() {
			return new ModelPreferences(hints, costPriority, speedPriority, intelligencePriority);
		}
	}
} 

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ModelHint(@JsonProperty("name") String name) {
		public static ModelHint of(String name) {
			return new ModelHint(name);
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record SamplingMessage(
		@JsonProperty("role") Role role,
		@JsonProperty("content") Content content) {
	} 

	// Sampling and Message Creation
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CreateMessageRequest(
		@JsonProperty("messages") List<SamplingMessage> messages,
		@JsonProperty("modelPreferences") ModelPreferences modelPreferences,
		@JsonProperty("systemPrompt") String systemPrompt,
		@JsonProperty("includeContext") ContextInclusionStrategy includeContext,
		@JsonProperty("temperature") Double temperature,
		@JsonProperty("maxTokens") int maxTokens,
		@JsonProperty("stopSequences") List<String> stopSequences, 			
		@JsonProperty("metadata") Map<String, Object> metadata) implements Request {

		public enum ContextInclusionStrategy {
			@JsonProperty("none") NONE,
			@JsonProperty("thisServer") THIS_SERVER,
			@JsonProperty("allServers") ALL_SERVERS
		}
		
		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private List<SamplingMessage> messages;
			private ModelPreferences modelPreferences;
			private String systemPrompt;
			private ContextInclusionStrategy includeContext;
			private Double temperature;
			private int maxTokens;
			private List<String> stopSequences;
			private Map<String, Object> metadata;

			public Builder messages(List<SamplingMessage> messages) {
				this.messages = messages;
				return this;
			}

			public Builder modelPreferences(ModelPreferences modelPreferences) {
				this.modelPreferences = modelPreferences;
				return this;
			}

			public Builder systemPrompt(String systemPrompt) {
				this.systemPrompt = systemPrompt;
				return this;
			}

			public Builder includeContext(ContextInclusionStrategy includeContext) {
				this.includeContext = includeContext;
				return this;
			}

			public Builder temperature(Double temperature) {
				this.temperature = temperature;
				return this;
			}

			public Builder maxTokens(int maxTokens) {
				this.maxTokens = maxTokens;
				return this;
			}

			public Builder stopSequences(List<String> stopSequences) {
				this.stopSequences = stopSequences;
				return this;
			}

			public Builder metadata(Map<String, Object> metadata) {
				this.metadata = metadata;
				return this;
			}

			public CreateMessageRequest build() {
				return new CreateMessageRequest(messages, modelPreferences, systemPrompt,
					includeContext, temperature, maxTokens, stopSequences, metadata);
			}
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CreateMessageResult(
		@JsonProperty("role") Role role,
		@JsonProperty("content") Content content,
		@JsonProperty("model") String model,
		@JsonProperty("stopReason") StopReason stopReason) {
		
		public enum StopReason {
			@JsonProperty("endTurn") END_TURN,
			@JsonProperty("stopSequence") STOP_SEQUENCE,
			@JsonProperty("maxTokens") MAX_TOKENS
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private Role role = Role.ASSISTANT;
			private Content content;
			private String model;
			private StopReason stopReason = StopReason.END_TURN;

			public Builder role(Role role) {
				this.role = role;
				return this;
			}

			public Builder content(Content content) {
				this.content = content;
				return this;
			}

			public Builder model(String model) {
				this.model = model;
				return this;
			}

			public Builder stopReason(StopReason stopReason) {
				this.stopReason = stopReason;
				return this;
			}

			public Builder message(String message) {
				this.content = new TextContent(message);
				return this;
			}

			public CreateMessageResult build() {
				return new CreateMessageResult(role, content, model, stopReason);
			}
		}
	}

	// ---------------------------
	// Pagination Interfaces
	// ---------------------------
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record PaginatedRequest(@JsonProperty("cursor") String cursor) {
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record PaginatedResult(@JsonProperty("nextCursor") String nextCursor) {
	}

	// ---------------------------
	// Progress and Logging
	// ---------------------------
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ProgressNotification(
		@JsonProperty("progressToken") String progressToken,
		@JsonProperty("progress") double progress,
		@JsonProperty("total") Double total) {
	}

	/**
	 * The Model Context Protocol (MCP) provides a standardized way for servers to send
	 * structured log messages to clients. Clients can control logging verbosity by
	 * setting minimum log levels, with servers sending notifications containing severity
	 * levels, optional logger names, and arbitrary JSON-serializable data.
	 *
	 * @param level The severity levels. The minimum log level is set by the client.
	 * @param logger The logger that generated the message.
	 * @param data JSON-serializable logging data.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record LoggingMessageNotification (
		@JsonProperty("level") LoggingLevel level,
		@JsonProperty("logger") String logger,
		@JsonProperty("data") String data) {

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private LoggingLevel level = LoggingLevel.INFO;
			private String logger = "server";
			private String data;

			public Builder level(LoggingLevel level) {
				this.level = level;
				return this;
			}

			public Builder logger(String logger) {
				this.logger = logger;
				return this;
			}

			public Builder data(String data) {
				this.data = data;
				return this;
			}

			public LoggingMessageNotification build() {
				return new LoggingMessageNotification(level, logger, data);
			}
		}
	}

	public enum LoggingLevel {
		@JsonProperty("debug") DEBUG(0),
		@JsonProperty("info") INFO(1),
		@JsonProperty("notice") NOTICE(2),
		@JsonProperty("warning") WARNING(3),
		@JsonProperty("error") ERROR(4),
		@JsonProperty("critical") CRITICAL(5),
		@JsonProperty("alert") ALERT(6),
		@JsonProperty("emergency") EMERGENCY(7);

		private final int level;

		LoggingLevel(int level) {
			this.level = level;
		}

		public int level() {
			return level;
		}

	} 

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record SetLevelRequest(@JsonProperty("level") LoggingLevel level) {
	}

	// ---------------------------
	// Autocomplete
	// ---------------------------
	public sealed interface CompleteReference permits PromptReference, ResourceReference {
		String type();

		String identifier();
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record PromptReference(
		@JsonProperty("type") String type,
		@JsonProperty("name") String name) implements CompleteReference {

		public PromptReference(String name) {
			this("ref/prompt", name);
		}

		@Override
		public String identifier() {
			return name();
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ResourceReference(
		@JsonProperty("type") String type,
		@JsonProperty("uri") String uri) implements CompleteReference {

		public ResourceReference(String uri) {
			this("ref/resource", uri);
		}

		@Override
		public String identifier() {
			return uri();
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CompleteRequest(
		@JsonProperty("ref") McpSchema.CompleteReference ref,
		@JsonProperty("argument") CompleteArgument argument) implements Request {

		public record CompleteArgument(
			@JsonProperty("name") String name,
			@JsonProperty("value") String value) {
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CompleteResult(@JsonProperty("values") CompleteCompletion completion) { 
			
		public record CompleteCompletion(
			@JsonProperty("values") List<String> values,
			@JsonProperty("total") Integer total,
			@JsonProperty("hasMore") Boolean hasMore) {
		}
	}

	// ---------------------------
	// Content Types
	// ---------------------------
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes({ @JsonSubTypes.Type(value = TextContent.class, name = "text"),
			@JsonSubTypes.Type(value = ImageContent.class, name = "image"),
			@JsonSubTypes.Type(value = EmbeddedResource.class, name = "resource") })
	public sealed interface Content permits TextContent, ImageContent, EmbeddedResource {
		default String type() {
			if (this instanceof TextContent) {
				return "text";
			}
			else if (this instanceof ImageContent) {
				return "image";
			}
			else if (this instanceof EmbeddedResource) {
				return "resource";
			}
			throw new IllegalArgumentException("Unknown content type: " + this);
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TextContent( 
		@JsonProperty("audience") List<Role> audience,
		@JsonProperty("priority") Double priority,
		@JsonProperty("text") String text) implements Content { 

		public TextContent(String content) {
			this(null, null, content);
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ImageContent( 
		@JsonProperty("audience") List<Role> audience,
		@JsonProperty("priority") Double priority,
		@JsonProperty("data") String data,
		@JsonProperty("mimeType") String mimeType) implements Content { 
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record EmbeddedResource( 
		@JsonProperty("audience") List<Role> audience,
		@JsonProperty("priority") Double priority,
		@JsonProperty("resource") ResourceContents resource) implements Content { 
	}

	// ---------------------------
	// Roots
	// ---------------------------
	/**
	 * Represents a root directory or file that the server can operate on.
	 *
	 * @param uri The URI identifying the root. This *must* start with file:// for now.
	 * This restriction may be relaxed in future versions of the protocol to allow other
	 * URI schemes.
	 * @param name An optional name for the root. This can be used to provide a
	 * human-readable identifier for the root, which may be useful for display purposes or
	 * for referencing the root in other parts of the application.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Root( 
		@JsonProperty("uri") String uri,
		@JsonProperty("name") String name) {
	} 

	/**
	 * The client's response to a roots/list request from the server. This result contains
	 * an array of Root objects, each representing a root directory or file that the
	 * server can operate on.
	 *
	 * @param roots An array of Root objects, each representing a root directory or file
	 * that the server can operate on.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ListRootsResult( 
		@JsonProperty("roots") List<Root> roots) {
	} 

}
