package com.llmagent.llm.input;

import com.llmagent.data.message.AiMessage;
import com.llmagent.data.message.SystemMessage;
import com.llmagent.data.message.UserMessage;
import com.llmagent.util.StringUtil;

import java.util.Objects;

/**
 * Represents a prompt (an input text sent to the LLM).
 * A prompt usually contains instructions, contextual information, end-user input, etc.
 * A Prompt is typically created by applying one or multiple values to a PromptTemplate.
 */
public class Prompt {
    private final String text;

    /**
     * Create a new Prompt.
     * @param text the text of the prompt.
     */
    public Prompt(String text) {
        this.text = text;
    }

    /**
     * The text of the prompt.
     * @return the text of the prompt.
     */
    public String text() {
        return text;
    }

    /**
     * Convert this prompt to a SystemMessage.
     * @return the SystemMessage.
     */
    public SystemMessage toSystemMessage() {
        return SystemMessage.systemMessage(text);
    }

    /**
     * Convert this prompt to a UserMessage with specified userName.
     * @return the UserMessage.
     */
    public UserMessage toUserMessage(String userName) {
        return UserMessage.userMessage(userName, text);
    }

    /**
     * Convert this prompt to a UserMessage.
     * @return the UserMessage.
     */
    public UserMessage toUserMessage() {
        return UserMessage.userMessage(text);
    }

    /**
     * Convert this prompt to an AiMessage.
     * @return the AiMessage.
     */
    public AiMessage toAiMessage() {
        return AiMessage.aiMessage(text);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Prompt that = (Prompt) o;
        return Objects.equals(this.text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public String toString() {
        return "Prompt {" +
                " text = " + StringUtil.quoted(text) +
                " }";
    }

    /**
     * Create a new Prompt.
     * @param text the text of the prompt.
     * @return the new Prompt.
     */
    public static Prompt from(String text) {
        return new Prompt(text);
    }
}
