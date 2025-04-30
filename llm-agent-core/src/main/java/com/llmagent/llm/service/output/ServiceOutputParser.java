package com.llmagent.llm.service.output;

import com.llmagent.data.message.AiMessage;
import com.llmagent.llm.chat.request.json.JsonSchema;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.LlmResult;
import com.llmagent.llm.output.TokenStream;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

import static com.llmagent.util.TypeUtils.*;
import static com.llmagent.util.ValidationUtil.ensureNotNull;

public class ServiceOutputParser {

    private final OutputParserFactory outputParserFactory;

    public ServiceOutputParser() {
        this(new DefaultOutputParserFactory());
    }

    ServiceOutputParser(OutputParserFactory outputParserFactory) {
        this.outputParserFactory = ensureNotNull(outputParserFactory, "outputParserFactory");
    }

    public Object parse(ChatResponse chatResponse, Type returnType) {

        if (typeHasRawClass(returnType, LlmResult.class)) {
            // In the case of returnType = Result<List<String>>, returnType will be set to List<String>
            returnType = resolveFirstGenericParameterType(returnType);
        }

        // In the case of returnType = List<String> these two would be set like:
        // rawClass = List.class
        // typeArgumentClass = String.class
        Class<?> rawClass = getRawClass(returnType);
        Class<?> typeArgumentClass = resolveFirstGenericParameterClass(returnType);

        if (rawClass == LlmResponse.class) {
            // legacy
            return LlmResponse.from(chatResponse.aiMessage(), chatResponse.tokenUsage(), chatResponse.finishReason());
        }

        AiMessage aiMessage = chatResponse.aiMessage();
        if (rawClass == AiMessage.class) {
            return aiMessage;
        }

        String text = aiMessage.content();
        if (rawClass == String.class) {
            return text;
        }

        OutputParser<?> outputParser = outputParserFactory.get(rawClass, typeArgumentClass);
        return outputParser.parse(text);
    }

    public Optional<JsonSchema> jsonSchema(Type returnType) {

        if (typeHasRawClass(returnType, LlmResult.class)) {
            // In the case of returnType = Result<List<String>>, returnType will be set to List<String>
            returnType = resolveFirstGenericParameterType(returnType);
        }

        // In the case of returnType = List<String> these two would be set like:
        // rawClass = List.class
        // typeArgumentClass = String.class
        Class<?> rawClass = getRawClass(returnType);
        Class<?> typeArgumentClass = resolveFirstGenericParameterClass(returnType);

        if (schemaNotRequired(rawClass)) {
            return Optional.empty();
        }

        OutputParser<?> outputParser = outputParserFactory.get(rawClass, typeArgumentClass);
        return outputParser.jsonSchema();
    }

    public String outputFormatInstructions(Type returnType) {

        if (typeHasRawClass(returnType, LlmResult.class)) {
            // In the case of returnType = Result<List<String>>, returnType will be set to List<String>
            returnType = resolveFirstGenericParameterType(returnType);
        }

        // In the case of returnType = List<String> these two would be set like:
        // rawClass = List.class
        // typeArgumentClass = String.class
        Class<?> rawClass = getRawClass(returnType);
        Class<?> typeArgumentClass = resolveFirstGenericParameterClass(returnType);

        if (schemaNotRequired(rawClass)) {
            return "";
        }

        OutputParser<?> outputParser = outputParserFactory.get(rawClass, typeArgumentClass);
        String formatInstructions = outputParser.formatInstructions();
        if (!formatInstructions.startsWith("\nYou must")) {
            formatInstructions = "\nYou must answer strictly in the following format: " + formatInstructions;
        }
        return formatInstructions;
    }

    private static boolean schemaNotRequired(Class<?> type) {
        return type == String.class
                || type == AiMessage.class
                || type == TokenStream.class
                || type == LlmResponse.class
                || type == Map.class;
    }
}
