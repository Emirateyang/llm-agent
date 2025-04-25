package com.llmagent.openai;

import com.llmagent.llm.StreamingResponseHandler;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.llm.completion.StreamingCompletionModel;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.openai.client.OpenAiClient;
import com.llmagent.openai.completion.CompletionChoice;
import com.llmagent.openai.completion.CompletionModelName;
import com.llmagent.openai.completion.CompletionRequest;
import com.llmagent.openai.token.StreamOptions;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.net.Proxy;
import java.time.Duration;
import java.util.Map;

import static com.llmagent.openai.OpenAiHelper.*;
import static com.llmagent.openai.chat.ChatLanguageModelName.GPT_3_5_TURBO;
import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.ObjectUtil.isNotNullOrEmpty;
import static com.llmagent.util.ServiceHelper.loadFactories;
import static java.time.Duration.ofSeconds;

/**
 * Represents an OpenAI language model with a completion interface, such as gpt-3.5-turbo-instruct.
 * The model's response is streamed token by token and should be handled with {@link StreamingResponseHandler}.
 * However, it's recommended to use {@link OpenAiStreamingChatModel} instead,
 * as it offers more advanced features like function calling, multi-turn conversations, etc.
 */
@Slf4j
public class OpenAiStreamingCompletionModel implements StreamingCompletionModel {

    private final OpenAiClient client;
    private final String modelName;
    private final Double temperature;

    @Builder
    public OpenAiStreamingCompletionModel(OpenAiStreamingCompletionModelBuilder builder) {

        this.client = OpenAiClient.builder()
                .baseUrl(getOrDefault(builder.baseUrl, OPENAI_URL))
                .openAiApiKey(builder.apiKey)
                .organizationId(builder.organizationId)
                .projectId(builder.projectId)
                .callTimeout(getOrDefault(builder.timeout, ofSeconds(30)))
                .connectTimeout(getOrDefault(builder.timeout, ofSeconds(15)))
                .readTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .writeTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .proxy(builder.proxy)
                .logRequests(builder.logRequests)
                .logStreamingResponses(builder.logResponses)
                .userAgent(DEFAULT_USER_AGENT)
                .customHeaders(builder.customHeaders)
                .build();
        this.modelName = getOrDefault(builder.modelName, GPT_3_5_TURBO.toString());
        this.temperature = getOrDefault(builder.temperature, 0.7);
    }

    public String modelName() {
        return modelName;
    }

    @Override
    public void generate(String prompt, StreamingResponseHandler<String> handler) {

        CompletionRequest request = CompletionRequest.builder()
                .stream(true)
                .streamOptions(StreamOptions.builder()
                        .includeUsage(true)
                        .build())
                .model(modelName)
                .prompt(prompt)
                .temperature(temperature)
                .build();

        OpenAiStreamingResponseBuilder responseBuilder = new OpenAiStreamingResponseBuilder();

        client.completion(request)
                .onPartialResponse(partialResponse -> {
                    responseBuilder.append(partialResponse);
                    for (CompletionChoice choice : partialResponse.choices()) {
                        String token = choice.text();
                        if (isNotNullOrEmpty(token)) {
                            handler.onNext(token);
                        }
                    }
                })
                .onComplete(() -> {
                    ChatResponse chatResponse = responseBuilder.build();
                    handler.onComplete(LlmResponse.from(
                            chatResponse.aiMessage().content(),
                            chatResponse.metadata().tokenUsage(),
                            chatResponse.metadata().finishReason()
                    ));
                })
                .onError(handler::onError)
                .execute();
    }

    public static OpenAiStreamingCompletionModelBuilder builder() {
        for (OpenAiStreamingCompletionModelBuilderFactory factory : loadFactories(OpenAiStreamingCompletionModelBuilderFactory.class)) {
            return factory.get();
        }
        return new OpenAiStreamingCompletionModelBuilder();
    }

    public static class OpenAiStreamingCompletionModelBuilder {

        private String baseUrl;
        private String apiKey;
        private String organizationId;
        private String projectId;

        private String modelName;
        private Double temperature;
        private Duration timeout;
        private Boolean logRequests;
        private Boolean logResponses;
        private Map<String, String> customHeaders;
        private Proxy proxy;

        public OpenAiStreamingCompletionModelBuilder() {
            // This is public so it can be extended
        }

        public OpenAiStreamingCompletionModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public OpenAiStreamingCompletionModelBuilder modelName(CompletionModelName modelName) {
            this.modelName = modelName.toString();
            return this;
        }

        public OpenAiStreamingCompletionModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public OpenAiStreamingCompletionModelBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public OpenAiStreamingCompletionModelBuilder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public OpenAiStreamingCompletionModelBuilder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public OpenAiStreamingCompletionModelBuilder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public OpenAiStreamingCompletionModelBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public OpenAiStreamingCompletionModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public OpenAiStreamingCompletionModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public OpenAiStreamingCompletionModelBuilder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public OpenAiStreamingCompletionModelBuilder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public OpenAiStreamingCompletionModel build() {
            return new OpenAiStreamingCompletionModel(this);
        }
    }
}
