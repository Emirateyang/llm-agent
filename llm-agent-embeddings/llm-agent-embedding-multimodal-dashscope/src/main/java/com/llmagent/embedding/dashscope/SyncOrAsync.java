package com.llmagent.embedding.dashscope;

import java.util.function.Consumer;

public interface SyncOrAsync<Response> {

    Response execute();

    AsyncResponseHandling onResponse(Consumer<Response> responseHandler);
}
