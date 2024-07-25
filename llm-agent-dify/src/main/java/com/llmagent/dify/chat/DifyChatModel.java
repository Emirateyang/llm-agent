package com.llmagent.dify.chat;

import com.llmagent.dify.client.DifyClient;
import com.llmagent.data.message.AiMessage;
import com.llmagent.data.message.ChatMessage;
import com.llmagent.dify.exception.DifyHttpException;
import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.util.ObjectUtil;
import lombok.Builder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.llmagent.dify.DifyHelper.*;
import static com.llmagent.util.RetryUtil.withRetry;

public class DifyChatModel implements ChatLanguageModel {

    private final DifyClient client;
    private final int maxRetries;

    private final String user;
    private final String conversationId;
    private final String responseMode;

    private final Map<String, Object> inputs;
    private final boolean autoGenerateName;
    private final List<DifyFileContent> files;

    @Builder
    public DifyChatModel(String baseUrl,
                           String apiKey,
                           String user,
                           String conversationId,
                           Map<String, Object> inputs,
                           boolean autoGenerateName,
                           List<DifyFileContent> files,
                           String responseMode,
                           Duration timeout,
                           Integer maxRetries,
                           Boolean logRequests,
                           Boolean logResponses) {

        timeout = ObjectUtil.getOrDefault(timeout, Duration.ofSeconds(60));
        inputs = ObjectUtil.getOrDefault(inputs, Map.of());
        responseMode = ObjectUtil.getOrDefault(responseMode, ResponseMode.BLOCKING.toString());

        this.client = DifyClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .callTimeout(timeout)
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();

        this.user = user;
        this.conversationId = conversationId;
        this.responseMode = responseMode;
        this.inputs = inputs;
        this.autoGenerateName = autoGenerateName;
        this.files = files;

        this.maxRetries = ObjectUtil.getOrDefault(maxRetries, 3);
    }

    public static DifyChatModel withApiKey(String apiKey) {
        return builder().apiKey(apiKey).build();
    }

    @Override
    public LlmResponse<AiMessage> generate(List<ChatMessage> messages) {

        DifyMessageRequest.Builder requestBuilder = DifyMessageRequest.builder()
                .inputs(inputs)
                .query(toDifyMessage(messages))
                .responseMode(responseMode)
                .conversationId(conversationId)
                .user(user);
        if (this.files != null) {
            requestBuilder.files(files);
        }

        DifyMessageRequest request = requestBuilder.build();

        try {
            DifyChatCompletionResponse chatCompletionResponse = withRetry(() -> client.chatCompletion(request).execute(), maxRetries);

            return LlmResponse.from(
                aiMessageFrom(chatCompletionResponse),
                tokenUsageFrom(chatCompletionResponse.getMetadata().getUsage()),
                retrieverResourceFrom(chatCompletionResponse.getMetadata().getRetrieverResources())
            );
        } catch (RuntimeException ex) {
            throw ex;
        }
    }
}
