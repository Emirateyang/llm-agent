package com.llmagent.embedding.doubao;

import com.llmagent.llm.output.LlmResponse;
import com.llmagent.embedding.doubao.client.DefaultEmbeddingClient;
import lombok.Builder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.RetryUtil.withRetryMappingExceptions;
import static com.llmagent.embedding.doubao.DoubaoAiHelper.TEXT_MODAL_API_URL;
import static com.llmagent.embedding.doubao.DoubaoAiHelper.tokenUsageFrom;
import static java.time.Duration.ofSeconds;

/**
 * Represents a Text-Embedding model from Doubao.
 */
public class EmbeddingModel {

    private final DefaultEmbeddingClient client;
    private final String modelName;
    private final Integer dimensions;
    private final Integer maxRetries;

    @Builder
    public EmbeddingModel(MultimodalEmbeddingModelBuilder builder) {

        this.client = DefaultEmbeddingClient.builder()
                .apiKey(builder.apiKey)
                .baseUrl(getOrDefault(builder.baseUrl, TEXT_MODAL_API_URL))
                .callTimeout(getOrDefault(builder.timeout, ofSeconds(30)))
                .connectTimeout(getOrDefault(builder.timeout, ofSeconds(15)))
                .readTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .writeTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .build();
        this.modelName = builder.modelName;
        this.dimensions = builder.dimensions;
        this.maxRetries = getOrDefault(builder.maxRetries, 2);
    }

    protected Integer knownDimension() {
        if (dimensions != null) {
            return dimensions;
        }
        return EmbeddingModelName.knownDimension(modelName());
    }

    public String modelName() {
        return modelName;
    }

    public List<LlmResponse<EmbeddingOutput>> embed(List<String> content) {
        EmbeddingRequest request = EmbeddingRequest.builder().input(content).
                model(EmbeddingModelName.TEXT_MODAL_EMBEDDING_240715).build();

        EmbeddingResponse response = withRetryMappingExceptions(() -> client.embedding(request).execute(), maxRetries);

        List<LlmResponse<EmbeddingOutput>> result = new ArrayList<>();
        for (EmbeddingOutput output : response.data()) {
            result.add(LlmResponse.from(output, tokenUsageFrom(response.usage())));
        }
        return result;
    }


    public static MultimodalEmbeddingModelBuilder builder() {
        return new MultimodalEmbeddingModelBuilder();
    }

    public static class MultimodalEmbeddingModelBuilder {

        private String baseUrl;
        private String apiKey;
        private Integer maxRetries;
        private String modelName;
        private Integer dimensions;
        private Duration timeout;
        private Map<String, String> customHeaders;

        public MultimodalEmbeddingModelBuilder() {
            // This is public so it can be extended
        }

        public MultimodalEmbeddingModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public MultimodalEmbeddingModelBuilder modelName(EmbeddingModelName modelName) {
            this.modelName = modelName.toString();
            return this;
        }

        public MultimodalEmbeddingModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public MultimodalEmbeddingModelBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public MultimodalEmbeddingModelBuilder dimensions(Integer dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public MultimodalEmbeddingModelBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public MultimodalEmbeddingModelBuilder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public MultimodalEmbeddingModelBuilder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public EmbeddingModel build() {
            return new EmbeddingModel(this);
        }
    }

}
