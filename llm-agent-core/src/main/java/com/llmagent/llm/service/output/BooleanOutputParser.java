package com.llmagent.llm.service.output;

import com.llmagent.llm.chat.request.json.JsonObjectSchema;
import com.llmagent.llm.chat.request.json.JsonSchema;

import java.util.Optional;

import static com.llmagent.util.ParsingUtils.outputParsingException;
import static com.llmagent.util.ParsingUtils.parseAsStringOrJson;

public class BooleanOutputParser  implements OutputParser<Boolean> {

    @Override
    public Boolean parse(String text) {
        return parseAsStringOrJson(text, BooleanOutputParser::parseBoolean, Boolean.class);
    }

    private static boolean parseBoolean(String text) {
        String trimmed = text.trim();
        if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(trimmed);
        } else {
            throw outputParsingException(text, Boolean.class);
        }
    }

    @Override
    public Optional<JsonSchema> jsonSchema() {
        JsonSchema jsonSchema = JsonSchema.builder()
                .name("boolean")
                .rootElement(JsonObjectSchema.builder()
                        .addBooleanProperty("value")
                        .required("value")
                        .build())
                .build();
        return Optional.of(jsonSchema);
    }

    @Override
    public String formatInstructions() {
        return "one of [true, false]";
    }
}
