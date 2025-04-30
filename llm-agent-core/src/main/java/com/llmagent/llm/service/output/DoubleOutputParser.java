package com.llmagent.llm.service.output;

import com.llmagent.llm.chat.request.json.JsonObjectSchema;
import com.llmagent.llm.chat.request.json.JsonSchema;

import java.util.Optional;

import static com.llmagent.util.ParsingUtils.parseAsStringOrJson;

public class DoubleOutputParser implements OutputParser<Double> {

    @Override
    public Double parse(String text) {
        return parseAsStringOrJson(text, Double::parseDouble, Double.class);
    }

    @Override
    public Optional<JsonSchema> jsonSchema() {
        JsonSchema jsonSchema = JsonSchema.builder()
                .name("number")
                .rootElement(JsonObjectSchema.builder()
                        .addNumberProperty("value")
                        .required("value")
                        .build())
                .build();
        return Optional.of(jsonSchema);
    }

    @Override
    public String formatInstructions() {
        return "floating point number";
    }
}
