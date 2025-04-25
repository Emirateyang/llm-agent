package com.llmagent.azure.embedding;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.EmbeddingItem;
import com.azure.ai.openai.models.Embeddings;
import com.azure.ai.openai.models.EmbeddingsOptions;
import com.azure.core.credential.KeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.ProxyOptions;
import com.llmagent.azure.AzureAiEmbeddingModelBuilderFactory;
import com.llmagent.data.segment.TextSegment;
import com.llmagent.llm.embedding.DimensionAwareEmbeddingModel;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.vector.store.VectorData;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.llmagent.azure.AzureAiHelper.setupSyncClient;
import static com.llmagent.azure.embedding.AzureAiEmbeddingModelName.TEXT_EMBEDDING_ADA_002;
import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.ServiceHelper.loadFactories;
import static com.llmagent.vector.store.VectorData.from;
import static java.util.stream.Collectors.toList;

public class AzureAiEmbeddingModel extends DimensionAwareEmbeddingModel {

    private static final int BATCH_SIZE = 16;

    private OpenAIClient client;
    private final String deploymentName;
    private final Integer dimensions;

    private AzureAiEmbeddingModel(OpenAIClient client,
                                  String deploymentName,
                                  Integer dimensions) {
        this(deploymentName, dimensions);
        this.client = client;
    }

    public AzureAiEmbeddingModel(
         String endpoint,
         String serviceVersion,
         String apiKey,
         String deploymentName,
         Duration timeout,
         Integer maxRetries,
         ProxyOptions proxyOptions,
         boolean logRequestsAndResponses,
         String userAgentSuffix,
         Integer dimensions,
         Map<String, String> customHeaders) {

        this(deploymentName, dimensions);
        this.client = setupSyncClient(endpoint, serviceVersion, apiKey, timeout, maxRetries, proxyOptions, logRequestsAndResponses, userAgentSuffix, customHeaders);
    }

    public AzureAiEmbeddingModel(
         String endpoint,
         String serviceVersion,
         KeyCredential keyCredential,
         String deploymentName,
         Duration timeout,
         Integer maxRetries,
         ProxyOptions proxyOptions,
         boolean logRequestsAndResponses,
         String userAgentSuffix,
         Integer dimensions,
         Map<String, String> customHeaders) {

        this(deploymentName, dimensions);
        this.client = setupSyncClient(endpoint, serviceVersion, keyCredential, timeout, maxRetries, proxyOptions, logRequestsAndResponses, userAgentSuffix, customHeaders);
    }

    public AzureAiEmbeddingModel(
        String endpoint,
        String serviceVersion,
        TokenCredential tokenCredential,
        String deploymentName,
        Duration timeout,
        Integer maxRetries,
        ProxyOptions proxyOptions,
        boolean logRequestsAndResponses,
        String userAgentSuffix,
        Integer dimensions,
        Map<String, String> customHeaders) {

        this(deploymentName, dimensions);
        this.client = setupSyncClient(endpoint, serviceVersion, tokenCredential, timeout, maxRetries, proxyOptions, logRequestsAndResponses, userAgentSuffix, customHeaders);
    }

    private AzureAiEmbeddingModel(String deploymentName,
                                      Integer dimensions) {

        this.deploymentName = getOrDefault(deploymentName, TEXT_EMBEDDING_ADA_002.modelName());
        this.dimensions = dimensions;
    }

    /**
     * Embeds the provided text segments, processing a maximum of 16 segments at a time.
     * For more information, refer to the documentation <a href="https://learn.microsoft.com/en-us/azure/ai-services/openai/faq#i-am-trying-to-use-embeddings-and-received-the-error--invalidrequesterror--too-many-inputs--the-max-number-of-inputs-is-1---how-do-i-fix-this-">here</a>.
     *
     * @param textSegments A list of text segments.
     * @return A list of corresponding embeddings.
     */
    @Override
    public LlmResponse<List<VectorData>> embedAll(List<TextSegment> textSegments) {

        List<String> texts = textSegments.stream()
                .map(TextSegment::text)
                .collect(toList());

        return embedTexts(texts);
    }

    private LlmResponse<List<VectorData>> embedTexts(List<String> texts) {

        List<VectorData> embeddings = new ArrayList<>();

        int inputTokenCount = 0;
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {

            List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));

            EmbeddingsOptions options = new EmbeddingsOptions(batch);
            Embeddings response = client.getEmbeddings(deploymentName, options);

            for (EmbeddingItem embeddingItem : response.getData()) {
                VectorData embedding = from(embeddingItem.getEmbedding());
                embeddings.add(embedding);
            }
            inputTokenCount += response.getUsage().getPromptTokens();
        }

        return LlmResponse.from(
                embeddings,
                new TokenUsage(inputTokenCount)
        );
    }


    public static Builder builder() {
        for (AzureAiEmbeddingModelBuilderFactory factory : loadFactories(AzureAiEmbeddingModelBuilderFactory.class)) {
            return factory.get();
        }
        return new Builder();
    }

    @Override
    protected Integer knownDimension() {
        if(dimensions != null)
            return dimensions;
        return AzureAiEmbeddingModelName.knownDimension(deploymentName);
    }

    public static class Builder {

        private String endpoint;
        private String serviceVersion;
        private String apiKey;
        private KeyCredential keyCredential;
        private TokenCredential tokenCredential;
        private String deploymentName;
        private Duration timeout;
        private Integer maxRetries;
        private ProxyOptions proxyOptions;
        private boolean logRequestsAndResponses;
        private OpenAIClient openAIClient;
        private String userAgentSuffix;
        private Integer dimensions;
        private Map<String, String> customHeaders;

        /**
         * Sets the Azure OpenAI endpoint. This is a mandatory parameter.
         *
         * @param endpoint The Azure OpenAI endpoint in the format: https://{resource}.openai.azure.com/
         * @return builder
         */
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * Sets the Azure OpenAI API service version. This is a mandatory parameter.
         *
         * @param serviceVersion The Azure OpenAI API service version in the format: 2023-05-15
         * @return builder
         */
        public Builder serviceVersion(String serviceVersion) {
            this.serviceVersion = serviceVersion;
            return this;
        }

        /**
         * Sets the Azure OpenAI API key.
         *
         * @param apiKey The Azure OpenAI API key.
         * @return builder
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Used to authenticate with the OpenAI service, instead of Azure OpenAI.
         * This automatically sets the endpoint to https://api.openai.com/v1.
         *
         * @param nonAzureApiKey The non-Azure OpenAI API key
         * @return builder
         */
        public Builder nonAzureApiKey(String nonAzureApiKey) {
            this.keyCredential = new KeyCredential(nonAzureApiKey);
            this.endpoint = "https://api.openai.com/v1";
            return this;
        }

        /**
         * Used to authenticate to Azure OpenAI with Azure Active Directory credentials.
         *
         * @param tokenCredential the credentials to authenticate with Azure Active Directory
         * @return builder
         */
        public Builder tokenCredential(TokenCredential tokenCredential) {
            this.tokenCredential = tokenCredential;
            return this;
        }

        /**
         * Sets the deployment name in Azure OpenAI. This is a mandatory parameter.
         *
         * @param deploymentName The Deployment name.
         * @return builder
         */
        public Builder deploymentName(String deploymentName) {
            this.deploymentName = deploymentName;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder proxyOptions(ProxyOptions proxyOptions) {
            this.proxyOptions = proxyOptions;
            return this;
        }

        public Builder logRequestsAndResponses(boolean logRequestsAndResponses) {
            this.logRequestsAndResponses = logRequestsAndResponses;
            return this;
        }

        /**
         * Sets the Azure OpenAI client. This is an optional parameter, if you need more flexibility than using the endpoint, serviceVersion, apiKey, deploymentName parameters.
         *
         * @param openAIClient The Azure OpenAI client.
         * @return builder
         */
        public Builder openAIClient(OpenAIClient openAIClient) {
            this.openAIClient = openAIClient;
            return this;
        }

        public Builder userAgentSuffix(String userAgentSuffix) {
            this.userAgentSuffix = userAgentSuffix;
            return this;
        }

        public Builder dimensions(Integer dimensions){
            this.dimensions = dimensions;
            return this;
        }

        public Builder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public AzureAiEmbeddingModel build() {
            if (openAIClient == null) {
                if (tokenCredential != null) {
                    return new AzureAiEmbeddingModel(
                            endpoint,
                            serviceVersion,
                            tokenCredential,
                            deploymentName,
                            timeout,
                            maxRetries,
                            proxyOptions,
                            logRequestsAndResponses,
                            userAgentSuffix,
                            dimensions,
                            customHeaders
                    );
                } else if (keyCredential != null) {
                    return new AzureAiEmbeddingModel(
                            endpoint,
                            serviceVersion,
                            keyCredential,
                            deploymentName,
                            timeout,
                            maxRetries,
                            proxyOptions,
                            logRequestsAndResponses,
                            userAgentSuffix,
                            dimensions,
                            customHeaders
                    );
                }
                return new AzureAiEmbeddingModel(
                        endpoint,
                        serviceVersion,
                        apiKey,
                        deploymentName,
                        timeout,
                        maxRetries,
                        proxyOptions,
                        logRequestsAndResponses,
                        userAgentSuffix,
                        dimensions,
                        customHeaders
                );
            } else {
                return new AzureAiEmbeddingModel(
                        openAIClient,
                        deploymentName,
                        dimensions
                );
            }
        }
    }
}
