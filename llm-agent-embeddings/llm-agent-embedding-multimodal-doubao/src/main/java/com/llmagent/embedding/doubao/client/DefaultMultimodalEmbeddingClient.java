package com.llmagent.embedding.doubao.client;

import com.llmagent.embedding.doubao.EmbeddingRequest;
import com.llmagent.embedding.doubao.EmbeddingResponse;
import com.llmagent.embedding.doubao.api.DoubaoApi;
import com.llmagent.embedding.http.RequestExecutor;
import com.llmagent.embedding.http.SyncOrAsync;
import com.llmagent.embedding.http.header.AuthorizationHeaderInjector;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

import static com.llmagent.embedding.json.Json.GSON;
import static com.llmagent.util.ObjectUtil.getOrDefault;
import static java.time.Duration.ofSeconds;

public class DefaultMultimodalEmbeddingClient extends MultimodalEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultMultimodalEmbeddingClient.class);

    public final String baseUrl;
    private final OkHttpClient okHttpClient;
    private final DoubaoApi doubaoApi;

    public DefaultMultimodalEmbeddingClient(String apiKey) {
        this(new Builder().apiKey(apiKey));
    }

    public DefaultMultimodalEmbeddingClient(Builder serviceBuilder) {

        this.baseUrl = serviceBuilder.baseUrl;
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .callTimeout(getOrDefault(serviceBuilder.callTimeout, ofSeconds(30)))
                .connectTimeout(getOrDefault(serviceBuilder.connectTimeout, ofSeconds(15)))
                .readTimeout(getOrDefault(serviceBuilder.readTimeout, ofSeconds(60)))
                .writeTimeout(getOrDefault(serviceBuilder.writeTimeout, ofSeconds(60)));

        if (serviceBuilder.apiKey == null) {
            throw new IllegalArgumentException("apiKey must be defined");
        }
        okHttpClientBuilder.addInterceptor(new AuthorizationHeaderInjector(serviceBuilder.apiKey));
        this.okHttpClient = okHttpClientBuilder.build();

        Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(serviceBuilder.baseUrl).client(okHttpClient);
        // fastjson is not compatible with retrofit2 now, use gson instead
        // might change to fastjson in the future
        retrofitBuilder.addConverterFactory(GsonConverterFactory.create(GSON));
        this.doubaoApi = retrofitBuilder.build().create(DoubaoApi.class);
    }

    @Override
    public SyncOrAsync<EmbeddingResponse> embedding(EmbeddingRequest request) {
        return new RequestExecutor<>(doubaoApi.embeddings(request), r -> r);
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

    public static class Builder extends MultimodalEmbeddingClient.Builder<DefaultMultimodalEmbeddingClient, Builder> {
        public DefaultMultimodalEmbeddingClient build() {
            return new DefaultMultimodalEmbeddingClient(this);
        }
    }

}
