package com.llmagent.embedding.doubao;

import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
public final class EmbeddingOutput {

    private List<Float> embedding;
    private String object;

    public List<Float> embedding() {
        return this.getEmbedding();
    }

    private boolean equalTo(EmbeddingOutput another) {
        return Objects.equals(embedding, another.embedding);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(embedding);
        return h;
    }

    @Override
    public String toString() {
        return "EmbeddingOutput{"
                + "embedding=" + embedding
                + "}";
    }
}
