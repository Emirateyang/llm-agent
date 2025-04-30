package com.llmagent.llm.output;

import com.llmagent.data.message.ChatMessage;
import com.llmagent.llm.rag.content.Content;
import com.llmagent.llm.service.LlmServiceContext;
import com.llmagent.llm.tool.ToolExecutor;
import com.llmagent.llm.tool.ToolSpecification;

import java.util.List;
import java.util.Map;

/**
 * Parameters for creating an {@link LlmServiceTokenStream}.
 */
public class LlmServiceTokenStreamParameters {
    private final List<ChatMessage> messages;
    private final List<ToolSpecification> toolSpecifications;
    private final Map<String, ToolExecutor> toolExecutors;
    private final List<Content> retrievedContents;
    private final LlmServiceContext context;
    private final Object memoryId;

    protected LlmServiceTokenStreamParameters(Builder builder) {
        this.messages = builder.messages;
        this.toolSpecifications = builder.toolSpecifications;
        this.toolExecutors = builder.toolExecutors;
        this.retrievedContents = builder.retrievedContents;
        this.context = builder.context;
        this.memoryId = builder.memoryId;
    }

    /**
     * @return the messages
     */
    public List<ChatMessage> messages() {
        return messages;
    }

    /**
     * @return the tool specifications
     */
    public List<ToolSpecification> toolSpecifications() {
        return toolSpecifications;
    }

    /**
     * @return the tool executors
     */
    public Map<String, ToolExecutor> toolExecutors() {
        return toolExecutors;
    }

    /**
     * @return the retrieved contents
     */
    public List<Content> retrievedContents() {
        return retrievedContents;
    }

    /**
     * @return the AI service context
     */
    public LlmServiceContext context() {
        return context;
    }

    /**
     * @return the memory ID
     */
    public Object memoryId() {
        return memoryId;
    }

    /**
     * Creates a new builder for {@link LlmServiceTokenStreamParameters}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LlmServiceTokenStreamParameters}.
     */
    public static class Builder {

        private List<ChatMessage> messages;
        private List<ToolSpecification> toolSpecifications;
        private Map<String, ToolExecutor> toolExecutors;
        private List<Content> retrievedContents;
        private LlmServiceContext context;
        private Object memoryId;

        protected Builder() {
        }

        /**
         * Sets the messages.
         *
         * @param messages the messages
         * @return this builder
         */
        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Sets the tool specifications.
         *
         * @param toolSpecifications the tool specifications
         * @return this builder
         */
        public Builder toolSpecifications(List<ToolSpecification> toolSpecifications) {
            this.toolSpecifications = toolSpecifications;
            return this;
        }

        /**
         * Sets the tool executors.
         *
         * @param toolExecutors the tool executors
         * @return this builder
         */
        public Builder toolExecutors(Map<String, ToolExecutor> toolExecutors) {
            this.toolExecutors = toolExecutors;
            return this;
        }

        /**
         * Sets the retrieved contents.
         *
         * @param retrievedContents the retrieved contents
         * @return this builder
         */
        public Builder retrievedContents(List<Content> retrievedContents) {
            this.retrievedContents = retrievedContents;
            return this;
        }

        /**
         * Sets the AI service context.
         *
         * @param context the AI service context
         * @return this builder
         */
        public Builder context(LlmServiceContext context) {
            this.context = context;
            return this;
        }

        /**
         * Sets the memory ID.
         *
         * @param memoryId the memory ID
         * @return this builder
         */
        public Builder memoryId(Object memoryId) {
            this.memoryId = memoryId;
            return this;
        }

        /**
         * Builds a new {@link LlmServiceTokenStreamParameters}.
         *
         * @return a new {@link LlmServiceTokenStreamParameters}
         */
        public LlmServiceTokenStreamParameters build() {
            return new LlmServiceTokenStreamParameters(this);
        }
    }
}
