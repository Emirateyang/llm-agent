package com.llmagent.embedding.doubao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

@JsonDeserialize(builder = TokenDetail.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class TokenDetail {
    @JsonProperty
    private final Integer imageTokens;
    @JsonProperty
    private final Integer textTokens;
    public TokenDetail(Builder builder) {
        this.imageTokens = builder.imageTokens;
        this.textTokens = builder.textTokens;
    }

    public Integer imageTokens() {
        return imageTokens;
    }

    public Integer textTokens() {
        return textTokens;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof TokenDetail
                && equalTo((TokenDetail) another);
    }

    private boolean equalTo(TokenDetail another) {
        return Objects.equals(imageTokens, another.imageTokens)
                && Objects.equals(textTokens, another.textTokens);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(imageTokens);
        h += (h << 5) + Objects.hashCode(textTokens);
        return h;
    }

    @Override
    public String toString() {
        return "TokenDetail {"
                + "imageTokens=" + imageTokens
                + ", textTokens=" + textTokens
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class Builder {

        private Integer imageTokens;
        private Integer textTokens;

        public Builder imageTokens(Integer imageTokens) {
            this.imageTokens = imageTokens;
            return this;
        }

        public Builder textTokens(Integer textTokens) {
            this.textTokens = textTokens;
            return this;
        }

        public TokenDetail build() {
            return new TokenDetail(this);
        }
    }
}
