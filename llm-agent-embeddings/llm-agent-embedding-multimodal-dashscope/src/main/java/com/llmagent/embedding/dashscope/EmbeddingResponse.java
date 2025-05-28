package com.llmagent.embedding.dashscope;

import com.llmagent.llm.output.MultimodalTokenUsage;
import com.llmagent.vector.store.MultimodalEmbeddingOutput;

import java.util.Objects;

public final class EmbeddingResponse {
    private final MultimodalEmbeddingOutput output;
    private final MultimodalTokenUsage usage;

    private EmbeddingResponse(Builder builder) {
        this.output = builder.output;
        this.usage = builder.usage;
    }


    public MultimodalEmbeddingOutput output() {
        return output;
    }

    public MultimodalTokenUsage usage() {
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

        private MultimodalEmbeddingOutput output;
        private MultimodalTokenUsage usage;

        private Builder() {
        }

        public Builder output(MultimodalEmbeddingOutput output) {
            this.output = output;
            return this;
        }

        public Builder usage(MultimodalTokenUsage usage) {
            this.usage = usage;
            return this;
        }

        public EmbeddingResponse build() {
            return new EmbeddingResponse(this);
        }
    }
}
