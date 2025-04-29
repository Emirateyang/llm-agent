package com.llmagent.openai;

import com.llmagent.data.image.Image;
import com.llmagent.data.message.*;
import com.llmagent.data.message.Content;
import com.llmagent.data.message.SystemMessage;
import com.llmagent.data.message.ToolMessage;
import com.llmagent.data.message.UserMessage;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.chat.request.ToolChoice;
import com.llmagent.llm.chat.request.ChatRequestParameters;
import com.llmagent.llm.chat.request.json.JsonObjectSchema;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.llm.chat.response.ChatResponseMetadata;
import com.llmagent.llm.tool.*;
import com.llmagent.openai.chat.*;
import com.llmagent.llm.output.FinishReason;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.openai.chat.AssistantMessage;
import com.llmagent.openai.chat.ChatCompletionResponse;
import com.llmagent.openai.chat.ContentType;
import com.llmagent.openai.image.ImageDetail;
import com.llmagent.openai.image.ImageUrl;
import com.llmagent.openai.token.Usage;
import com.llmagent.openai.tool.*;
import com.llmagent.openai.tool.Tool;

import java.util.*;

import static com.llmagent.exception.Exceptions.illegalArgument;
import static com.llmagent.llm.chat.request.json.JsonSchemaElementHelper.toMap;
import static com.llmagent.llm.chat.response.ResponseFormat.JSON;
import static com.llmagent.openai.ResponseFormatType.JSON_OBJECT;
import static com.llmagent.openai.ResponseFormatType.JSON_SCHEMA;
import static java.util.stream.Collectors.toList;

/**
 * 将 LLM 消息转换为 OpenAI 消息
 */
public class OpenAiHelper {
    static final String OPENAI_URL = "https://api.openai.com/v1";

    static final String DEFAULT_USER_AGENT = "llm-agent-openai";

    public static List<Message> toOpenAiMessages(List<ChatMessage> messages) {
        return messages.stream()
                .map(OpenAiHelper::toOpenAiMessage)
                .collect(toList());
    }

    public static Message toOpenAiMessage(ChatMessage message) {
        if (message instanceof SystemMessage) {
            return com.llmagent.openai.chat.SystemMessage.from(((SystemMessage) message).content());
        }

        if (message instanceof UserMessage userMessage) {
            if (userMessage.hasSingleText()) {
                return com.llmagent.openai.chat.UserMessage.builder()
                        .content(userMessage.singleText())
                        .name(userMessage.name())
                        .build();
            } else {
                return com.llmagent.openai.chat.UserMessage.builder()
                        .content(userMessage.contents().stream()
                                .map(OpenAiHelper::toOpenAiContent)
                                .collect(toList()))
                        .name(userMessage.name())
                        .build();
            }
        }

        if (message instanceof AiMessage aiMessage) {
            if (!aiMessage.hasToolRequests()) {
                return AssistantMessage.from(aiMessage.content());
            }

            List<ToolCall> toolCalls = aiMessage.toolRequests().stream()
                    .map(it -> ToolCall.builder()
                            .id(it.id())
                            .type(ToolType.FUNCTION)
                            .function(FunctionCall.builder()
                                    .name(it.name())
                                    .arguments(it.arguments())
                                    .build())
                            .build())
                    .collect(toList());

            return AssistantMessage.builder()
                    .toolCalls(toolCalls)
                    .build();
        }

        if (message instanceof ToolMessage toolMessage) {
            return com.llmagent.openai.chat.ToolMessage.from(toolMessage.id(), toolMessage.content());
        }

        throw illegalArgument("Unknown message type: " + message.type());
    }

    private static com.llmagent.openai.chat.Content toOpenAiContent(Content content) {
        if (content instanceof TextContent) {
            return toOpenAiContent((TextContent) content);
        } else if (content instanceof ImageContent) {
            return toOpenAiContent((ImageContent) content);
        } else {
            throw illegalArgument("Unknown content type: " + content);
        }
    }

    private static com.llmagent.openai.chat.Content toOpenAiContent(TextContent content) {
        return com.llmagent.openai.chat.Content.builder()
                .type(ContentType.TEXT)
                .text(content.text())
                .build();
    }

    private static com.llmagent.openai.chat.Content toOpenAiContent(ImageContent content) {
        return com.llmagent.openai.chat.Content.builder()
                .type(ContentType.IMAGE_URL)
                .imageUrl(ImageUrl.builder()
                        .url(toUrl(content.image()))
                        .detail(toDetail(content.detailLevel()))
                        .build())
                .build();
    }

    private static String toUrl(Image image) {
        if (image.url() != null) {
            return image.url().toString();
        }
        return String.format("data:%s;base64,%s", image.mimeType(), image.base64Data());
    }

    private static ImageDetail toDetail(ImageContent.DetailLevel detailLevel) {
        if (detailLevel == null) {
            return null;
        }
        return ImageDetail.valueOf(detailLevel.name());
    }

    public static List<Tool> toTools(Collection<ToolSpecification> toolSpecifications, boolean strict) {
        if (toolSpecifications == null) {
            return null;
        }
        return toolSpecifications.stream()
                .map(a -> toTool(a, strict))
                .collect(toList());
    }

    private static Tool toTool(ToolSpecification toolSpecification, boolean strict) {
        Function function = Function.builder()
                .name(toolSpecification.name())
                .description(toolSpecification.description())
                .parameters(toOpenAiParameters(toolSpecification.parameters(), strict))
                .build();
        return Tool.from(function);
    }

    private static Map<String, Object> toOpenAiParameters(JsonObjectSchema parameters, boolean strict) {
        if (parameters != null) {
            return toMap(parameters, strict);
        } else {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "object");
            map.put("properties", new HashMap<>());
            map.put("required", new ArrayList<>());
            if (strict) {
                // When strict, additionalProperties must be false:
                // See https://platform.openai.com/docs/guides/structured-outputs/additionalproperties-false-must-always-be-set-in-objects?api-mode=chat#additionalproperties-false-must-always-be-set-in-objects
                map.put("additionalProperties", false);
            }
            return map;
        }
    }

    public static AiMessage aiMessageFrom(ChatCompletionResponse response) {
        AssistantMessage assistantMessage = response.choices().get(0).message();

        List<ToolCall> toolCalls = assistantMessage.toolCalls();
        if (toolCalls != null && !toolCalls.isEmpty()) {
            List<ToolRequest> ToolRequests = toolCalls.stream()
                    .filter(toolCall -> toolCall.type() == ToolType.FUNCTION)
                    .map(OpenAiHelper::toToolRequest)
                    .collect(toList());
            return AiMessage.aiMessage(ToolRequests);
        }

        return AiMessage.aiMessage(assistantMessage.content());
    }

    private static ToolRequest toToolRequest(ToolCall toolCall) {
        FunctionCall functionCall = toolCall.function();
        return ToolRequest.builder()
                .id(toolCall.id())
                .name(functionCall.name())
                .arguments(functionCall.arguments())
                .build();
    }

    public static TokenUsage tokenUsageFrom(Usage openAiUsage) {
        if (openAiUsage == null) {
            return null;
        }
        return new TokenUsage(
                openAiUsage.promptTokens(),
                openAiUsage.completionTokens(),
                openAiUsage.totalTokens()
        );
    }

    public static FinishReason finishReasonFrom(String openAiFinishReason) {
        if (openAiFinishReason == null) {
            return null;
        }
        switch (openAiFinishReason) {
            case "stop":
                return FinishReason.STOP;
            case "length":
                return FinishReason.LENGTH;
            case "tool_calls":
            case "function_call":
                return FinishReason.TOOL_EXECUTION;
            case "content_filter":
                return FinishReason.CONTENT_FILTER;
            default:
                return null;
        }
    }

    static boolean isOpenAiModel(String modelName) {
        if (modelName == null) {
            return false;
        }
        for (ChatLanguageModelName openAiChatModelName : ChatLanguageModelName.values()) {
            if (modelName.contains(openAiChatModelName.toString())) {
                return true;
            }
        }
        return false;
    }

    static LlmResponse<AiMessage> removeTokenUsage(LlmResponse<AiMessage> response) {
        return LlmResponse.from(response.content(), null, response.finishReason());
    }

    static ChatRequest createModelListenerRequest(ChatCompletionRequest request,
                                                  List<ChatMessage> messages,
                                                  List<ToolSpecification> toolSpecifications) {
        return ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder()
                .modelName(request.model())
                .temperature(request.temperature())
                .topP(request.topP())
                .maxOutputTokens(request.maxTokens())
                .toolSpecifications(toolSpecifications).build()
                ).build();
    }

    static ChatResponse createModelListenerResponse(String responseId,
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
                        .finishReason(response.finishReason()).build()).build();
    }

    public static ToolChoiceMode toOpenAiToolChoice(ToolChoice toolChoice) {
        if (toolChoice == null) {
            return null;
        }

        return switch (toolChoice) {
            case AUTO -> ToolChoiceMode.AUTO;
            case REQUIRED -> ToolChoiceMode.REQUIRED;
        };
    }

    static ResponseFormat toOpenAiResponseFormat(com.llmagent.llm.chat.response.ResponseFormat responseFormat, Boolean strict) {
        if (responseFormat == null || responseFormat.type() == com.llmagent.llm.chat.response.ResponseFormatType.TEXT) {
            return null;
        }

        com.llmagent.llm.chat.request.json.JsonSchema jsonSchema = responseFormat.jsonSchema();
        if (jsonSchema == null) {
            return ResponseFormat.builder()
                    .type(JSON_OBJECT)
                    .build();
        } else {
            if (!(jsonSchema.rootElement() instanceof JsonObjectSchema)) {
                throw new IllegalArgumentException(
                        "For OpenAI, the root element of the JSON Schema must be a JsonObjectSchema, but it was: "
                                + jsonSchema.rootElement().getClass());
            }
            com.llmagent.openai.json.JsonSchema openAiJsonSchema = com.llmagent.openai.json.JsonSchema.builder()
                    .name(jsonSchema.name())
                    .strict(strict)
                    .schema(toMap(jsonSchema.rootElement(), strict))
                    .build();
            return ResponseFormat.builder()
                    .type(JSON_SCHEMA)
                    .jsonSchema(openAiJsonSchema)
                    .build();
        }
    }

    static ChatCompletionRequest.Builder toOpenAiChatRequest(
            ChatRequest chatRequest,
            OpenAiChatRequestParameters parameters,
            Boolean strictTools,
            Boolean strictJsonSchema) {
        return ChatCompletionRequest.builder()
                .messages(toOpenAiMessages(chatRequest.messages()))
                // common parameters
                .model(parameters.modelName())
                .temperature(parameters.temperature())
                .topP(parameters.topP())
                .frequencyPenalty(parameters.frequencyPenalty())
                .presencePenalty(parameters.presencePenalty())
                .maxTokens(parameters.maxOutputTokens())
                .stop(parameters.stopSequences())
                .tools(toTools(parameters.toolSpecifications(), strictTools))
                .toolChoice(toOpenAiToolChoice(parameters.toolChoice()))
                .responseFormat(toOpenAiResponseFormat(parameters.responseFormat(), strictJsonSchema))
                // OpenAI specific parameters
                .maxCompletionTokens(parameters.maxCompletionTokens())
                .logitBias(parameters.logitBias())
                .parallelToolCalls(parameters.parallelToolCalls())
                .seed(parameters.seed())
                .user(parameters.user())
                .store(parameters.store())
                .metadata(parameters.metadata())
                .serviceTier(parameters.serviceTier())
                .reasoningEffort(parameters.reasoningEffort());
    }

    static ChatCompletionRequest.Builder toOpenAiChatRequest(
            List<ChatMessage> messages,
            OpenAiChatRequestParameters parameters,
            Boolean strictJsonSchema) {
        return ChatCompletionRequest.builder()
                .messages(toOpenAiMessages(messages))
                // common parameters
                .model(parameters.modelName())
                .temperature(parameters.temperature())
                .topP(parameters.topP())
                .frequencyPenalty(parameters.frequencyPenalty())
                .presencePenalty(parameters.presencePenalty())
                .maxTokens(parameters.maxOutputTokens())
                .stop(parameters.stopSequences())
                .responseFormat(toOpenAiResponseFormat(parameters.responseFormat(), strictJsonSchema))
                // OpenAI specific parameters
                .maxCompletionTokens(parameters.maxCompletionTokens())
                .logitBias(parameters.logitBias())
                .parallelToolCalls(parameters.parallelToolCalls())
                .seed(parameters.seed())
                .user(parameters.user())
                .store(parameters.store())
                .metadata(parameters.metadata())
                .serviceTier(parameters.serviceTier())
                .reasoningEffort(parameters.reasoningEffort());
    }

    public static com.llmagent.llm.chat.response.ResponseFormat fromOpenAiResponseFormat(String responseFormat) {
        if ("json_object".equals(responseFormat)) {
            return JSON;
        } else {
            return null;
        }
    }
}
