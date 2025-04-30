package com.llmagent.mcp.client.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmagent.mcp.client.McpClientTransport;
import com.llmagent.mcp.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implementation of the MCP Stdio transport that communicates with a server process using
 * standard input/output streams. Messages are exchanged as newline-delimited JSON-RPC
 * messages over stdin/stdout, with errors and debug information sent to stderr.
 *
 * <br>
 * The client typically initiates the communication by launching the MCP server as a separate process.
 * This server process is often started by the client using a command.
 */
public class StdioClientTransport implements McpClientTransport {

	private static final Logger logger = LoggerFactory.getLogger(StdioClientTransport.class);

	/** The server process being communicated with */
	private Process process;
	private volatile boolean isClosing = false;

	private final String command;
	private final List<String> args;
	private final Map<String, String> environment;
	private final boolean logEvents;
	private final ObjectMapper objectMapper;

	/* intended to handle incoming JSON-RPC messages from the server process */
	private final Sinks.Many<McpSchema.JSONRPCMessage> inboundSink;
	/* intended to handle outgoing JSON-RPC messages to the server process */
	private final Sinks.Many<McpSchema.JSONRPCMessage> outboundSink;
	/* intended to handle error messages from the server process */
	private final Sinks.Many<String> errorSink;

	/** Scheduler for handling inbound messages from the server process */
	private Scheduler inboundScheduler;
	/** Scheduler for handling outbound messages to the server process */
	private Scheduler outboundScheduler;
	/** Scheduler for handling error messages from the server process */
	private Scheduler errorScheduler;

	private Consumer<String> stdErrorHandler = error -> logger.info("STDERR Message received: {}", error);

	/**
	 * Creates a new StdioClientTransport instance with the specified command and arguments.
	 */
	public StdioClientTransport(Builder builder) {
		this.command = builder.command;
		this.args = builder.args;
		this.environment = builder.environment;
		this.logEvents = builder.logEvents;

		this.objectMapper = new ObjectMapper();
		/* only one subscriber */
		this.inboundSink = Sinks.many().unicast().onBackpressureBuffer();
		this.outboundSink = Sinks.many().unicast().onBackpressureBuffer();
		this.errorSink = Sinks.many().unicast().onBackpressureBuffer();

		// Start threads
		this.inboundScheduler = Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(), "inbound");
		this.outboundScheduler = Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(), "outbound");
		this.errorScheduler = Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(), "error");
	}

	/**
	 * sends a message to the peer asynchronously.
	 * Messages are sent in JSON-RPC format as specified by the MCP protocol.
	 * @param message the {@link McpSchema.JSONRPCMessage} to be sent to the server
	 */
	@Override
	public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
		if (logEvents) {
			logger.debug("MSG_SEND> {}", message);
		}
		if (this.outboundSink.tryEmitNext(message).isSuccess()) {
			return Mono.empty();
		} else {
			return Mono.error(new RuntimeException("Failed to enqueue message"));
		}
	}

	/**
	 * Gracefully closes the transport by destroying the process and disposing of the schedulers.
	 * This method sends a TERM signal to the process and waits for it to exit before cleaning up resources.
	 */
	@Override
	public Mono<Void> closeGracefully() {
		return Mono.fromRunnable(() -> {
			isClosing = true;
			logger.debug("Initiating graceful shutdown");
		}).then(Mono.defer(() -> {
			// First complete all sinks to stop accepting new messages
			inboundSink.tryEmitComplete();
			outboundSink.tryEmitComplete();
			errorSink.tryEmitComplete();

			// Give a short time for any pending messages to be processed
			return Mono.delay(Duration.ofMillis(100));
		})).then(Mono.defer(() -> {
			logger.debug("Sending TERM to process");
			if (this.process != null) {
				this.process.destroy();
				return Mono.fromFuture(process.onExit());
			} else {
				logger.warn("Process not started");
				return Mono.empty();
			}
		})).doOnNext(process -> {
			if (process.exitValue() != 0) {
                logger.warn("Process terminated with code {}", process.exitValue());
			}
		}).then(Mono.fromRunnable(() -> {
			try {
				// The Threads are blocked on readLine so disposeGracefully would not
				// interrupt them, therefore we issue an async hard dispose.
				inboundScheduler.dispose();
				errorScheduler.dispose();
				outboundScheduler.dispose();

				logger.debug("Graceful shutdown completed");
			} catch (Exception e) {
				logger.error("Error during graceful shutdown", e);
			}
		})).then().subscribeOn(Schedulers.boundedElastic());
	}

	/**
	 * Checks the health of the transport by verifying that the process is still alive.
	 */
	@Override
	public void checkHealth() {
		if (!process.isAlive()) {
			throw new IllegalStateException("Process is not alive");
		}
	}

	/**
	 * Unmarshal the given data into an object of the specified type.
	 * @param data the data to unmarshal
	 * @param typeReference the type reference for the object to unmarshal
	 */
	@Override
	public <T> T unmarshal(Object data, TypeReference<T> typeReference) {
		return this.objectMapper.convertValue(data, typeReference);
	}

	/**
	 * Starts the server process and initializes the message processing streams.
	 * This method sets up the process with the configured command, arguments, and environment,
	 * then starts the inbound, outbound, and error processing threads.
	 */
	@Override
	public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
		return Mono.<Void>fromRunnable(() -> {
			handleIncomingMessages(handler);
			handleIncomingErrors();

			// Prepare command and environment
			List<String> fullCommand = new ArrayList<>();
			fullCommand.add(this.command);
			fullCommand.addAll(this.args);

			ProcessBuilder processBuilder = this.getProcessBuilder();
			processBuilder.command(fullCommand);
			processBuilder.environment().putAll(this.environment);

			// Start the process
			try {
				this.process = processBuilder.start();
			} catch (IOException e) {
				throw new RuntimeException("Failed to start process with command: " + fullCommand, e);
			}

			// Validate process streams
			if (this.process.getInputStream() == null || process.getOutputStream() == null) {
				this.process.destroy();
				throw new RuntimeException("Process input or output stream is null");
			}

			// Start threads
			startInboundProcessing();
			startOutboundProcessing();
			startErrorProcessing();
		}).subscribeOn(Schedulers.boundedElastic());
	}

	/**
	 * Creates and returns a new ProcessBuilder instance. Protected to allow overriding in tests.
	 * @return A new ProcessBuilder instance
	 */
	protected ProcessBuilder getProcessBuilder() {
		return new ProcessBuilder();
	}

	protected void handleOutbound(Function<Flux<McpSchema.JSONRPCMessage>, Flux<McpSchema.JSONRPCMessage>> outboundConsumer) {
		outboundConsumer.apply(outboundSink.asFlux()).doOnComplete(() -> {
			isClosing = true;
			outboundSink.tryEmitComplete();
		}).doOnError(e -> {
			if (!isClosing) {
				logger.error("Error in outbound processing", e);
				isClosing = true;
				outboundSink.tryEmitComplete();
			}
		}).subscribe();
	}

	private void handleIncomingMessages(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> inboundMessageHandler) {
		this.inboundSink.asFlux()
				.flatMap(message -> Mono.just(message)
						.transform(inboundMessageHandler)
						.contextWrite(ctx -> ctx.put("observation", "myObservation")))
				.subscribe();
	}

	private void handleIncomingErrors() {
		this.errorSink.asFlux().subscribe(e -> {
			this.stdErrorHandler.accept(e);
		});
	}

	/**
	 * Starts the inbound processing thread that reads JSON-RPC messages from the process's input stream.
	 * Messages are deserialized and emitted to the inbound sink.
	 */
	private void startInboundProcessing() {
		this.inboundScheduler.schedule(() -> {
			try (BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while (!isClosing && (line = processReader.readLine()) != null) {
					try {
						if (logEvents) {
							logger.debug("MSG_RECEIVED< {}", line);
						}
						McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.objectMapper, line);
						if (!this.inboundSink.tryEmitNext(message).isSuccess()) {
							if (!isClosing) {
								logger.error("Failed to enqueue inbound message: {}", message);
							}
							break;
						}
					} catch (Exception e) {
						if (!isClosing) {
                            logger.warn("Error processing inbound message for line: {}", line);
						}
						// cannot break here, as stopping may result in the server returning a message
						// that is not JSON-RPC, causing the server to hang
//						break;
					}
				}
			} catch (IOException e) {
				if (!isClosing) {
					logger.error("Error reading from input stream", e);
				}
			} finally {
				isClosing = true;
				inboundSink.tryEmitComplete();
			}
		});
	}

	/**
	 * Starts the outbound processing thread that writes JSON-RPC messages to the process's output stream.
	 * Messages are serialized to JSON and written with a newline
	 * delimiter.
	 */
	private void startOutboundProcessing() {
		this.handleOutbound(messages -> messages
			// this bit is important since writes come from user threads, and we
			// want to ensure that the actual writing happens on a dedicated thread
			.publishOn(outboundScheduler)
			.publishOn(Schedulers.boundedElastic())
			.handle((message, s) -> {
				if (message != null && !isClosing) {
					try {
						String jsonMessage = objectMapper.writeValueAsString(message);
						// Escape any embedded newlines in the JSON message as per spec:
						// https://spec.modelcontextprotocol.io/specification/basic/transports/#stdio
						// - Messages are delimited by newlines, and MUST NOT contain embedded newlines.
						jsonMessage = jsonMessage.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");

						var os = this.process.getOutputStream();
						synchronized (os) {
							os.write(jsonMessage.getBytes(StandardCharsets.UTF_8));
							os.write("\n".getBytes(StandardCharsets.UTF_8));
							os.flush();
						}
						s.next(message);
					} catch (IOException e) {
						s.error(new RuntimeException(e));
					}
				}
			}));
	}

	/**
	 * Starts the error processing thread that reads from the process's error stream.
	 * Error messages are logged and emitted to the error sink.
	 */
	private void startErrorProcessing() {
		this.errorScheduler.schedule(() -> {
			try (BufferedReader processErrorReader = new BufferedReader(
					new InputStreamReader(process.getErrorStream()))) {
				String line;
				while (!isClosing && (line = processErrorReader.readLine()) != null) {
					try {
						if (!this.errorSink.tryEmitNext(line).isSuccess()) {
							if (!isClosing) {
								logger.error("Failed to emit error message");
							}
							break;
						}
					} catch (Exception e) {
						if (!isClosing) {
							logger.error("Error processing error message", e);
						}
						break;
					}
				}
			} catch (IOException e) {
				if (!isClosing) {
					logger.error("Error reading from error stream", e);
				}
			} finally {
				isClosing = true;
				errorSink.tryEmitComplete();
			}
		});
	}

	public static class Builder {

		private String command;
		private List<String> args;
		private Map<String, String> environment;
		private boolean logEvents;

		public Builder command(String command) {
			this.command = command;
			return this;
		}

		public Builder args(List<String> args) {
			this.args = args;
			return this;
		}

		public Builder environment(Map<String, String> environment) {
			this.environment = environment;
			return this;
		}

		public Builder logEvents(boolean logEvents) {
			this.logEvents = logEvents;
			return this;
		}

		public StdioClientTransport build() {
			if (command == null || command.isEmpty()) {
				throw new IllegalArgumentException("Missing command");
			}
			if (args == null || args.isEmpty()) {
				throw new IllegalArgumentException("Missing args");
			}
			if (environment == null) {
				environment = Map.of();
			}
			return new StdioClientTransport(this);
		}
	}
}
