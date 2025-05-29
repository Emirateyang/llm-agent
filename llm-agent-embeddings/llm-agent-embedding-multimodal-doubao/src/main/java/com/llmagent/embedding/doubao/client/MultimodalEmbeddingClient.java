package com.llmagent.embedding.doubao.client;

import com.llmagent.embedding.doubao.EmbeddingRequest;
import com.llmagent.embedding.doubao.EmbeddingResponse;
import com.llmagent.embedding.http.SyncOrAsync;

import java.time.Duration;
import java.util.Map;

import static com.llmagent.embedding.doubao.DoubaoAiHelper.MULTI_MODAL_API_URL;

public abstract class MultimodalEmbeddingClient {

    public abstract SyncOrAsync<EmbeddingResponse> embedding(EmbeddingRequest request);

    public abstract void shutdown();

    @SuppressWarnings("rawtypes")
    public static Builder builder() {
        return DefaultMultimodalEmbeddingClient.builder();
    }

    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends MultimodalEmbeddingClient, B extends Builder<T, B>> {

        public String baseUrl = MULTI_MODAL_API_URL;
        public String apiKey;
        public Duration callTimeout = Duration.ofSeconds(60);
        public Duration connectTimeout = Duration.ofSeconds(60);
        public Duration readTimeout = Duration.ofSeconds(60);
        public Duration writeTimeout = Duration.ofSeconds(60);
        public Map<String, String> customHeaders;

        public abstract T build();

        /**
         * @param baseUrl Base URL of multimodal embedding service.
         * @return builder
         */
        public B baseUrl(String baseUrl) {
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("baseUrl cannot be null or empty");
            }
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            return (B) this;
        }

        /**
         * @param apiKey multimodal embedding service API key.
         * @return builder
         */
        public B apiKey(String apiKey) {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "apiKey cannot be null or empty."
                );
            }
            this.apiKey = apiKey;
            return (B) this;
        }

        public B callTimeout(Duration callTimeout) {
            if (callTimeout == null) {
                throw new IllegalArgumentException("callTimeout cannot be null");
            }
            this.callTimeout = callTimeout;
            return (B) this;
        }

        public B connectTimeout(Duration connectTimeout) {
            if (connectTimeout == null) {
                throw new IllegalArgumentException("connectTimeout cannot be null");
            }
            this.connectTimeout = connectTimeout;
            return (B) this;
        }

        public B readTimeout(Duration readTimeout) {
            if (readTimeout == null) {
                throw new IllegalArgumentException("readTimeout cannot be null");
            }
            this.readTimeout = readTimeout;
            return (B) this;
        }

        public B writeTimeout(Duration writeTimeout) {
            if (writeTimeout == null) {
                throw new IllegalArgumentException("writeTimeout cannot be null");
            }
            this.writeTimeout = writeTimeout;
            return (B) this;
        }

        /**
         * Custom headers to be added to each HTTP request.
         *
         * @param customHeaders a map of headers
         * @return builder
         */
        public B customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return (B) this;
        }
    }
}
