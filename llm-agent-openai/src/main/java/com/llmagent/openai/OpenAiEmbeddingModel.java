package com.llmagent.openai;

import com.llmagent.data.segment.TextSegment;
import com.llmagent.llm.embedding.DimensionAwareEmbeddingModel;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.openai.client.OpenAiClient;
import com.llmagent.openai.embedding.EmbeddingModelName;
import com.llmagent.openai.embedding.EmbeddingRequest;
import com.llmagent.openai.embedding.EmbeddingResponse;
import com.llmagent.vector.store.VectorData;
import lombok.Builder;

import java.net.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.llmagent.openai.OpenAiHelper.tokenUsageFrom;
import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.RetryUtil.withRetryMappingExceptions;
import static com.llmagent.util.ServiceHelper.loadFactories;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

/**
 * Represents an OpenAI embedding model, such as text-embedding-ada-002.
 */
public class OpenAiEmbeddingModel extends DimensionAwareEmbeddingModel {

    private final OpenAiClient client;
    private final String modelName;
    private final Integer dimensions;
    private final String user;
    private final Integer maxRetries;
    private final Integer maxSegmentsPerBatch;

    @Builder
    public OpenAiEmbeddingModel(OpenAiEmbeddingModelBuilder builder) {

        this.client = OpenAiClient.builder()
                .openAiApiKey(builder.apiKey)
                .baseUrl(builder.baseUrl)
                .organizationId(builder.organizationId)
                .projectId(builder.projectId)
                .callTimeout(getOrDefault(builder.timeout, ofSeconds(30)))
                .connectTimeout(getOrDefault(builder.timeout, ofSeconds(15)))
                .readTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .writeTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .proxy(builder.proxy)
                .logRequests(builder.logRequests)
                .logResponses(builder.logResponses)
                .userAgent(OpenAiHelper.DEFAULT_USER_AGENT)
                .customHeaders(builder.customHeaders)
                .build();
        this.modelName = builder.modelName;
        this.dimensions = builder.dimensions;
        this.user = builder.user;
        this.maxRetries = getOrDefault(builder.maxRetries, 2);
        this.maxSegmentsPerBatch = getOrDefault(builder.maxSegmentsPerBatch, 2048);
    }

    @Override
    protected Integer knownDimension() {
        if (dimensions != null) {
            return dimensions;
        }
        return EmbeddingModelName.knownDimension(modelName());
    }

    public String modelName() {
        return modelName;
    }

    public LlmResponse<List<VectorData>> embedAll(List<TextSegment> textSegments) {
        List<String> texts = textSegments.stream()
                .map(TextSegment::text)
                .collect(toList());

        List<List<String>> textBatches = partition(texts, maxSegmentsPerBatch);

        return embedBatchedTexts(textBatches);
    }

    private List<List<String>> partition(List<String> inputList, int size) {
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < inputList.size(); i += size) {
            int toIndex = Math.min(i + size, inputList.size());
            result.add(inputList.subList(i, toIndex));
        }
        return result;
    }

    private LlmResponse<List<VectorData>> embedTexts(List<String> texts) {

        EmbeddingRequest request = EmbeddingRequest.builder()
                .input(texts)
                .model(modelName)
                .dimensions(dimensions)
                .user(user)
                .build();

        EmbeddingResponse response = withRetryMappingExceptions(() -> client.embedding(request).execute(), maxRetries);

        List<VectorData> embeddings = response.data().stream()
                .map(embedding -> VectorData.from(embedding.embedding()))
                .collect(toList());

        return LlmResponse.from(embeddings, tokenUsageFrom(response.usage()));
    }

    private LlmResponse<List<VectorData>> embedBatchedTexts(List<List<String>> textBatches) {
        List<LlmResponse<List<VectorData>>> responses = new ArrayList<>();
        for (List<String> batch : textBatches) {
            LlmResponse<List<VectorData>> response = embedTexts(batch);
            responses.add(response);
        }
        return LlmResponse.from(
                responses.stream()
                        .flatMap(response -> response.content().stream())
                        .toList(),
                responses.stream()
                        .map(LlmResponse::tokenUsage)
                        .filter(Objects::nonNull)
                        .reduce(TokenUsage::sum)
                        .orElse(null));
    }

    public static OpenAiEmbeddingModelBuilder builder() {
        for (OpenAiEmbeddingModelBuilderFactory factory : loadFactories(OpenAiEmbeddingModelBuilderFactory.class)) {
            return factory.get();
        }
        return new OpenAiEmbeddingModelBuilder();
    }

    public static class OpenAiEmbeddingModelBuilder {

        private String baseUrl;
        private String apiKey;
        private String organizationId;
        private String projectId;

        private String modelName;
        private Integer dimensions;
        private String user;
        private Duration timeout;
        private Integer maxRetries;
        private Integer maxSegmentsPerBatch;
        private Boolean logRequests;
        private Boolean logResponses;
        private Map<String, String> customHeaders;
        private Proxy proxy;

        public OpenAiEmbeddingModelBuilder() {
            // This is public so it can be extended
        }

        public OpenAiEmbeddingModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public OpenAiEmbeddingModelBuilder modelName(EmbeddingModelName modelName) {
            this.modelName = modelName.toString();
            return this;
        }

        public OpenAiEmbeddingModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public OpenAiEmbeddingModelBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public OpenAiEmbeddingModelBuilder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public OpenAiEmbeddingModelBuilder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public OpenAiEmbeddingModelBuilder dimensions(Integer dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public OpenAiEmbeddingModelBuilder user(String user) {
            this.user = user;
            return this;
        }

        public OpenAiEmbeddingModelBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public OpenAiEmbeddingModelBuilder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public OpenAiEmbeddingModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public OpenAiEmbeddingModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public OpenAiEmbeddingModelBuilder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public OpenAiEmbeddingModelBuilder maxSegmentsPerBatch(Integer maxSegmentsPerBatch) {
            this.maxSegmentsPerBatch = maxSegmentsPerBatch;
            return this;
        }

        public OpenAiEmbeddingModelBuilder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public OpenAiEmbeddingModel build() {
            return new OpenAiEmbeddingModel(this);
        }
    }

}
