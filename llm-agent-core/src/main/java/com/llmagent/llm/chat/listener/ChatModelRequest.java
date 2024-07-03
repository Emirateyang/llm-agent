package com.llmagent.llm.chat.listener;

import com.llmagent.Experimental;
import com.llmagent.data.message.ChatMessage;
import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.chat.StreamingChatLanguageModel;
import com.llmagent.llm.tool.ToolSpecification;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

/**
 * A request to the {@link ChatLanguageModel} or {@link StreamingChatLanguageModel},
 * intended to be used with {@link ChatModelListener}.
 */
@Experimental
public class ChatModelRequest {

    private final String model;
    private final Double temperature;
    private final Double topP;
    private final Integer maxTokens;
    private final List<ChatMessage> messages;
    private final List<ToolSpecification> toolSpecifications;

    @Builder
    public ChatModelRequest(String model,
                            Double temperature,
                            Double topP,
                            Integer maxTokens,
                            List<ChatMessage> messages,
                            List<ToolSpecification> toolSpecifications) {
        this.model = model;
        this.temperature = temperature;
        this.topP = topP;
        this.maxTokens = maxTokens;
        this.messages = messages == null ? null : Collections.unmodifiableList(messages);
        this.toolSpecifications = toolSpecifications == null ? null : Collections.unmodifiableList(toolSpecifications);
    }

    public String model() {
        return model;
    }

    public Double temperature() {
        return temperature;
    }

    public Double topP() {
        return topP;
    }

    public Integer maxTokens() {
        return maxTokens;
    }

    public List<ChatMessage> messages() {
        return messages;
    }

    public List<ToolSpecification> toolSpecifications() {
        return toolSpecifications;
    }
}
