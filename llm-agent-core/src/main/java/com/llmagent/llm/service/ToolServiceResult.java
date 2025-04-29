package com.llmagent.llm.service;

import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.llm.tool.ToolExecution;

import java.util.List;
import java.util.Objects;

public class ToolServiceResult {
    private final ChatResponse chatResponse;
    private final List<ToolExecution> toolExecutions;

    public ToolServiceResult(ChatResponse chatResponse,
                             List<ToolExecution> toolExecutions) {
        this.chatResponse = chatResponse;
        this.toolExecutions = toolExecutions;
    }

    public ChatResponse chatResponse() {
        return chatResponse;
    }

    public List<ToolExecution> toolExecutions() {
        return toolExecutions;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ToolServiceResult) obj;
        return Objects.equals(this.chatResponse, that.chatResponse) &&
                Objects.equals(this.toolExecutions, that.toolExecutions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatResponse, toolExecutions);
    }

    @Override
    public String toString() {
        return "ToolServiceResult{" +
                "chatResponse=" + chatResponse +
                ", toolExecutions=" + toolExecutions +
                '}';
    }
}
