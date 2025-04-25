package com.llmagent.openai;

import com.llmagent.data.message.AiMessage;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.llm.output.FinishReason;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.openai.chat.OpenAiChatResponseMetadata;
import com.llmagent.openai.token.Usage;
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
import java.util.concurrent.atomic.AtomicReference;

import static com.llmagent.openai.OpenAiHelper.finishReasonFrom;
import static com.llmagent.openai.OpenAiHelper.tokenUsageFrom;
import static com.llmagent.util.StringUtil.isNullOrBlank;
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

    private final AtomicReference<String> id = new AtomicReference<>();
    private final AtomicReference<Long> created = new AtomicReference<>();
    private final AtomicReference<String> model = new AtomicReference<>();
    private final AtomicReference<String> serviceTier = new AtomicReference<>();
    private final AtomicReference<String> systemFingerprint = new AtomicReference<>();
    private final AtomicReference<TokenUsage> tokenUsage = new AtomicReference<>();
    private final AtomicReference<FinishReason> finishReason = new AtomicReference<>();

    public void append(ChatCompletionResponse partialResponse) {
        if (partialResponse == null) {
            return;
        }

        if (!isNullOrBlank(partialResponse.id())) {
            this.id.set(partialResponse.id());
        }
        if (partialResponse.created() != null) {
            this.created.set(partialResponse.created());
        }
        if (!isNullOrBlank(partialResponse.model())) {
            this.model.set(partialResponse.model());
        }
        if (!isNullOrBlank(partialResponse.serviceTier())) {
            this.serviceTier.set(partialResponse.serviceTier());
        }
        if (!isNullOrBlank(partialResponse.systemFingerprint())) {
            this.systemFingerprint.set(partialResponse.systemFingerprint());
        }

        Usage usage = partialResponse.usage();
        if (usage != null) {
            this.tokenUsage.set(tokenUsageFrom(usage));
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
            this.finishReason.set(finishReasonFrom(finishReason));
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

        Usage usage = partialResponse.usage();
        if (usage != null) {
            this.tokenUsage.set(tokenUsageFrom(usage));
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
            this.finishReason.set(finishReasonFrom(finishReason));
        }

        String token = completionChoice.text();
        if (token != null) {
            contentBuilder.append(token);
        }
    }

//    public LlmResponse<AiMessage> build() {
//
//        String content = contentBuilder.toString();
//        if (!content.isEmpty()) {
//            return LlmResponse.from(
//                    AiMessage.from(content),
//                    tokenUsage.get(),
//                    finishReason.get()
//            );
//        }
//
//        String toolName = toolNameBuilder.toString();
//        if (!toolName.isEmpty()) {
//            ToolRequest toolExecutionRequest = ToolRequest.builder()
//                    .name(toolName)
//                    .arguments(toolArgumentsBuilder.toString())
//                    .build();
//            return LlmResponse.from(
//                    AiMessage.from(toolExecutionRequest),
//                    tokenUsage.get(),
//                    finishReason.get()
//            );
//        }
//
//        if (!indexToToolExecutionRequestBuilder.isEmpty()) {
//            List<ToolRequest> toolRequests = indexToToolExecutionRequestBuilder.values().stream()
//                    .map(it -> ToolRequest.builder()
//                            .id(it.idBuilder.toString())
//                            .name(it.nameBuilder.toString())
//                            .arguments(it.argumentsBuilder.toString())
//                            .build())
//                    .collect(toList());
//            return LlmResponse.from(
//                    AiMessage.from(toolRequests),
//                    tokenUsage.get(),
//                    finishReason.get()
//            );
//        }
//        return null;
//    }

    public ChatResponse build() {

        OpenAiChatResponseMetadata chatResponseMetadata = OpenAiChatResponseMetadata.builder()
                .id(id.get())
                .modelName(model.get())
                .tokenUsage(tokenUsage.get())
                .finishReason(finishReason.get())
                .created(created.get())
                .serviceTier(serviceTier.get())
                .systemFingerprint(systemFingerprint.get())
                .build();

        String text = contentBuilder.toString();

        String toolName = toolNameBuilder.toString();
        if (!toolName.isEmpty()) {
            ToolRequest toolExecutionRequest = ToolRequest.builder()
                    .name(toolName)
                    .arguments(toolArgumentsBuilder.toString())
                    .build();

            AiMessage aiMessage = isNullOrBlank(text) ?
                    AiMessage.from(toolExecutionRequest) :
                    AiMessage.from(text, singletonList(toolExecutionRequest));

            return ChatResponse.builder()
                    .aiMessage(aiMessage)
                    .metadata(chatResponseMetadata)
                    .build();
        }

        if (!indexToToolExecutionRequestBuilder.isEmpty()) {
            List<ToolRequest> toolExecutionRequests = indexToToolExecutionRequestBuilder.values().stream()
                    .map(it -> ToolRequest.builder()
                            .id(it.idBuilder.toString())
                            .name(it.nameBuilder.toString())
                            .arguments(it.argumentsBuilder.toString())
                            .build())
                    .collect(toList());

            AiMessage aiMessage = isNullOrBlank(text) ?
                    AiMessage.from(toolExecutionRequests) :
                    AiMessage.from(text, toolExecutionRequests);

            return ChatResponse.builder()
                    .aiMessage(aiMessage)
                    .metadata(chatResponseMetadata)
                    .build();
        }

        if (!isNullOrBlank(text)) {
            AiMessage aiMessage = AiMessage.from(text);
            return ChatResponse.builder()
                    .aiMessage(aiMessage)
                    .metadata(chatResponseMetadata)
                    .build();
        }

        return null;
    }

    private static class ToolRequestBuilder {

        private final StringBuffer idBuilder = new StringBuffer();
        private final StringBuffer nameBuilder = new StringBuffer();
        private final StringBuffer argumentsBuilder = new StringBuffer();
    }
}
