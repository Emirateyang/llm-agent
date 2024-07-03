package com.llmagent.openai.exception;

import java.io.IOException;

public class ExceptionUtil {

    public static RuntimeException toException(retrofit2.Response<?> response) throws IOException {
        return new OpenAiHttpException(response.code(), response.errorBody().string());
    }

    public static RuntimeException toException(okhttp3.Response response) throws IOException {
        return new OpenAiHttpException(response.code(), response.body().string());
    }

    public static <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
