package com.llmagent.openai.client;

import com.llmagent.openai.*;
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
import com.llmagent.util.ServiceHelper;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public abstract class OpenAiClient {
    public abstract SyncOrAsyncOrStreaming<CompletionResponse> completion(CompletionRequest request);

    public abstract SyncOrAsyncOrStreaming<String> completion(String prompt);

    public abstract SyncOrAsyncOrStreaming<ChatCompletionResponse> chatCompletion(ChatCompletionRequest request);

    public abstract SyncOrAsyncOrStreaming<String> chatCompletion(String userMessage);

    public abstract SyncOrAsync<EmbeddingResponse> embedding(EmbeddingRequest request);

    public abstract SyncOrAsync<List<Float>> embedding(String input);

    public abstract SyncOrAsync<ModerationResponse> moderation(ModerationRequest request);

    public abstract SyncOrAsync<ModerationResult> moderation(String input);

    public abstract SyncOrAsync<GenerateImagesResponse> imagesGeneration(GenerateImagesRequest request);

    public abstract void shutdown();

    @SuppressWarnings("rawtypes")
    public static OpenAiClient.Builder builder() {
        for (OpenAiClientBuilderFactory factory : ServiceHelper.loadFactories(OpenAiClientBuilderFactory.class)) {
            return factory.get();
        }
        // fallback to the default
        return DefaultOpenAiClient.builder();
    }

    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends OpenAiClient, B extends Builder<T, B>> {

        public String baseUrl = "https://api.openai.com/v1/";
        public String organizationId;
        public String apiVersion;
        public String openAiApiKey;
        public Duration callTimeout = Duration.ofSeconds(60);
        public Duration connectTimeout = Duration.ofSeconds(60);
        public Duration readTimeout = Duration.ofSeconds(60);
        public Duration writeTimeout = Duration.ofSeconds(60);
        public Proxy proxy;
        public String userAgent;
        public boolean logRequests;
        public boolean logResponses;
        public LogLevel logLevel = LogLevel.DEBUG;
        public boolean logStreamingResponses;
        public Path persistTo;
        public Map<String, String> customHeaders;

        public abstract T build();

        /**
         * @param baseUrl Base URL of OpenAI API.
         *                For OpenAI (default): "https://api.openai.com/v1/"
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
         *
         * @param organizationId The organizationId for OpenAI: https://platform.openai.com/docs/api-reference/organization-optional
         * @return builder
         */
        public B organizationId(String organizationId) {
            this.organizationId = organizationId;
            return (B) this;
        }

        /**
         * @param apiVersion Version of the API in the YYYY-MM-DD format.
         * @return builder
         */
        public B apiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return (B) this;
        }

        /**
         * @param openAiApiKey OpenAI API key.
         *                     Will be injected in HTTP headers like this: "Authorization: Bearer ${openAiApiKey}"
         * @return builder
         */
        public B openAiApiKey(String openAiApiKey) {
            if (openAiApiKey == null || openAiApiKey.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "openAiApiKey cannot be null or empty. API keys can be generated here: https://platform.openai.com/account/api-keys"
                );
            }
            this.openAiApiKey = openAiApiKey;
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

        public B proxy(Proxy.Type type, String ip, int port) {
            this.proxy = new Proxy(type, new InetSocketAddress(ip, port));
            return (B) this;
        }

        public B proxy(Proxy proxy) {
            this.proxy = proxy;
            return (B) this;
        }

        public B userAgent(String userAgent) {
            this.userAgent = userAgent;
            return (B) this;
        }

        public B logRequests() {
            return logRequests(true);
        }

        public B logRequests(Boolean logRequests) {
            if (logRequests == null) {
                logRequests = false;
            }
            this.logRequests = logRequests;
            return (B) this;
        }

        public B logLevel(LogLevel logLevel) {
            if (logLevel == null) {
                logLevel = LogLevel.DEBUG;
            }
            this.logLevel = logLevel;
            return (B) this;
        }

        public B logResponses() {
            return logResponses(true);
        }

        public B logResponses(Boolean logResponses) {
            if (logResponses == null) {
                logResponses = false;
            }
            this.logResponses = logResponses;
            return (B) this;
        }

        public B logStreamingResponses() {
            return logStreamingResponses(true);
        }

        public B logStreamingResponses(Boolean logStreamingResponses) {
            if (logStreamingResponses == null) {
                logStreamingResponses = false;
            }
            this.logStreamingResponses = logStreamingResponses;
            return (B) this;
        }

        /**
         * Generated response will be persisted under <code>java.io.tmpdir</code>. Used with images generation for the moment only.
         * The URL within <code>dev.ai4j.openai4j.image.GenerateImagesResponse</code> will contain the URL to local images then.
         *
         * @return builder
         */
        public B withPersisting() {
            persistTo = Paths.get(System.getProperty("java.io.tmpdir"));
            return (B) this;
        }

        /**
         * Generated response will be persisted under provided path. Used with images generation for the moment only.
         * The URL within <code>dev.ai4j.openai4j.image.GenerateImagesResponse</code> will contain the URL to local images then.
         *
         * @param persistTo path
         * @return builder
         */
        public B persistTo(Path persistTo) {
            this.persistTo = persistTo;
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
