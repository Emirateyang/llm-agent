package com.llmagent.openai;

import com.llmagent.data.message.ChatMessage;
import com.llmagent.data.segment.TextSegment;
import com.llmagent.llm.Tokenizer;
import com.llmagent.llm.chat.TokenCountEstimator;
import com.llmagent.llm.embedding.DimensionAwareEmbeddingModel;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.openai.client.OpenAiClient;
import com.llmagent.openai.embedding.EmbeddingModel;
import com.llmagent.openai.embedding.EmbeddingRequest;
import com.llmagent.openai.embedding.EmbeddingResponse;
import com.llmagent.util.ObjectUtil;
import com.llmagent.vector.store.VectorData;
import lombok.Builder;

import java.net.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.llmagent.openai.OpenAiHelper.tokenUsageFrom;
import static com.llmagent.util.RetryUtil.withRetry;
import static java.util.stream.Collectors.toList;

public class OpenAiEmbeddingModel extends DimensionAwareEmbeddingModel implements TokenCountEstimator {

    private final OpenAiClient client;
    private final String modelName;
    private final Integer dimensions;
    private final String user;
    private final Integer maxRetries;
    private final Tokenizer tokenizer;

    @Builder
    public OpenAiEmbeddingModel(String baseUrl,
                                String apiKey,
                                String organizationId,
                                String modelName,
                                Integer dimensions,
                                String user,
                                Duration timeout,
                                Integer maxRetries,
                                Proxy proxy,
                                Boolean logRequests,
                                Boolean logResponses,
                                Tokenizer tokenizer,
                                Map<String, String> customHeaders) {

        baseUrl = ObjectUtil.getOrDefault(baseUrl, OpenAiHelper.OPENAI_URL);

        timeout = ObjectUtil.getOrDefault(timeout, Duration.ofSeconds(60));

        this.client = OpenAiClient.builder()
                .openAiApiKey(apiKey)
                .baseUrl(baseUrl)
                .organizationId(organizationId)
                .callTimeout(timeout)
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .proxy(proxy)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .userAgent(OpenAiHelper.DEFAULT_USER_AGENT)
                .customHeaders(customHeaders)
                .build();
        this.modelName = ObjectUtil.getOrDefault(modelName, EmbeddingModel.TEXT_EMBEDDING_ADA_002.toString());
        this.dimensions = dimensions;
        this.user = user;
        this.maxRetries = ObjectUtil.getOrDefault(maxRetries, 3);
        this.tokenizer = ObjectUtil.getOrDefault(tokenizer, OpenAiTokenizer::new);
    }

    @Override
    protected Integer knownDimension() {
        if (dimensions != null) {
            return dimensions;
        }
        return EmbeddingModel.knownDimension(modelName());
    }

    public String modelName() {
        return modelName;
    }

    public LlmResponse<List<VectorData>> embedAll(List<TextSegment> textSegments) {
        List<String> texts = textSegments.stream()
                .map(TextSegment::text)
                .collect(toList());

        return embedTexts(texts);
    }

    private LlmResponse<List<VectorData>> embedTexts(List<String> texts) {

        EmbeddingRequest request = EmbeddingRequest.builder()
                .input(texts)
                .model(modelName)
                .dimensions(dimensions)
                .user(user)
                .build();

        EmbeddingResponse response = withRetry(() -> client.embedding(request).execute(), maxRetries);

        List<VectorData> embeddings = response.data().stream()
                .map(embedding -> VectorData.from(embedding.embedding()))
                .collect(toList());

        return LlmResponse.from(embeddings, tokenUsageFrom(response.usage()));
    }

    @Override
    public int estimateTokenCount(String text) {
        return tokenizer.estimateTokenCountInText(text);
    }

    @Override
    public int estimateTokenCount(List<ChatMessage> messages) {
        return tokenizer.estimateTokenCountInMessages(messages);
    }
}
