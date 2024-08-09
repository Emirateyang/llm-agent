package com.llmagent.azure;

import com.llmagent.azure.embedding.AzureAiEmbeddingModel;

import java.util.function.Supplier;

public interface AzureAiEmbeddingModelBuilderFactory extends Supplier<AzureAiEmbeddingModel.Builder> {
}
