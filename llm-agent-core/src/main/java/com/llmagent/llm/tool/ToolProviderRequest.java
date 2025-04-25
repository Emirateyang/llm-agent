package com.llmagent.llm.tool;

import com.llmagent.data.message.UserMessage;

/**
 * Encapsulate the details of a request that a tool provider might need to process.
 */
public class ToolProviderRequest {
    private final Object chatMemoryId;
    private final UserMessage userMessage;

    public ToolProviderRequest(Object chatMemoryId, UserMessage userMessage) {
        this.chatMemoryId = chatMemoryId;
        this.userMessage = userMessage;
    }

    public Object chatMemoryId() {
        return chatMemoryId;
    }

    public UserMessage userMessage() {
        return userMessage;
    }
}
