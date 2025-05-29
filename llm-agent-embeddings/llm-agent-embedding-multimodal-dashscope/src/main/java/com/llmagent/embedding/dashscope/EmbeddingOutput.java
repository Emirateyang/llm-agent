package com.llmagent.embedding.dashscope;

import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
public final class EmbeddingOutput {

    private List<Embeddings> embeddings;

    public List<Embeddings> embeddings() {
        return this.getEmbeddings();
    }

    @Data
    public static class Embeddings {
        private int index;
        private List<Float> embedding;
        private String type;

        private boolean equalTo(Embeddings another) {
            return Objects.equals(index, another.index)
                    && Objects.equals(embedding, another.embedding)
                    && Objects.equals(type, another.type);
        }

        @Override
        public int hashCode() {
            int h = 5381;
            h += (h << 5) + Objects.hashCode(index);
            h += (h << 5) + Objects.hashCode(embedding);
            h += (h << 5) + Objects.hashCode(type);
            return h;
        }

        @Override
        public String toString() {
            return "Embeddings{"
                    + "index=" + index
                    + "embedding=" + embedding
                    + "type=" + type
                    + "}";
        }
    }

    private boolean equalTo(EmbeddingOutput another) {
        return Objects.equals(embeddings, another.embeddings);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(embeddings);
        return h;
    }

    @Override
    public String toString() {
        return "EmbeddingOutput{"
                + "embeddings=" + embeddings
                + "}";
    }
}
