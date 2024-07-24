package com.llmagent.dify;

import java.util.function.Consumer;

public interface AsyncResponseHandling {
    ErrorHandling onError(Consumer<Throwable> errorHandler);

    ErrorHandling ignoreErrors();
}
