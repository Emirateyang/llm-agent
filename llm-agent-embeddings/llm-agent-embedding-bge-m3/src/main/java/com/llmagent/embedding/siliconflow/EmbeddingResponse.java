package com.llmagent.embedding.siliconflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;
import java.util.Objects;

@JsonDeserialize(builder = EmbeddingResponse.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class EmbeddingResponse {

    @JsonProperty
    private final String model;
    @JsonProperty
    private final String object;
    @JsonProperty
    private final Usage usage;
    @JsonProperty
    private final List<EmbeddingOutput> data;

    public EmbeddingResponse(Builder builder) {
        this.model = builder.model;
        this.object = builder.object;
        this.usage = builder.usage;
        this.data = builder.data;
    }

    public String model() {
        return model;
    }

    public String object() {
        return object;
    }

    public Usage usage() {
        return usage;
    }

    public List<EmbeddingOutput> data() {
        return data;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof EmbeddingResponse
                && equalTo((EmbeddingResponse) another);
    }

    private boolean equalTo(EmbeddingResponse another) {
        return Objects.equals(model, another.model)
                && Objects.equals(object, another.object)
                && Objects.equals(data, another.data)
                && Objects.equals(usage, another.usage);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(model);
        h += (h << 5) + Objects.hashCode(object);
        h += (h << 5) + Objects.hashCode(data);
        h += (h << 5) + Objects.hashCode(usage);
        return h;
    }

    @Override
    public String toString() {
        return "EmbeddingResponse {"
                + ", model=" + model
                + ", object=" + object
                + ", data=" + data
                + ", usage=" + usage
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class Builder {
        private String model;
        private String object;
        private List<EmbeddingOutput> data;
        private Usage usage;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder object(String object) {
            this.object = object;
            return this;
        }

        public Builder data(List<EmbeddingOutput> data) {
            this.data = data;
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
