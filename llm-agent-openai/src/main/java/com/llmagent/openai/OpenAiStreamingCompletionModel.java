package com.llmagent.openai;

import com.llmagent.data.message.AiMessage;
import com.llmagent.data.message.ChatMessage;
import com.llmagent.llm.StreamingResponseHandler;
import com.llmagent.llm.Tokenizer;
import com.llmagent.llm.chat.TokenCountEstimator;
import com.llmagent.llm.chat.listener.*;
import com.llmagent.llm.completion.StreamingCompletionModel;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.openai.chat.*;
import com.llmagent.openai.client.OpenAiClient;
import com.llmagent.openai.completion.CompletionChoice;
import com.llmagent.openai.completion.CompletionRequest;
import com.llmagent.util.StringUtil;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.net.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.llmagent.openai.OpenAiHelper.*;
import static com.llmagent.openai.chat.ChatCompletionModel.GPT_3_5_TURBO;
import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.ServiceHelper.loadFactories;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;

/**
 * Represents an OpenAI language model with a completion interface, such as gpt-3.5-turbo-instruct.
 * The model's response is streamed token by token and should be handled with {@link StreamingResponseHandler}.
 * However, it's recommended to use {@link OpenAiStreamingChatModel} instead,
 * as it offers more advanced features like function calling, multi-turn conversations, etc.
 */
@Slf4j
public class OpenAiStreamingCompletionModel implements StreamingCompletionModel, TokenCountEstimator {

    private final OpenAiClient client;
    private final String modelName;
    private final Double temperature;
    private final Double topP;
    private final List<String> stop;
    private final Integer maxTokens;
    private final Double presencePenalty;
    private final Double frequencyPenalty;
    private final Map<String, Integer> logitBias;
    private final String responseFormat;
    private final Integer seed;
    private final String user;
    private final Tokenizer tokenizer;
    private final boolean isOpenAiModel;
    private final List<ChatModelListener> listeners;

    @Builder
    public OpenAiStreamingCompletionModel(String baseUrl,
                                          String apiKey,
                                          String organizationId,
                                          String modelName,
                                          Double temperature,
                                          Double topP,
                                          List<String> stop,
                                          Integer maxTokens,
                                          Double presencePenalty,
                                          Double frequencyPenalty,
                                          Map<String, Integer> logitBias,
                                          String responseFormat,
                                          Integer seed,
                                          String user,
                                          Duration timeout,
                                          Proxy proxy,
                                          Boolean logRequests,
                                          Boolean logResponses,
                                          Tokenizer tokenizer,
                                          Map<String, String> customHeaders,
                                          List<ChatModelListener> listeners) {

        timeout = getOrDefault(timeout, ofSeconds(60));

        this.client = OpenAiClient.builder()
                .baseUrl(getOrDefault(baseUrl, OPENAI_URL))
                .openAiApiKey(apiKey)
                .organizationId(organizationId)
                .callTimeout(timeout)
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .proxy(proxy)
                .logRequests(logRequests)
                .logStreamingResponses(logResponses)
                .userAgent(DEFAULT_USER_AGENT)
                .customHeaders(customHeaders)
                .build();
        this.modelName = getOrDefault(modelName, GPT_3_5_TURBO.toString());
        this.temperature = getOrDefault(temperature, 0.7);
        this.topP = topP;
        this.stop = stop;
        this.maxTokens = maxTokens;
        this.presencePenalty = presencePenalty;
        this.frequencyPenalty = frequencyPenalty;
        this.logitBias = logitBias;
        this.responseFormat = responseFormat;
        this.seed = seed;
        this.user = user;
        this.tokenizer = getOrDefault(tokenizer, OpenAiTokenizer::new);
        this.isOpenAiModel = isOpenAiModel(this.modelName);
        this.listeners = listeners == null ? emptyList() : new ArrayList<>(listeners);
    }

    public String modelName() {
        return modelName;
    }

    @Override
    public void generate(String prompt, StreamingResponseHandler<AiMessage> handler) {

        CompletionRequest request = CompletionRequest.builder()
                .stream(true)
                .model(modelName)
                .prompt(prompt)
                .temperature(temperature)
                .build();

        int inputTokenCount = countInputTokens(prompt);
        OpenAiStreamingResponseBuilder responseBuilder = new OpenAiStreamingResponseBuilder(inputTokenCount);

        client.completion(request)
                .onPartialResponse(partialResponse -> {
                    responseBuilder.append(partialResponse);
                    for (CompletionChoice choice : partialResponse.choices()) {
                        String token = choice.text();
                        if (StringUtil.hasText(token)) {
                            handler.onNext(token);
                        }
                    }
                })
                .onComplete(() -> {
                    LlmResponse<AiMessage> response = responseBuilder.build(tokenizer, false);
                    handler.onComplete(response);
                })
                .onError(handler::onError)
                .execute();
    }

    private int countInputTokens(String prompt) {
        return tokenizer.estimateTokenCountInText(prompt);
    }

    @Override
    public int estimateTokenCount(List<ChatMessage> messages) {
        return tokenizer.estimateTokenCountInMessages(messages);
    }

    public static OpenAiStreamingCompletionModel withApiKey(String apiKey) {
        return builder().apiKey(apiKey).build();
    }

    public static OpenAiStreamingCompletionModelBuilder builder() {
        for (OpenAiStreamingCompletionModelBuilderFactory factory : loadFactories(OpenAiStreamingCompletionModelBuilderFactory.class)) {
            return factory.get();
        }
        return new OpenAiStreamingCompletionModelBuilder();
    }

    public static class OpenAiStreamingCompletionModelBuilder {

        public OpenAiStreamingCompletionModelBuilder() {
            // This is public so it can be extended
            // By default with Lombok it becomes package private
        }

        public OpenAiStreamingCompletionModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public OpenAiStreamingCompletionModelBuilder modelName(ChatCompletionModel modelName) {
            this.modelName = modelName.toString();
            return this;
        }
    }
}
