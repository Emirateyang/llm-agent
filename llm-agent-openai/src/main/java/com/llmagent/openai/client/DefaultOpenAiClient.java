package com.llmagent.openai.client;

import com.llmagent.openai.PersistorConverterFactory;
import com.llmagent.openai.RequestExecutor;
import com.llmagent.openai.SyncOrAsync;
import com.llmagent.openai.SyncOrAsyncOrStreaming;
import com.llmagent.openai.api.OpenAiApi;
import com.llmagent.openai.chat.ChatCompletionRequest;
import com.llmagent.openai.chat.ChatCompletionResponse;
import com.llmagent.openai.completion.CompletionRequest;
import com.llmagent.openai.completion.CompletionResponse;
import com.llmagent.openai.embedding.EmbeddingRequest;
import com.llmagent.openai.embedding.EmbeddingResponse;
import com.llmagent.openai.image.GenerateImagesRequest;
import com.llmagent.openai.image.GenerateImagesResponse;
import com.llmagent.openai.moderation.ModerationRequest;
import com.llmagent.openai.moderation.ModerationResponse;
import com.llmagent.openai.moderation.ModerationResult;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.llmagent.openai.json.Json.GSON;

public class DefaultOpenAiClient extends OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultOpenAiClient.class);

    private final String baseUrl;
    private final String apiVersion;
    private final OkHttpClient okHttpClient;
    private final OpenAiApi openAiApi;
    private final boolean logStreamingResponses;

    public DefaultOpenAiClient(String apiKey) {
        this(new Builder().openAiApiKey(apiKey));
    }

    private DefaultOpenAiClient(Builder serviceBuilder) {
        this.baseUrl = serviceBuilder.baseUrl;
        this.apiVersion = serviceBuilder.apiVersion;

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .callTimeout(serviceBuilder.callTimeout)
                .connectTimeout(serviceBuilder.connectTimeout)
                .readTimeout(serviceBuilder.readTimeout)
                .writeTimeout(serviceBuilder.writeTimeout);

        if (serviceBuilder.openAiApiKey == null) {
            throw new IllegalArgumentException("openAiApiKey must be defined");
        }
        okHttpClientBuilder.addInterceptor(new AuthorizationHeaderInjector(serviceBuilder.openAiApiKey));

        Map<String, String> headers = new HashMap<>();
        if (serviceBuilder.organizationId != null) {
            headers.put("OpenAI-Organization", serviceBuilder.organizationId);
        }
        if (serviceBuilder.userAgent != null) {
            headers.put("User-Agent", serviceBuilder.userAgent);
        }
        if (serviceBuilder.customHeaders != null) {
            headers.putAll(serviceBuilder.customHeaders);
        }
        if (!headers.isEmpty()) {
            okHttpClientBuilder.addInterceptor(new GenericHeaderInjector(headers));
        }

        if (serviceBuilder.proxy != null) {
            okHttpClientBuilder.proxy(serviceBuilder.proxy);
        }

        if (serviceBuilder.logRequests) {
            okHttpClientBuilder.addInterceptor(new RequestLoggingInterceptor(serviceBuilder.logLevel));
        }

        if (serviceBuilder.logResponses) {
            okHttpClientBuilder.addInterceptor(new ResponseLoggingInterceptor(serviceBuilder.logLevel));
        }
        this.logStreamingResponses = serviceBuilder.logStreamingResponses;

        this.okHttpClient = okHttpClientBuilder.build();

        Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(serviceBuilder.baseUrl).client(okHttpClient);

        if (serviceBuilder.persistTo != null) {
            retrofitBuilder.addConverterFactory(new PersistorConverterFactory(serviceBuilder.persistTo));
        }

        // fastjson is not compatible with retrofit2 now, use gson instead
        // might change to fastjson in the future
        retrofitBuilder.addConverterFactory(GsonConverterFactory.create(GSON));
        this.openAiApi = retrofitBuilder.build().create(OpenAiApi.class);
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

    public static class Builder extends OpenAiClient.Builder<DefaultOpenAiClient, Builder> {

        public DefaultOpenAiClient build() {
            return new DefaultOpenAiClient(this);
        }
    }

    @Override
    public SyncOrAsyncOrStreaming<CompletionResponse> completion(CompletionRequest request) {
        CompletionRequest syncRequest = CompletionRequest.builder().from(request).stream(null).build();

        return new RequestExecutor<>(
                openAiApi.completions(syncRequest, apiVersion),
                r -> r,
                okHttpClient,
                formatUrl("completions"),
                () -> CompletionRequest.builder().from(request).stream(true).build(),
                CompletionResponse.class,
                r -> r,
                logStreamingResponses
        );
    }

    @Override
    public SyncOrAsyncOrStreaming<String> completion(String prompt) {
        CompletionRequest request = CompletionRequest.builder().prompt(prompt).build();

        CompletionRequest syncRequest = CompletionRequest.builder().from(request).stream(null).build();

        return new RequestExecutor<>(
                openAiApi.completions(syncRequest, apiVersion),
                CompletionResponse::text,
                okHttpClient,
                formatUrl("completions"),
                () -> CompletionRequest.builder().from(request).stream(true).build(),
                CompletionResponse.class,
                CompletionResponse::text,
                logStreamingResponses
        );
    }

    @Override
    public SyncOrAsyncOrStreaming<ChatCompletionResponse> chatCompletion(ChatCompletionRequest request) {
        ChatCompletionRequest syncRequest = ChatCompletionRequest.builder().from(request).stream(null).build();

        return new RequestExecutor<>(
                openAiApi.chatCompletions(syncRequest, apiVersion),
                r -> r,
                okHttpClient,
                formatUrl("chat/completions"),
                () -> ChatCompletionRequest.builder().from(request).stream(true).build(),
                ChatCompletionResponse.class,
                r -> r,
                logStreamingResponses
        );
    }

    @Override
    public SyncOrAsyncOrStreaming<String> chatCompletion(String userMessage) {
        ChatCompletionRequest request = ChatCompletionRequest.builder().addUserMessage(userMessage).build();

        ChatCompletionRequest syncRequest = ChatCompletionRequest.builder().from(request).stream(null).build();

        return new RequestExecutor<>(
                openAiApi.chatCompletions(syncRequest, apiVersion),
                ChatCompletionResponse::content,
                okHttpClient,
                formatUrl("chat/completions"),
                () -> ChatCompletionRequest.builder().from(request).stream(true).build(),
                ChatCompletionResponse.class,
                r -> r.choices().get(0).delta().content(),
                logStreamingResponses
        );
    }

    @Override
    public SyncOrAsync<EmbeddingResponse> embedding(EmbeddingRequest request) {
        return new RequestExecutor<>(openAiApi.embeddings(request, apiVersion), r -> r);
    }

    @Override
    public SyncOrAsync<List<Float>> embedding(String input) {
        EmbeddingRequest request = EmbeddingRequest.builder().input(input).build();

        return new RequestExecutor<>(openAiApi.embeddings(request, apiVersion), EmbeddingResponse::embedding);
    }

    @Override
    public SyncOrAsync<ModerationResponse> moderation(ModerationRequest request) {
        return new RequestExecutor<>(openAiApi.moderations(request, apiVersion), r -> r);
    }

    @Override
    public SyncOrAsync<ModerationResult> moderation(String input) {
        ModerationRequest request = ModerationRequest.builder().input(input).build();

        return new RequestExecutor<>(openAiApi.moderations(request, apiVersion), r -> r.results().get(0));
    }

    @Override
    public SyncOrAsync<GenerateImagesResponse> imagesGeneration(GenerateImagesRequest request) {
        return new RequestExecutor<>(openAiApi.imagesGenerations(request, apiVersion), r -> r);
    }

    private String formatUrl(String endpoint) {
        return baseUrl + endpoint + apiVersionQueryParam();
    }

    private String apiVersionQueryParam() {
        if (apiVersion == null || apiVersion.trim().isEmpty()) {
            return "";
        }
        return "?api-version=" + apiVersion;
    }
}
