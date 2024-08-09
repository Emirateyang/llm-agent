package com.llmagent.azure.chat;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.KeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.ProxyOptions;
import com.llmagent.azure.AzureAiHelper;
import com.llmagent.azure.AzureAiStreamingChatModelBuilderFactory;
import com.llmagent.azure.AzureAiStreamingResponseBuilder;
import com.llmagent.azure.AzureAiTokenizer;
import com.llmagent.data.message.AiMessage;
import com.llmagent.data.message.ChatMessage;
import com.llmagent.llm.StreamingResponseHandler;
import com.llmagent.llm.Tokenizer;
import com.llmagent.llm.chat.StreamingChatLanguageModel;
import com.llmagent.llm.chat.TokenCountEstimator;
import com.llmagent.llm.chat.listener.*;
import com.llmagent.llm.output.FinishReason;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.tool.ToolSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.llmagent.azure.AzureAiHelper.*;
import static com.llmagent.data.message.AiMessage.aiMessage;
import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.ObjectUtil.isNullOrEmpty;
import static com.llmagent.util.ServiceHelper.loadFactories;
import static com.llmagent.util.StringUtil.isNotNullOrBlank;
import static java.util.Collections.singletonList;

public class AzureAiStreamingChatModel implements StreamingChatLanguageModel, TokenCountEstimator {
    private static final Logger logger = LoggerFactory.getLogger(AzureAiStreamingChatModel.class);

    private OpenAIClient client;
    private OpenAIAsyncClient asyncClient;
    private final String deploymentName;
    private final Tokenizer tokenizer;
    private final Integer maxTokens;
    private final Double temperature;
    private final Double topP;
    private final Map<String, Integer> logitBias;
    private final String user;
    private final Integer n;
    private final List<String> stop;
    private final Double presencePenalty;
    private final Double frequencyPenalty;
    private final List<AzureChatExtensionConfiguration> dataSources;
    private final AzureChatEnhancementConfiguration enhancements;
    private final Long seed;
    private final ChatCompletionsResponseFormat responseFormat;
    private final List<ChatModelListener> listeners;

    public AzureAiStreamingChatModel(OpenAIClient client,
                                     OpenAIAsyncClient asyncClient,
                                     String deploymentName,
                                     Tokenizer tokenizer,
                                     Integer maxTokens,
                                     Double temperature,
                                     Double topP,
                                     Map<String, Integer> logitBias,
                                     String user,
                                     Integer n,
                                     List<String> stop,
                                     Double presencePenalty,
                                     Double frequencyPenalty,
                                     List<AzureChatExtensionConfiguration> dataSources,
                                     AzureChatEnhancementConfiguration enhancements,
                                     Long seed,
                                     ChatCompletionsResponseFormat responseFormat,
                                     List<ChatModelListener> listeners) {

        this(deploymentName, tokenizer, maxTokens, temperature, topP, logitBias, user, n, stop, presencePenalty, frequencyPenalty, dataSources, enhancements, seed, responseFormat, listeners);

        if (asyncClient != null) {
            this.asyncClient = asyncClient;
        } else if(client != null) {
            this.client = client;
        } else {
            throw new IllegalStateException("No client available");
        }
    }

    public AzureAiStreamingChatModel(String endpoint,
                                         String serviceVersion,
                                         String apiKey,
                                         String deploymentName,
                                         Tokenizer tokenizer,
                                         Integer maxTokens,
                                         Double temperature,
                                         Double topP,
                                         Map<String, Integer> logitBias,
                                         String user,
                                         Integer n,
                                         List<String> stop,
                                         Double presencePenalty,
                                         Double frequencyPenalty,
                                         List<AzureChatExtensionConfiguration> dataSources,
                                         AzureChatEnhancementConfiguration enhancements,
                                         Long seed,
                                         ChatCompletionsResponseFormat responseFormat,
                                         Duration timeout,
                                         Integer maxRetries,
                                         ProxyOptions proxyOptions,
                                         boolean logRequestsAndResponses,
                                         boolean useAsyncClient,
                                         List<ChatModelListener> listeners,
                                         String userAgentSuffix) {

        this(deploymentName, tokenizer, maxTokens, temperature, topP, logitBias, user, n, stop, presencePenalty, frequencyPenalty, dataSources, enhancements, seed, responseFormat, listeners);
        if(useAsyncClient)
            this.asyncClient = setupAsyncClient(endpoint, serviceVersion, apiKey, timeout, maxRetries, proxyOptions, logRequestsAndResponses, userAgentSuffix);
        else
            this.client = setupSyncClient(endpoint, serviceVersion, apiKey, timeout, maxRetries, proxyOptions, logRequestsAndResponses, userAgentSuffix);  }

    public AzureAiStreamingChatModel(String endpoint,
                                         String serviceVersion,
                                         KeyCredential keyCredential,
                                         String deploymentName,
                                         Tokenizer tokenizer,
                                         Integer maxTokens,
                                         Double temperature,
                                         Double topP,
                                         Map<String, Integer> logitBias,
                                         String user,
                                         Integer n,
                                         List<String> stop,
                                         Double presencePenalty,
                                         Double frequencyPenalty,
                                         List<AzureChatExtensionConfiguration> dataSources,
                                         AzureChatEnhancementConfiguration enhancements,
                                         Long seed,
                                         ChatCompletionsResponseFormat responseFormat,
                                         Duration timeout,
                                         Integer maxRetries,
                                         ProxyOptions proxyOptions,
                                         boolean logRequestsAndResponses,
                                         boolean useAsyncClient,
                                         List<ChatModelListener> listeners,
                                         String userAgentSuffix) {

        this(deploymentName, tokenizer, maxTokens, temperature, topP, logitBias, user, n, stop, presencePenalty, frequencyPenalty, dataSources, enhancements, seed, responseFormat, listeners);
        if(useAsyncClient)
            this.asyncClient = setupAsyncClient(endpoint, serviceVersion, keyCredential, timeout, maxRetries, proxyOptions, logRequestsAndResponses, userAgentSuffix);
        else
            this.client = setupSyncClient(endpoint, serviceVersion, keyCredential, timeout, maxRetries, proxyOptions, logRequestsAndResponses, userAgentSuffix);
    }

    public AzureAiStreamingChatModel(String endpoint,
                                         String serviceVersion,
                                         TokenCredential tokenCredential,
                                         String deploymentName,
                                         Tokenizer tokenizer,
                                         Integer maxTokens,
                                         Double temperature,
                                         Double topP,
                                         Map<String, Integer> logitBias,
                                         String user,
                                         Integer n,
                                         List<String> stop,
                                         Double presencePenalty,
                                         Double frequencyPenalty,
                                         List<AzureChatExtensionConfiguration> dataSources,
                                         AzureChatEnhancementConfiguration enhancements,
                                         Long seed,
                                         ChatCompletionsResponseFormat responseFormat,
                                         Duration timeout,
                                         Integer maxRetries,
                                         ProxyOptions proxyOptions,
                                         boolean logRequestsAndResponses,
                                         boolean useAsyncClient,
                                         List<ChatModelListener> listeners,
                                         String userAgentSuffix) {

        this(deploymentName, tokenizer, maxTokens, temperature, topP, logitBias, user, n, stop, presencePenalty, frequencyPenalty, dataSources, enhancements, seed, responseFormat, listeners);
        if(useAsyncClient)
            this.asyncClient = setupAsyncClient(endpoint, serviceVersion, tokenCredential, timeout, maxRetries, proxyOptions, logRequestsAndResponses, userAgentSuffix);
        else
            this.client = setupSyncClient(endpoint, serviceVersion, tokenCredential, timeout, maxRetries, proxyOptions, logRequestsAndResponses, userAgentSuffix);
    }


    private AzureAiStreamingChatModel(String deploymentName,
                                          Tokenizer tokenizer,
                                          Integer maxTokens,
                                          Double temperature,
                                          Double topP,
                                          Map<String, Integer> logitBias,
                                          String user,
                                          Integer n,
                                          List<String> stop,
                                          Double presencePenalty,
                                          Double frequencyPenalty,
                                          List<AzureChatExtensionConfiguration> dataSources,
                                          AzureChatEnhancementConfiguration enhancements,
                                          Long seed,
                                          ChatCompletionsResponseFormat responseFormat,
                                          List<ChatModelListener> listeners) {

        this.deploymentName = getOrDefault(deploymentName, "gpt-35-turbo");
        this.tokenizer = getOrDefault(tokenizer, AzureAiTokenizer::new);
        this.maxTokens = maxTokens;
        this.temperature = getOrDefault(temperature, 0.7);
        this.topP = topP;
        this.logitBias = logitBias;
        this.user = user;
        this.n = n;
        this.stop = stop;
        this.presencePenalty = presencePenalty;
        this.frequencyPenalty = frequencyPenalty;
        this.dataSources = dataSources;
        this.enhancements = enhancements;
        this.seed = seed;
        this.responseFormat = responseFormat;
        this.listeners = listeners == null ? Collections.emptyList() : new ArrayList<>(listeners);
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        generate(messages, null, null, handler);
    }

    @Override
    public void generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications, StreamingResponseHandler<AiMessage> handler) {
        generate(messages, toolSpecifications, null, handler);
    }

    @Override
    public void generate(List<ChatMessage> messages, ToolSpecification toolSpecification, StreamingResponseHandler<AiMessage> handler) {
        generate(messages, null, toolSpecification, handler);
    }

    private void generate(List<ChatMessage> messages,
                          List<ToolSpecification> toolSpecifications,
                          ToolSpecification toolThatMustBeExecuted,
                          StreamingResponseHandler<AiMessage> handler
    ) {
        ChatCompletionsOptions options = new ChatCompletionsOptions(AzureAiHelper.toOpenAiMessages(messages))
                .setStream(true)
                .setModel(deploymentName)
                .setMaxTokens(maxTokens)
                .setTemperature(temperature)
                .setTopP(topP)
                .setLogitBias(logitBias)
                .setUser(user)
                .setN(n)
                .setStop(stop)
                .setPresencePenalty(presencePenalty)
                .setFrequencyPenalty(frequencyPenalty)
                .setDataSources(dataSources)
                .setEnhancements(enhancements)
                .setSeed(seed)
                .setResponseFormat(responseFormat);

        int inputTokenCount = tokenizer.estimateTokenCountInMessages(messages);

        if (toolThatMustBeExecuted != null) {
            options.setTools(toToolDefinitions(singletonList(toolThatMustBeExecuted)));
            options.setToolChoice(toToolChoice(toolThatMustBeExecuted));
            inputTokenCount += tokenizer.estimateTokenCountInForcefulToolSpecification(toolThatMustBeExecuted);
        }
        if (!isNullOrEmpty(toolSpecifications)) {
            options.setTools(toToolDefinitions(toolSpecifications));
            inputTokenCount += tokenizer.estimateTokenCountInToolSpecifications(toolSpecifications);
        }

        AzureAiStreamingResponseBuilder responseBuilder = new AzureAiStreamingResponseBuilder(inputTokenCount);

        ChatModelRequest modelListenerRequest = createModelListenerRequest(options, messages, toolSpecifications);
        Map<Object, Object> attributes = new ConcurrentHashMap<>();
        ChatModelRequestContext requestContext = new ChatModelRequestContext(modelListenerRequest, attributes);
        listeners.forEach(listener -> {
            try {
                listener.onRequest(requestContext);
            } catch (Exception e) {
                logger.warn("Exception while calling model listener", e);
            }
        });

        // Sync version
        if(client != null) {
            syncCall(toolThatMustBeExecuted, handler, options, responseBuilder, requestContext);
        } else if(asyncClient != null) {
            asyncCall(toolThatMustBeExecuted, handler, options, responseBuilder, requestContext);
        }
    }

    private void handleResponseException(Throwable throwable, StreamingResponseHandler<AiMessage> handler) {
        if (throwable instanceof HttpResponseException) {
            HttpResponseException httpResponseException = (HttpResponseException) throwable;
            logger.info("Error generating response, {}", httpResponseException.getValue());
            FinishReason exceptionFinishReason = contentFilterManagement(httpResponseException, "content_filter");
            LlmResponse<AiMessage> response =  LlmResponse.from(
                    aiMessage(httpResponseException.getMessage()),
                    null,
                    exceptionFinishReason
            );
            handler.onComplete(response);
        } else {
            handler.onError(throwable);
        }
    }

    private void asyncCall(ToolSpecification toolThatMustBeExecuted, StreamingResponseHandler<AiMessage> handler,
                           ChatCompletionsOptions options, AzureAiStreamingResponseBuilder responseBuilder,
                           ChatModelRequestContext requestContext) {
        Flux<ChatCompletions> chatCompletionsStream = asyncClient.getChatCompletionsStream(deploymentName, options);

        AtomicReference<String> responseId = new AtomicReference<>();
        chatCompletionsStream.subscribe(chatCompletion -> {
                    responseBuilder.append(chatCompletion);
                    handle(chatCompletion, handler);

                    if (isNotNullOrBlank(chatCompletion.getId())) {
                        responseId.set(chatCompletion.getId());
                    }
                },
                throwable -> {
                    ChatModelErrorContext errorContext = new ChatModelErrorContext(
                            throwable,
                            requestContext.request(),
                            null,
                            requestContext.attributes()
                    );

                    listeners.forEach(listener -> {
                        try {
                            listener.onError(errorContext);
                        } catch (Exception e2) {
                            logger.warn("Exception while calling model listener", e2);
                        }
                    });
                    handleResponseException(throwable, handler);
                },
                () -> {
                    LlmResponse<AiMessage> response = responseBuilder.build(tokenizer, toolThatMustBeExecuted != null);
                    ChatModelResponse modelListenerResponse = createModelListenerResponse(
                            responseId.get(),
                            options.getModel(),
                            response
                    );
                    ChatModelResponseContext responseContext = new ChatModelResponseContext(
                            modelListenerResponse,
                            requestContext.request(),
                            requestContext.attributes()
                    );
                    listeners.forEach(listener -> {
                        try {
                            listener.onResponse(responseContext);
                        } catch (Exception e) {
                            logger.warn("Exception while calling model listener", e);
                        }
                    });
                    handler.onComplete(response);
                });
    }

    private void syncCall(ToolSpecification toolThatMustBeExecuted,
                          StreamingResponseHandler<AiMessage> handler,
                          ChatCompletionsOptions options, AzureAiStreamingResponseBuilder responseBuilder,
                          ChatModelRequestContext requestContext) {
        try {
            AtomicReference<String> responseId = new AtomicReference<>();

            client.getChatCompletionsStream(deploymentName, options)
                    .stream()
                    .forEach(chatCompletions -> {
                        responseBuilder.append(chatCompletions);
                        handle(chatCompletions, handler);

                        if (isNotNullOrBlank(chatCompletions.getId())) {
                            responseId.set(chatCompletions.getId());
                        }
                    });
            LlmResponse<AiMessage> response = responseBuilder.build(tokenizer, toolThatMustBeExecuted != null);
            ChatModelResponse modelListenerResponse = createModelListenerResponse(
                    responseId.get(),
                    options.getModel(),
                    response
            );
            ChatModelResponseContext responseContext = new ChatModelResponseContext(
                    modelListenerResponse,
                    requestContext.request(),
                    requestContext.attributes()
            );
            listeners.forEach(listener -> {
                try {
                    listener.onResponse(responseContext);
                } catch (Exception e) {
                    logger.warn("Exception while calling model listener", e);
                }
            });

            handler.onComplete(response);
        } catch (Exception exception) {
            handleResponseException(exception, handler);

            ChatModelErrorContext errorContext = new ChatModelErrorContext(
                    exception,
                    requestContext.request(),
                    null,
                    requestContext.attributes()
            );

            listeners.forEach(listener -> {
                try {
                    listener.onError(errorContext);
                } catch (Exception e2) {
                    logger.warn("Exception while calling model listener", e2);
                }
            });
        }
    }


    private static void handle(ChatCompletions chatCompletions,
                               StreamingResponseHandler<AiMessage> handler) {

        List<ChatChoice> choices = chatCompletions.getChoices();
        if (choices == null || choices.isEmpty()) {
            return;
        }
        com.azure.ai.openai.models.ChatResponseMessage delta = choices.get(0).getDelta();
        String content = delta.getContent();
        if (content != null) {
            handler.onNext(content);
        }
    }

    @Override
    public int estimateTokenCount(List<ChatMessage> messages) {
        return tokenizer.estimateTokenCountInMessages(messages);
    }

    public static Builder builder() {
        for (AzureAiStreamingChatModelBuilderFactory factory : loadFactories(AzureAiStreamingChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new Builder();
    }

    public static class Builder {

        private String endpoint;
        private String serviceVersion;
        private String apiKey;
        private KeyCredential keyCredential;
        private TokenCredential tokenCredential;
        private String deploymentName;
        private Tokenizer tokenizer;
        private Integer maxTokens;
        private Double temperature;
        private Double topP;
        private Map<String, Integer> logitBias;
        private String user;
        private Integer n;
        private List<String> stop;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private Duration timeout;
        List<AzureChatExtensionConfiguration> dataSources;
        AzureChatEnhancementConfiguration enhancements;
        Long seed;
        ChatCompletionsResponseFormat responseFormat;
        private Integer maxRetries;
        private ProxyOptions proxyOptions;
        private boolean logRequestsAndResponses;
        private OpenAIClient openAIClient;
        private OpenAIAsyncClient openAIAsyncClient;
        private boolean useAsyncClient = true;
        private String userAgentSuffix;
        private List<ChatModelListener> listeners;

        /**
         * Sets the Azure OpenAI endpoint. This is a mandatory parameter.
         *
         * @param endpoint The Azure OpenAI endpoint in the format: https://{resource}.openai.azure.com/
         * @return builder
         */
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * Sets the Azure OpenAI API service version. This is a mandatory parameter.
         *
         * @param serviceVersion The Azure OpenAI API service version in the format: 2023-05-15
         * @return builder
         */
        public Builder serviceVersion(String serviceVersion) {
            this.serviceVersion = serviceVersion;
            return this;
        }

        /**
         * Sets the Azure OpenAI API key.
         *
         * @param apiKey The Azure OpenAI API key.
         * @return builder
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Used to authenticate with the OpenAI service, instead of Azure OpenAI.
         * This automatically sets the endpoint to https://api.openai.com/v1.
         *
         * @param nonAzureApiKey The non-Azure OpenAI API key
         * @return builder
         */
        public Builder nonAzureApiKey(String nonAzureApiKey) {
            this.keyCredential = new KeyCredential(nonAzureApiKey);
            this.endpoint = "https://api.openai.com/v1";
            return this;
        }

        /**
         * Used to authenticate to Azure OpenAI with Azure Active Directory credentials.
         * @param tokenCredential the credentials to authenticate with Azure Active Directory
         * @return builder
         */
        public Builder tokenCredential(TokenCredential tokenCredential) {
            this.tokenCredential = tokenCredential;
            return this;
        }

        /**
         * Sets the deployment name in Azure OpenAI. This is a mandatory parameter.
         *
         * @param deploymentName The Deployment name.
         * @return builder
         */
        public Builder deploymentName(String deploymentName) {
            this.deploymentName = deploymentName;
            return this;
        }

        public Builder tokenizer(Tokenizer tokenizer) {
            this.tokenizer = tokenizer;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        public Builder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder dataSources(List<AzureChatExtensionConfiguration> dataSources) {
            this.dataSources = dataSources;
            return this;
        }

        public Builder enhancements(AzureChatEnhancementConfiguration enhancements) {
            this.enhancements = enhancements;
            return this;
        }

        public Builder seed(Long seed) {
            this.seed = seed;
            return this;
        }

        public Builder responseFormat(ChatCompletionsResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder proxyOptions(ProxyOptions proxyOptions) {
            this.proxyOptions = proxyOptions;
            return this;
        }

        public Builder logRequestsAndResponses(boolean logRequestsAndResponses) {
            this.logRequestsAndResponses = logRequestsAndResponses;
            return this;
        }

        /**
         * Sets the Azure OpenAI client. This is an optional parameter, if you need more flexibility than using the endpoint, serviceVersion, apiKey, deploymentName parameters.
         *
         * @param openAIAsyncClient The Azure OpenAI client.
         * @return builder
         */
        public Builder openAIAsyncClient(OpenAIAsyncClient openAIAsyncClient) {
            this.openAIAsyncClient = openAIAsyncClient;
            return this;
        }

        public Builder userAgentSuffix(String userAgentSuffix) {
            this.userAgentSuffix = userAgentSuffix;
            return this;
        }

        public Builder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public AzureAiStreamingChatModel build() {
            if (openAIClient == null) {
                if (tokenCredential != null) {
                    return new AzureAiStreamingChatModel(
                            endpoint,
                            serviceVersion,
                            tokenCredential,
                            deploymentName,
                            tokenizer,
                            maxTokens,
                            temperature,
                            topP,
                            logitBias,
                            user,
                            n,
                            stop,
                            presencePenalty,
                            frequencyPenalty,
                            dataSources,
                            enhancements,
                            seed,
                            responseFormat,
                            timeout,
                            maxRetries,
                            proxyOptions,
                            logRequestsAndResponses,
                            useAsyncClient,
                            listeners,
                            userAgentSuffix
                    );
                } else if (keyCredential != null) {
                    return new AzureAiStreamingChatModel(
                            endpoint,
                            serviceVersion,
                            keyCredential,
                            deploymentName,
                            tokenizer,
                            maxTokens,
                            temperature,
                            topP,
                            logitBias,
                            user,
                            n,
                            stop,
                            presencePenalty,
                            frequencyPenalty,
                            dataSources,
                            enhancements,
                            seed,
                            responseFormat,
                            timeout,
                            maxRetries,
                            proxyOptions,
                            logRequestsAndResponses,
                            useAsyncClient,
                            listeners,
                            userAgentSuffix
                    );
                }
                return new AzureAiStreamingChatModel(
                        endpoint,
                        serviceVersion,
                        apiKey,
                        deploymentName,
                        tokenizer,
                        maxTokens,
                        temperature,
                        topP,
                        logitBias,
                        user,
                        n,
                        stop,
                        presencePenalty,
                        frequencyPenalty,
                        dataSources,
                        enhancements,
                        seed,
                        responseFormat,
                        timeout,
                        maxRetries,
                        proxyOptions,
                        logRequestsAndResponses,
                        useAsyncClient,
                        listeners,
                        userAgentSuffix
                );
            } else {
                return new AzureAiStreamingChatModel(
                        openAIClient,
                        openAIAsyncClient,
                        deploymentName,
                        tokenizer,
                        maxTokens,
                        temperature,
                        topP,
                        logitBias,
                        user,
                        n,
                        stop,
                        presencePenalty,
                        frequencyPenalty,
                        dataSources,
                        enhancements,
                        seed,
                        responseFormat,
                        listeners
                );
            }
        }
    }
}
