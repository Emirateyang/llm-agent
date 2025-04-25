package com.llmagent.openai.chat;

import com.llmagent.openai.token.Usage;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public  final class ChatCompletionResponse {

    private final String id;
    private final Long created;
    private final String model;
    private final List<ChatCompletionChoice> choices;
    private final Usage usage;
    private final String systemFingerprint;
    private final String serviceTier;

    private ChatCompletionResponse(Builder builder) {
        this.id = builder.id;
        this.created = builder.created;
        this.model = builder.model;
        this.choices = builder.choices;
        this.usage = builder.usage;
        this.systemFingerprint = builder.systemFingerprint;
        this.serviceTier = builder.serviceTier;
    }

    public String id() {
        return id;
    }

    public Long created() {
        return created;
    }

    public String model() {
        return model;
    }

    public List<ChatCompletionChoice> choices() {
        return choices;
    }

    public Usage usage() {
        return usage;
    }

    public String systemFingerprint() {
        return systemFingerprint;
    }

    public String serviceTier() {
        return serviceTier;
    }

    /**
     * Convenience method to get the content of the message from the first choice.
     */
    public String content() {
        return choices().get(0).message().content();
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof ChatCompletionResponse
                && equalTo((ChatCompletionResponse) another);
    }

    private boolean equalTo(ChatCompletionResponse another) {
        return Objects.equals(id, another.id)
                && Objects.equals(created, another.created)
                && Objects.equals(model, another.model)
                && Objects.equals(choices, another.choices)
                && Objects.equals(usage, another.usage)
                && Objects.equals(systemFingerprint, another.systemFingerprint)
                && Objects.equals(serviceTier, another.serviceTier);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(id);
        h += (h << 5) + Objects.hashCode(created);
        h += (h << 5) + Objects.hashCode(model);
        h += (h << 5) + Objects.hashCode(choices);
        h += (h << 5) + Objects.hashCode(usage);
        h += (h << 5) + Objects.hashCode(systemFingerprint);
        h += (h << 5) + Objects.hashCode(serviceTier);
        return h;
    }

    @Override
    public String toString() {
        return "ChatCompletionResponse{"
                + "id=" + id
                + ", created=" + created
                + ", model=" + model
                + ", choices=" + choices
                + ", usage=" + usage
                + ", systemFingerprint=" + systemFingerprint
                + ", serviceTier=" + serviceTier
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id;
        private Long created;
        private String model;
        private List<ChatCompletionChoice> choices;
        private Usage usage;
        private String systemFingerprint;
        private String serviceTier;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder created(Long created) {
            this.created = created;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder choices(List<ChatCompletionChoice> choices) {
            if (choices != null) {
                this.choices = Collections.unmodifiableList(choices);
            }
            return this;
        }

        public Builder usage(Usage usage) {
            this.usage = usage;
            return this;
        }

        public Builder systemFingerprint(String systemFingerprint) {
            this.systemFingerprint = systemFingerprint;
            return this;
        }

        public Builder serviceTier(String serviceTier) {
            this.serviceTier = serviceTier;
            return this;
        }

        public ChatCompletionResponse build() {
            return new ChatCompletionResponse(this);
        }
    }
}
