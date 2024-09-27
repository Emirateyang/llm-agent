package com.llmagent.dify.chat;

import com.llmagent.data.message.AiMessage;
import com.llmagent.dify.DifyStreamingCompletionModelBuilderFactory;
import com.llmagent.dify.DifyStreamingResponseBuilder;
import com.llmagent.dify.client.DifyClient;
import com.llmagent.llm.StreamingResponseHandler;
import com.llmagent.llm.completion.StreamingCompletionModel;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.util.ObjectUtil;
import com.llmagent.util.StringUtil;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.llmagent.util.ServiceHelper.loadFactories;
import static com.llmagent.util.StringUtil.isNullOrBlank;

/**
 * Represents a Dify language model with a chat completion interface which support by Dify
 * The model's response is streamed token by token and should be handled with {@link StreamingResponseHandler}.
 * You can find description of parameters in your Dify applications.
 */
@Slf4j
public class DifyStreamingCompletionModel implements StreamingCompletionModel {

    private final DifyClient client;
    private final String user;
    private final String conversationId;
    private final String responseMode;

    private final Map<String, Object> inputs;
    private final boolean autoGenerateName;
    private final List<DifyFileContent> files;

    @Builder
    public DifyStreamingCompletionModel(String baseUrl,
                                        String apiKey,
                                        String user,
                                        String conversationId,
                                        Map<String, Object> inputs,
                                        boolean autoGenerateName,
                                        List<DifyFileContent> files,
                                        Duration timeout,
                                        Boolean logRequests,
                                        Boolean logResponses) {

        timeout = ObjectUtil.getOrDefault(timeout, Duration.ofSeconds(60));
        inputs = ObjectUtil.getOrDefault(inputs, Map.of());
        String responseMode = ResponseMode.STREAMING.toString();

        this.client = DifyClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .callTimeout(timeout)
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .breakOnToolCalled(false)
                .build();

        this.user = user;
        this.conversationId = conversationId;
        this.responseMode = responseMode;
        this.inputs = inputs;
        this.autoGenerateName = autoGenerateName;
        this.files = files;
    }
    @Override
    public void generate(String prompt, StreamingResponseHandler<AiMessage> handler) {

        DifyMessageRequest.Builder requestBuilder = DifyMessageRequest.builder()
                .inputs(inputs)
                .responseMode(responseMode)
                .conversationId(conversationId)
                .autoGenerateName(autoGenerateName)
                .user(user);
        if (this.files != null) {
            requestBuilder.files(files);
        }

        DifyMessageRequest request = requestBuilder.build();

        DifyStreamingResponseBuilder responseBuilder = new DifyStreamingResponseBuilder();

        AtomicReference<String> responseId = new AtomicReference<>();
        client.streamingCompletion(request)
                .onPartialResponse(partialResponse -> {
                    responseBuilder.append(partialResponse);
                    handle(partialResponse, handler);

                    if (!isNullOrBlank(partialResponse.getId())) {
                        responseId.set(partialResponse.getId());
                    }
                })
                .onComplete(() -> {
                    LlmResponse<AiMessage> response = createResponse(responseBuilder);
                    handler.onComplete(response);
                })
                .onError(handler::onError)
                .execute();
    }

    private LlmResponse<AiMessage> createResponse(DifyStreamingResponseBuilder responseBuilder) {
        return responseBuilder.build();
    }

    private static void handle(DifyStreamingChatCompletionResponse partialResponse,
                               StreamingResponseHandler<AiMessage> handler) {

        if ("message_end".equalsIgnoreCase(partialResponse.getType())) {
            return;
        }

        String content = partialResponse.getAnswer();
        if (StringUtil.hasText(content)) {
            handler.onNext(content);
        }
    }

    public static DifyStreamingCompletionModel withApiKey(String apiKey) {
        return builder().apiKey(apiKey).build();
    }

    public static DifyStreamingCompletionModelBuilder builder() {
        for (DifyStreamingCompletionModelBuilderFactory factory : loadFactories(DifyStreamingCompletionModelBuilderFactory.class)) {
            return factory.get();
        }
        return new DifyStreamingCompletionModelBuilder();
    }

    public static class DifyStreamingCompletionModelBuilder {

        public DifyStreamingCompletionModelBuilder() {
            // This is public so it can be extended
            // By default with Lombok it becomes package private
        }
    }
}
