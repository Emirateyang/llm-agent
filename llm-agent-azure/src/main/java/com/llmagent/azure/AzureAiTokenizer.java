package com.llmagent.azure;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.IntArrayList;
import com.llmagent.azure.chat.AzureAiChatModelName;
import com.llmagent.azure.embedding.AzureAiEmbeddingModelName;
import com.llmagent.data.message.*;
import com.llmagent.llm.Tokenizer;
import com.llmagent.llm.tool.ToolParameters;
import com.llmagent.llm.tool.ToolRequest;
import com.llmagent.llm.tool.ToolSpecification;
import com.llmagent.util.JsonUtil;
import com.llmagent.util.StringUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.llmagent.azure.chat.AzureAiChatModelName.*;
import static com.llmagent.exception.Exceptions.illegalArgument;

public class AzureAiTokenizer implements Tokenizer {
    private final String modelName;
    private final Optional<Encoding> encoding;

    /**
     * Creates an instance of the {@code AzureOpenAiTokenizer} for the "gpt-3.5-turbo" model.
     * It should be suitable for most OpenAI models, as most of them use the same cl100k_base encoding (except for GPT-4o).
     */
    public AzureAiTokenizer() {
        this(GPT_3_5_TURBO.modelType());
    }

    /**
     * Creates an instance of the {@code AzureOpenAiTokenizer} for a given {@link AzureAiChatModelName}.
     */
    public AzureAiTokenizer(AzureAiChatModelName modelName) {
        this(modelName.modelType());
    }

    /**
     * Creates an instance of the {@code AzureOpenAiTokenizer} for a given {@link AzureAiEmbeddingModelName}.
     */
    public AzureAiTokenizer(AzureAiEmbeddingModelName modelName) {
        this(modelName.modelType());
    }

    /**
     * Creates an instance of the {@code AzureOpenAiTokenizer} for a given model name.
     */
    public AzureAiTokenizer(String modelName) {
        this.modelName = modelName;
        // If the model is unknown, we should NOT fail fast during the creation of AzureOpenAiTokenizer.
        // Doing so would cause the failure of every OpenAI***Model that uses this tokenizer.
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
                ToolRequest toolRequest = aiMessage.toolRequests().get(0);
                tokenCount += estimateTokenCountInText(toolRequest.name()) * 2;
                tokenCount += estimateTokenCountInText(toolRequest.arguments());
            } else {
                tokenCount += 15;
                for (ToolRequest toolRequest : aiMessage.toolRequests()) {
                    tokenCount += 7;
                    tokenCount += estimateTokenCountInText(toolRequest.name());

                    Map<?, ?> arguments = JsonUtil.fromJson(toolRequest.arguments(), Map.class);
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

    @Override
    public int estimateTokenCountInToolSpecifications(Iterable<ToolSpecification> toolSpecifications) {
        int tokenCount = 16;
        for (ToolSpecification toolSpecification : toolSpecifications) {
            tokenCount += 6;
            tokenCount += estimateTokenCountInText(toolSpecification.name());
            if (toolSpecification.description() != null) {
                tokenCount += 2;
                tokenCount += estimateTokenCountInText(toolSpecification.description());
            }
            tokenCount += estimateTokenCountInToolParameters(toolSpecification.parameters());
        }
        return tokenCount;
    }

    private int estimateTokenCountInToolParameters(ToolParameters parameters) {
        if (parameters == null) {
            return 0;
        }

        int tokenCount = 3;
        Map<String, Map<String, Object>> properties = parameters.properties();
        if (isOneOfLatestModels()) {
            tokenCount += properties.size() - 1;
        }
        for (String property : properties.keySet()) {
            if (isOneOfLatestModels()) {
                tokenCount += 2;
            } else {
                tokenCount += 3;
            }
            tokenCount += estimateTokenCountInText(property);
            for (Map.Entry<String, Object> entry : properties.get(property).entrySet()) {
                if ("type".equals(entry.getKey())) {
                    if ("array".equals(entry.getValue()) && isOneOfLatestModels()) {
                        tokenCount += 1;
                    }
                    // TODO object
                } else if ("description".equals(entry.getKey())) {
                    tokenCount += 2;
                    tokenCount += estimateTokenCountInText(entry.getValue().toString());
                    if (isOneOfLatestModels() && parameters.required().contains(property)) {
                        tokenCount += 1;
                    }
                } else if ("enum".equals(entry.getKey())) {
                    if (isOneOfLatestModels()) {
                        tokenCount -= 2;
                    } else {
                        tokenCount -= 3;
                    }
                    for (Object enumValue : (Object[]) entry.getValue()) {
                        tokenCount += 3;
                        tokenCount += estimateTokenCountInText(enumValue.toString());
                    }
                }
            }
        }
        return tokenCount;
    }

    @Override
    public int estimateTokenCountInForcefulToolSpecification(ToolSpecification toolSpecification) {
        int tokenCount = estimateTokenCountInToolSpecifications(Collections.singletonList(toolSpecification));
        tokenCount += 4;
        tokenCount += estimateTokenCountInText(toolSpecification.name());
        if (isOneOfLatestModels()) {
            tokenCount += 3;
        }
        return tokenCount;
    }

    public List<Integer> encode(String text) {
        return encoding.orElseThrow(unknownModelException())
                .encodeOrdinary(text).boxed();
    }

    public List<Integer> encode(String text, int maxTokensToEncode) {
        return encoding.orElseThrow(unknownModelException())
                .encodeOrdinary(text, maxTokensToEncode).getTokens().boxed();
    }

    public String decode(List<Integer> tokens) {

        IntArrayList intArrayList = new IntArrayList();
        for (Integer token : tokens) {
            intArrayList.add(token);
        }

        return encoding.orElseThrow(unknownModelException())
                .decode(intArrayList);
    }

    private Supplier<IllegalArgumentException> unknownModelException() {
        return () -> illegalArgument("Model '%s' is unknown to jtokkit", modelName);
    }

    @Override
    public int estimateTokenCountInToolRequests(Iterable<ToolRequest> toolRequests) {

        int tokenCount = 0;

        int toolsCount = 0;
        int toolsWithArgumentsCount = 0;
        int toolsWithoutArgumentsCount = 0;

        int totalArgumentsCount = 0;

        for (ToolRequest toolRequest : toolRequests) {
            tokenCount += 4;
            tokenCount += estimateTokenCountInText(toolRequest.name());
            tokenCount += estimateTokenCountInText(toolRequest.arguments());

            int argumentCount = countArguments(toolRequest.arguments());
            if (argumentCount == 0) {
                toolsWithoutArgumentsCount++;
            } else {
                toolsWithArgumentsCount++;
            }
            totalArgumentsCount += argumentCount;

            toolsCount++;
        }

        if (modelName.equals(GPT_3_5_TURBO_1106.toString()) || isOneOfLatestGpt4Models()) {
            tokenCount += 16;
            tokenCount += 3 * toolsWithoutArgumentsCount;
            tokenCount += toolsCount;
            if (totalArgumentsCount > 0) {
                tokenCount -= 1;
                tokenCount -= 2 * totalArgumentsCount;
                tokenCount += 2 * toolsWithArgumentsCount;
                tokenCount += toolsCount;
            }
        }

        if (modelName.equals(GPT_4_1106_PREVIEW.toString())) {
            tokenCount += 3;
            if (toolsCount > 1) {
                tokenCount += 18;
                tokenCount += 15 * toolsCount;
                tokenCount += totalArgumentsCount;
                tokenCount -= 3 * toolsWithoutArgumentsCount;
            }
        }

        return tokenCount;
    }

    @Override
    public int estimateTokenCountInForcefulToolRequest(ToolRequest toolRequest) {

        if (isOneOfLatestGpt4Models()) {
            int argumentsCount = countArguments(toolRequest.arguments());
            if (argumentsCount == 0) {
                return 1;
            } else {
                return estimateTokenCountInText(toolRequest.arguments());
            }
        }

        int tokenCount = estimateTokenCountInToolRequests(Collections.singletonList(toolRequest));
        tokenCount -= 4;
        tokenCount -= estimateTokenCountInText(toolRequest.name());

        if (modelName.equals(GPT_3_5_TURBO_1106.toString())) {
            int argumentsCount = countArguments(toolRequest.arguments());
            if (argumentsCount == 0) {
                return 1;
            }
            tokenCount -= 19;
            tokenCount += 2 * argumentsCount;
        }

        return tokenCount;
    }

    static int countArguments(String arguments) {
        if (StringUtil.isNullOrBlank(arguments)) {
            return 0;
        }
        Map<?, ?> argumentsMap = JsonUtil.fromJson(arguments, Map.class);
        return argumentsMap.size();
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
