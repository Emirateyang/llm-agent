package com.llmagent.azure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.llmagent.azure.chat.AzureAiChatModelName;
import com.llmagent.azure.embedding.AzureAiEmbeddingModelName;
import com.llmagent.data.message.*;
import com.llmagent.llm.chat.TokenCountEstimator;
import com.llmagent.llm.tool.ToolRequest;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.llmagent.azure.chat.AzureAiChatModelName.*;
import static com.llmagent.exception.Exceptions.illegalArgument;
import static com.llmagent.util.ValidationUtil.ensureNotBlank;

public class AzureOpenAiTokenCountEstimator implements TokenCountEstimator {

    private final String modelName;
    private final Optional<Encoding> encoding;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Creates an instance of the {@code AzureOpenAiTokenCountEstimator} for the "gpt-3.5-turbo" model.
     *
     * @deprecated Please use other constructors and specify the model name explicitly.
     */
    @Deprecated(forRemoval = true)
    public AzureOpenAiTokenCountEstimator() {
        this(GPT_3_5_TURBO.modelType());
    }

    /**
     * Creates an instance of the {@code AzureOpenAiTokenCountEstimator} for a given {@link AzureAiChatModelName}.
     */
    public AzureOpenAiTokenCountEstimator(AzureAiChatModelName modelName) {
        this(modelName.modelType());
    }

    /**
     * Creates an instance of the {@code AzureOpenAiTokenCountEstimator} for a given {@link AzureAiEmbeddingModelName}.
     */
    public AzureOpenAiTokenCountEstimator(AzureAiEmbeddingModelName modelName) {
        this(modelName.modelType());
    }

    /**
     * Creates an instance of the {@code AzureOpenAiTokenCountEstimator} for a given model name.
     */
    public AzureOpenAiTokenCountEstimator(String modelName) {
        this.modelName = ensureNotBlank(modelName, "modelName");
        // If the model is unknown, we should NOT fail fast during the creation of AzureOpenAiTokenCountEstimator.
        // Doing so would cause the failure of every OpenAI***Model that uses this token count estimator.
        // This is done to account for situations when a new OpenAI model is available,
        // but JTokkit does not yet support it.
        this.encoding = Encodings.newLazyEncodingRegistry().getEncodingForModel(modelName);
    }

    public int estimateTokenCountInText(String text) {
        return encoding.orElseThrow(unknownModelException())
                .countTokensOrdinary(text);
    }

    @Override
    public int estimateTokenCountInMessage(ChatMessage message) {
        int tokenCount = 1; // 1 token for role
        tokenCount += extraTokensPerMessage();

        if (message instanceof SystemMessage) {
            tokenCount += estimateTokenCountIn((SystemMessage) message);
        } else if (message instanceof UserMessage) {
            tokenCount += estimateTokenCountIn((UserMessage) message);
        } else if (message instanceof AiMessage) {
            tokenCount += estimateTokenCountIn((AiMessage) message);
        } else if (message instanceof ToolMessage) {
            tokenCount += estimateTokenCountIn((ToolMessage) message);
        } else {
            throw new IllegalArgumentException("Unknown message type: " + message);
        }

        return tokenCount;
    }

    private int estimateTokenCountIn(SystemMessage systemMessage) {
        return estimateTokenCountInText(systemMessage.content());
    }

    private int estimateTokenCountIn(UserMessage userMessage) {
        int tokenCount = 0;

        for (Content content : userMessage.contents()) {
            if (content instanceof TextContent) {
                tokenCount += estimateTokenCountInText(((TextContent) content).text());
            } else if (content instanceof ImageContent) {
                tokenCount += 85; // TODO implement for HIGH/AUTO detail level
            } else {
                throw illegalArgument("Unknown content type: " + content);
            }
        }

        if (userMessage.name() != null && !modelName.equals(GPT_4_VISION_PREVIEW.toString())) {
            tokenCount += extraTokensPerName();
            tokenCount += estimateTokenCountInText(userMessage.name());
        }

        return tokenCount;
    }

    private int estimateTokenCountIn(AiMessage aiMessage) {
        int tokenCount = 0;

        if (aiMessage.content() != null) {
            tokenCount += estimateTokenCountInText(aiMessage.content());
        }

        if (aiMessage.toolRequests() != null) {
            if (isOneOfLatestModels()) {
                tokenCount += 6;
            } else {
                tokenCount += 3;
            }
            if (aiMessage.toolRequests().size() == 1) {
                tokenCount -= 1;
                ToolRequest toolExecutionRequest = aiMessage.toolRequests().get(0);
                tokenCount += estimateTokenCountInText(toolExecutionRequest.name()) * 2;
                tokenCount += estimateTokenCountInText(toolExecutionRequest.arguments());
            } else {
                tokenCount += 15;
                for (ToolRequest toolExecutionRequest : aiMessage.toolRequests()) {
                    tokenCount += 7;
                    tokenCount += estimateTokenCountInText(toolExecutionRequest.name());

                    Map<?, ?> arguments;
                    try {
                        arguments = OBJECT_MAPPER.readValue(toolExecutionRequest.arguments(), Map.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    for (Map.Entry<?, ?> argument : arguments.entrySet()) {
                        tokenCount += 2;
                        tokenCount += estimateTokenCountInText(argument.getKey().toString());
                        tokenCount += estimateTokenCountInText(argument.getValue().toString());
                    }
                }
            }
        }

        return tokenCount;
    }

    private int estimateTokenCountIn(ToolMessage toolMessage) {
        return estimateTokenCountInText(toolMessage.content());
    }

    private int extraTokensPerMessage() {
        if (modelName.equals(GPT_3_5_TURBO_0301.modelName())) {
            return 4;
        } else {
            return 3;
        }
    }

    private int extraTokensPerName() {
        if (modelName.equals(GPT_3_5_TURBO_0301.toString())) {
            return -1; // if there's a name, the role is omitted
        } else {
            return 1;
        }
    }

    @Override
    public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
        // see https://github.com/openai/openai-cookbook/blob/main/examples/How_to_count_tokens_with_tiktoken.ipynb

        int tokenCount = 3; // every reply is primed with <|start|>assistant<|message|>
        for (ChatMessage message : messages) {
            tokenCount += estimateTokenCountInMessage(message);
        }
        return tokenCount;
    }

    private Supplier<IllegalArgumentException> unknownModelException() {
        return () -> illegalArgument("Model '%s' is unknown to jtokkit", modelName);
    }

    private boolean isOneOfLatestModels() {
        return isOneOfLatestGpt3Models() || isOneOfLatestGpt4Models();
    }

    private boolean isOneOfLatestGpt3Models() {
        return modelName.equals(GPT_3_5_TURBO_1106.toString())
                || modelName.equals(GPT_3_5_TURBO.toString());
    }

    private boolean isOneOfLatestGpt4Models() {
        return modelName.equals(GPT_4_TURBO.toString())
                || modelName.equals(GPT_4_1106_PREVIEW.toString())
                || modelName.equals(GPT_4_0125_PREVIEW.toString());
    }
}
