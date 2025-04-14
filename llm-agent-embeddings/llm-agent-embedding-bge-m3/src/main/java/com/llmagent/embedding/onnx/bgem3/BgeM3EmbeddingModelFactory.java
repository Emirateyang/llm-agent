package com.llmagent.embedding.onnx.bgem3;

import com.llmagent.llm.embedding.EmbeddingModel;
import com.llmagent.llm.embedding.OnnxEmbeddingModelFactory;

public class BgeM3EmbeddingModelFactory implements OnnxEmbeddingModelFactory {
    @Override
    public EmbeddingModel create(String modelPath) {
        return new BgeM3EmbeddingModel(modelPath);
    }
}
