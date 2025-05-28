package com.llmagent.embedding.dashscope;

import java.util.HashMap;
import java.util.Map;

public enum EmbeddingModelName {

    MULTI_MODAL_EMBEDDING_V1("multimodal-embedding-v1", 1024);

    private final String value;
    private final Integer dimension;

    EmbeddingModelName(String value, Integer dimension) {
        this.value = value;
        this.dimension = dimension;
    }

    @Override
    public String toString() {
        return value;
    }

    public Integer dimension() {
        return dimension;
    }

    private static final Map<String, Integer> KNOWN_DIMENSION = new HashMap<>(EmbeddingModelName.values().length);

    static {
        for (EmbeddingModelName embeddingModelName : EmbeddingModelName.values()) {
            KNOWN_DIMENSION.put(embeddingModelName.toString(), embeddingModelName.dimension());
        }
    }

    public static Integer knownDimension(String modelName) {
        return KNOWN_DIMENSION.get(modelName);
    }
}
