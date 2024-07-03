package com.llmagent.data.message;

import com.llmagent.data.Role;
import com.llmagent.llm.tool.ToolRequest;
import com.llmagent.util.StringUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.llmagent.data.message.ChatMessageType.AI;

public class AiMessage implements ChatMessage {

    private final Role role = Role.ASSISTANT;

    private final String content;
    private final List<ToolRequest> toolRequests;

    /**
     * Create a new {@link AiMessage} with the given text.
     *
     * @param content the text of the message.
     */
    public AiMessage(String content) {
        this.content = content;
        this.toolRequests = null;
    }

    /**
     * Create a new {@link AiMessage} with the given tool execution requests.
     *
     * @param toolRequests the tool execution requests of the message.
     */
    public AiMessage(List<ToolRequest> toolRequests) {
        this.content = null;
        this.toolRequests = toolRequests;
    }

    /**
     * Create a new {@link AiMessage} with the given text and tool execution requests.
     *
     * @param content                  the text of the message.
     * @param toolRequests the tool execution requests of the message.
     */
    public AiMessage(String content, List<ToolRequest> toolRequests) {
        this.content = content;
        this.toolRequests = toolRequests;
    }

    /**
     * Get the text of the message.
     *
     * @return the text of the message.
     */
    public String content() {
        return content;
    }

    public Role role() {
        return role;
    }

    /**
     * Get the tool execution requests of the message.
     *
     * @return the tool execution requests of the message.
     */
    public List<ToolRequest> toolRequests() {
        return toolRequests;
    }

    /**
     * Check if the message has ToolExecutionRequests.
     *
     * @return true if the message has ToolExecutionRequests, false otherwise.
     */
    public boolean hasToolRequests() {
        return toolRequests != null && !toolRequests.isEmpty();
    }

    @Override
    public ChatMessageType type() {
        return AI;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiMessage that = (AiMessage) o;
        return Objects.equals(this.content, that.content)
                && Objects.equals(this.toolRequests, that.toolRequests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, toolRequests);
    }

    @Override
    public String toString() {
        return "AiMessage {" +
                " content = " + StringUtil.quoted(content) +
                " toolExecutionRequests = " + toolRequests +
                " }";
    }

    /**
     * Create a new {@link AiMessage} with the given text.
     *
     * @param text the text of the message.
     * @return the new {@link AiMessage}.
     */
    public static AiMessage from(String text) {
        return new AiMessage(text);
    }

    /**
     * Create a new {@link AiMessage} with the given tool execution requests.
     *
     * @param toolExecutionRequests the tool execution requests of the message.
     * @return the new {@link AiMessage}.
     */
    public static AiMessage from(ToolRequest... toolExecutionRequests) {
        return from(Arrays.asList(toolExecutionRequests));
    }

    /**
     * Create a new {@link AiMessage} with the given tool execution requests.
     *
     * @param toolExecutionRequests the tool execution requests of the message.
     * @return the new {@link AiMessage}.
     */
    public static AiMessage from(List<ToolRequest> toolExecutionRequests) {
        return new AiMessage(toolExecutionRequests);
    }

    /**
     * Create a new {@link AiMessage} with the given text.
     *
     * @param text the text of the message.
     * @return the new {@link AiMessage}.
     */
    public static AiMessage aiMessage(String text) {
        return from(text);
    }

    /**
     * Create a new {@link AiMessage} with the given tool execution requests.
     *
     * @param toolRequests the tool execution requests of the message.
     * @return the new {@link AiMessage}.
     */
    public static AiMessage aiMessage(ToolRequest... toolRequests) {
        return aiMessage(Arrays.asList(toolRequests));
    }

    /**
     * Create a new {@link AiMessage} with the given tool execution requests.
     *
     * @param toolExecutionRequests the tool execution requests of the message.
     * @return the new {@link AiMessage}.
     */
    public static AiMessage aiMessage(List<ToolRequest> toolExecutionRequests) {
        return from(toolExecutionRequests);
    }
}
