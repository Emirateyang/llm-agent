package com.llmagent.openai;

import com.llmagent.llm.chat.listener.*;
import com.llmagent.openai.chat.ChatCompletionModel;
import com.llmagent.openai.chat.ChatCompletionRequest;
import com.llmagent.openai.chat.ChatCompletionResponse;
import com.llmagent.openai.client.OpenAiClient;
import com.llmagent.data.message.AiMessage;
import com.llmagent.data.message.ChatMessage;
import com.llmagent.openai.exception.OpenAiHttpException;
import com.llmagent.llm.Tokenizer;
import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.chat.TokenCountEstimator;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.tool.ToolSpecification;
import com.llmagent.util.ObjectUtil;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.net.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.llmagent.openai.OpenAiHelper.*;
import static com.llmagent.util.ServiceHelper.loadFactories;
import static com.llmagent.util.RetryUtil.withRetry;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Represents an OpenAI language model with a chat completion interface, such as gpt-3.5-turbo and gpt-4.
 * You can find description of parameters <a href="https://platform.openai.com/docs/api-reference/chat/create">here</a>.
 */
@Slf4j
public class OpenAiChatModel implements ChatLanguageModel, TokenCountEstimator {
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
    private final Integer maxRetries;
    private final Tokenizer tokenizer;
    private final List<ChatModelListener> listeners;

    @Builder
    public OpenAiChatModel(String baseUrl,
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
                           Integer maxRetries,
                           Proxy proxy,
                           Boolean logRequests,
                           Boolean logResponses,
                           Tokenizer tokenizer,
                           Map<String, String> customHeaders,
                           List<ChatModelListener> listeners) {

        baseUrl = ObjectUtil.getOrDefault(baseUrl, OpenAiHelper.OPENAI_URL);

        timeout = ObjectUtil.getOrDefault(timeout, Duration.ofSeconds(60));

        this.client = OpenAiClient.builder()
                .openAiApiKey(apiKey)
                .baseUrl(baseUrl)
                .organizationId(organizationId)
                .callTimeout(timeout)
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .proxy(proxy)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .userAgent(OpenAiHelper.DEFAULT_USER_AGENT)
                .customHeaders(customHeaders)
                .build();
        this.modelName = ObjectUtil.getOrDefault(modelName, ChatCompletionModel.GPT_3_5_TURBO.toString());
        this.temperature = ObjectUtil.getOrDefault(temperature, 0.2);
        this.topP = topP;
        this.stop = stop;
        this.maxTokens = maxTokens;
        this.presencePenalty = presencePenalty;
        this.frequencyPenalty = frequencyPenalty;
        this.logitBias = logitBias;
        this.responseFormat = responseFormat;
        this.seed = seed;
        this.user = user;
        this.maxRetries = ObjectUtil.getOrDefault(maxRetries, 3);
        this.tokenizer = ObjectUtil.getOrDefault(tokenizer, OpenAiTokenizer::new);
        this.listeners = listeners == null ? emptyList() : new ArrayList<>(listeners);
    }

    public String modelName() {
        return modelName;
    }

    @Override
    public LlmResponse<AiMessage> generate(List<ChatMessage> messages) {
        return generate(messages, null, null);
    }

    @Override
    public LlmResponse<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        return generate(messages, toolSpecifications, null);
    }

    @Override
    public LlmResponse<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
        return generate(messages, singletonList(toolSpecification), toolSpecification);
    }

    private LlmResponse<AiMessage> generate(List<ChatMessage> messages,
                                            List<ToolSpecification> toolSpecifications,
                                            ToolSpecification toolThatMustBeExecuted) {
        ChatCompletionRequest.Builder requestBuilder = ChatCompletionRequest.builder()
                .model(modelName)
                .messages(toOpenAiMessages(messages))
                .temperature(temperature)
                .topP(topP)
                .stop(stop)
                .maxTokens(maxTokens)
                .presencePenalty(presencePenalty)
                .frequencyPenalty(frequencyPenalty)
                .logitBias(logitBias)
                .responseFormat(responseFormat)
                .seed(seed)
                .user(user);

        if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
            requestBuilder.tools(toTools(toolSpecifications));
        }
        if (toolThatMustBeExecuted != null) {
            requestBuilder.toolChoice(toolThatMustBeExecuted.name());
        }

        ChatCompletionRequest request = requestBuilder.build();

        ChatModelRequest modelListenerRequest = createModelListenerRequest(request, messages, toolSpecifications);
        Map<Object, Object> attributes = new ConcurrentHashMap<>();
        ChatModelRequestContext requestContext = new ChatModelRequestContext(modelListenerRequest, attributes);
        listeners.forEach(listener -> {
            try {
                listener.onRequest(requestContext);
            } catch (Exception e) {
                log.warn("Exception while calling model listener", e);
            }
        });

        try {
            ChatCompletionResponse chatCompletionResponse = withRetry(() -> client.chatCompletion(request).execute(), maxRetries);

            LlmResponse<AiMessage> response = LlmResponse.from (
                    aiMessageFrom(chatCompletionResponse),
                    tokenUsageFrom(chatCompletionResponse.usage()),
                    finishReasonFrom(chatCompletionResponse.choices().get(0).finishReason())
            );

            ChatModelResponse modelListenerResponse = createModelListenerResponse(
                    chatCompletionResponse.id(),
                    chatCompletionResponse.model(),
                    response
            );
            ChatModelResponseContext responseContext = new ChatModelResponseContext(
                    modelListenerResponse,
                    modelListenerRequest,
                    attributes
            );
            listeners.forEach(listener -> {
                try {
                    listener.onResponse(responseContext);
                } catch (Exception e) {
                    log.warn("Exception while calling model listener", e);
                }
            });

            return response;
        } catch (RuntimeException e) {

            Throwable error;
            if (e.getCause() instanceof OpenAiHttpException) {
                error = e.getCause();
            } else {
                error = e;
            }

            ChatModelErrorContext errorContext = new ChatModelErrorContext(
                    error,
                    modelListenerRequest,
                    null,
                    attributes
            );

            listeners.forEach(listener -> {
                try {
                    listener.onError(errorContext);
                } catch (Exception e2) {
                    log.warn("Exception while calling model listener", e2);
                }
            });

            throw e;
        }
    }

    @Override
    public int estimateTokenCount(List<ChatMessage> messages) {
        return tokenizer.estimateTokenCountInMessages(messages);
    }

    /**
     * with simple and fast to build chatModel
     */
    public static OpenAiChatModel withApiKey(String apiKey) {
        return builder().apiKey(apiKey).build();
    }

    public static OpenAiChatModelBuilder builder() {
        for (OpenAiChatModelBuilderFactory factory : loadFactories(OpenAiChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new OpenAiChatModelBuilder();
    }

    public static class OpenAiChatModelBuilder {

        public OpenAiChatModelBuilder() {
            // This is public, so it can be extended
            // By default with Lombok it becomes package private
        }

        public OpenAiChatModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public OpenAiChatModelBuilder modelName(ChatCompletionModel modelName) {
            this.modelName = modelName.toString();
            return this;
        }
    }
}
