package com.llmagent.openai.chat;

import com.llmagent.data.Role;
import com.llmagent.openai.tool.ToolCall;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class AssistantMessage implements Message {

    private final Role role = Role.ASSISTANT;
    private final String content;
    private final String name;
    private final List<ToolCall> toolCalls;

    private AssistantMessage(Builder builder) {
        this.content = builder.content;
        this.name = builder.name;
        this.toolCalls = builder.toolCalls;
    }

    public Role role() {
        return role;
    }

    public String content() {
        return content;
    }

    public String name() {
        return name;
    }

    public List<ToolCall> toolCalls() {
        return toolCalls;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof AssistantMessage
                && equalTo((AssistantMessage) another);
    }

    private boolean equalTo(AssistantMessage another) {
        return Objects.equals(role, another.role)
                && Objects.equals(content, another.content)
                && Objects.equals(name, another.name)
                && Objects.equals(toolCalls, another.toolCalls);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(role);
        h += (h << 5) + Objects.hashCode(content);
        h += (h << 5) + Objects.hashCode(name);
        h += (h << 5) + Objects.hashCode(toolCalls);
        return h;
    }

    @Override
    public String toString() {
        return "AssistantMessage{"
                + "role=" + role
                + ", content=" + content
                + ", name=" + name
                + ", toolCalls=" + toolCalls
                + "}";
    }

    public static AssistantMessage from(String content) {
        return AssistantMessage.builder()
                .content(content)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String content;
        private String name;
        private List<ToolCall> toolCalls;

        private Builder() {
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder toolCalls(ToolCall... toolCalls) {
            return toolCalls(Arrays.asList(toolCalls));
        }

        public Builder toolCalls(List<ToolCall> toolCalls) {
            if (toolCalls != null) {
                this.toolCalls = Collections.unmodifiableList(toolCalls);
            }
            return this;
        }

        public AssistantMessage build() {
            return new AssistantMessage(this);
        }
    }
}
