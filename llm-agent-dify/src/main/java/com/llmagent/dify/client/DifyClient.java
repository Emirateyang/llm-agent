package com.llmagent.dify.client;

import com.llmagent.dify.DifyClientBuilderFactory;
import com.llmagent.dify.SyncOrAsync;
import com.llmagent.dify.SyncOrAsyncOrStreaming;
import com.llmagent.dify.chat.DifyChatCompletionResponse;
import com.llmagent.dify.chat.DifyMessageRequest;
import com.llmagent.dify.chat.DifyStreamingChatCompletionResponse;
import com.llmagent.logger.LogLevel;
import com.llmagent.util.ServiceHelper;

import java.time.Duration;

public abstract class DifyClient {

    public abstract SyncOrAsyncOrStreaming<DifyStreamingChatCompletionResponse> streamingChatCompletion(DifyMessageRequest request);

    public abstract SyncOrAsync<DifyChatCompletionResponse> chatCompletion(DifyMessageRequest request);

    public static DifyClient.Builder builder() {
        for (DifyClientBuilderFactory factory : ServiceHelper.loadFactories(DifyClientBuilderFactory.class)) {
            return factory.get();
        }
        // fallback to the default
        return DefaultDifyClient.builder();
    }

    public abstract static class Builder<T extends DifyClient, B extends Builder<T, B>> {

        public String baseUrl;
        public String apiKey;
        public Duration callTimeout = Duration.ofSeconds(60);
        public Duration connectTimeout = Duration.ofSeconds(60);
        public Duration readTimeout = Duration.ofSeconds(60);
        public Duration writeTimeout = Duration.ofSeconds(60);
        public boolean logRequests;
        public boolean logResponses;
        public boolean logStreamingResponses;
        public LogLevel logLevel = LogLevel.DEBUG;

        public boolean breakOnToolCalled;

        public abstract T build();

        /**
         * @param baseUrl Base URL of Dify.
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
         * @param apiKey Dify API key.
         * @return builder
         */
        public B apiKey(String apiKey) {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "apiKey cannot be null or empty. "
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

        public B breakOnToolCalled() {
            return breakOnToolCalled(false);
        }

        public B breakOnToolCalled(Boolean breakOnToolCalled) {
            if (breakOnToolCalled == null) {
                breakOnToolCalled = false;
            }
            this.breakOnToolCalled = breakOnToolCalled;
            return (B) this;
        }
    }
}
