package com.llmagent.openai;

import com.llmagent.data.message.AiMessage;
import com.llmagent.llm.Tokenizer;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.openai.tool.FunctionCall;
import com.llmagent.openai.tool.ToolCall;
import com.llmagent.llm.tool.ToolRequest;
import com.llmagent.openai.chat.ChatCompletionChoice;
import com.llmagent.openai.chat.ChatCompletionResponse;
import com.llmagent.openai.chat.Delta;
import com.llmagent.openai.completion.CompletionChoice;
import com.llmagent.openai.completion.CompletionResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.llmagent.openai.OpenAiHelper.finishReasonFrom;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * This class needs to be thread safe because it is called when a streaming result comes back
 * and there is no guarantee that this thread will be the same as the one that initiated the request,
 * in fact it almost certainly won't be.
 */
public class OpenAiStreamingResponseBuilder {

    private final StringBuffer contentBuilder = new StringBuffer();

    private final StringBuffer toolNameBuilder = new StringBuffer();
    private final StringBuffer toolArgumentsBuilder = new StringBuffer();

    private final Map<Integer, ToolRequestBuilder> indexToToolExecutionRequestBuilder = new ConcurrentHashMap<>();

    private volatile String finishReason;

    private final Integer inputTokenCount;

    public OpenAiStreamingResponseBuilder(Integer inputTokenCount) {
        this.inputTokenCount = inputTokenCount;
    }

    public void append(ChatCompletionResponse partialResponse) {
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

        String finishReason = chatCompletionChoice.finishReason();
        if (finishReason != null) {
            this.finishReason = finishReason;
        }

        Delta delta = chatCompletionChoice.delta();
        if (delta == null) {
            return;
        }

        String content = delta.content();
        if (content != null) {
            contentBuilder.append(content);
            return;
        }

        if (delta.toolCalls() != null && !delta.toolCalls().isEmpty()) {
            ToolCall toolCall = delta.toolCalls().get(0);

            ToolRequestBuilder toolRequestBuilder
                    = indexToToolExecutionRequestBuilder.computeIfAbsent(toolCall.index(), idx -> new ToolRequestBuilder());

            if (toolCall.id() != null) {
                toolRequestBuilder.idBuilder.append(toolCall.id());
            }

            FunctionCall functionCall = toolCall.function();

            if (functionCall.name() != null) {
                toolRequestBuilder.nameBuilder.append(functionCall.name());
            }

            if (functionCall.arguments() != null) {
                toolRequestBuilder.argumentsBuilder.append(functionCall.arguments());
            }
        }
    }

    public void append(CompletionResponse partialResponse) {
        if (partialResponse == null) {
            return;
        }

        List<CompletionChoice> choices = partialResponse.choices();
        if (choices == null || choices.isEmpty()) {
            return;
        }

        CompletionChoice completionChoice = choices.get(0);
        if (completionChoice == null) {
            return;
        }

        String finishReason = completionChoice.finishReason();
        if (finishReason != null) {
            this.finishReason = finishReason;
        }

        String token = completionChoice.text();
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

        if (!indexToToolExecutionRequestBuilder.isEmpty()) {
            List<ToolRequest> toolRequests = indexToToolExecutionRequestBuilder.values().stream()
                    .map(it -> ToolRequest.builder()
                            .id(it.idBuilder.toString())
                            .name(it.nameBuilder.toString())
                            .arguments(it.argumentsBuilder.toString())
                            .build())
                    .collect(toList());
            return LlmResponse.from(
                    AiMessage.from(toolRequests),
                    tokenUsage(toolRequests, tokenizer, forcefulToolExecution),
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
                outputTokenCount += tokenizer.estimateTokenCountInForcefulToolExecutionRequest(toolRequest);
            }
        } else {
            outputTokenCount = tokenizer.estimateTokenCountInToolExecutionRequests(toolRequests);
        }

        return new TokenUsage(inputTokenCount, outputTokenCount);
    }

    private static class ToolRequestBuilder {

        private final StringBuffer idBuilder = new StringBuffer();
        private final StringBuffer nameBuilder = new StringBuffer();
        private final StringBuffer argumentsBuilder = new StringBuffer();
    }
}
