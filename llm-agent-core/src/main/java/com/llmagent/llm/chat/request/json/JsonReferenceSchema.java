package com.llmagent.llm.chat.request.json;

import java.util.Objects;

import static com.llmagent.util.StringUtil.quoted;

public class JsonReferenceSchema implements JsonSchemaElement {

    private final String reference;

    public JsonReferenceSchema(Builder builder) {
        this.reference = builder.reference;
    }

    public String reference() {
        return reference;
    }

    @Override
    public String description() {
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String reference;

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public JsonReferenceSchema build() {
            return new JsonReferenceSchema(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonReferenceSchema that = (JsonReferenceSchema) o;
        return Objects.equals(this.reference, that.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference);
    }

    @Override
    public String toString() {
        return "JsonReferenceSchema {" +
                "reference = " + quoted(reference) +
                " }";
    }
}
