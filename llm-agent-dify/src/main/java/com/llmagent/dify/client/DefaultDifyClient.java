package com.llmagent.dify.client;

import com.llmagent.dify.RequestExecutor;
import com.llmagent.dify.SyncOrAsync;
import com.llmagent.dify.SyncOrAsyncOrStreaming;
import com.llmagent.dify.api.DifyApi;
import com.llmagent.dify.chat.DifyChatCompletionResponse;
import com.llmagent.dify.chat.DifyMessageRequest;
import com.llmagent.dify.chat.DifyStreamingChatCompletionResponse;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

import static com.llmagent.dify.json.Json.GSON;

public class DefaultDifyClient extends DifyClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultDifyClient.class);
    private final String baseUrl;
    private final OkHttpClient okHttpClient;
    private final DifyApi difyApi;
    private final boolean logStreamingResponses;

    private final boolean breakOnToolCalled;

    private DefaultDifyClient(Builder serviceBuilder) {
        this.baseUrl = serviceBuilder.baseUrl;

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .callTimeout(serviceBuilder.callTimeout)
                .connectTimeout(serviceBuilder.connectTimeout)
                .readTimeout(serviceBuilder.readTimeout)
                .writeTimeout(serviceBuilder.writeTimeout);

        if (serviceBuilder.apiKey == null) {
            throw new IllegalArgumentException("apiKey must be defined");
        }
        okHttpClientBuilder.addInterceptor(new AuthorizationHeaderInjector(serviceBuilder.apiKey));

        if (serviceBuilder.logRequests) {
            okHttpClientBuilder.addInterceptor(new RequestLoggingInterceptor(serviceBuilder.logLevel));
        }

        if (serviceBuilder.logResponses) {
            okHttpClientBuilder.addInterceptor(new ResponseLoggingInterceptor(serviceBuilder.logLevel));
        }
        this.logStreamingResponses = serviceBuilder.logStreamingResponses;

        this.okHttpClient = okHttpClientBuilder.build();

        Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(serviceBuilder.baseUrl).client(okHttpClient);

        // fastjson is not compatible with retrofit2 now, use gson instead
        // might change to fastjson in the future
        retrofitBuilder.addConverterFactory(GsonConverterFactory.create(GSON));
        this.difyApi = retrofitBuilder.build().create(DifyApi.class);

        this.breakOnToolCalled = serviceBuilder.breakOnToolCalled;
    }

    public void shutdown() {
        okHttpClient.dispatcher().executorService().shutdown();

        okHttpClient.connectionPool().evictAll();

        Cache cache = okHttpClient.cache();
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException e) {
                log.error("Failed to close cache", e);
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends DifyClient.Builder<DefaultDifyClient, Builder> {
        public DefaultDifyClient build() {
            return new DefaultDifyClient(this);
        }
    }

//    @Override
//    public SyncOrAsyncOrStreaming<CompletionResponse> completion(CompletionRequest request) {
//        CompletionRequest syncRequest = CompletionRequest.builder().from(request).stream(null).build();
//
//        return new RequestExecutor<>(
//                openAiApi.completions(syncRequest, apiVersion),
//                r -> r,
//                okHttpClient,
//                formatUrl("completions"),
//                () -> CompletionRequest.builder().from(request).stream(true).build(),
//                CompletionResponse.class,
//                r -> r,
//                logStreamingResponses
//        );
//    }
//
    @Override
    public SyncOrAsyncOrStreaming<DifyStreamingChatCompletionResponse> streamingCompletion(DifyMessageRequest request) {
        DifyMessageRequest syncRequest = DifyMessageRequest.builder().from(request).build();

        return new RequestExecutor<>(
                difyApi.streamingCompletion(syncRequest),
                r -> r,
                okHttpClient,
                formatUrl("completion-messages"),
                () -> DifyMessageRequest.builder().from(request).build(),
                DifyStreamingChatCompletionResponse.class,
                r -> r,
                logStreamingResponses,
                false
        );
    }
    @Override
    public SyncOrAsyncOrStreaming<DifyStreamingChatCompletionResponse> streamingChatCompletion(DifyMessageRequest request) {
        DifyMessageRequest syncRequest = DifyMessageRequest.builder().from(request).build();

        return new RequestExecutor<>(
                difyApi.streamingChatCompletion(syncRequest),
                r -> r,
                okHttpClient,
                formatUrl("chat-messages"),
                () -> DifyMessageRequest.builder().from(request).build(),
                DifyStreamingChatCompletionResponse.class,
                r -> r,
                logStreamingResponses,
                breakOnToolCalled
        );
    }

    @Override
    public SyncOrAsync<DifyChatCompletionResponse> chatCompletion(DifyMessageRequest request) {
        DifyMessageRequest syncRequest = DifyMessageRequest.builder().from(request).build();

        return new RequestExecutor<>(
                difyApi.chatCompletion(syncRequest),
                r -> r,
                okHttpClient,
                formatUrl("chat-messages"),
                () -> DifyMessageRequest.builder().from(request).build(),
                DifyChatCompletionResponse.class,
                r -> r,
                logStreamingResponses
        );
    }

    private String formatUrl(String endpoint) {
        return baseUrl + endpoint;
    }
}
