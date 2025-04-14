package com.llmagent.embedding.onnx;

import ai.djl.huggingface.tokenizers.Encoding;
import com.llmagent.data.message.*;
import com.llmagent.llm.Tokenizer;
import com.llmagent.llm.tool.ToolRequest;
import com.llmagent.llm.tool.ToolSpecification;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.Files.newInputStream;

/**
 * This class is used to tokenize text using the Hugging Face tokenizer, which is a popular library for natural language processing (NLP).
 */
public class HuggingFaceTokenizer implements Tokenizer {

    private final ai.djl.huggingface.tokenizers.HuggingFaceTokenizer tokenizer;

    /**
     * Creates an instance of a {@code HuggingFaceTokenizer} using a built-in {@code tokenizer.json} file.
     */
    public HuggingFaceTokenizer() {
        Map<String, String> options = new HashMap<>();
        options.put("padding", "false");
        options.put("truncation", "false");

        this.tokenizer = createFrom(getClass().getResourceAsStream("/bert-tokenizer.json"), options);
    }

    /**
     * Creates an instance of a {@code HuggingFaceTokenizer} using a provided {@code tokenizer.json} file.
     *
     * @param pathToTokenizer The path to the tokenizer file (e.g., "/path/to/tokenizer.json")
     */
    public HuggingFaceTokenizer(Path pathToTokenizer) {
        this(pathToTokenizer, null);
    }

    /**
     * Creates an instance of a {@code HuggingFaceTokenizer} using a provided {@code tokenizer.json} file
     * and a map of DJL's tokenizer options.
     *
     * @param pathToTokenizer The path to the tokenizer file (e.g., "/path/to/tokenizer.json")
     * @param options         The DJL's tokenizer options
     */
    public HuggingFaceTokenizer(Path pathToTokenizer, Map<String, String> options) {
        try {
            this.tokenizer = createFrom(newInputStream(pathToTokenizer), options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an instance of a {@code HuggingFaceTokenizer} using a provided {@code tokenizer.json} file.
     *
     * @param pathToTokenizer The path to the tokenizer file (e.g., "/path/to/tokenizer.json")
     */
    public HuggingFaceTokenizer(String pathToTokenizer) {
        this(pathToTokenizer, null);
    }

    /**
     * Creates an instance of a {@code HuggingFaceTokenizer} using a provided {@code tokenizer.json} file
     * and a map of DJL's tokenizer options.
     *
     * @param pathToTokenizer The path to the tokenizer file (e.g., "/path/to/tokenizer.json")
     * @param options         The DJL's tokenizer options
     */
    public HuggingFaceTokenizer(String pathToTokenizer, Map<String, String> options) {
        try {
            this.tokenizer = createFrom(newInputStream(Paths.get(pathToTokenizer)), options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HuggingFaceTokenizer(InputStream tokenizerStream, Map<String, String> options) {
        try {
            this.tokenizer = createFrom(tokenizerStream, options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ai.djl.huggingface.tokenizers.HuggingFaceTokenizer createFrom(InputStream tokenizer,
                                                                                 Map<String, String> options) {
        try {
            return ai.djl.huggingface.tokenizers.HuggingFaceTokenizer.newInstance(tokenizer, options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int estimateTokenCountInText(String text) {
        Encoding encoding = tokenizer.encode(text, false, true);
        return encoding.getTokens().length;
    }

    public List<String> tokenize(String sentence) {
        return tokenizer.tokenize(sentence);
    }

    public Encoding encode(String text) {
        return tokenizer.encode(text);
    }

    public Encoding encode(String text, boolean addSpecialTokens, boolean withOverflowingTokens) {
        return tokenizer.encode(text, addSpecialTokens, withOverflowingTokens);
    }

    public String buildSentence(List<String> tokens) {
        return tokenizer.buildSentence(tokens);
    }

    @Override
    public int estimateTokenCountInMessage(ChatMessage message) {
        if (message instanceof SystemMessage systemMessage) {
            return estimateTokenCountInText(systemMessage.content());
        } else if (message instanceof UserMessage userMessage) {
            return estimateTokenCountInText(userMessage.singleText());
        } else if (message instanceof AiMessage aiMessage) {
            return estimateTokenCountInText(aiMessage.content());
        } else if (message instanceof ToolMessage toolMessage) {
            return estimateTokenCountInText(toolMessage.content());
        } else {
            throw new IllegalArgumentException("Unknown message type: " + message);
        }
    }

    @Override
    public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
        int tokens = 0;
        for (ChatMessage message : messages) {
            tokens += estimateTokenCountInMessage(message);
        }
        return tokens;
    }

    @Override
    public int estimateTokenCountInToolSpecifications(Iterable<ToolSpecification> toolSpecifications) {
        return 0;
    }

    @Override
    public int estimateTokenCountInToolRequests(Iterable<ToolRequest> toolRequests) {
        return 0;
    }

}
