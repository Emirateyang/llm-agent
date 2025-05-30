package com.llmagent.mcp.client.transport;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.io.IOException;

/**
 * A Server-Sent Events (SSE) client implementation using OkHttp.
 * This client establishes a connection to an SSE endpoint and
 * processes the incoming event stream, delivering parsed SSE messages.
 *
 * <p>
 * The client supports standard SSE event fields including:
 * <ul>
 * <li>event - The event type (defaults to "message" if not specified)</li>
 * <li>id - The event ID</li>
 * <li>data - The event payload data</li>
 * </ul>
 *
 * <p>
 * Events are delivered to a provided {@link SseEventHandler} which can process events and
 * handle any errors that occur during the connection.
 *
 */
public class FlowSseClient {

	private final OkHttpClient okHttpClient;
	private final Request.Builder globalRequestBuilder;

	/**
	 * Record class representing a Server-Sent Event with its standard fields.
	 *
	 * @param id   the event ID (maybe null)
	 * @param type the event type (defaults to "message" if not specified in the stream)
	 * @param data the event payload data
	 */
	public static record SseEvent(String id, String type, String data) {
	}

	/**
	 * Interface for handling SSE events and errors. Implementations can process received
	 * events and handle any errors that occur during the SSE connection.
	 */
	public interface SseEventHandler {

		/**
		 * Called when an SSE event is received.
		 *
		 * @param event the received SSE event containing id, type, and data
		 */
		void onEvent(SseEvent event);

		/**
		 * Called when an error occurs during the SSE connection.
		 *
		 * @param error the error that occurred
		 */
		void onError(Throwable error);

	}

	/**
	 * Creates a new FlowSseClient with the specified OkHttpClient.
	 *
	 * @param okHttpClient the {@link OkHttpClient} instance to use for SSE connections
	 */
	public FlowSseClient(OkHttpClient okHttpClient) {
		this(okHttpClient, new Request.Builder()); // Initialize with a default builder
	}

	/**
	 * Creates a new FlowSseClient with the specified OkHttpClient and a base Request.Builder.
	 * This builder can be pre-configured with common headers or settings.
	 *
	 * @param okHttpClient       the {@link OkHttpClient} instance to use for SSE connections
	 * @param baseRequestBuilder a {@link Request.Builder} to use as a base for SSE requests.
	 * It can be pre-configured with headers, tags, etc.
	 */
	public FlowSseClient(OkHttpClient okHttpClient, Request.Builder baseRequestBuilder) {
		this.okHttpClient = okHttpClient;
		this.globalRequestBuilder = baseRequestBuilder; // Store the base builder
	}

	/**
	 * Subscribes to an SSE endpoint and processes the event stream.
	 *
	 * <p>
	 * This method establishes a connection to the specified URL and begins processing the
	 * SSE stream. Events are parsed by OkHttp's SSE module and delivered to the provided
	 * event handler. The connection remains active until either an error occurs, the server
	 * closes the connection, or the returned {@link EventSource} is explicitly cancelled.
	 *
	 * @param url          the SSE endpoint URL to connect to
	 * @param eventHandler the handler that will receive SSE events and error notifications
	 * @return an {@link EventSource} instance which can be used to cancel the SSE connection.
	 * @throws IllegalArgumentException if the URL is malformed.
	 */
	public EventSource subscribe(String url, SseEventHandler eventHandler) {
		// Create a new request builder for this specific subscription,
		// potentially based on the global one.
		Request request = new Request.Builder()
				.url(url)
				.header("Accept", "text/event-stream")
				.header("Cache-Control", "no-cache")
				.get()
				.build();

		EventSource.Factory factory = EventSources.createFactory(this.okHttpClient);

		EventSourceListener listener = new EventSourceListener() {
			@Override
			public void onOpen(EventSource eventSource, Response response) {
				// Connection opened, check status code
				int status = response.code();
				if (status != 200 && status != 201 && status != 202 && status != 206) {
					// Mimic original behavior of throwing an error that gets caught
					// by exceptionally block, which then calls eventHandler.onError
					eventHandler.onError(new IOException("Failed to connect to SSE stream. Unexpected status code: " + status + " Message: " + response.message()));
					eventSource.cancel(); // Important to cancel if status is not OK for SSE
				}
				// If status is OK, events will start flowing to onEvent.
			}

			@Override
			public void onEvent(EventSource eventSource, String id, String type, String data) {
				// OkHttp-SSE already handles parsing of id, event type, and data.
				// It also handles multi-line data fields correctly.
				String eventType = (type != null) ? type : "message"; // Default to "message" if not specified
				SseEvent event = new SseEvent(id, eventType, data);
				eventHandler.onEvent(event);
			}

			@Override
			public void onClosed(EventSource eventSource) {
				// Connection was closed normally.
				// If you need to signal completion to the eventHandler, you could add an onComplete() method to it.
				// The original Flow.Subscriber had onComplete, but SseEventHandler does not.
			}

			@Override
			public void onFailure(EventSource eventSource, Throwable t, Response response) {
				// Handle connection failures or errors during the stream.
				Throwable errorToReport = t;
				if (errorToReport == null && response != null) {
					errorToReport = new IOException("SSE stream error. Status: " + response.code() + " Message: " + response.message());
				} else if (errorToReport == null) {
					errorToReport = new IOException("Unknown SSE stream failure.");
				}
				eventHandler.onError(errorToReport);
			}
		};

		// Start listening for events. This is an asynchronous operation.
		return factory.newEventSource(request, listener);
	}
}