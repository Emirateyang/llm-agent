package com.llmagent.embedding.dashscope;

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
    private final Integer inputTokens;
    @JsonProperty
    private final Integer imageCount;
    @JsonProperty
    private final Integer duration;
    public Usage(Builder builder) {
        this.inputTokens = builder.inputTokens;
        this.imageCount = builder.imageCount;
        this.duration = builder.duration;
    }

    public Integer inputTokens() {
        return inputTokens;
    }

    public Integer imageCount() {
        return imageCount;
    }

    public Integer duration() {
        return duration;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof Usage
                && equalTo((Usage) another);
    }

    private boolean equalTo(Usage another) {
        return Objects.equals(inputTokens, another.inputTokens)
                && Objects.equals(imageCount, another.imageCount)
                && Objects.equals(duration, another.duration);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(inputTokens);
        h += (h << 5) + Objects.hashCode(imageCount);
        h += (h << 5) + Objects.hashCode(duration);
        return h;
    }

    @Override
    public String toString() {
        return "Usage {"
                + "inputTokens=" + inputTokens
                + ", imageCount=" + imageCount
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
        private Integer imageCount;
        private Integer duration;

        public Builder inputTokens(Integer inputTokens) {
            this.inputTokens = inputTokens;
            return this;
        }

        public Builder imageCount(Integer imageCount) {
            this.imageCount = imageCount;
            return this;
        }

        public Builder duration(Integer duration) {
            this.duration = duration;
            return this;
        }


        public Usage build() {
            return new Usage(this);
        }
    }
}
