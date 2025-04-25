package com.llmagent.openai.tool;

import com.llmagent.llm.tool.SchemaProperty;
import com.llmagent.llm.tool.ToolParameters;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Function {

    private final String name;
    private final String description;
    private final Boolean strict;
    private final ToolParameters parameters;

    private Function(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.strict = builder.strict;
        this.parameters = builder.parameters;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public ToolParameters parameters() {
        return parameters;
    }

    public Boolean strict() {
        return strict;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof Function
                && equalTo((Function) another);
    }

    private boolean equalTo(Function another) {
        return Objects.equals(name, another.name)
                && Objects.equals(description, another.description)
                && Objects.equals(strict, another.strict)
                && Objects.equals(parameters, another.parameters);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(name);
        h += (h << 5) + Objects.hashCode(description);
        h += (h << 5) + Objects.hashCode(strict);
        h += (h << 5) + Objects.hashCode(parameters);
        return h;
    }

    @Override
    public String toString() {
        return "Function{"
                + "name=" + name
                + ", description=" + description
                + ", strict=" + strict
                + ", parameters=" + parameters
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String name;
        private String description;
        private Boolean strict;
        private ToolParameters parameters;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder strict(Boolean strict) {
            this.strict = strict;
            return this;
        }

        public Builder parameters(ToolParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder addParameter(String name, SchemaProperty... jsonSchemaProperties) {
            addOptionalParameter(name, jsonSchemaProperties);
            this.parameters.required().add(name);
            return this;
        }

        public Builder addOptionalParameter(String name, SchemaProperty... schemaProperties) {
            if (this.parameters == null) {
                this.parameters = ToolParameters.builder().build();
            }

            Map<String, Object> schemaPropertiesMap = new HashMap<>();
            for (SchemaProperty schemaProperty : schemaProperties) {
                schemaPropertiesMap.put(schemaProperty.key(), schemaProperty.value());
            }

            this.parameters.properties().put(name, schemaPropertiesMap);
            return this;
        }

        public Function build() {
            return new Function(this);
        }
    }
}
