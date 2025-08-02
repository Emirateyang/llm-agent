package com.llmagent.embedding.siliconflow;

import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
public final class EmbeddingOutput {

    private Integer index;
    private List<Float> embedding;
    private String object;

    public Integer index() {
        return this.getIndex();
    }

    public List<Float> embedding() {
        return this.getEmbedding();
    }

    private boolean equalTo(EmbeddingOutput another) {
        return Objects.equals(index, another.index)
                && Objects.equals(embedding, another.embedding);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(index);
        h += (h << 5) + Objects.hashCode(embedding);
        return h;
    }

    @Override
    public String toString() {
        return "EmbeddingOutput{"
                + "index=" + index
                + "embedding=" + embedding
                + "}";
    }
}
