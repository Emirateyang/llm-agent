package com.llmagent.openai.chat;

import com.llmagent.data.Role;
import com.llmagent.openai.tool.ToolCall;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Delta {

    private final Role role;
    private final String content;
    private final List<ToolCall> toolCalls;

    private Delta(Builder builder) {
        this.role = builder.role;
        this.content = builder.content;
        this.toolCalls = builder.toolCalls;
    }

    public Role role() {
        return role;
    }

    public String content() {
        return content;
    }

    public List<ToolCall> toolCalls() {
        return toolCalls;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof Delta
                && equalTo((Delta) another);
    }

    private boolean equalTo(Delta another) {
        return Objects.equals(role, another.role)
                && Objects.equals(content, another.content)
                && Objects.equals(toolCalls, another.toolCalls);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(role);
        h += (h << 5) + Objects.hashCode(content);
        h += (h << 5) + Objects.hashCode(toolCalls);
        return h;
    }

    @Override
    public String toString() {
        return "Delta{"
                + "role=" + role
                + ", content=" + content
                + ", toolCalls=" + toolCalls
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Role role;
        private String content;
        private List<ToolCall> toolCalls;

        private Builder() {
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder toolCalls(List<ToolCall> toolCalls) {
            if (toolCalls != null) {
                this.toolCalls = Collections.unmodifiableList(toolCalls);
            }
            return this;
        }

        public Delta build() {
            return new Delta(this);
        }
    }
}
