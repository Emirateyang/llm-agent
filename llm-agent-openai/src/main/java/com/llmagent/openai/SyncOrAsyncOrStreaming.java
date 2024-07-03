package com.llmagent.openai;

import java.util.function.Consumer;

public interface SyncOrAsyncOrStreaming<Response> extends SyncOrAsync<Response> {

    StreamingResponseHandling onPartialResponse(Consumer<Response> partialResponseHandler);
}
