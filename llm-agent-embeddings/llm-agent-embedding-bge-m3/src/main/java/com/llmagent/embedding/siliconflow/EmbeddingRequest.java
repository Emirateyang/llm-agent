package com.llmagent.embedding.siliconflow;

import java.util.List;
import java.util.Objects;

public final class EmbeddingRequest {

    private final String model;
    private final String input;
    private final Integer dimensions;

    private EmbeddingRequest(Builder builder) {
        this.model = builder.model;
        this.input = builder.input;
        this.dimensions = builder.dimensions;
    }

    public String model() {
        return model;
    }

    public String input() {
        return input;
    }

    public Integer dimensions() {
        return dimensions;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof EmbeddingRequest
                && equalTo((EmbeddingRequest) another);
    }

    private boolean equalTo(EmbeddingRequest another) {
        return Objects.equals(model, another.model)
                && Objects.equals(input, another.input)
                && Objects.equals(dimensions, another.dimensions);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(model);
        h += (h << 5) + Objects.hashCode(input);
        h += (h << 5) + Objects.hashCode(dimensions);
        return h;
    }

    @Override
    public String toString() {
        return "EmbeddingRequest{"
                + "model=" + model
                + ", input=" + input
                + ", dimensions=" + dimensions
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String model = EmbeddingModelName.TEXT_MODAL_EMBEDDING.toString();
        private String input;
        private Integer dimensions;

        private Builder() {
        }

        public Builder model(EmbeddingModelName model) {
            return model(model.toString());
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder input(String input) {
            this.input = input;
            return this;
        }

        public Builder dimensions(Integer dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public EmbeddingRequest build() {
            return new EmbeddingRequest(this);
        }
    }
}
