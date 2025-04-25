package com.llmagent.openai.completion;


import com.llmagent.openai.token.Usage;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CompletionResponse {
    private final String id;
    private final Integer created;
    private final String model;
    private final List<CompletionChoice> choices;
    private final Usage usage;

    private CompletionResponse(Builder builder) {
        this.id = builder.id;
        this.created = builder.created;
        this.model = builder.model;
        this.choices = builder.choices;
        this.usage = builder.usage;
    }

    public String id() {
        return id;
    }

    public Integer created() {
        return created;
    }

    public String model() {
        return model;
    }

    public List<CompletionChoice> choices() {
        return choices;
    }

    public Usage usage() {
        return usage;
    }

    /**
     * Convenience method to get the text from the first choice.
     */
    public String text() {
        return choices().get(0).text();
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof CompletionResponse
                && equalTo((CompletionResponse) another);
    }

    private boolean equalTo(CompletionResponse another) {
        return Objects.equals(id, another.id)
                && Objects.equals(created, another.created)
                && Objects.equals(model, another.model)
                && Objects.equals(choices, another.choices)
                && Objects.equals(usage, another.usage);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(id);
        h += (h << 5) + Objects.hashCode(created);
        h += (h << 5) + Objects.hashCode(model);
        h += (h << 5) + Objects.hashCode(choices);
        h += (h << 5) + Objects.hashCode(usage);
        return h;
    }

    @Override
    public String toString() {
        return "CompletionResponse{"
                + "id=" + id
                + ", created=" + created
                + ", model=" + model
                + ", choices=" + choices
                + ", usage=" + usage
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id;
        private Integer created;
        private String model;
        private List<CompletionChoice> choices;
        private Usage usage;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder created(Integer created) {
            this.created = created;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder choices(List<CompletionChoice> choices) {
            if (choices != null) {
                this.choices = Collections.unmodifiableList(choices);
            }
            return this;
        }

        public Builder usage(Usage usage) {
            this.usage = usage;
            return this;
        }

        public CompletionResponse build() {
            return new CompletionResponse(this);
        }
    }
}
