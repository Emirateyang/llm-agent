package com.llmagent.data.message;

import com.llmagent.data.Role;
import com.llmagent.llm.tool.ToolRequest;
import com.llmagent.util.StringUtil;

import java.util.Objects;

public class ToolMessage implements ChatMessage {

    private final Role role = Role.TOOL;

    private final String id;
    private final String toolName;
    private final String content;

    /**
     * Creates a {@link ToolMessage}.
     * @param id the id of the tool.
     * @param toolName the name of the tool.
     * @param content the result of the tool execution.
     */
    public ToolMessage(String id, String toolName, String content) {
        this.id = id;
        this.toolName = toolName;
        this.content = content;
    }

    public ToolMessage(String id, String content) {
        this.id = id;
        this.content = content;
        this.toolName = "";
    }

    /**
     * Returns the id of the tool.
     * @return the id of the tool.
     */
    public String id() {
        return id;
    }

    /**
     * Returns the name of the tool.
     * @return the name of the tool.
     */
    public String toolName() {
        return toolName;
    }

    /**
     * Returns the result of the tool execution.
     * @return the result of the tool execution.
     */
    public String content() {
        return content;
    }

    public Role role() {
        return role;
    }

    @Override
    public ChatMessageType type() {
        return ChatMessageType.TOOL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolMessage that = (ToolMessage) o;
        return Objects.equals(this.id, that.id)
                && Objects.equals(this.toolName, that.toolName)
                && Objects.equals(this.content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, toolName, content);
    }

    @Override
    public String toString() {
        return "ToolResultMessage {" +
                " id = " + StringUtil.quoted(id) +
                " toolName = " + StringUtil.quoted(toolName) +
                " text = " + StringUtil.quoted(content) +
                " }";
    }

    /**
     * Creates a {@link ToolMessage} from a {@link ToolRequest} and the result of the tool execution.
     * @param request the request.
     * @param toolExecutionResult the result of the tool execution.
     * @return the {@link ToolMessage}.
     */
    public static ToolMessage from(ToolRequest request, String toolExecutionResult) {
        return new ToolMessage(request.id(), request.name(), toolExecutionResult);
    }

    /**
     * Creates a {@link ToolMessage} from a {@link ToolRequest} and the result of the tool execution.
     * @param id the id of the tool.
     * @param toolName the name of the tool.
     * @param toolExecutionResult the result of the tool execution.
     * @return the {@link ToolMessage}.
     */
    public static ToolMessage from(String id, String toolName, String toolExecutionResult) {
        return new ToolMessage(id, toolName, toolExecutionResult);
    }

    public static ToolMessage from(String id, String content) {
        return new ToolMessage(id, content);
    }

    /**
     * Creates a {@link ToolMessage} from a {@link ToolRequest} and the result of the tool execution.
     * @param request the request.
     * @param toolExecutionResult the result of the tool execution.
     * @return the {@link ToolMessage}.
     */
    public static ToolMessage ToolResultMessage(ToolRequest request, String toolExecutionResult) {
        return from(request, toolExecutionResult);
    }

    /**
     * Creates a {@link ToolMessage} from a {@link ToolRequest} and the result of the tool execution.
     * @param id the id of the tool.
     * @param toolName the name of the tool.
     * @param toolExecutionResult the result of the tool execution.
     * @return the {@link ToolMessage}.
     */
    public static ToolMessage ToolResultMessage(String id, String toolName, String toolExecutionResult) {
        return from(id, toolName, toolExecutionResult);
    }
}
