package com.llmagent.openai;

import com.llmagent.llm.ModelProvider;
import com.llmagent.llm.StreamingResponseHandler;
import com.llmagent.llm.chat.StreamingChatLanguageModel;
import com.llmagent.llm.chat.listener.*;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.chat.request.ChatRequestParameters;
import com.llmagent.llm.chat.request.DefaultChatRequestParameters;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.llm.chat.response.StreamingChatResponseHandler;
import com.llmagent.openai.chat.*;
import com.llmagent.openai.client.OpenAiClient;
import com.llmagent.openai.token.StreamOptions;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.net.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.llmagent.llm.ModelProvider.OPEN_AI;
import static com.llmagent.openai.OpenAiHelper.*;
import static com.llmagent.util.ObjectUtil.*;
import static com.llmagent.util.ServiceHelper.loadFactories;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;

/**
 * Represents an OpenAI language model with a chat completion interface, such as gpt-4o-mini.
 * The model's response is streamed token by token and should be handled with {@link StreamingResponseHandler}.
 * You can find description of parameters <a href="https://platform.openai.com/docs/api-reference/chat/create">here</a>.
 */
@Slf4j
public class OpenAiStreamingChatModel implements StreamingChatLanguageModel {

    private final OpenAiClient client;
    private final OpenAiChatRequestParameters requestParameters;
    private final boolean isOpenAiModel;
    private final Boolean strictJsonSchema;
    private final Boolean strictTools;
    private final List<ChatModelListener> listeners;

    @Builder
    public OpenAiStreamingChatModel(OpenAiStreamingChatModelBuilder builder) {

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

        ChatRequestParameters commonParameters;
        if (builder.requestParameters != null) {
            commonParameters = builder.requestParameters;
        } else {
            commonParameters = DefaultChatRequestParameters.builder().build();
        }
        OpenAiChatRequestParameters openAiParameters;
        if (builder.requestParameters instanceof OpenAiChatRequestParameters openAiChatRequestParameters) {
            openAiParameters = openAiChatRequestParameters;
        } else {
            openAiParameters = OpenAiChatRequestParameters.builder().build();
        }

        this.requestParameters = OpenAiChatRequestParameters.builder()
                // common parameters
                .modelName(getOrDefault(builder.modelName, commonParameters.modelName()))
                .temperature(getOrDefault(builder.temperature, commonParameters.temperature()))
                .topP(getOrDefault(builder.topP, commonParameters.topP()))
                .frequencyPenalty(getOrDefault(builder.frequencyPenalty, commonParameters.frequencyPenalty()))
                .presencePenalty(getOrDefault(builder.presencePenalty, commonParameters.presencePenalty()))
                .maxOutputTokens(getOrDefault(builder.maxTokens, commonParameters.maxOutputTokens()))
                .stopSequences(getOrDefault(builder.stop, () -> copyIfNotNull(commonParameters.stopSequences())))
                .toolSpecifications(copyIfNotNull(commonParameters.toolSpecifications()))
                .toolChoice(commonParameters.toolChoice())
                .responseFormat(getOrDefault(fromOpenAiResponseFormat(builder.responseFormat), commonParameters.responseFormat()))
                // OpenAI-specific parameters
                .maxCompletionTokens(getOrDefault(builder.maxCompletionTokens, openAiParameters.maxCompletionTokens()))
                .logitBias(getOrDefault(builder.logitBias, () -> copyIfNotNull(openAiParameters.logitBias())))
                .parallelToolCalls(getOrDefault(builder.parallelToolCalls, openAiParameters.parallelToolCalls()))
                .seed(getOrDefault(builder.seed, openAiParameters.seed()))
                .user(getOrDefault(builder.user, openAiParameters.user()))
                .store(getOrDefault(builder.store, openAiParameters.store()))
                .metadata(getOrDefault(builder.metadata, () -> copyIfNotNull(openAiParameters.metadata())))
                .serviceTier(getOrDefault(builder.serviceTier, openAiParameters.serviceTier()))
                .reasoningEffort(openAiParameters.reasoningEffort())
                .build();

        this.isOpenAiModel = isOpenAiModel(commonParameters.modelName());
        this.strictJsonSchema = getOrDefault(builder.strictJsonSchema, false);
        this.strictTools = getOrDefault(builder.strictTools, false);
        this.listeners = builder.listeners == null ? emptyList() : new ArrayList<>(builder.listeners);
    }

    @Override
    public OpenAiChatRequestParameters defaultRequestParameters() {
        return requestParameters;
    }

    @Override
    public List<ChatModelListener> listeners() {
        return listeners;
    }

    @Override
    public ModelProvider provider() {
        return OPEN_AI;
    }

//    @Override
//    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
//        generate(messages, null, null, handler);
//    }
//
//    @Override
//    public void generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications, StreamingResponseHandler<AiMessage> handler) {
//        generate(messages, toolSpecifications, null, handler);
//    }
//
//    @Override
//    public void generate(List<ChatMessage> messages, ToolSpecification toolSpecification, StreamingResponseHandler<AiMessage> handler) {
//        generate(messages, null, toolSpecification, handler);
//    }
//
//    private void generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications,
//                          ToolSpecification toolThatMustBeExecuted, StreamingResponseHandler<AiMessage> handler) {
//        ChatCompletionRequest.Builder requestBuilder = toOpenAiChatRequest(messages, this.requestParameters, strictJsonSchema)
//                .stream(true)
//                .streamOptions(StreamOptions.builder()
//                        .includeUsage(true)
//                        .build());
//
//        if (toolThatMustBeExecuted != null) {
//            requestBuilder.tools(toTools(singletonList(toolThatMustBeExecuted), strictTools));
//            requestBuilder.toolChoice(toolThatMustBeExecuted.name());
//        } else if (!isNullOrEmpty(toolSpecifications)) {
//            requestBuilder.tools(toTools(toolSpecifications, strictTools));
//        }
//
//        ChatCompletionRequest request = requestBuilder.build();
//
//        ChatRequest modelListenerRequest = createModelListenerRequest(request, messages, toolSpecifications);
//        Map<Object, Object> attributes = new ConcurrentHashMap<>();
//        ChatModelRequestContext requestContext = new ChatModelRequestContext(modelListenerRequest,
//                provider(), attributes);
//        listeners.forEach(listener -> {
//            try {
//                listener.onRequest(requestContext);
//            } catch (Exception e) {
//                log.warn("Exception while calling model listener", e);
//            }
//        });
//
//        OpenAiStreamingResponseBuilder responseBuilder = new OpenAiStreamingResponseBuilder();
//
//        client.chatCompletion(request)
//                .onPartialResponse(partialResponse -> {
//                    responseBuilder.append(partialResponse);
//                    handle(partialResponse, handler);
//                })
//                .onComplete(() -> {
//                    LlmResponse<AiMessage> response = createResponse(responseBuilder);
//
//                    ChatResponse modelListenerResponse = responseBuilder.buildChatResponse();
//                    ChatModelResponseContext responseContext = new ChatModelResponseContext(
//                            modelListenerResponse, modelListenerRequest, provider(), attributes
//                    );
//                    listeners.forEach(listener -> {
//                        try {
//                            listener.onResponse(responseContext);
//                        } catch (Exception e) {
//                            log.warn("Exception while calling model listener", e);
//                        }
//                    });
//
//                    handler.onComplete(response);
//                })
//                .onError(error -> {
//                    ChatModelErrorContext errorContext = new ChatModelErrorContext(
//                            error, modelListenerRequest, provider(), attributes
//                    );
//
//                    listeners.forEach(listener -> {
//                        try {
//                            listener.onError(errorContext);
//                        } catch (Exception e) {
//                            log.warn("Exception while calling model listener", e);
//                        }
//                    });
//
//                    handler.onError(error);
//                })
//                .execute();
//    }
//
//    private LlmResponse<AiMessage> createResponse(OpenAiStreamingResponseBuilder responseBuilder) {
//        LlmResponse<AiMessage> response = responseBuilder.build();
//        if (isOpenAiModel) {
//            return response;
//        }
//        return removeTokenUsage(response);
//    }
//
//
//    private static void handle(ChatCompletionResponse partialResponse,
//                               StreamingResponseHandler<AiMessage> handler) {
//        if (partialResponse == null) {
//            return;
//        }
//        List<ChatCompletionChoice> choices = partialResponse.choices();
//        if (choices == null || choices.isEmpty()) {
//            return;
//        }
//        ChatCompletionChoice chatCompletionChoice = choices.get(0);
//        if (chatCompletionChoice == null) {
//            return;
//        }
//        Delta delta = chatCompletionChoice.delta();
//        if (delta == null) {
//            return;
//        }
//        String content = delta.content();
//        if (!isNullOrEmpty(content)) {
//            handler.onNext(content);
//        }
//    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {

        OpenAiChatRequestParameters parameters = (OpenAiChatRequestParameters) chatRequest.parameters();

        ChatCompletionRequest openAiRequest =
                toOpenAiChatRequest(chatRequest, parameters, strictTools, strictJsonSchema)
                        .stream(true)
                        .streamOptions(StreamOptions.builder()
                                .includeUsage(true)
                                .build())
                        .build();

        OpenAiStreamingResponseBuilder openAiResponseBuilder = new OpenAiStreamingResponseBuilder();

        client.chatCompletion(openAiRequest)
                .onPartialResponse(partialResponse -> {
                    openAiResponseBuilder.append(partialResponse);
                    handle(partialResponse, handler);
                })
                .onComplete(() -> {
                    ChatResponse chatResponse = openAiResponseBuilder.build();
                    handler.onCompleteResponse(chatResponse);
                })
                .onError(handler::onError)
                .execute();
    }

    private static void handle(ChatCompletionResponse partialResponse,
                               StreamingChatResponseHandler handler) {
        if (partialResponse == null) {
            return;
        }

        List<ChatCompletionChoice> choices = partialResponse.choices();
        if (choices == null || choices.isEmpty()) {
            return;
        }

        ChatCompletionChoice chatCompletionChoice = choices.get(0);
        if (chatCompletionChoice == null) {
            return;
        }

        Delta delta = chatCompletionChoice.delta();
        if (delta == null) {
            return;
        }

        String content = delta.content();
        if (!isNullOrEmpty(content)) {
            handler.onPartialResponse(content);
        }
    }

    public static OpenAiStreamingChatModelBuilder builder() {
        for (OpenAiStreamingChatModelBuilderFactory factory : loadFactories(OpenAiStreamingChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new OpenAiStreamingChatModelBuilder();
    }

    public static class OpenAiStreamingChatModelBuilder {

        private String baseUrl;
        private String apiKey;
        private String organizationId;
        private String projectId;
        private Proxy proxy;

        private ChatRequestParameters requestParameters;
        private String modelName;
        private Double temperature;
        private Double topP;
        private List<String> stop;
        private Integer maxTokens;
        private Integer maxCompletionTokens;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private Map<String, Integer> logitBias;
        private String responseFormat;
        private Boolean strictJsonSchema;
        private Integer seed;
        private String user;
        private Boolean strictTools;
        private Boolean parallelToolCalls;
        private Boolean store;
        private Map<String, String> metadata;
        private String serviceTier;
        private Duration timeout;
        private Boolean logRequests;
        private Boolean logResponses;
        private Map<String, String> customHeaders;
        private List<ChatModelListener> listeners;

        public OpenAiStreamingChatModelBuilder() {
            // This is public so it can be extended
        }

        /**
         * Sets default common {@link ChatRequestParameters} or OpenAI-specific {@link OpenAiChatRequestParameters}.
         * <br>
         * When a parameter is set via an individual builder method (e.g., {@link #modelName(String)}),
         * its value takes precedence over the same parameter set via {@link ChatRequestParameters}.
         */
        public OpenAiStreamingChatModelBuilder requestParameters(ChatRequestParameters parameters) {
            this.requestParameters = parameters;
            return this;
        }

        public OpenAiStreamingChatModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public OpenAiStreamingChatModelBuilder modelName(ChatLanguageModelName modelName) {
            this.modelName = modelName.toString();
            return this;
        }

        public OpenAiStreamingChatModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public OpenAiStreamingChatModelBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public OpenAiStreamingChatModelBuilder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public OpenAiStreamingChatModelBuilder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public OpenAiStreamingChatModelBuilder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public OpenAiStreamingChatModelBuilder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public OpenAiStreamingChatModelBuilder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public OpenAiStreamingChatModelBuilder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public OpenAiStreamingChatModelBuilder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public OpenAiStreamingChatModelBuilder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public OpenAiStreamingChatModelBuilder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public OpenAiStreamingChatModelBuilder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public OpenAiStreamingChatModelBuilder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public OpenAiStreamingChatModelBuilder strictJsonSchema(Boolean strictJsonSchema) {
            this.strictJsonSchema = strictJsonSchema;
            return this;
        }

        public OpenAiStreamingChatModelBuilder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public OpenAiStreamingChatModelBuilder user(String user) {
            this.user = user;
            return this;
        }

        public OpenAiStreamingChatModelBuilder strictTools(Boolean strictTools) {
            this.strictTools = strictTools;
            return this;
        }

        public OpenAiStreamingChatModelBuilder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public OpenAiStreamingChatModelBuilder store(Boolean store) {
            this.store = store;
            return this;
        }

        public OpenAiStreamingChatModelBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public OpenAiStreamingChatModelBuilder serviceTier(String serviceTier) {
            this.serviceTier = serviceTier;
            return this;
        }

        public OpenAiStreamingChatModelBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public OpenAiStreamingChatModelBuilder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public OpenAiStreamingChatModelBuilder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public OpenAiStreamingChatModel build() {
            return new OpenAiStreamingChatModel(this);
        }
    }
}
