package com.llmagent.llm.input;

import com.llmagent.llm.prompt.StructuredPromptFactory;

import static com.llmagent.util.ServiceHelper.loadFactories;

public class StructuredPromptProcessor {

    private StructuredPromptProcessor() {
    }

    private static final StructuredPromptFactory FACTORY = factory();

    private static StructuredPromptFactory factory() {
        for (StructuredPromptFactory factory : loadFactories(StructuredPromptFactory.class)) {
            return factory;
        }
        return new DefaultStructuredPromptFactory();
    }

    /**
     * Converts the given structured prompt to a prompt.
     *
     * @param structuredPrompt the structured prompt.
     * @return the prompt.
     */
    public static Prompt toPrompt(Object structuredPrompt) {
        return FACTORY.toPrompt(structuredPrompt);
    }
}
