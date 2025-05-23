package com.llmagent.llm.service.output;

import com.llmagent.llm.chat.request.json.JsonObjectSchema;
import com.llmagent.llm.chat.request.json.JsonSchema;

import java.util.Optional;

import static com.llmagent.llm.service.DefaultToolExecutor.getBoundedLongValue;
import static com.llmagent.util.ParsingUtils.parseAsStringOrJson;

public class IntegerOutputParser implements OutputParser<Integer> {

    @Override
    public Integer parse(String text) {
        return parseAsStringOrJson(text, IntegerOutputParser::parseInteger, Integer.class);
    }

    private static Integer parseInteger(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException nfe) {
            return (int) getBoundedLongValue(text, "int", Integer.class, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
    }

    @Override
    public Optional<JsonSchema> jsonSchema() {
        JsonSchema jsonSchema = JsonSchema.builder()
                .name("integer")
                .rootElement(JsonObjectSchema.builder()
                        .addIntegerProperty("value")
                        .required("value")
                        .build())
                .build();
        return Optional.of(jsonSchema);
    }

    @Override
    public String formatInstructions() {
        return "integer number";
    }
}
