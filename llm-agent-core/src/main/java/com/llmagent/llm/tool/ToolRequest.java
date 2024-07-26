package com.llmagent.llm.tool;

import com.llmagent.util.StringUtil;

import java.util.Objects;

public class ToolRequest {
    private final String id;
    private final String name;
    private final String arguments;

    // dify only
    private String observation;

    /**
     * Creates a {@link ToolRequest} from a {@link Builder}.
     * @param builder the builder.
     */
    private ToolRequest(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.arguments = builder.arguments;
    }

    /**
     * Returns the id of the tool.
     * @return the id of the tool.
     */
    public String id() {
        return id;
    }

    /**
     * Returns the name of the tool.
     * @return the name of the tool.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the arguments of the tool.
     * @return the arguments of the tool.
     */
    public String arguments() {
        return arguments;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof ToolRequest
                && equalTo((ToolRequest) another);
    }

    private boolean equalTo(ToolRequest another) {
        return Objects.equals(id, another.id)
                && Objects.equals(name, another.name)
                && Objects.equals(arguments, another.arguments);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(id);
        h += (h << 5) + Objects.hashCode(name);
        h += (h << 5) + Objects.hashCode(arguments);
        return h;
    }

    @Override
    public String toString() {
        return "ToolRequest {"
                + " id = " + StringUtil.quoted(id)
                + ", name = " + StringUtil.quoted(name)
                + ", arguments = " + StringUtil.quoted(arguments)
                + " }";
    }

    /**
     * Creates builder to build {@link ToolRequest}.
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@code ToolRequest} builder static inner class.
     */
    public static final class Builder {
        private String id;
        private String name;
        private String arguments;

        private String observation;

        /**
         * Creates a builder for {@code ToolRequest}.
         */
        private Builder() {
        }

        /**
         * Sets the {@code id}.
         * @param id the {@code id}
         * @return the {@code Builder}
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the {@code name}.
         * @param name the {@code name}
         * @return the {@code Builder}
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the {@code arguments}.
         * @param arguments the {@code arguments}
         * @return the {@code Builder}
         */
        public Builder arguments(String arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder observation(String observation) {
            this.observation = observation;
            return this;
        }

        /**
         * Returns a {@code ToolRequest} built from the parameters previously set.
         * @return a {@code ToolRequest}
         */
        public ToolRequest build() {
            return new ToolRequest(this);
        }
    }
}
