package com.llmagent.dify.chat;

import com.llmagent.dify.client.DifyClient;
import com.llmagent.data.message.AiMessage;
import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.util.ObjectUtil;
import lombok.Builder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.llmagent.dify.DifyHelper.*;
import static com.llmagent.util.RetryUtil.withRetryMappingExceptions;

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
                           Duration timeout,
                           Integer maxRetries,
                           Boolean logRequests,
                           Boolean logResponses) {

        timeout = ObjectUtil.getOrDefault(timeout, Duration.ofSeconds(60));
        inputs = ObjectUtil.getOrDefault(inputs, Map.of());
        String responseMode = ResponseMode.BLOCKING.toString();

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

//    @Override
//    public LlmResponse<AiMessage> generate(List<ChatMessage> messages) {
//
//        DifyMessageRequest.Builder requestBuilder = DifyMessageRequest.builder()
//                .inputs(inputs)
//                .query(toDifyMessage(messages))
//                .responseMode(responseMode)
//                .conversationId(conversationId)
//                .autoGenerateName(autoGenerateName)
//                .user(user);
//        if (this.files != null) {
//            requestBuilder.files(files);
//        }
//
//        DifyMessageRequest request = requestBuilder.build();
//
//        try {
//            DifyChatCompletionResponse chatCompletionResponse = withRetry(() -> client.chatCompletion(request).execute(), maxRetries);
//
//            return LlmResponse.from(
//                aiMessageFrom(chatCompletionResponse),
//                tokenUsageFrom(chatCompletionResponse.getMetadata().getUsage()),
//                retrieverResourceFrom(chatCompletionResponse.getMetadata().getRetrieverResources())
//            );
//        } catch (RuntimeException ex) {
//            throw ex;
//        }
//    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        DifyChatRequestParameters.Builder parameters = DifyChatRequestParameters.builder()
                .user(user)
                .inputs(inputs)
                .conversationId(conversationId)
                .responseMode(responseMode)
                .autoGenerateName(autoGenerateName)
                .files(files);


        DifyMessageRequest difyRequest = toDifyChatRequest(chatRequest, parameters.build());

        try {
            DifyChatCompletionResponse chatCompletionResponse = withRetryMappingExceptions(() -> client.chatCompletion(difyRequest).execute(), maxRetries);
            DifyChatResponseMetadata chatResponseMetadata = DifyChatResponseMetadata.builder()
                    .tokenUsage(tokenUsageFrom(chatCompletionResponse.getMetadata().getUsage()))
                    .conversationId(chatCompletionResponse.getConversationId())
                    .retrieverResources(retrieverResourceFrom(chatCompletionResponse.getMetadata().getRetrieverResources()))
                    .build();

            AiMessage aiMessage = aiMessageFrom(chatCompletionResponse);
            return ChatResponse.builder()
                    .aiMessage(aiMessage)
                    .metadata(chatResponseMetadata)
                    .build();
        } catch (RuntimeException ex) {
            throw ex;
        }
    }
}
