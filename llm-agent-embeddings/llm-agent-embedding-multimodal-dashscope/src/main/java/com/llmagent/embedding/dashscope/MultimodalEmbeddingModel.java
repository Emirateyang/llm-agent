package com.llmagent.embedding.dashscope;

import com.llmagent.data.segment.TextSegment;
import com.llmagent.embedding.dashscope.client.DefaultMultimodalEmbeddingClient;
import com.llmagent.llm.embedding.DimensionAwareEmbeddingModel;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.vector.store.MultimodalEmbeddingOutput;
import com.llmagent.vector.store.VectorData;
import lombok.Builder;

import java.net.Proxy;
import java.time.Duration;
import java.util.*;

import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.RetryUtil.withRetryMappingExceptions;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

/**
 * Represents an Multimodal-Embedding model from dashscope Aliyun.
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
                .baseUrl(getOrDefault(builder.baseUrl, "https://dashscope.aliyuncs.com/api/v1/"))
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

    public LlmResponse<MultimodalEmbeddingOutput> embedImage(String imageUrl) {
        Map<String, String> content = new HashMap<>();
        content.put("image", imageUrl);
        return embedImage(content);
    }

    public LlmResponse<MultimodalEmbeddingOutput> embedImage(String format, String base64) {
        Map<String, String> content = new HashMap<>();
        content.put("image", "data:image/" + format + ";base64," + base64);
        return embedImage(content);
    }

    private LlmResponse<MultimodalEmbeddingOutput> embedImage(Map<String, String> content) {

        List<Map<String, String>> contents = new ArrayList<>();
        contents.add(content);
        Map<String, Object> input = new HashMap<>();
        input.put("contents", contents);

        EmbeddingRequest request = EmbeddingRequest.builder().input(input).
                model(EmbeddingModelName.MULTI_MODAL_EMBEDDING_V1).build();

        EmbeddingResponse response = withRetryMappingExceptions(() -> client.embedding(request).execute(), maxRetries);

        return LlmResponse.from(response.output(), response.usage());
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
