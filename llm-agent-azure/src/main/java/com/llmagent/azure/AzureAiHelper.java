package com.llmagent.azure;

import com.azure.ai.openai.*;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.KeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientProvider;
import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Header;
import com.azure.core.util.HttpClientOptions;
import com.llmagent.data.image.Image;
import com.llmagent.data.message.*;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.chat.request.ChatRequestParameters;
import com.llmagent.llm.chat.request.json.JsonObjectSchema;
import com.llmagent.llm.chat.request.json.JsonSchema;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.llm.chat.response.ChatResponseMetadata;
import com.llmagent.llm.chat.response.ResponseFormat;
import com.llmagent.llm.chat.response.ResponseFormatType;
import com.llmagent.llm.output.FinishReason;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.llm.tool.ToolRequest;
import com.llmagent.llm.tool.ToolSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.*;

import static com.llmagent.data.message.AiMessage.aiMessage;
import static com.llmagent.llm.chat.request.json.JsonSchemaElementHelper.toMap;
import static com.llmagent.llm.output.FinishReason.*;
import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.ObjectUtil.isNullOrEmpty;
import static com.llmagent.util.StringUtil.isNullOrBlank;
import static com.llmagent.util.ValidationUtil.ensureNotBlank;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

public class AzureAiHelper {
    private static final Logger logger = LoggerFactory.getLogger(AzureAiHelper.class);

    public static final String DEFAULT_USER_AGENT = "langchain4j-azure-openai";

    public static OpenAIClient setupSyncClient(String endpoint, String serviceVersion, Object credential, Duration timeout, Integer maxRetries, ProxyOptions proxyOptions, boolean logRequestsAndResponses, String userAgentSuffix, Map<String, String> customHeaders) {
        OpenAIClientBuilder openAIClientBuilder = setupOpenAIClientBuilder(endpoint, serviceVersion, credential, timeout, maxRetries, proxyOptions, logRequestsAndResponses, userAgentSuffix, customHeaders);
        return openAIClientBuilder.buildClient();
    }

    public static OpenAIAsyncClient setupAsyncClient(String endpoint, String serviceVersion, Object credential, Duration timeout, Integer maxRetries, ProxyOptions proxyOptions, boolean logRequestsAndResponses, String userAgentSuffix, Map<String, String> customHeaders) {
        OpenAIClientBuilder openAIClientBuilder = setupOpenAIClientBuilder(endpoint, serviceVersion, credential, timeout, maxRetries, proxyOptions, logRequestsAndResponses, userAgentSuffix, customHeaders);
        return openAIClientBuilder.buildAsyncClient();
    }

    private static OpenAIClientBuilder setupOpenAIClientBuilder(String endpoint, String serviceVersion, Object credential, Duration timeout, Integer maxRetries, ProxyOptions proxyOptions, boolean logRequestsAndResponses, String userAgentSuffix, Map<String, String> customHeaders) {
        timeout = getOrDefault(timeout, ofSeconds(60));
        HttpClientOptions clientOptions = new HttpClientOptions();
        clientOptions.setConnectTimeout(timeout);
        clientOptions.setResponseTimeout(timeout);
        clientOptions.setReadTimeout(timeout);
        clientOptions.setWriteTimeout(timeout);
        clientOptions.setProxyOptions(proxyOptions);

        String userAgent = DEFAULT_USER_AGENT;
        if (userAgentSuffix != null && !userAgentSuffix.isEmpty()) {
            userAgent = DEFAULT_USER_AGENT + "-" + userAgentSuffix;
        }
        List<Header> headers = new ArrayList<>();
        headers.add(new Header("User-Agent", userAgent));
        if (customHeaders != null) {
            customHeaders.forEach((name, value) -> headers.add(new Header(name, value)));
        }
        clientOptions.setHeaders(headers);
        HttpClient httpClient = new NettyAsyncHttpClientProvider().createInstance(clientOptions);

        HttpLogOptions httpLogOptions = new HttpLogOptions();
        if (logRequestsAndResponses) {
            httpLogOptions.setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS);
        }

        maxRetries = getOrDefault(maxRetries, 2);
        ExponentialBackoffOptions exponentialBackoffOptions = new ExponentialBackoffOptions();
        exponentialBackoffOptions.setMaxRetries(maxRetries);
        RetryOptions retryOptions = new RetryOptions(exponentialBackoffOptions);

        OpenAIClientBuilder openAIClientBuilder = new OpenAIClientBuilder()
                .endpoint(ensureNotBlank(endpoint, "endpoint"))
                .serviceVersion(getOpenAIServiceVersion(serviceVersion))
                .httpClient(httpClient)
                .clientOptions(clientOptions)
                .httpLogOptions(httpLogOptions)
                .retryOptions(retryOptions);

        if (credential instanceof String) {
            openAIClientBuilder.credential(new AzureKeyCredential((String) credential));
        } else if (credential instanceof KeyCredential) {
            openAIClientBuilder.credential((KeyCredential) credential);
        } else if (credential instanceof TokenCredential) {
            openAIClientBuilder.credential((TokenCredential) credential);
        } else {
            throw new IllegalArgumentException("Unsupported credential type: " + credential.getClass());
        }

        return openAIClientBuilder;

    }

    private static OpenAIClientBuilder authenticate(TokenCredential tokenCredential) {
        return new OpenAIClientBuilder()
                .credential(tokenCredential);
    }

    public static OpenAIServiceVersion getOpenAIServiceVersion(String serviceVersion) {
        for (OpenAIServiceVersion version : OpenAIServiceVersion.values()) {
            if (version.getVersion().equals(serviceVersion)) {
                return version;
            }
        }
        return OpenAIServiceVersion.getLatest();
    }

    public static List<ChatRequestMessage> toOpenAiMessages(List<ChatMessage> messages) {

        return messages.stream()
                .map(AzureAiHelper::toOpenAiMessage)
                .collect(toList());
    }

    public static ChatRequestMessage toOpenAiMessage(ChatMessage message) {
        if (message instanceof AiMessage) {
            AiMessage aiMessage = (AiMessage) message;
            ChatRequestAssistantMessage chatRequestAssistantMessage = new ChatRequestAssistantMessage(getOrDefault(aiMessage.content(), ""));
            chatRequestAssistantMessage.setToolCalls(toolExecutionRequestsFrom(message));
            return chatRequestAssistantMessage;
        } else if (message instanceof ToolMessage) {
            ToolMessage toolMessage = (ToolMessage) message;
            return new ChatRequestToolMessage(toolMessage.content(), toolMessage.id());
        } else if (message instanceof SystemMessage) {
            SystemMessage systemMessage = (SystemMessage) message;
            return new ChatRequestSystemMessage(systemMessage.content());
        } else if (message instanceof UserMessage) {
            UserMessage userMessage = (UserMessage) message;
            ChatRequestUserMessage chatRequestUserMessage;
            if (userMessage.hasSingleText()) {
                chatRequestUserMessage = new ChatRequestUserMessage(((TextContent) userMessage.contents().get(0)).text());
            } else {
                chatRequestUserMessage = new ChatRequestUserMessage(userMessage.contents().stream()
                        .map(content -> {
                            if (content instanceof TextContent) {
                                String text = ((TextContent) content).text();
                                return new ChatMessageTextContentItem(text);
                            } else if (content instanceof ImageContent) {
                                ImageContent imageContent = (ImageContent) content;
                                if (imageContent.image().url() == null) {
                                    throw new IllegalArgumentException("Image URL is not present. Base64 encoded images are not supported at the moment.");
                                }
                                ChatMessageImageUrl imageUrl = new ChatMessageImageUrl(imageContent.image().url().toString());
                                return new ChatMessageImageContentItem(imageUrl);
                            } else {
                                throw new IllegalArgumentException("Unsupported content type: " + content.type());
                            }
                        })
                        .collect(toList()));
            }
            chatRequestUserMessage.setName(nameFrom(message));
            return chatRequestUserMessage;
        } else {
            throw new IllegalArgumentException("Unsupported message type: " + message.type());
        }
    }

    private static String nameFrom(ChatMessage message) {
        if (message instanceof UserMessage) {
            return ((UserMessage) message).name();
        }

        if (message instanceof ToolMessage) {
            return ((ToolMessage) message).toolName();
        }

        return null;
    }

    private static List<ChatCompletionsToolCall> toolExecutionRequestsFrom(ChatMessage message) {
        if (message instanceof AiMessage) {
            AiMessage aiMessage = (AiMessage) message;
            if (aiMessage.hasToolRequests()) {
                return aiMessage.toolRequests().stream()
                        .map(toolExecutionRequest -> new ChatCompletionsFunctionToolCall(toolExecutionRequest.id(), new FunctionCall(toolExecutionRequest.name(), toolExecutionRequest.arguments())))
                        .collect(toList());

            }
        }
        return null;
    }

    public static List<ChatCompletionsToolDefinition> toToolDefinitions(Collection<ToolSpecification> toolSpecifications) {
        return toolSpecifications.stream()
                .map(AzureAiHelper::toToolDefinition)
                .collect(toList());
    }

    private static ChatCompletionsToolDefinition toToolDefinition(ToolSpecification toolSpecification) {
        ChatCompletionsFunctionToolDefinitionFunction functionDefinition = new ChatCompletionsFunctionToolDefinitionFunction(toolSpecification.name());
        functionDefinition.setDescription(toolSpecification.description());
        functionDefinition.setParameters(getParameters(toolSpecification));
        return new ChatCompletionsFunctionToolDefinition(functionDefinition);
    }

    public static ChatCompletionsToolSelection toToolChoice(ToolSpecification toolThatMustBeExecuted) {
        FunctionCall functionCall = new FunctionCall(toolThatMustBeExecuted.name(), getParameters(toolThatMustBeExecuted).toString());
        ChatCompletionsToolCall toolToCall = new ChatCompletionsFunctionToolCall(toolThatMustBeExecuted.name(), functionCall);
        return ChatCompletionsToolSelection.fromBinaryData(BinaryData.fromObject(toolToCall));
    }

    private static BinaryData getParameters(ToolSpecification toolSpecification) {
        return toOpenAiParameters(toolSpecification.parameters());
    }

    private static final Map<String, Object> NO_PARAMETER_DATA = new HashMap<>();

    static {
        NO_PARAMETER_DATA.put("type", "object");
        NO_PARAMETER_DATA.put("properties", new HashMap<>());
    }

    private static BinaryData toOpenAiParameters(JsonObjectSchema toolParameters) {
        Parameters parameters = new Parameters();
        if (toolParameters == null) {
            return BinaryData.fromObject(NO_PARAMETER_DATA);
        }
        parameters.setProperties(toMap(toolParameters.properties()));
        parameters.setRequired(toolParameters.required());
        return BinaryData.fromObject(parameters);
    }

    private static class Parameters {

        private final String type = "object";

        private Map<String, Map<String, Object>> properties = new HashMap<>();

        private List<String> required = new ArrayList<>();

        public String getType() {
            return this.type;
        }

        public Map<String, Map<String, Object>> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Map<String, Object>> properties) {
            this.properties = properties;
        }

        public List<String> getRequired() {
            return required;
        }

        public void setRequired(List<String> required) {
            this.required = required;
        }
    }

    public static AiMessage aiMessageFrom(ChatResponseMessage chatResponseMessage) {
        String text = chatResponseMessage.getContent();

        if (isNullOrEmpty(chatResponseMessage.getToolCalls())) {
            return aiMessage(text);
        } else {
            List<ToolRequest> toolRequests = chatResponseMessage.getToolCalls()
                    .stream()
                    .filter(toolCall -> toolCall instanceof ChatCompletionsFunctionToolCall)
                    .map(toolCall -> (ChatCompletionsFunctionToolCall) toolCall)
                    .map(chatCompletionsFunctionToolCall ->
                            ToolRequest.builder()
                                    .id(chatCompletionsFunctionToolCall.getId())
                                    .name(chatCompletionsFunctionToolCall.getFunction().getName())
                                    .arguments(chatCompletionsFunctionToolCall.getFunction().getArguments())
                                    .build())
                    .collect(toList());

            return isNullOrBlank(text) ?
                    aiMessage(toolRequests) :
                    aiMessage(text, toolRequests);
        }
    }

    public static Image imageFrom(ImageGenerationData imageGenerationData) {
        Image.Builder imageBuilder = Image.builder()
                .revisedPrompt(imageGenerationData.getRevisedPrompt());

        String urlString = imageGenerationData.getUrl();
        String imageData = imageGenerationData.getBase64Data();
        if (urlString != null) {
            try {
                URI uri = new URI(urlString);
                imageBuilder.url(uri);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else if (imageData != null) {
            imageBuilder.base64Data(imageData);
        }

        return imageBuilder.build();
    }

    public static TokenUsage tokenUsageFrom(CompletionsUsage openAiUsage) {
        if (openAiUsage == null) {
            return null;
        }
        return new TokenUsage(
                openAiUsage.getPromptTokens(),
                openAiUsage.getCompletionTokens(),
                openAiUsage.getTotalTokens()
        );
    }

    public static FinishReason finishReasonFrom(CompletionsFinishReason openAiFinishReason) {
        if (openAiFinishReason == null) {
            return null;
        } else if (openAiFinishReason == CompletionsFinishReason.STOPPED) {
            return STOP;
        } else if (openAiFinishReason == CompletionsFinishReason.TOKEN_LIMIT_REACHED) {
            return LENGTH;
        } else if (openAiFinishReason == CompletionsFinishReason.CONTENT_FILTERED) {
            return CONTENT_FILTER;
        } else if (openAiFinishReason == CompletionsFinishReason.FUNCTION_CALL) {
            return TOOL_EXECUTION;
        } else {
            return null;
        }
    }

    public static ChatResponse createListenerResponse(String responseId,
                                               String responseModel,
                                               LlmResponse<AiMessage> response) {
        if (response == null) {
            return null;
        }

        return ChatResponse.builder()
                .aiMessage(response.content())
                .metadata(ChatResponseMetadata.builder()
                        .id(responseId)
                        .modelName(responseModel)
                        .tokenUsage(response.tokenUsage())
                        .finishReason(response.finishReason())
                        .build())
                .build();
    }

    public static ChatCompletionsResponseFormat toAzureOpenAiResponseFormat(ResponseFormat responseFormat, boolean strict) {
        if (responseFormat == null || responseFormat.type() == ResponseFormatType.TEXT) {
            return new ChatCompletionsTextResponseFormat();
        } else if (responseFormat.type() != ResponseFormatType.JSON) {
            throw new IllegalArgumentException("Unsupported response format: " + responseFormat);
        }

        JsonSchema jsonSchema = responseFormat.jsonSchema();
        if (jsonSchema == null) {
            return new ChatCompletionsJsonResponseFormat();
        } else {
            if (!(jsonSchema.rootElement() instanceof JsonObjectSchema)) {
                throw new IllegalArgumentException("For Azure OpenAI, the root element of the JSON Schema must be a JsonObjectSchema, but it was: " + jsonSchema.rootElement().getClass());
            }
            ChatCompletionsJsonSchemaResponseFormatJsonSchema schema = new ChatCompletionsJsonSchemaResponseFormatJsonSchema(jsonSchema.name());
            schema.setStrict(strict);
            Map<String, Object> schemaMap = toMap(jsonSchema.rootElement(), strict);
            schema.setSchema(BinaryData.fromObject(schemaMap));
            return new ChatCompletionsJsonSchemaResponseFormat(schema);
        }
    }

    public static ChatRequest createListenerRequest(ChatCompletionsOptions options,
                                             List<ChatMessage> messages,
                                             List<ToolSpecification> toolSpecifications) {
        return ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder()
                        .modelName(options.getModel())
                        .temperature(options.getTemperature())
                        .topP(options.getTopP())
                        .maxOutputTokens(options.getMaxTokens())
                        .toolSpecifications(toolSpecifications)
                        .build())
                .build();
    }
}
