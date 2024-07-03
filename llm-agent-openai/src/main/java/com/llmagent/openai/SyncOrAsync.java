package com.llmagent.openai;

import java.util.function.Consumer;

public interface SyncOrAsync<Response> {

    Response execute();

    AsyncResponseHandling onResponse(Consumer<Response> responseHandler);
}
