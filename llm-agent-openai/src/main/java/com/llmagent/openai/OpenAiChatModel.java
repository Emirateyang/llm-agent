package com.llmagent.openai;

import com.llmagent.llm.ModelProvider;
import com.llmagent.llm.chat.Capability;
import com.llmagent.llm.chat.listener.*;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.chat.request.ChatRequestParameters;
import com.llmagent.llm.chat.request.DefaultChatRequestParameters;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.openai.chat.*;
import com.llmagent.openai.client.OpenAiClient;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.net.Proxy;
import java.time.Duration;
import java.util.*;

import static com.llmagent.llm.ModelProvider.OPEN_AI;
import static com.llmagent.llm.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static com.llmagent.openai.OpenAiHelper.*;
import static com.llmagent.util.ObjectUtil.copyIfNotNull;
import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.RetryUtil.withRetryMappingExceptions;
import static com.llmagent.util.ServiceHelper.loadFactories;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.*;

/**
 * Represents an OpenAI language model with a chat completion interface, such as gpt-4o-mini and o1.
 * You can find description of parameters <a href="https://platform.openai.com/docs/api-reference/chat/create">here</a>.
 */
@Slf4j
public class OpenAiChatModel implements com.llmagent.llm.chat.ChatLanguageModel {
    private final OpenAiClient client;
    private final Integer maxRetries;

    private final OpenAiChatRequestParameters requestParameters;
    private final String responseFormat;
    private final Set<Capability> supportedCapabilities;
    private final Boolean strictJsonSchema;
    private final Boolean strictTools;
    private final List<ChatModelListener> listeners;

    @Builder
    public OpenAiChatModel(OpenAiChatModelBuilder builder) {

        this.client = OpenAiClient.builder()
                .openAiApiKey(builder.apiKey)
                .baseUrl(getOrDefault(builder.baseUrl, OPENAI_URL))
                .organizationId(builder.organizationId)
                .projectId(builder.projectId)
                .callTimeout(getOrDefault(builder.timeout, ofSeconds(30)))
                .connectTimeout(getOrDefault(builder.timeout, ofSeconds(15)))
                .readTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .writeTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .proxy(builder.proxy)
                .logRequests(builder.logRequests)
                .logResponses(builder.logResponses)
                .userAgent(OpenAiHelper.DEFAULT_USER_AGENT)
                .customHeaders(builder.customHeaders)
                .build();
        this.maxRetries = getOrDefault(builder.maxRetries, 3);

        ChatRequestParameters commonParameters;
        if (builder.defaultRequestParameters != null) {
            commonParameters = builder.defaultRequestParameters;
        } else {
            commonParameters = DefaultChatRequestParameters.builder().build();
        }

        OpenAiChatRequestParameters openAiParameters;
        if (builder.defaultRequestParameters instanceof OpenAiChatRequestParameters openAiChatRequestParameters) {
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

        this.responseFormat = builder.responseFormat;
        this.supportedCapabilities = new HashSet<>(getOrDefault(builder.supportedCapabilities, emptySet()));
        this.strictJsonSchema = getOrDefault(builder.strictJsonSchema, false);
        this.strictTools = getOrDefault(builder.strictTools, false);

        this.listeners = builder.listeners == null ? emptyList() : new ArrayList<>(builder.listeners);
    }

    @Override
    public OpenAiChatRequestParameters defaultRequestParameters() {
        return requestParameters;
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        Set<Capability> capabilities = new HashSet<>(supportedCapabilities);
        if ("json_schema".equals(responseFormat)) {
            capabilities.add(RESPONSE_FORMAT_JSON_SCHEMA);
        }
        return capabilities;
    }

//    @Override
//    public LlmResponse<AiMessage> generate(List<ChatMessage> messages) {
//        return generate(messages, null, null);
//    }
//
//    @Override
//    public LlmResponse<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
//        return generate(messages, toolSpecifications, null);
//    }
//
//    @Override
//    public LlmResponse<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
//        return generate(messages, singletonList(toolSpecification), toolSpecification);
//    }
//
//    private LlmResponse<AiMessage> generate(List<ChatMessage> messages,
//                                            List<ToolSpecification> toolSpecifications,
//                                            ToolSpecification toolThatMustBeExecuted) {
//        ChatCompletionRequest.Builder requestBuilder = toOpenAiChatRequest(messages,
//                this.requestParameters, strictJsonSchema);
//
//
//        if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
//            requestBuilder.tools(toTools(toolSpecifications, strictTools));
//        } else if (toolThatMustBeExecuted != null) {
//            requestBuilder.tools(toTools(singletonList(toolThatMustBeExecuted), strictTools));
//            requestBuilder.toolChoice(toolThatMustBeExecuted.name());
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
//        try {
//            ChatCompletionResponse chatCompletionResponse = withRetry(() -> client.chatCompletion(request).execute(), maxRetries);
//
//            LlmResponse<AiMessage> response = LlmResponse.from (
//                    aiMessageFrom(chatCompletionResponse),
//                    tokenUsageFrom(chatCompletionResponse.usage()),
//                    finishReasonFrom(chatCompletionResponse.choices().get(0).finishReason())
//            );
//
//            ChatResponse modelListenerResponse = createModelListenerResponse(
//                    chatCompletionResponse.id(),
//                    chatCompletionResponse.model(),
//                    response
//            );
//            ChatModelResponseContext responseContext = new ChatModelResponseContext(
//                    modelListenerResponse,
//                    modelListenerRequest,
//                    provider(),
//                    attributes
//            );
//            listeners.forEach(listener -> {
//                try {
//                    listener.onResponse(responseContext);
//                } catch (Exception e) {
//                    log.warn("Exception while calling model listener", e);
//                }
//            });
//
//            return response;
//        } catch (RuntimeException e) {
//
//            Throwable error;
//            if (e.getCause() instanceof OpenAiHttpException) {
//                error = e.getCause();
//            } else {
//                error = e;
//            }
//
//            ChatModelErrorContext errorContext = new ChatModelErrorContext(
//                    error,
//                    modelListenerRequest,
//                    null,
//                    attributes
//            );
//
//            listeners.forEach(listener -> {
//                try {
//                    listener.onError(errorContext);
//                } catch (Exception e2) {
//                    log.warn("Exception while calling model listener", e2);
//                }
//            });
//
//            throw e;
//        }
//    }

    /**
     * a new interface that recommended to be used for interacting with the language model.
     * @param chatRequest the chat request
     */
    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        OpenAiChatRequestParameters parameters = (OpenAiChatRequestParameters) chatRequest.parameters();

        ChatCompletionRequest openAiRequest =
                toOpenAiChatRequest(chatRequest, parameters, strictTools, strictJsonSchema).build();

        ChatCompletionResponse openAiResponse = withRetryMappingExceptions(() ->
                client.chatCompletion(openAiRequest).execute(), maxRetries);

        OpenAiChatResponseMetadata responseMetadata = OpenAiChatResponseMetadata.builder()
                .id(openAiResponse.id())
                .modelName(openAiResponse.model())
                .tokenUsage(tokenUsageFrom(openAiResponse.usage()))
                .finishReason(finishReasonFrom(openAiResponse.choices().get(0).finishReason()))
                .created(openAiResponse.created())
                .serviceTier(openAiResponse.serviceTier())
                .systemFingerprint(openAiResponse.systemFingerprint())
                .build();

        return ChatResponse.builder()
                .aiMessage(aiMessageFrom(openAiResponse))
                .metadata(responseMetadata)
                .build();
    }

    public static OpenAiChatModelBuilder builder() {
        for (OpenAiChatModelBuilderFactory factory : loadFactories(OpenAiChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new OpenAiChatModelBuilder();
    }

    public static class OpenAiChatModelBuilder {

        private String baseUrl;
        private String apiKey;
        private String organizationId;
        private String projectId;

        private ChatRequestParameters defaultRequestParameters;
        private String modelName;
        private Double temperature;
        private Double topP;
        private List<String> stop;
        private Integer maxTokens;
        private Integer maxCompletionTokens;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private Map<String, Integer> logitBias;
        private Set<Capability> supportedCapabilities;
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
        private Integer maxRetries;
        private Boolean logRequests;
        private Boolean logResponses;
        private Map<String, String> customHeaders;
        private List<ChatModelListener> listeners;
        private Proxy proxy;

        public OpenAiChatModelBuilder() {
            // This is public so it can be extended
        }

        /**
         * Sets default common {@link ChatRequestParameters} or OpenAI-specific {@link OpenAiChatRequestParameters}.
         * <br>
         * When a parameter is set via an individual builder method (e.g., {@link #modelName(String)}),
         * its value takes precedence over the same parameter set via {@link ChatRequestParameters}.
         */
        public OpenAiChatModelBuilder defaultRequestParameters(ChatRequestParameters parameters) {
            this.defaultRequestParameters = parameters;
            return this;
        }

        public OpenAiChatModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public OpenAiChatModelBuilder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public OpenAiChatModelBuilder modelName(ChatLanguageModelName modelName) {
            this.modelName = modelName.toString();
            return this;
        }

        public OpenAiChatModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public OpenAiChatModelBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public OpenAiChatModelBuilder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public OpenAiChatModelBuilder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public OpenAiChatModelBuilder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public OpenAiChatModelBuilder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public OpenAiChatModelBuilder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public OpenAiChatModelBuilder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public OpenAiChatModelBuilder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public OpenAiChatModelBuilder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public OpenAiChatModelBuilder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public OpenAiChatModelBuilder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public OpenAiChatModelBuilder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public OpenAiChatModelBuilder supportedCapabilities(Set<Capability> supportedCapabilities) {
            this.supportedCapabilities = supportedCapabilities;
            return this;
        }

        public OpenAiChatModelBuilder supportedCapabilities(Capability... supportedCapabilities) {
            return supportedCapabilities(new HashSet<>(asList(supportedCapabilities)));
        }

        public OpenAiChatModelBuilder strictJsonSchema(Boolean strictJsonSchema) {
            this.strictJsonSchema = strictJsonSchema;
            return this;
        }

        public OpenAiChatModelBuilder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public OpenAiChatModelBuilder user(String user) {
            this.user = user;
            return this;
        }

        public OpenAiChatModelBuilder strictTools(Boolean strictTools) {
            this.strictTools = strictTools;
            return this;
        }

        public OpenAiChatModelBuilder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public OpenAiChatModelBuilder store(Boolean store) {
            this.store = store;
            return this;
        }

        public OpenAiChatModelBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public OpenAiChatModelBuilder serviceTier(String serviceTier) {
            this.serviceTier = serviceTier;
            return this;
        }

        public OpenAiChatModelBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public OpenAiChatModelBuilder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public OpenAiChatModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public OpenAiChatModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public OpenAiChatModelBuilder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public OpenAiChatModelBuilder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public OpenAiChatModel build() {
            return new OpenAiChatModel(this);
        }
    }

    @Override
    public ModelProvider provider() {
        return OPEN_AI;
    }
}
