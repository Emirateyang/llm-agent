package com.llmagent.azure;

import com.azure.ai.openai.models.*;
import com.llmagent.data.message.AiMessage;
import com.llmagent.llm.chat.TokenCountEstimator;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.llm.tool.ToolRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.llmagent.azure.AzureAiHelper.finishReasonFrom;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * This class needs to be thread safe because it is called when a streaming result comes back
 * and there is no guarantee that this thread will be the same as the one that initiated the request,
 * in fact it almost certainly won't be.
 */
public class AzureOpenAiStreamingResponseBuilder {

    private final StringBuffer contentBuilder = new StringBuffer();
    private final StringBuffer toolNameBuilder = new StringBuffer();
    private final StringBuffer toolArgumentsBuilder = new StringBuffer();
    private String toolExecutionsIndex = "call_undefined";
    private final Map<String, ToolRequestBuilder> toolRequestBuilderHashMap = new HashMap<>();
    private volatile CompletionsFinishReason finishReason;

    private final Integer inputTokenCount;

    public AzureOpenAiStreamingResponseBuilder(Integer inputTokenCount) {
        this.inputTokenCount = inputTokenCount;
    }

    public void append(ChatCompletions completions) {
        if (completions == null) {
            return;
        }

        List<ChatChoice> choices = completions.getChoices();
        if (choices == null || choices.isEmpty()) {
            return;
        }

        ChatChoice chatCompletionChoice = choices.get(0);
        if (chatCompletionChoice == null) {
            return;
        }

        CompletionsFinishReason finishReason = chatCompletionChoice.getFinishReason();
        if (finishReason != null) {
            this.finishReason = finishReason;
        }

        com.azure.ai.openai.models.ChatResponseMessage delta = chatCompletionChoice.getDelta();
        if (delta == null) {
            return;
        }

        String content = delta.getContent();
        if (content != null) {
            contentBuilder.append(content);
            return;
        }

        if (delta.getToolCalls() != null && !delta.getToolCalls().isEmpty()) {
            for (ChatCompletionsToolCall toolCall : delta.getToolCalls()) {
                ToolRequestBuilder toolExecutionRequestBuilder;
                if (toolCall.getId() != null) {
                    toolExecutionsIndex = toolCall.getId();
                    toolExecutionRequestBuilder = new ToolRequestBuilder();
                    toolExecutionRequestBuilder.idBuilder.append(toolExecutionsIndex);
                    toolRequestBuilderHashMap.put(toolExecutionsIndex, toolExecutionRequestBuilder);
                } else {
                    toolExecutionRequestBuilder = toolRequestBuilderHashMap.get(toolExecutionsIndex);
                    if (toolExecutionRequestBuilder == null) {
                        throw new IllegalStateException("Function without an id defined in the tool call");
                    }
                }
                if (toolCall instanceof ChatCompletionsFunctionToolCall) {
                    ChatCompletionsFunctionToolCall functionCall = (ChatCompletionsFunctionToolCall) toolCall;
                    if (functionCall.getFunction().getName() != null) {
                        toolExecutionRequestBuilder.nameBuilder.append(functionCall.getFunction().getName());
                    }
                    if (functionCall.getFunction().getArguments() != null) {
                        toolExecutionRequestBuilder.argumentsBuilder.append(functionCall.getFunction().getArguments());
                    }
                }
            }
        }
    }

    public void append(Completions completions) {
        if (completions == null) {
            return;
        }

        List<Choice> choices = completions.getChoices();
        if (choices == null || choices.isEmpty()) {
            return;
        }

        Choice completionChoice = choices.get(0);
        if (completionChoice == null) {
            return;
        }

        CompletionsFinishReason completionsFinishReason = completionChoice.getFinishReason();
        if (completionsFinishReason != null) {
            this.finishReason = completionsFinishReason;
        }

        String token = completionChoice.getText();
        if (token != null) {
            contentBuilder.append(token);
        }
    }

    public LlmResponse<AiMessage> build(TokenCountEstimator tokenCountEstimator) {

        String content = contentBuilder.toString();
        TokenUsage tokenUsage =
                content.isEmpty() ? new TokenUsage(inputTokenCount, 0) : tokenUsage(content, tokenCountEstimator);

        String toolName = toolNameBuilder.toString();
        if (!toolName.isEmpty()) {
            ToolRequest toolExecutionRequest = ToolRequest.builder()
                    .name(toolName)
                    .arguments(toolArgumentsBuilder.toString())
                    .build();
            return LlmResponse.from(
                    !content.isEmpty() ?
                            AiMessage.from(content, singletonList(toolExecutionRequest)) :
                            AiMessage.from(toolExecutionRequest),
                    tokenUsage,
                    finishReasonFrom(finishReason)
            );
        }

        if (!toolRequestBuilderHashMap.isEmpty()) {
            List<ToolRequest> toolExecutionRequests = toolRequestBuilderHashMap.values().stream()
                    .map(it -> ToolRequest.builder()
                            .id(it.idBuilder.toString())
                            .name(it.nameBuilder.toString())
                            .arguments(it.argumentsBuilder.toString())
                            .build())
                    .collect(toList());
            return LlmResponse.from(
                    !content.isEmpty() ?
                            AiMessage.from(content, toolExecutionRequests) :
                            AiMessage.from(toolExecutionRequests),
                    tokenUsage,
                    finishReasonFrom(finishReason)
            );
        }

        if (!content.isEmpty()) {
            return LlmResponse.from(
                    AiMessage.from(content),
                    tokenUsage(content, tokenCountEstimator),
                    finishReasonFrom(finishReason)
            );
        }

        return null;
    }

    private TokenUsage tokenUsage(String content, TokenCountEstimator tokenCountEstimator) {
        if (tokenCountEstimator == null) {
            return null;
        }
        int outputTokenCount = tokenCountEstimator.estimateTokenCountInText(content);
        return new TokenUsage(inputTokenCount, outputTokenCount);
    }

    private static class ToolRequestBuilder {

        private final StringBuffer idBuilder = new StringBuffer();
        private final StringBuffer nameBuilder = new StringBuffer();
        private final StringBuffer argumentsBuilder = new StringBuffer();
    }
}
