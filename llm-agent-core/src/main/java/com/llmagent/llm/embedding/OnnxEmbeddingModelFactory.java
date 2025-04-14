package com.llmagent.llm.embedding;

/**
 * A factory for creating {@link EmbeddingModel} instances.
 */
public interface OnnxEmbeddingModelFactory {

    EmbeddingModel create(String modelPath);
}
