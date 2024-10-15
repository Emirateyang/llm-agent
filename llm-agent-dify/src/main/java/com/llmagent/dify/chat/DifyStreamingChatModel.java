package com.llmagent.dify.chat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.llmagent.data.message.AiMessage;
import com.llmagent.data.message.ChatMessage;
import com.llmagent.dify.DifyStreamingChatModelBuilderFactory;
import com.llmagent.dify.DifyStreamingResponseBuilder;
import com.llmagent.dify.client.DifyClient;
import com.llmagent.llm.StreamingResponseHandler;
import com.llmagent.llm.chat.StreamingChatLanguageModel;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.util.ObjectUtil;
import com.llmagent.util.StringUtil;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static com.llmagent.dify.DifyHelper.toDifyMessage;
import static com.llmagent.util.ServiceHelper.loadFactories;
import static com.llmagent.util.StringUtil.isNullOrBlank;

/**
 * Represents a Dify language model with a chat completion interface which support by Dify
 * The model's response is streamed token by token and should be handled with {@link StreamingResponseHandler}.
 * You can find description of parameters in your Dify applications.
 */
@Slf4j
public class DifyStreamingChatModel implements StreamingChatLanguageModel {

    private final DifyClient client;
    private final String user;
    private final String conversationId;
    private final String responseMode;

    private final Map<String, Object> inputs;
    private final boolean autoGenerateName;
    private final List<DifyFileContent> files;

    private final boolean breakOnToolCalled;
    @Builder
    public DifyStreamingChatModel(String baseUrl,
                                  String apiKey,
                                  String user,
                                  String conversationId,
                                  Map<String, Object> inputs,
                                  boolean autoGenerateName,
                                  List<DifyFileContent> files,
                                  boolean breakOnToolCalled,
                                  Duration timeout,
                                  Boolean logRequests,
                                  Boolean logResponses) {

        timeout = ObjectUtil.getOrDefault(timeout, Duration.ofSeconds(60));
        inputs = ObjectUtil.getOrDefault(inputs, Map.of());
        breakOnToolCalled = ObjectUtil.getOrDefault(breakOnToolCalled, false);
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
                .breakOnToolCalled(breakOnToolCalled)
                .build();

        this.user = user;
        this.conversationId = conversationId;
        this.responseMode = responseMode;
        this.inputs = inputs;
        this.autoGenerateName = autoGenerateName;
        this.files = files;
        this.breakOnToolCalled = breakOnToolCalled;
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {

        DifyMessageRequest.Builder requestBuilder = DifyMessageRequest.builder()
                .inputs(inputs)
                .query(toDifyMessage(messages))
                .responseMode(responseMode)
                .conversationId(conversationId)
                .autoGenerateName(autoGenerateName)
                .breakOnToolCalled(breakOnToolCalled)
                .user(user);
        if (this.files != null) {
            requestBuilder.files(files);
        }

        DifyMessageRequest request = requestBuilder.build();

        DifyStreamingResponseBuilder responseBuilder = new DifyStreamingResponseBuilder();

        AtomicReference<String> responseId = new AtomicReference<>();
        client.streamingChatCompletion(request)
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

        String event = partialResponse.getEvent();
        String tool = partialResponse.getTool();
        String toolInput = partialResponse.getToolInput();
        Integer position = partialResponse.getPosition();
        String thought = partialResponse.getThought();
        String content = partialResponse.getAnswer();
        String observation = partialResponse.getObservation();
//        System.out.println("event:" + event + " | toolInput: " + toolInput + " | observation: " + partialResponse.getObservation() + " | answer: " + partialResponse.getAnswer() + " | position: " + position + " | thought: " + thought);

        StreamResponse4Customer customerResponse = null;
        if ("message_end".equalsIgnoreCase(event)) {
            customerResponse = StreamResponse4Customer.builder().event("done").build();
            handler.onNext(JSON.toJSONString(customerResponse));
            return;
        } else if ("tts_message_end".equalsIgnoreCase(event)) {
            return;
        }

        if ("agent_thought".equals(event) && position != null) {
            if (StringUtil.noText(toolInput) && StringUtil.noText(thought)) {
                // thinking
                customerResponse = StreamResponse4Customer.builder().event("think").content(StringUtil.noText(content) ? "" : content).build();
            } else if (StringUtil.hasText(toolInput)) {
                if (StringUtil.hasText(observation)) {
                    // tool called
                    customerResponse = StreamResponse4Customer.builder().event("toolCall")
                            .toolCall(fromToolCall(toolInput))
                            .observation(JSON.parseObject(observation).getJSONObject(tool).getString("result")).build();
                } else {
                    // calling
                    customerResponse = StreamResponse4Customer.builder().event("toolCall")
                            .toolCall(fromToolCall(toolInput)).build();
                }
            }
        } else if ("agent_message".equals(event) && StringUtil.hasText(content)) {
            customerResponse = StreamResponse4Customer.builder().event("answer")
                    .content(content).build();
        }

        if (customerResponse != null) {
            handler.onNext(JSON.toJSONString(customerResponse));
        }
    }

    public static DifyStreamingChatModel withApiKey(String apiKey) {
        return builder().apiKey(apiKey).build();
    }

    public static DifyStreamingChatModelBuilder builder() {
        for (DifyStreamingChatModelBuilderFactory factory : loadFactories(DifyStreamingChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new DifyStreamingChatModelBuilder();
    }

    public static class DifyStreamingChatModelBuilder {

        public DifyStreamingChatModelBuilder() {
            // This is public so it can be extended
            // By default with Lombok it becomes package private
        }
    }

    public static ToolCallInResponse fromToolCall(String toolInput) {
        JSONObject jsonObject = JSON.parseObject(toolInput);
        String name = jsonObject.keySet().iterator().next();
        JSONObject params = jsonObject.getJSONObject(name);

        ToolCallInResponse toolCall = new ToolCallInResponse();
        toolCall.setName(name);
        toolCall.setParams(params);

        return toolCall;
    }
}
