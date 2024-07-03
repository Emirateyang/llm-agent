package com.llmagent.openai.moderation;

import com.llmagent.llm.moderation.ModerationModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ModerationRequest {
    private final String model;
    private final List<String> input;

    private ModerationRequest(Builder builder) {
        this.model = builder.model;
        this.input = builder.input;
    }

    public String model() {
        return model;
    }

    public List<String> input() {
        return input;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof ModerationRequest
                && equalTo((ModerationRequest) another);
    }

    private boolean equalTo(ModerationRequest another) {
        return Objects.equals(model, another.model)
                && Objects.equals(input, another.input);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(model);
        h += (h << 5) + Objects.hashCode(input);
        return h;
    }

    @Override
    public String toString() {
        return "ModerationRequest{"
                + "model=" + model
                + ", input=" + input
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String model;
        private List<String> input;

        private Builder() {
        }

        public Builder model(ModerationModel model) {
            return model(model.toString());
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder input(List<String> input) {
            if (input != null) {
                this.input = Collections.unmodifiableList(input);
            }
            return this;
        }

        public Builder input(String input) {
            return input(Collections.singletonList(input));
        }

        public ModerationRequest build() {
            return new ModerationRequest(this);
        }
    }
}
