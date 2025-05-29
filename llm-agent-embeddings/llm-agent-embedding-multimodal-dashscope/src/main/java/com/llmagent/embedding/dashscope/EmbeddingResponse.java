package com.llmagent.embedding.dashscope;

import java.util.Objects;

public final class EmbeddingResponse {
    private final EmbeddingOutput output;
    private final Usage usage;

    private EmbeddingResponse(Builder builder) {
        this.output = builder.output;
        this.usage = builder.usage;
    }


    public EmbeddingOutput output() {
        return output;
    }

    public Usage usage() {
        return usage;
    }


    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof EmbeddingResponse
                && equalTo((EmbeddingResponse) another);
    }

    private boolean equalTo(EmbeddingResponse another) {
        return Objects.equals(output, another.output)
                && Objects.equals(usage, another.usage);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(output);
        h += (h << 5) + Objects.hashCode(usage);
        return h;
    }

    @Override
    public String toString() {
        return "EmbeddingResponse{"
                + "output=" + output
                + ", usage=" + usage
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private EmbeddingOutput output;
        private Usage usage;

        private Builder() {
        }

        public Builder output(EmbeddingOutput output) {
            this.output = output;
            return this;
        }

        public Builder usage(Usage usage) {
            this.usage = usage;
            return this;
        }

        public EmbeddingResponse build() {
            return new EmbeddingResponse(this);
        }
    }
}
