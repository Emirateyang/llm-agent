package com.llmagent.llm.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

@JsonDeserialize(builder = MultimodalTokenUsage.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class MultimodalTokenUsage {
    @JsonProperty
    private final Integer inputTokens;
    @JsonProperty
    private final Integer totalTokens;
    @JsonProperty
    private final Integer duration;
    public MultimodalTokenUsage(Builder builder) {
        this.inputTokens = builder.inputTokens;
        this.totalTokens = builder.totalTokens;
        this.duration = builder.duration;
    }

    public Integer inputTokens() {
        return inputTokens;
    }

    public Integer totalTokens() {
        return totalTokens;
    }

    public Integer duration() {
        return duration;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof MultimodalTokenUsage
                && equalTo((MultimodalTokenUsage) another);
    }

    private boolean equalTo(MultimodalTokenUsage another) {
        return Objects.equals(inputTokens, another.inputTokens)
                && Objects.equals(totalTokens, another.totalTokens)
                && Objects.equals(duration, another.duration);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(inputTokens);
        h += (h << 5) + Objects.hashCode(totalTokens);
        h += (h << 5) + Objects.hashCode(duration);
        return h;
    }

    @Override
    public String toString() {
        return "MultimodalTokenUsage {"
                + "inputTokens=" + inputTokens
                + ", totalTokens=" + totalTokens
                + ", duration=" + duration
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class Builder {

        private Integer inputTokens;
        private Integer totalTokens;
        private Integer duration;

        public Builder inputTokens(Integer inputTokens) {
            this.inputTokens = inputTokens;
            return this;
        }

        public Builder totalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
            return this;
        }

        public Builder duration(Integer duration) {
            this.duration = duration;
            return this;
        }


        public MultimodalTokenUsage build() {
            return new MultimodalTokenUsage(this);
        }
    }
}
