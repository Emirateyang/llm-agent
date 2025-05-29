package com.llmagent.embedding.http.header;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GenericHeaderInjector implements Interceptor {
    private final Map<String, String> headers = new HashMap<>();

    GenericHeaderInjector(Map<String, String> headers) {
        Optional.ofNullable(headers) .ifPresent(this.headers::putAll);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();

        // Add headers
        this.headers.forEach(builder::addHeader);
        return chain.proceed(builder.build());
    }
}
