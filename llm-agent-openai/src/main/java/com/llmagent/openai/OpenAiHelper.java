package com.llmagent.openai;

import com.llmagent.data.image.Image;
import com.llmagent.data.message.*;
import com.llmagent.data.message.Content;
import com.llmagent.data.message.SystemMessage;
import com.llmagent.data.message.ToolMessage;
import com.llmagent.data.message.UserMessage;
import com.llmagent.llm.tool.*;
import com.llmagent.openai.chat.*;
import com.llmagent.llm.chat.listener.ChatModelRequest;
import com.llmagent.llm.chat.listener.ChatModelResponse;
import com.llmagent.llm.output.FinishReason;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.openai.chat.AssistantMessage;
import com.llmagent.openai.chat.ChatCompletionResponse;
import com.llmagent.openai.chat.ContentType;
import com.llmagent.openai.image.ImageDetail;
import com.llmagent.openai.image.ImageUrl;
import com.llmagent.openai.tool.Function;
import com.llmagent.openai.tool.FunctionCall;
import com.llmagent.openai.tool.Tool;
import com.llmagent.openai.tool.ToolCall;

import java.util.Collection;
import java.util.List;

import static com.llmagent.exception.Exceptions.illegalArgument;
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

    public static List<Tool> toTools(Collection<ToolSpecification> toolSpecifications) {
        return toolSpecifications.stream()
                .map(OpenAiHelper::toTool)
                .collect(toList());
    }

    private static Tool toTool(ToolSpecification toolSpecification) {
        Function function = Function.builder()
                .name(toolSpecification.name())
                .description(toolSpecification.description())
                .parameters(toolSpecification.parameters())
                .build();
        return Tool.from(function);
    }

//    private static com.llmagent.openai.chat.Parameters toOpenAiParameters(ToolParameters toolParameters) {
//        if (toolParameters == null) {
//            return com.llmagent.openai.chat.Parameters.builder().build();
//        }
//        return com.llmagent.openai.chat.Parameters.builder()
//                .properties(toolParameters.properties())
//                .required(toolParameters.required())
//                .build();
//    }

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
        for (ChatCompletionModel openAiChatModelName : ChatCompletionModel.values()) {
            if (modelName.contains(openAiChatModelName.toString())) {
                return true;
            }
        }
        return false;
    }

    static LlmResponse<AiMessage> removeTokenUsage(LlmResponse<AiMessage> response) {
        return LlmResponse.from(response.content(), null, response.finishReason());
    }

    static ChatModelRequest createModelListenerRequest(ChatCompletionRequest request,
                                                       List<ChatMessage> messages,
                                                       List<ToolSpecification> toolSpecifications) {
        return ChatModelRequest.builder()
                .model(request.model())
                .temperature(request.temperature())
                .topP(request.topP())
                .maxTokens(request.maxTokens())
                .messages(messages)
                .toolSpecifications(toolSpecifications)
                .build();
    }

    static ChatModelResponse createModelListenerResponse(String responseId,
                                                         String responseModel,
                                                         LlmResponse<AiMessage> response) {
        if (response == null) {
            return null;
        }

        return ChatModelResponse.builder()
                .id(responseId)
                .model(responseModel)
                .tokenUsage(response.tokenUsage())
                .finishReason(response.finishReason())
                .aiMessage(response.content())
                .build();
    }
}
