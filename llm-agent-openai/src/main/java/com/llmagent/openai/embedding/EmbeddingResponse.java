package com.llmagent.openai.embedding;

import com.llmagent.openai.token.Usage;
import com.llmagent.vector.store.VectorData;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class EmbeddingResponse {
    private final String model;
    private final List<VectorData> data;
    private final Usage usage;

    private EmbeddingResponse(Builder builder) {
        this.model = builder.model;
        this.data = builder.data;
        this.usage = builder.usage;
    }

    public String model() {
        return model;
    }

    public List<VectorData> data() {
        return data;
    }

    public Usage usage() {
        return usage;
    }

    /**
     * Convenience method to get the embedding from the first data.
     */
    public List<Float> embedding() {
        return data.get(0).vectorAsList();
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof EmbeddingResponse
                && equalTo((EmbeddingResponse) another);
    }

    private boolean equalTo(EmbeddingResponse another) {
        return Objects.equals(model, another.model)
                && Objects.equals(data, another.data)
                && Objects.equals(usage, another.usage);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(model);
        h += (h << 5) + Objects.hashCode(data);
        h += (h << 5) + Objects.hashCode(usage);
        return h;
    }

    @Override
    public String toString() {
        return "EmbeddingResponse{"
                + "model=" + model
                + ", data=" + data
                + ", usage=" + usage
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String model;
        private List<VectorData> data;
        private Usage usage;

        private Builder() {
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder data(List<VectorData> data) {
            if (data != null) {
                this.data = Collections.unmodifiableList(data);
            }
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
