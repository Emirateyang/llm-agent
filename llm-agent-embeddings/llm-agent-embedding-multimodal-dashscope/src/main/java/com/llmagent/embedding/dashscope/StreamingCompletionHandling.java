package com.llmagent.embedding.dashscope;

import java.util.function.Consumer;

public interface StreamingCompletionHandling {
    ErrorHandling onError(Consumer<Throwable> errorHandler);

    ErrorHandling ignoreErrors();
}
