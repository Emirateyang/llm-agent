package com.llmagent.llm.input;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.llmagent.llm.prompt.StructuredPromptFactory;

import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.PropertyAccessor.FIELD;

public class DefaultStructuredPromptFactory implements StructuredPromptFactory {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .visibility(FIELD, ANY)
            .build();

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    /**
     * Create a default structured prompt factory.
     */
    public DefaultStructuredPromptFactory() {
    }

    @Override
    public Prompt toPrompt(Object structuredPrompt) {
        StructuredPrompt annotation = StructuredPrompt.Util.validateStructuredPrompt(structuredPrompt);

        String promptTemplateString = StructuredPrompt.Util.join(annotation);
        PromptTemplate promptTemplate = PromptTemplate.from(promptTemplateString);

        Map<String, Object> variables = extractVariables(structuredPrompt);

        return promptTemplate.apply(variables);
    }

    /**
     * Extracts the variables from the structured prompt.
     *
     * @param structuredPrompt The structured prompt.
     * @return The variables map.
     */
    private static Map<String, Object> extractVariables(Object structuredPrompt) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(structuredPrompt);
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
