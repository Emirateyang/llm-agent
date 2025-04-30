package com.llmagent.llm.prompt;

import com.llmagent.llm.input.Prompt;

public interface StructuredPromptFactory {
    /**
     * Converts the given structured prompt to a prompt.
     * @param structuredPrompt the structured prompt.
     * @return the prompt.
     */
    Prompt toPrompt(Object structuredPrompt);
}
