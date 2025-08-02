package com.llmagent.embedding.siliconflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

@JsonDeserialize(builder = Usage.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class Usage {
    @JsonProperty
    private final Integer promptTokens;
    @JsonProperty
    private final Integer totalTokens;

    public Usage(Builder builder) {
        this.promptTokens = builder.promptTokens;
        this.totalTokens = builder.totalTokens;
    }

    public Integer promptTokens() {
        return promptTokens;
    }

    public Integer totalTokens() {
        return totalTokens;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof Usage
                && equalTo((Usage) another);
    }

    private boolean equalTo(Usage another) {
        return Objects.equals(promptTokens, another.promptTokens)
                && Objects.equals(totalTokens, another.totalTokens);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(promptTokens);
        h += (h << 5) + Objects.hashCode(totalTokens);
        return h;
    }

    @Override
    public String toString() {
        return "Usage {"
                + "promptTokens=" + promptTokens
                + ", totalTokens=" + totalTokens
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class Builder {

        private Integer promptTokens;
        private Integer totalTokens;

        public Builder inputTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        public Builder totalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
            return this;
        }


        public Usage build() {
            return new Usage(this);
        }
    }
}
