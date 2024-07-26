package com.llmagent.dify;

import com.llmagent.dify.client.ResponseLoggingInterceptor;
import com.llmagent.dify.exception.ExceptionUtil;
import com.llmagent.dify.json.Json;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class StreamingRequestExecutor<Req, Resp, RespContent> {

    private static final Logger log = LoggerFactory.getLogger(StreamingRequestExecutor.class);

    private final OkHttpClient okHttpClient;
    private final String endpointUrl;
    private final Supplier<Req> requestWithStreamSupplier;
    private final Class<Resp> responseClass;
    private final Function<Resp, RespContent> streamEventContentExtractor;
    private final boolean logStreamingResponses;
    private final ResponseLoggingInterceptor responseLogger = new ResponseLoggingInterceptor();

    StreamingRequestExecutor(OkHttpClient okHttpClient, String endpointUrl,
                             Supplier<Req> requestWithStreamSupplier, Class<Resp> responseClass,
                             Function<Resp, RespContent> streamEventContentExtractor, boolean logStreamingResponses) {
        this.okHttpClient = okHttpClient;
        this.endpointUrl = endpointUrl;
        this.requestWithStreamSupplier = requestWithStreamSupplier;
        this.responseClass = responseClass;
        this.streamEventContentExtractor = streamEventContentExtractor;
        this.logStreamingResponses = logStreamingResponses;
    }

    StreamingResponseHandling onPartialResponse(Consumer<RespContent> partialResponseHandler) {

        return new StreamingResponseHandling() {

            @Override
            public StreamingCompletionHandling onComplete(Runnable streamingCompletionCallback) {
                return new StreamingCompletionHandling() {

                    @Override
                    public ErrorHandling onError(Consumer<Throwable> errorHandler) {
                        return new ErrorHandling() {

                            @Override
                            public ResponseHandle execute() {
                                return stream(
                                        partialResponseHandler,
                                        streamingCompletionCallback,
                                        errorHandler
                                );
                            }
                        };
                    }

                    @Override
                    public ErrorHandling ignoreErrors() {
                        return new ErrorHandling() {

                            @Override
                            public ResponseHandle execute() {
                                return stream(
                                        partialResponseHandler,
                                        streamingCompletionCallback,
                                        (e) -> {
                                            // intentionally ignoring because user called ignoreErrors()
                                        }
                                );
                            }
                        };
                    }
                };
            }

            @Override
            public ErrorHandling onError(Consumer<Throwable> errorHandler) {
                return new ErrorHandling() {

                    @Override
                    public ResponseHandle execute() {
                        return stream(
                                partialResponseHandler,
                                () -> {
                                    // intentionally ignoring because user did not provide callback
                                },
                                errorHandler
                        );
                    }
                };
            }

            @Override
            public ErrorHandling ignoreErrors() {
                return new ErrorHandling() {

                    @Override
                    public ResponseHandle execute() {
                        return stream(
                                partialResponseHandler,
                                () -> {
                                    // intentionally ignoring because user did not provide callback
                                },
                                (e) -> {
                                    // intentionally ignoring because user called ignoreErrors()
                                }
                        );
                    }
                };
            }
        };
    }

    private ResponseHandle stream(
            Consumer<RespContent> partialResponseHandler,
            Runnable streamingCompletionCallback,
            Consumer<Throwable> errorHandler) {

        Req request = requestWithStreamSupplier.get();

        String requestJson = Json.toJson(request);

        okhttp3.Request okHttpRequest = new okhttp3.Request.Builder()
                .url(endpointUrl)
                .post(RequestBody.create(requestJson, MediaType.get("application/json; charset=utf-8")))
                .build();

        ResponseHandle responseHandle = new ResponseHandle();

        EventSourceListener eventSourceListener = new EventSourceListener() {

            @Override
            public void onOpen(EventSource eventSource, okhttp3.Response response) {
                if (responseHandle.cancelled) {
                    eventSource.cancel();
                    return;
                }

                if (logStreamingResponses) {
                    responseLogger.log(response);
                }
            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                if (responseHandle.cancelled) {
                    eventSource.cancel();
                    return;
                }

                if (logStreamingResponses) {
                    log.debug("onEvent() {}", data);
                }

                if ("[DONE]".equals(data)) {
                    streamingCompletionCallback.run();
                    return;
                }

                try {
                    Resp response = Json.fromJson(data, responseClass);
                    RespContent responseContent = streamEventContentExtractor.apply(response);
                    if (responseContent != null) {
                        partialResponseHandler.accept(responseContent); // do not handle exception, fail-fast
                    }
                } catch (Exception e) {
                    errorHandler.accept(e);
                }

                // dify自己处理了[Done]，需要检查message_end
                if (data.contains("\"event\": \"message_end\"")) {
                    streamingCompletionCallback.run();
                }
            }

            @Override
            public void onClosed(EventSource eventSource) {
                if (responseHandle.cancelled) {
                    eventSource.cancel();
                    return;
                }

                if (logStreamingResponses) {
                    log.debug("onClosed()");
                }
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, okhttp3.Response response) {
                if (responseHandle.cancelled) {
                    return;
                }

                // TODO remove this when migrating from okhttp
                if (t instanceof IllegalArgumentException && "byteCount < 0: -1".equals(t.getMessage())) {
                    streamingCompletionCallback.run();
                    return;
                }

                if (logStreamingResponses) {
                    log.debug("onFailure()", t);
                    responseLogger.log(response);
                }

                if (t != null) {
                    errorHandler.accept(t); // TODO also include information from response?
                } else {
                    try {
                        errorHandler.accept(ExceptionUtil.toException(response));
                    } catch (IOException e) {
                        errorHandler.accept(e); // TODO right thing to do?
                    }
                }
            }
        };

        EventSources.createFactory(okHttpClient)
                .newEventSource(okHttpRequest, eventSourceListener);

        return responseHandle;
    }
}
