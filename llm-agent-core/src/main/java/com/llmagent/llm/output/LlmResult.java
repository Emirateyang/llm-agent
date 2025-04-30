package com.llmagent.llm.output;

import com.llmagent.data.message.Content;
import com.llmagent.llm.tool.ToolExecution;

import java.util.List;

import static com.llmagent.util.ObjectUtil.copyIfNotNull;
import static com.llmagent.util.ValidationUtil.ensureNotNull;

/**
 * Represents the result of an LLM Service invocation.
 * It contains actual content (LLM response) and additional information associated with it,
 * such as {@link TokenUsage} and sources ({@link Content}s retrieved during RAG).
 *
 * @param <T> The type of the content. Can be of any return type supported by LLM Services
 */
public class LlmResult<T> {

        private final T content;
        private final TokenUsage tokenUsage;
        private final List<Content> sources;
        private final FinishReason finishReason;
        private final List<ToolExecution> toolExecutions;

    public LlmResult(T content, TokenUsage tokenUsage, List<Content> sources, FinishReason finishReason, List<ToolExecution> toolExecutions) {
            this.content = ensureNotNull(content, "content");
            this.tokenUsage = tokenUsage;
            this.sources = copyIfNotNull(sources);
            this.finishReason = finishReason;
            this.toolExecutions = copyIfNotNull(toolExecutions);
        }

        public static <T> ResultBuilder<T> builder() {
            return new ResultBuilder<T>();
        }

        public T content() {
            return content;
        }

        public TokenUsage tokenUsage() {
            return tokenUsage;
        }

        public List<Content> sources() {
            return sources;
        }

        public FinishReason finishReason() {
            return finishReason;
        }

        public List<ToolExecution> toolExecutions() {
            return toolExecutions;
        }

        public static class ResultBuilder<T> {
            private T content;
            private TokenUsage tokenUsage;
            private List<Content> sources;
            private FinishReason finishReason;
            private List<ToolExecution> toolExecutions;

            ResultBuilder() {
            }

            public ResultBuilder<T> content(T content) {
                this.content = content;
                return this;
            }

            public ResultBuilder<T> tokenUsage(TokenUsage tokenUsage) {
                this.tokenUsage = tokenUsage;
                return this;
            }

            public ResultBuilder<T> sources(List<Content> sources) {
                this.sources = sources;
                return this;
            }

            public ResultBuilder<T> finishReason(FinishReason finishReason) {
                this.finishReason = finishReason;
                return this;
            }

            public ResultBuilder<T> toolExecutions(List<ToolExecution> toolExecutions) {
                this.toolExecutions = toolExecutions;
                return this;
            }

            public LlmResult<T> build() {
                return new LlmResult<T>(this.content, this.tokenUsage, this.sources, this.finishReason, this.toolExecutions);
            }
        }
}
