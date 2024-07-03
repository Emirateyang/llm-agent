package com.llmagent.openai;

import java.util.function.Consumer;

public interface StreamingCompletionHandling {
    ErrorHandling onError(Consumer<Throwable> errorHandler);

    ErrorHandling ignoreErrors();
}
