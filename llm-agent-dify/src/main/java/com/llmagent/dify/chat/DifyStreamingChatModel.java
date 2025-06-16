package com.llmagent.dify.chat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.llmagent.dify.DifyStreamingChatModelBuilderFactory;
import com.llmagent.dify.DifyStreamingResponseBuilder;
import com.llmagent.dify.client.DifyClient;
import com.llmagent.llm.StreamingResponseHandler;
import com.llmagent.llm.chat.StreamingChatLanguageModel;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.llm.chat.response.StreamingChatResponseHandler;
import com.llmagent.util.ObjectUtil;
import com.llmagent.util.StringUtil;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.llmagent.dify.DifyHelper.toDifyChatRequest;
import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.ServiceHelper.loadFactories;
import static java.time.Duration.ofSeconds;

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
    public DifyStreamingChatModel(DifyStreamingChatModelBuilder builder) {

        this.client = DifyClient.builder()
                .apiKey(builder.apiKey)
                .baseUrl(builder.baseUrl)
                .callTimeout(getOrDefault(builder.timeout, ofSeconds(30)))
                .connectTimeout(getOrDefault(builder.timeout, ofSeconds(15)))
                .readTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .writeTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .logRequests(builder.logRequests)
                .logResponses(builder.logResponses)
                .breakOnToolCalled(ObjectUtil.getOrDefault(builder.breakOnToolCalled, false))
                .build();

        this.user = builder.user;
        this.conversationId = builder.conversationId;
        this.responseMode = ResponseMode.STREAMING.toString();
        this.inputs = ObjectUtil.getOrDefault(builder.inputs, Map.of());;
        this.autoGenerateName = builder.autoGenerateName;
        this.files = builder.files;
        this.breakOnToolCalled = builder.breakOnToolCalled;
    }

//    @Override
//    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
//
//        DifyMessageRequest.Builder requestBuilder = DifyMessageRequest.builder()
//                .inputs(inputs)
//                .query(toDifyMessage(messages))
//                .responseMode(responseMode)
//                .conversationId(conversationId)
//                .autoGenerateName(autoGenerateName)
//                .breakOnToolCalled(breakOnToolCalled)
//                .user(user);
//        if (this.files != null) {
//            requestBuilder.files(files);
//        }
//
//        DifyMessageRequest request = requestBuilder.build();
//
//        DifyStreamingResponseBuilder responseBuilder = new DifyStreamingResponseBuilder();
//
//        client.streamingChatCompletion(request)
//                .onPartialResponse(partialResponse -> {
//                    if (!"message_replace".equalsIgnoreCase(partialResponse.getEvent())) {
//                        responseBuilder.append(partialResponse);
//                    }
//                    handle(partialResponse, handler);
//                })
//                .onComplete(() -> {
//                    LlmResponse<AiMessage> response = createResponse(responseBuilder);
//                    handler.onComplete(response);
//                })
//                .onError(handler::onError)
//                .execute();
//    }


    private static void handle(DifyStreamingChatCompletionResponse partialResponse,
                               StreamingChatResponseHandler handler) {

        String event = partialResponse.getEvent();
        String tool = partialResponse.getTool();
        String toolInput = partialResponse.getToolInput();
        Integer position = partialResponse.getPosition();
        String thought = partialResponse.getThought();
        String content = partialResponse.getAnswer();
        String observation = partialResponse.getObservation();

        StreamResponse4Customer customerResponse = null;
        if ("message_end".equalsIgnoreCase(event)) {
            customerResponse = StreamResponse4Customer.builder().event("done").build();
            customerResponse.setConversationId(partialResponse.getConversationId());
            handler.onPartialResponse(JSON.toJSONString(customerResponse));
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
                    if (observation.contains("tool response")) {
                        observation = observation.substring(0, observation.indexOf("tool response")) + "\"}";
                    }
                    JSONObject jsonObject = JSON.parseObject(observation);
                    Object value = jsonObject.get(tool);
                    String result = "";
                    if (value instanceof JSONObject) {
                        result = ((JSONObject) value).getString("result");
                    } else if (value instanceof String) {
                        result = (String) value;
                    }
                    if (StringUtil.isNotNullOrBlank(result)) {
                        customerResponse = StreamResponse4Customer.builder().event("toolCall")
                                .toolCall(fromToolCall(toolInput))
                                .observation(result).build();
                    }
                } else {
                    // calling
                    customerResponse = StreamResponse4Customer.builder().event("toolCall")
                            .toolCall(fromToolCall(toolInput)).build();
                }
            }
        } else if ("agent_message".equals(event) && StringUtil.hasText(content)) {
            customerResponse = StreamResponse4Customer.builder().event("answer")
                    .content(content).build();
        } else if ("message".equals(event) && StringUtil.hasText(content)) {
            customerResponse = StreamResponse4Customer.builder().event("answer")
                    .content(content).build();

        }
//        else if ("message_replace".equals(event) && StringUtil.hasText(content)) {
//            customerResponse = StreamResponse4Customer.builder().event("answer")
//                    .content(content).build();
//        }

        if (customerResponse != null) {
            customerResponse.setConversationId(partialResponse.getConversationId());
            handler.onPartialResponse(JSON.toJSONString(customerResponse));
        }
    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {

        DifyChatRequestParameters.Builder parameters = DifyChatRequestParameters.builder()
                .user(user)
                .inputs(inputs)
                .breakOnToolCalled(breakOnToolCalled)
                .conversationId(conversationId)
                .responseMode(responseMode)
                .autoGenerateName(autoGenerateName)
                .files(files);

        DifyMessageRequest difyRequest = toDifyChatRequest(chatRequest, parameters.build());

        DifyStreamingResponseBuilder responseBuilder = new DifyStreamingResponseBuilder();

        client.streamingChatCompletion(difyRequest)
                .onPartialResponse(partialResponse -> {
                    responseBuilder.append(partialResponse);
                    handle(partialResponse, handler);
                })
                .onComplete(() -> {
                    ChatResponse chatResponse = responseBuilder.build();
                    handler.onCompleteResponse(chatResponse);
                })
                .onError(handler::onError)
                .execute();
    }

    public static DifyStreamingChatModelBuilder builder() {
        for (DifyStreamingChatModelBuilderFactory factory : loadFactories(DifyStreamingChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new DifyStreamingChatModelBuilder();
    }

    public static class DifyStreamingChatModelBuilder {

        private String baseUrl;
        private String apiKey;
        private String user;
        private String conversationId;
        private Map<String, Object> inputs;
        private boolean autoGenerateName;
        private List<DifyFileContent> files;
        private boolean breakOnToolCalled;
        private Boolean logRequests;
        private Boolean logResponses;
        private Duration timeout;

        public DifyStreamingChatModelBuilder() {
            // This is public so it can be extended
            // By default with Lombok it becomes package private
        }

        public DifyStreamingChatModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public DifyStreamingChatModelBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public DifyStreamingChatModelBuilder user(String user) {
            this.user = user;
            return this;
        }

        public DifyStreamingChatModelBuilder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public DifyStreamingChatModelBuilder inputs(Map<String, Object> inputs) {
            this.inputs = inputs;
            return this;
        }

        public DifyStreamingChatModelBuilder autoGenerateName(boolean autoGenerateName) {
            this.autoGenerateName = autoGenerateName;
            return this;
        }

        public DifyStreamingChatModelBuilder files(List<DifyFileContent> files) {
            this.files = files;
            return this;
        }

        public DifyStreamingChatModelBuilder breakOnToolCalled(boolean breakOnToolCalled) {
            this.breakOnToolCalled = breakOnToolCalled;
            return this;
        }

        public DifyStreamingChatModelBuilder logRequests(boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public DifyStreamingChatModelBuilder logResponses(boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public DifyStreamingChatModelBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public DifyStreamingChatModel build() {
            return new DifyStreamingChatModel(this);
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
