package com.llmagent.openai.completion;

import java.util.*;

public final class Logprobs {
    private final List<String> tokens;
    private final List<Double> tokenLogprobs;
    private final List<Map<String, Double>> topLogprobs;
    private final List<Integer> textOffset;

    private Logprobs(Builder builder) {
        this.tokens = builder.tokens;
        this.tokenLogprobs = builder.tokenLogprobs;
        this.topLogprobs = builder.topLogprobs;
        this.textOffset = builder.textOffset;
    }

    public List<String> tokens() {
        return tokens;
    }

    public List<Double> tokenLogprobs() {
        return tokenLogprobs;
    }

    public List<Map<String, Double>> topLogprobs() {
        return topLogprobs;
    }

    public List<Integer> textOffset() {
        return textOffset;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof Logprobs
                && equalTo((Logprobs) another);
    }

    private boolean equalTo(Logprobs another) {
        return Objects.equals(tokens, another.tokens)
                && Objects.equals(tokenLogprobs, another.tokenLogprobs)
                && Objects.equals(topLogprobs, another.topLogprobs)
                && Objects.equals(textOffset, another.textOffset);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(tokens);
        h += (h << 5) + Objects.hashCode(tokenLogprobs);
        h += (h << 5) + Objects.hashCode(topLogprobs);
        h += (h << 5) + Objects.hashCode(textOffset);
        return h;
    }

    @Override
    public String toString() {
        return "Logprobs{"
                + "tokens=" + tokens
                + ", tokenLogprobs=" + tokenLogprobs
                + ", topLogprobs=" + topLogprobs
                + ", textOffset=" + textOffset
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private List<String> tokens;
        private List<Double> tokenLogprobs;
        private List<Map<String, Double>> topLogprobs;
        private List<Integer> textOffset;

        private Builder() {
        }

        public Builder tokens(List<String> tokens) {
            if (tokens != null) {
                this.tokens = Collections.unmodifiableList(tokens);
            }
            return this;
        }

        public Builder tokenLogprobs(List<Double> tokenLogprobs) {
            if (tokenLogprobs != null) {
                this.tokenLogprobs = Collections.unmodifiableList(tokenLogprobs);
            }
            return this;
        }

        public Builder topLogprobs(List<Map<String, Double>> topLogprobs) {
            if (topLogprobs != null) {
                List<Map<String, Double>> topLogprobsCopy = new ArrayList<>();
                for (Map<String, Double> map : topLogprobs) {
                    topLogprobsCopy.add(Collections.unmodifiableMap(map));
                }
                this.topLogprobs = Collections.unmodifiableList(topLogprobsCopy);
            }

            return this;
        }

        public Builder textOffset(List<Integer> textOffset) {
            if (textOffset != null) {
                this.textOffset = Collections.unmodifiableList(textOffset);
            }
            return this;
        }

        public Logprobs build() {
            return new Logprobs(this);
        }
    }
}
