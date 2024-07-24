package com.llmagent.dify;

import java.util.function.Consumer;

public interface SyncOrAsync<Response> {

    Response execute();

    AsyncResponseHandling onResponse(Consumer<Response> responseHandler);
}
