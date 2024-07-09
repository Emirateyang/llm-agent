package com.llmagent.openai.embedding;

import java.util.HashMap;
import java.util.Map;

public enum EmbeddingModel {

    TEXT_EMBEDDING_ADA_002("text-embedding-ada-002", 1536),

    TEXT_EMBEDDING_3_SMALL("text-embedding-3-small", 1536),
    TEXT_EMBEDDING_3_LARGE("text-embedding-3-large", 3072);

    private final String value;
    private final Integer dimension;

    EmbeddingModel(String value, Integer dimension) {
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

    private static final Map<String, Integer> KNOWN_DIMENSION = new HashMap<>(EmbeddingModel.values().length);

    static {
        for (EmbeddingModel embeddingModelName : EmbeddingModel.values()) {
            KNOWN_DIMENSION.put(embeddingModelName.toString(), embeddingModelName.dimension());
        }
    }

    public static Integer knownDimension(String modelName) {
        return KNOWN_DIMENSION.get(modelName);
    }
}
