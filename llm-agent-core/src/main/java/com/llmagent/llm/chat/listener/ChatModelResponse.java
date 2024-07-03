package com.llmagent.llm.chat.listener;

import com.llmagent.Experimental;
import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.chat.StreamingChatLanguageModel;
import com.llmagent.data.message.AiMessage;
import com.llmagent.llm.output.FinishReason;
import com.llmagent.llm.output.TokenUsage;
import lombok.Builder;

/**
 * A response from the {@link ChatLanguageModel} or {@link StreamingChatLanguageModel},
 * intended to be used with {@link ChatModelListener}.
 */
@Experimental
public class ChatModelResponse {

    private final String id;
    private final String model;
    private final TokenUsage tokenUsage;
    private final FinishReason finishReason;
    private final AiMessage aiMessage;

    @Builder
    public ChatModelResponse(String id,
                             String model,
                             TokenUsage tokenUsage,
                             FinishReason finishReason,
                             AiMessage aiMessage) {
        this.id = id;
        this.model = model;
        this.tokenUsage = tokenUsage;
        this.finishReason = finishReason;
        this.aiMessage = aiMessage;
    }

    public String id() {
        return id;
    }

    public String model() {
        return model;
    }

    public TokenUsage tokenUsage() {
        return tokenUsage;
    }

    public FinishReason finishReason() {
        return finishReason;
    }

    public AiMessage aiMessage() {
        return aiMessage;
    }
}
