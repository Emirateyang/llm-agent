package com.llmagent.openai.chat;

import com.llmagent.openai.token.StreamOptions;
import com.llmagent.openai.tool.Tool;
import com.llmagent.openai.tool.ToolChoice;
import com.llmagent.openai.tool.ToolChoiceMode;
import com.llmagent.openai.ResponseFormat;
import com.llmagent.openai.ResponseFormatType;

import java.util.*;

import static java.util.Collections.unmodifiableMap;

public final class ChatCompletionRequest {
    private final String model;
    private final List<Message> messages;
    private final Double temperature;
    private final Double topP;
    private final Integer n;
    private final Boolean stream;
    private final StreamOptions streamOptions;
    private final List<String> stop;
    private final Integer maxTokens;
    private final Integer maxCompletionTokens;
    private final Double presencePenalty;
    private final Double frequencyPenalty;
    private final Map<String, Integer> logitBias;
    private final String user;
    private final ResponseFormat responseFormat;
    private final Integer seed;
    private final List<Tool> tools;
    private final Object toolChoice;
    private final Boolean parallelToolCalls;
    private final Boolean store;
    private final Map<String, String> metadata;
    private final String reasoningEffort;
    private final String serviceTier;

    private ChatCompletionRequest(Builder builder) {
        this.model = builder.model;
        this.messages = builder.messages;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.n = builder.n;
        this.stream = builder.stream;
        this.streamOptions = builder.streamOptions;
        this.stop = builder.stop;
        this.maxTokens = builder.maxTokens;
        this.maxCompletionTokens = builder.maxCompletionTokens;
        this.presencePenalty = builder.presencePenalty;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.logitBias = builder.logitBias;
        this.user = builder.user;
        this.responseFormat = builder.responseFormat;
        this.seed = builder.seed;
        this.tools = builder.tools;
        this.toolChoice = builder.toolChoice;
        this.parallelToolCalls = builder.parallelToolCalls;
        this.store = builder.store;
        this.metadata = builder.metadata;
        this.reasoningEffort = builder.reasoningEffort;
        this.serviceTier = builder.serviceTier;
    }

    public String model() {
        return model;
    }

    public List<Message> messages() {
        return messages;
    }

    public Double temperature() {
        return temperature;
    }

    public Double topP() {
        return topP;
    }

    public Integer n() {
        return n;
    }

    public Boolean stream() {
        return stream;
    }

    public StreamOptions streamOptions() {
        return streamOptions;
    }

    public List<String> stop() {
        return stop;
    }

    public Integer maxTokens() {
        return maxTokens;
    }

    public Integer maxCompletionTokens() {
        return maxCompletionTokens;
    }

    public Double presencePenalty() {
        return presencePenalty;
    }

    public Double frequencyPenalty() {
        return frequencyPenalty;
    }

    public Map<String, Integer> logitBias() {
        return logitBias;
    }

    public String user() {
        return user;
    }

    public ResponseFormat responseFormat() {
        return responseFormat;
    }

    public Integer seed() {
        return seed;
    }

    public List<Tool> tools() {
        return tools;
    }

    public Object toolChoice() {
        return toolChoice;
    }

    public Boolean parallelToolCalls() {
        return parallelToolCalls;
    }

    public Boolean store() {
        return store;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public String reasoningEffort() {
        return reasoningEffort;
    }

    public String serviceTier() {
        return serviceTier;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof ChatCompletionRequest
                && equalTo((ChatCompletionRequest) another);
    }

    private boolean equalTo(ChatCompletionRequest another) {
        return Objects.equals(model, another.model)
                && Objects.equals(messages, another.messages)
                && Objects.equals(temperature, another.temperature)
                && Objects.equals(topP, another.topP)
                && Objects.equals(n, another.n)
                && Objects.equals(stream, another.stream)
                && Objects.equals(streamOptions, another.streamOptions)
                && Objects.equals(stop, another.stop)
                && Objects.equals(maxTokens, another.maxTokens)
                && Objects.equals(maxCompletionTokens, another.maxCompletionTokens)
                && Objects.equals(presencePenalty, another.presencePenalty)
                && Objects.equals(frequencyPenalty, another.frequencyPenalty)
                && Objects.equals(logitBias, another.logitBias)
                && Objects.equals(user, another.user)
                && Objects.equals(responseFormat, another.responseFormat)
                && Objects.equals(seed, another.seed)
                && Objects.equals(tools, another.tools)
                && Objects.equals(toolChoice, another.toolChoice)
                && Objects.equals(parallelToolCalls, another.parallelToolCalls)
                && Objects.equals(store, another.store)
                && Objects.equals(metadata, another.metadata)
                && Objects.equals(reasoningEffort, another.reasoningEffort)
                && Objects.equals(serviceTier, another.serviceTier);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(model);
        h += (h << 5) + Objects.hashCode(messages);
        h += (h << 5) + Objects.hashCode(temperature);
        h += (h << 5) + Objects.hashCode(topP);
        h += (h << 5) + Objects.hashCode(n);
        h += (h << 5) + Objects.hashCode(stream);
        h += (h << 5) + Objects.hashCode(streamOptions);
        h += (h << 5) + Objects.hashCode(stop);
        h += (h << 5) + Objects.hashCode(maxTokens);
        h += (h << 5) + Objects.hashCode(maxCompletionTokens);
        h += (h << 5) + Objects.hashCode(presencePenalty);
        h += (h << 5) + Objects.hashCode(frequencyPenalty);
        h += (h << 5) + Objects.hashCode(logitBias);
        h += (h << 5) + Objects.hashCode(user);
        h += (h << 5) + Objects.hashCode(responseFormat);
        h += (h << 5) + Objects.hashCode(seed);
        h += (h << 5) + Objects.hashCode(tools);
        h += (h << 5) + Objects.hashCode(toolChoice);
        h += (h << 5) + Objects.hashCode(parallelToolCalls);
        h += (h << 5) + Objects.hashCode(store);
        h += (h << 5) + Objects.hashCode(metadata);
        h += (h << 5) + Objects.hashCode(reasoningEffort);
        h += (h << 5) + Objects.hashCode(serviceTier);
        return h;
    }

    @Override
    public String toString() {
        return "ChatCompletionRequest{"
                + "model=" + model
                + ", messages=" + messages
                + ", temperature=" + temperature
                + ", topP=" + topP
                + ", n=" + n
                + ", stream=" + stream
                + ", streamOptions=" + streamOptions
                + ", stop=" + stop
                + ", maxTokens=" + maxTokens
                + ", maxCompletionTokens=" + maxCompletionTokens
                + ", presencePenalty=" + presencePenalty
                + ", frequencyPenalty=" + frequencyPenalty
                + ", logitBias=" + logitBias
                + ", user=" + user
                + ", responseFormat=" + responseFormat
                + ", seed=" + seed
                + ", tools=" + tools
                + ", toolChoice=" + toolChoice
                + ", parallelToolCalls=" + parallelToolCalls
                + ", store=" + store
                + ", metadata=" + metadata
                + ", reasoningEffort=" + reasoningEffort
                + ", serviceTier=" + serviceTier
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String model = ChatLanguageModelName.GPT_3_5_TURBO.toString();
        private List<Message> messages;
        private Double temperature;
        private Double topP;
        private Integer n;
        private Boolean stream;
        private StreamOptions streamOptions;
        private List<String> stop;
        private Integer maxTokens;
        private Integer maxCompletionTokens;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private Map<String, Integer> logitBias;
        private String user;
        private ResponseFormat responseFormat;
        private Integer seed;
        private List<Tool> tools;
        private Object toolChoice;
        private Boolean parallelToolCalls;
        private Boolean store;
        private Map<String, String> metadata;
        private String reasoningEffort;
        private String serviceTier;

        private Builder() {
        }

        public Builder from(ChatCompletionRequest instance) {
            model(instance.model);
            messages(instance.messages);
            temperature(instance.temperature);
            topP(instance.topP);
            n(instance.n);
            stream(instance.stream);
            streamOptions(instance.streamOptions);
            stop(instance.stop);
            maxTokens(instance.maxTokens);
            maxCompletionTokens(instance.maxCompletionTokens);
            presencePenalty(instance.presencePenalty);
            frequencyPenalty(instance.frequencyPenalty);
            logitBias(instance.logitBias);
            user(instance.user);
            responseFormat(instance.responseFormat);
            seed(instance.seed);
            tools(instance.tools);
            toolChoice(instance.toolChoice);
            parallelToolCalls(instance.parallelToolCalls);
            store(instance.store);
            metadata(instance.metadata);
            reasoningEffort(instance.reasoningEffort);
            serviceTier(instance.serviceTier);
            return this;
        }

        public Builder model(ChatLanguageModelName model) {
            return model(model.toString());
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<Message> messages) {
            if (messages != null) {
                this.messages = Collections.unmodifiableList(messages);
            }
            return this;
        }

        public Builder messages(Message... messages) {
            return messages(Arrays.asList(messages));
        }

        public Builder addSystemMessage(String systemMessage) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(SystemMessage.from(systemMessage));
            return this;
        }

        public Builder addUserMessage(String userMessage) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(UserMessage.from(userMessage));
            return this;
        }

        public Builder addAssistantMessage(String assistantMessage) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(AssistantMessage.from(assistantMessage));
            return this;
        }

        public Builder addToolMessage(String toolCallId, String content) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(ToolMessage.from(toolCallId, content));
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder streamOptions(StreamOptions streamOptions) {
            this.streamOptions = streamOptions;
            return this;
        }

        public Builder stop(List<String> stop) {
            if (stop != null) {
                this.stop = Collections.unmodifiableList(stop);
            }
            return this;
        }

        public Builder stop(String... stop) {
            return stop(Arrays.asList(stop));
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder logitBias(Map<String, Integer> logitBias) {
            if (logitBias != null) {
                this.logitBias = unmodifiableMap(logitBias);
            }
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder responseFormat(ResponseFormatType responseFormatType) {
            if (responseFormatType != null) {
                responseFormat = ResponseFormat.builder()
                        .type(responseFormatType).build();
            }
            return this;
        }

        public Builder responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public Builder tools(List<Tool> tools) {
            if (tools != null) {
                this.tools = Collections.unmodifiableList(tools);
            }
            return this;
        }

        public Builder tools(Tool... tools) {
            return tools(Arrays.asList(tools));
        }

        public Builder toolChoice(ToolChoiceMode toolChoiceMode) {
            this.toolChoice = toolChoiceMode;
            return this;
        }

        public Builder toolChoice(String functionName) {
            return toolChoice(ToolChoice.from(functionName));
        }

        public Builder toolChoice(Object toolChoice) {
            this.toolChoice = toolChoice;
            return this;
        }

        public Builder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public Builder store(Boolean store) {
            this.store = store;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            if (metadata != null) {
                this.metadata = unmodifiableMap(metadata);
            }
            return this;
        }

        public Builder reasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }

        public Builder serviceTier(String serviceTier) {
            this.serviceTier = serviceTier;
            return this;
        }

        public ChatCompletionRequest build() {
            return new ChatCompletionRequest(this);
        }
    }
}
