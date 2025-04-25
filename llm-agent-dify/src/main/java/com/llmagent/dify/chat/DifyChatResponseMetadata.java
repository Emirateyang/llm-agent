package com.llmagent.dify.chat;

import com.llmagent.llm.chat.response.ChatResponseMetadata;
import com.llmagent.llm.output.RetrieverResources;

import java.util.List;
import java.util.Objects;

public class DifyChatResponseMetadata extends ChatResponseMetadata {

    private final String conversationId;
    private final List<RetrieverResources> retrieverResources;

    private DifyChatResponseMetadata(Builder builder) {
        super(builder);
        this.conversationId = builder.conversationId;
        this.retrieverResources = builder.retrieverResources;
    }

    public String conversationId() {
        return conversationId;
    }

    public List<RetrieverResources> retrieverResources() {
        return retrieverResources;
    }

    @Override
    public Builder toBuilder() {
        return ((Builder) super.toBuilder(builder()))
                .conversationId(conversationId)
                .retrieverResources(retrieverResources);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DifyChatResponseMetadata that = (DifyChatResponseMetadata) o;
        return Objects.equals(conversationId, that.conversationId)
                && Objects.equals(retrieverResources, that.retrieverResources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                conversationId,
                retrieverResources
        );
    }

    @Override
    public String toString() {
        return "OpenAiChatResponseMetadata{" +
                "id='" + id() + '\'' +
                ", modelName='" + modelName() + '\'' +
                ", tokenUsage=" + tokenUsage() +
                ", finishReason=" + finishReason() +
                ", conversationId=" + conversationId +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ChatResponseMetadata.Builder<Builder> {

        private String conversationId;
        private List<RetrieverResources> retrieverResources;

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder retrieverResources(List<RetrieverResources> retrieverResources) {
            this.retrieverResources = retrieverResources;
            return this;
        }

        @Override
        public DifyChatResponseMetadata build() {
            return new DifyChatResponseMetadata(this);
        }
    }
}
