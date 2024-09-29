package com.llmagent.dify.chat;

import lombok.Data;

@Data
public class StreamResponse4Customer {

    private String event;
    private String content;
    private ToolCallInResponse toolCall;
    private String observation;

    private StreamResponse4Customer(StreamResponse4Customer.Builder builder) {
        this.event = builder.event;
        this.content = builder.content;
        this.toolCall = builder.toolCall;
        this.observation = builder.observation;
    }

    public static StreamResponse4Customer.Builder builder() {
        return new StreamResponse4Customer.Builder();
    }

    public static final class Builder {

        private String event;
        private String content;
        private ToolCallInResponse toolCall;
        private String observation;

        private Builder() {
        }

        public StreamResponse4Customer.Builder event(String event) {
            this.event = event;
            return this;
        }

        public StreamResponse4Customer.Builder content(String content) {
            this.content = content;
            return this;
        }

        public StreamResponse4Customer.Builder toolCall(ToolCallInResponse toolCall) {
            this.toolCall = toolCall;
            return this;
        }

        public StreamResponse4Customer.Builder observation(String observation) {
            this.observation = observation;
            return this;
        }

        public StreamResponse4Customer build() {
            return new StreamResponse4Customer(this);
        }
    }
}
