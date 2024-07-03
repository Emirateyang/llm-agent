package com.llmagent.llm.tool;

import com.llmagent.util.StringUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ToolSpecification {
    private final String name;
    private final String description;
    private final ToolParameters parameters;

    /**
     * Creates a {@link ToolSpecification} from a {@link Builder}.
     * @param builder the builder.
     */
    private ToolSpecification(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.parameters = builder.parameters;
    }

    /**
     * Returns the name of the tool.
     * @return the name of the tool.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the description of the tool.
     * @return the description of the tool.
     */
    public String description() {
        return description;
    }

    /**
     * Returns the parameters of the tool.
     * @return the parameters of the tool.
     */
    public ToolParameters parameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof ToolSpecification
                && equalTo((ToolSpecification) another);
    }

    private boolean equalTo(ToolSpecification another) {
        return Objects.equals(name, another.name)
                && Objects.equals(description, another.description)
                && Objects.equals(parameters, another.parameters);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(name);
        h += (h << 5) + Objects.hashCode(description);
        h += (h << 5) + Objects.hashCode(parameters);
        return h;
    }

    @Override
    public String toString() {
        return "ToolSpecification {"
                + " name = " + StringUtil.quoted(name)
                + ", description = " + StringUtil.quoted(description)
                + ", parameters = " + parameters
                + " }";
    }

    /**
     * Creates builder to build {@link ToolSpecification}.
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@code ToolSpecification} builder static inner class.
     */
    public static final class Builder {

        private String name;
        private String description;
        private ToolParameters parameters;

        /**
         * Creates a {@link Builder}.
         */
        private Builder() {
        }

        /**
         * Sets the {@code name}.
         * @param name the {@code name}
         * @return {@code this}
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the {@code description}.
         * @param description the {@code description}
         * @return {@code this}
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the {@code parameters}.
         * @param parameters the {@code parameters}
         * @return {@code this}
         */
        public Builder parameters(ToolParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Adds a parameter to the tool.
         * @param name the name of the parameter.
         * @param schemaProperties the properties of the parameter.
         * @return {@code this}
         */
        public Builder addParameter(String name, SchemaProperty... schemaProperties) {
            return addParameter(name, Arrays.asList(schemaProperties));
        }

        /**
         * Adds a parameter to the tool.
         * @param name the name of the parameter.
         * @param schemaProperties the properties of the parameter.
         * @return {@code this}
         */
        public Builder addParameter(String name, Iterable<SchemaProperty> schemaProperties) {
            addOptionalParameter(name, schemaProperties);
            this.parameters.required().add(name);
            return this;
        }

        /**
         * Adds an optional parameter to the tool.
         * @param name the name of the parameter.
         * @param schemaProperties the properties of the parameter.
         * @return {@code this}
         */
        public Builder addOptionalParameter(String name, SchemaProperty... schemaProperties) {
            return addOptionalParameter(name, Arrays.asList(schemaProperties));
        }

        /**
         * Adds an optional parameter to the tool.
         * @param name the name of the parameter.
         * @param schemaProperties the properties of the parameter.
         * @return {@code this}
         */
        public Builder addOptionalParameter(String name, Iterable<SchemaProperty> schemaProperties) {
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

        /**
         * Returns a {@code ToolSpecification} built from the parameters previously set.
         * @return a {@code ToolSpecification} built with parameters of this {@code ToolSpecification.Builder}
         */
        public ToolSpecification build() {
            return new ToolSpecification(this);
        }
    }
}
