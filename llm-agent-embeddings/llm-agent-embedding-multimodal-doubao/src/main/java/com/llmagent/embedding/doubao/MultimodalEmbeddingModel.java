package com.llmagent.embedding.doubao;

import com.llmagent.embedding.doubao.client.DefaultMultimodalEmbeddingClient;
import com.llmagent.llm.output.LlmResponse;
import lombok.Builder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.llmagent.embedding.doubao.DoubaoAiHelper.MULTI_MODAL_API_URL;
import static com.llmagent.embedding.doubao.DoubaoAiHelper.tokenUsageFrom;
import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.RetryUtil.withRetryMappingExceptions;
import static java.time.Duration.ofSeconds;

/**
 * Represents an Multimodal-Embedding model from Doubao.
 */
public class MultimodalEmbeddingModel {

    private final DefaultMultimodalEmbeddingClient client;
    private final String modelName;
    private final Integer dimensions;
    private final Integer maxRetries;

    @Builder
    public MultimodalEmbeddingModel(MultimodalEmbeddingModelBuilder builder) {

        this.client = DefaultMultimodalEmbeddingClient.builder()
                .apiKey(builder.apiKey)
                .baseUrl(getOrDefault(builder.baseUrl, MULTI_MODAL_API_URL))
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

    public LlmResponse<EmbeddingOutput> embedImage(String imageUrl) {
        Map<String, String> content = new HashMap<>();
        content.put("url", imageUrl);
        return embedImage(content);
    }

    public LlmResponse<EmbeddingOutput> embedImage(String format, String base64) {
        Map<String, String> content = new HashMap<>();
        content.put("url", "data:image/" + format + ";base64," + base64);
        return embedImage(content);
    }

    private LlmResponse<EmbeddingOutput> embedImage(Map<String, String> content) {

        Map<String, Object> image = new HashMap<>();
        image.put("image_url", content);
        image.put("type", "image_url");

        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(image);

        EmbeddingRequest request = EmbeddingRequest.builder().input(contents).
                model(EmbeddingModelName.MULTI_MODAL_EMBEDDING_250328).build();

        EmbeddingResponse response = withRetryMappingExceptions(() -> client.embedding(request).execute(), maxRetries);

        return LlmResponse.from(response.data(), tokenUsageFrom(response.usage()));
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

        public MultimodalEmbeddingModel build() {
            return new MultimodalEmbeddingModel(this);
        }
    }

}
