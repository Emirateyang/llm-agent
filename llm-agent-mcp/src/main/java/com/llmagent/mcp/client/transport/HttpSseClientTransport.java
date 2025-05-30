package com.llmagent.mcp.client.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmagent.mcp.client.McpClientTransport;
import com.llmagent.mcp.spec.McpException;
import com.llmagent.mcp.spec.McpSchema;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.ValidationUtil.ensureNotNull;

/**
 * Server-Sent Events (SSE) implementation of the {@link McpClientTransport}
 * that follows the MCP HTTP with SSE transport specification, using OkHttp HttpClient.
 *
 * <p>
 * This transport implementation establishes a bidirectional communication channel between
 * client and server using SSE for server-to-client messages and HTTP POST requests for
 * client-to-server messages. The transport:
 * <ul>
 * <li>Establishes an SSE connection to receive server messages</li>
 * <li>Handles endpoint discovery through SSE events</li>
 * <li>Manages message serialization/deserialization using Jackson</li>
 * <li>Provides graceful connection termination</li>
 * </ul>
 *
 * <p>
 * The transport supports two types of SSE events:
 * <ul>
 * <li>'endpoint' - Contains the URL for sending client messages</li>
 * <li>'message' - Contains JSON-RPC message payload</li>
 * </ul>
 *
 */
public class HttpSseClientTransport implements McpClientTransport {

	private static final Logger logger = LoggerFactory.getLogger(HttpSseClientTransport.class);

	/** SSE event type for JSON-RPC messages */
	private static final String MESSAGE_EVENT_TYPE = "message";

	/** SSE event type for endpoint discovery */
	private static final String ENDPOINT_EVENT_TYPE = "endpoint";

	/** Default SSE endpoint path */
	private static final String DEFAULT_SSE_ENDPOINT = "/sse";

	/** Base URI for the MCP server */
	private final String baseUri;

	/** SSE endpoint path */
	private final String sseEndpoint;

	/** SSE client for handling server-sent events. Uses the /sse endpoint */
	private final FlowSseClient sseClient;

	/**
	 * HTTP client for sending messages to the server. Uses HTTP POST over the message
	 * endpoint
	 */
	private final OkHttpClient httpClient;

	/** JSON object mapper for message serialization/deserialization */
	protected ObjectMapper objectMapper;

	/** Flag indicating if the transport is in closing state */
	private volatile boolean isClosing = false;

	/** Latch for coordinating endpoint discovery */
	private final CountDownLatch closeLatch = new CountDownLatch(1);

	/** Holds the discovered message endpoint URL */
	private final AtomicReference<String> messageEndpoint = new AtomicReference<>();

	/** Holds the SSE connection future */
	private final AtomicReference<CompletableFuture<Void>> connectionFuture = new AtomicReference<>();

	private final boolean logRequests;
	private final boolean logResponses;

	/**
	 * Creates a new transport instance with custom object mapper, and headers.
	 * @param baseUri the base URI of the MCP server
	 * @param sseEndpoint the SSE endpoint path
	 * @param objectMapper the object mapper for JSON serialization/deserialization
	 * @throws IllegalArgumentException if objectMapper, clientBuilder, or headers is null
	 */
	public HttpSseClientTransport(String baseUri, String sseEndpoint, ObjectMapper objectMapper) {
		this(new OkHttpClient.Builder().callTimeout(Duration.ofSeconds(10))
				.connectTimeout(Duration.ofSeconds(20))
				.readTimeout(Duration.ofSeconds(60))
				.writeTimeout(Duration.ofSeconds(60)).build(), baseUri, sseEndpoint, objectMapper, false, false);
	}

	/**
	 * Creates a new transport instance with custom HTTP client builder, object mapper,
	 * and headers.
	 * @param httpClient the HTTP client to use
	 * @param baseUri the base URI of the MCP server
	 * @param sseEndpoint the SSE endpoint path
	 * @param objectMapper the object mapper for JSON serialization/deserialization
	 * @throws IllegalArgumentException if objectMapper, clientBuilder, or headers is null
	 */
	HttpSseClientTransport(OkHttpClient httpClient, String baseUri, String sseEndpoint,
						   ObjectMapper objectMapper, boolean logRequests, boolean logResponses) {

		this.baseUri = ensureNotNull(baseUri, "baseUri must not be null");
		this.sseEndpoint = sseEndpoint;
		this.objectMapper = getOrDefault(objectMapper, new ObjectMapper());
		this.httpClient = httpClient;

		this.logRequests = logRequests;
		this.logResponses = logResponses;

		this.sseClient = new FlowSseClient(this.httpClient);
	}

	/**
	 * Creates a new builder for {@link HttpSseClientTransport}.
	 * @param baseUri the base URI of the MCP server
	 * @return a new builder instance
	 */
	public static Builder builder(String baseUri) {
		return new Builder().baseUri(baseUri);
	}

	/**
	 * Builder for {@link HttpSseClientTransport}.
	 */
	public static class Builder {

		private String baseUri;

		private String sseEndpoint = DEFAULT_SSE_ENDPOINT;

		private boolean logRequests = false;
		private boolean logResponses = false;

		private OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
				.callTimeout(Duration.ofSeconds(10))
				.connectTimeout(Duration.ofSeconds(20))
				.readTimeout(Duration.ofSeconds(60))
				.writeTimeout(Duration.ofSeconds(60));

		private ObjectMapper objectMapper = new ObjectMapper();

		/**
		 * Creates a new builder instance.
		 */
		public Builder() {
			// Default constructor
		}

		/**
		 * Sets the base URI.
		 * @param baseUri the base URI
		 * @return this builder
		 */
		public Builder baseUri(String baseUri) {
			this.baseUri = baseUri;
			return this;
		}

		/**
		 * Sets the SSE endpoint path.
		 * @param sseEndpoint the SSE endpoint path
		 * @return this builder
		 */
		public Builder sseEndpoint(String sseEndpoint) {
			this.sseEndpoint = sseEndpoint;
			return this;
		}

		/**
		 * Sets the HTTP client builder.
		 * @param clientBuilder the HTTP client builder
		 * @return this builder
		 */
		public Builder clientBuilder(OkHttpClient.Builder clientBuilder) {
			this.clientBuilder = clientBuilder;
			return this;
		}

		/**
		 * Sets the object mapper for JSON serialization/deserialization.
		 * @param objectMapper the object mapper
		 * @return this builder
		 */
		public Builder objectMapper(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
			return this;
		}

		public Builder logRequests(boolean logRequests) {
			this.logRequests = logRequests;
			return this;
		}

		public Builder logResponses(boolean logResponses) {
			this.logResponses = logResponses;
			return this;
		}

		/**
		 * Builds a new {@link HttpSseClientTransport} instance.
		 * @return a new transport instance
		 */
		public HttpSseClientTransport build() {
			return new HttpSseClientTransport(clientBuilder.build(), baseUri, sseEndpoint, objectMapper,
					this.logRequests, logResponses);
		}
	}

	/**
	 * Establishes the SSE connection with the server and sets up message handling.
	 *
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Initiates the SSE connection</li>
	 * <li>Handles endpoint discovery events</li>
	 * <li>Processes incoming JSON-RPC messages</li>
	 * </ul>
	 * @param handler the function to process received JSON-RPC messages
	 * @return a Mono that completes when the connection is established
	 */
	@Override
	public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		connectionFuture.set(future);

		sseClient.subscribe(this.baseUri + this.sseEndpoint, new FlowSseClient.SseEventHandler() {
			@Override
			public void onEvent(FlowSseClient.SseEvent event) {
				if (isClosing) {
					return;
				}
				if (logResponses) {
					logger.debug("sse onEvent: {}", event.type());
				}
				try {
					if (ENDPOINT_EVENT_TYPE.equals(event.type())) {
						String endpoint = event.data();
						messageEndpoint.set(endpoint);
						closeLatch.countDown();
						future.complete(null);
					}
					else if (MESSAGE_EVENT_TYPE.equals(event.type())) {
						McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, event.data());
						handler.apply(Mono.just(message)).subscribe();
					}
					else {
						logger.error("Received unrecognized SSE event type: {}", event.type());
					}
				} catch (IOException e) {
					logger.error("Error processing SSE event", e);
					future.completeExceptionally(e);
				}
			}

			@Override
			public void onError(Throwable error) {
				if (!isClosing) {
					logger.error("SSE connection error", error);
					future.completeExceptionally(error);
				}
			}
		});

		return Mono.fromFuture(future);
	}


	/**
	 * Sends a JSON-RPC message to the server.
	 *
	 * <p>
	 * This method waits for the message endpoint to be discovered before sending the
	 * message. The message is serialized to JSON and sent as an HTTP POST request.
	 * @param message the JSON-RPC message to send
	 * @return a Mono that completes when the message is sent
	 * @throws McpException if the message endpoint is not available or the wait times out
	 */
	@Override
	public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
		if (isClosing) {
			return Mono.empty();
		}

		try {
			if (!closeLatch.await(10, TimeUnit.SECONDS)) {
				return Mono.error(new McpException("Failed to wait for the message endpoint"));
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // 重置中断状态
			return Mono.error(new McpException("Failed to wait for the message endpoint"));
		}

		String endpoint = messageEndpoint.get();
		if (endpoint == null) {
			return Mono.error(new McpException("No message endpoint available"));
		}

		try {
			String jsonText = this.objectMapper.writeValueAsString(message);

			RequestBody body = RequestBody.create(jsonText, MediaType.get("application/json; charset=utf-f8"));
			Request request = new Request.Builder()
					.url(this.baseUri + endpoint)
					.post(body)
					.build();

			if (logRequests) {
				logger.debug("sendMessage request: {}", jsonText);
			}

			return Mono.create(sink -> {
				this.httpClient.newCall(request).enqueue(new Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						if (!isClosing) {
							sink.error(new RuntimeException("Failed to send message", e));
						} else {
							sink.success();
						}
					}

					@Override
					public void onResponse(Call call, Response response) throws IOException {
						try (Response managedResponse = response) {
							if (managedResponse.isSuccessful()) {
								int statusCode = managedResponse.code();
								if (statusCode == 200 || statusCode == 201 || statusCode == 202 || statusCode == 206) {
									sink.success(); // Mono<Void> 的成功信号
								} else {
									logger.error("Error sending message, unexpected successful status: {}", statusCode);
									sink.success();
								}
							} else {
								logger.error("Error sending message: {} - {}", managedResponse.code(), managedResponse.message());
								sink.success();
							}
						}
					}
				});
			});

		} catch (JsonProcessingException e) {
			if (!isClosing) {
				return Mono.error(new RuntimeException("Failed to serialize message", e));
			}
			return Mono.empty();
		}
	}

	@Override
	public void checkHealth() {
		// no specific checks right now
	}

	/**
	 * Gracefully closes the transport connection.
	 *
	 * <p>
	 * Sets the closing flag and cancels any pending connection future. This prevents new
	 * messages from being sent and allows ongoing operations to complete.
	 * @return a Mono that completes when the closing process is initiated
	 */
	@Override
	public Mono<Void> closeGracefully() {
		return Mono.fromRunnable(() -> {
			isClosing = true;
			CompletableFuture<Void> future = connectionFuture.get();
			if (future != null && !future.isDone()) {
				future.cancel(true);
			}
		});
	}

	/**
	 * Unmarshal data to the specified type using the configured object mapper.
	 * @param data the data to unmarshal
	 * @param typeRef the type reference for the target type
	 * @param <T> the target type
	 * @return the unmarshalled object
	 */
	@Override
	public <T> T unmarshal(Object data, TypeReference<T> typeRef) {
		return this.objectMapper.convertValue(data, typeRef);
	}

}
