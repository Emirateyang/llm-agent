package com.llmagent.azure;

import com.azure.ai.openai.models.*;
import com.llmagent.data.message.AiMessage;
import com.llmagent.llm.Tokenizer;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.llm.tool.ToolRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.llmagent.azure.AzureAiHelper.finishReasonFrom;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class AzureAiStreamingResponseBuilder {
    private Logger logger = LoggerFactory.getLogger(AzureAiStreamingResponseBuilder.class);

    private final StringBuffer contentBuilder = new StringBuffer();
    private final StringBuffer toolNameBuilder = new StringBuffer();
    private final StringBuffer toolArgumentsBuilder = new StringBuffer();
    private String toolExecutionsIndex = "call_undefined";
    private final Map<String, ToolRequestBuilder> toolRequestBuilderHashMap = new HashMap<>();
    private volatile CompletionsFinishReason finishReason;

    private final Integer inputTokenCount;

    public AzureAiStreamingResponseBuilder(Integer inputTokenCount) {
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
                ToolRequestBuilder toolRequestBuilder;
                if (toolCall.getId() != null) {
                    toolExecutionsIndex = toolCall.getId();
                    toolRequestBuilder = new ToolRequestBuilder();
                    toolRequestBuilder.idBuilder.append(toolExecutionsIndex);
                    toolRequestBuilderHashMap.put(toolExecutionsIndex, toolRequestBuilder);
                } else {
                    toolRequestBuilder = toolRequestBuilderHashMap.get(toolExecutionsIndex);
                    if (toolRequestBuilder == null) {
                        throw new IllegalStateException("Function without an id defined in the tool call");
                    }
                }
                if (toolCall instanceof ChatCompletionsFunctionToolCall) {
                    ChatCompletionsFunctionToolCall functionCall = (ChatCompletionsFunctionToolCall) toolCall;
                    if (functionCall.getFunction().getName() != null) {
                        toolRequestBuilder.nameBuilder.append(functionCall.getFunction().getName());
                    }
                    if (functionCall.getFunction().getArguments() != null) {
                        toolRequestBuilder.argumentsBuilder.append(functionCall.getFunction().getArguments());
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

    public LlmResponse<AiMessage> build(Tokenizer tokenizer, boolean forcefulToolExecution) {

        String content = contentBuilder.toString();
        if (!content.isEmpty()) {
            return LlmResponse.from(
                    AiMessage.from(content),
                    tokenUsage(content, tokenizer),
                    finishReasonFrom(finishReason)
            );
        }

        String toolName = toolNameBuilder.toString();
        if (!toolName.isEmpty()) {
            ToolRequest toolExecutionRequest = ToolRequest.builder()
                    .name(toolName)
                    .arguments(toolArgumentsBuilder.toString())
                    .build();
            return LlmResponse.from(
                    AiMessage.from(toolExecutionRequest),
                    tokenUsage(singletonList(toolExecutionRequest), tokenizer, forcefulToolExecution),
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
                    AiMessage.from(toolExecutionRequests),
                    tokenUsage(toolExecutionRequests, tokenizer, forcefulToolExecution),
                    finishReasonFrom(finishReason)
            );
        }

        return null;
    }

    private TokenUsage tokenUsage(String content, Tokenizer tokenizer) {
        if (tokenizer == null) {
            return null;
        }
        int outputTokenCount = tokenizer.estimateTokenCountInText(content);
        return new TokenUsage(inputTokenCount, outputTokenCount);
    }

    private TokenUsage tokenUsage(List<ToolRequest> toolRequests, Tokenizer tokenizer, boolean forcefulToolExecution) {
        if (tokenizer == null) {
            return null;
        }

        int outputTokenCount = 0;
        if (forcefulToolExecution) {
            // OpenAI calculates output tokens differently when tool is executed forcefully
            for (ToolRequest toolRequest : toolRequests) {
                outputTokenCount += tokenizer.estimateTokenCountInForcefulToolRequest(toolRequest);
            }
        } else {
            outputTokenCount = tokenizer.estimateTokenCountInToolRequests(toolRequests);
        }

        return new TokenUsage(inputTokenCount, outputTokenCount);
    }

    private static class ToolRequestBuilder {

        private final StringBuffer idBuilder = new StringBuffer();
        private final StringBuffer nameBuilder = new StringBuffer();
        private final StringBuffer argumentsBuilder = new StringBuffer();
    }
}
