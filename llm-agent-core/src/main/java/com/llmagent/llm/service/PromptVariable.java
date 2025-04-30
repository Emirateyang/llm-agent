package com.llmagent.llm.service;

import com.llmagent.data.message.SystemMessage;
import com.llmagent.data.message.UserMessage;
import com.llmagent.llm.input.PromptTemplate;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * When a parameter of a method in an LLM Service is annotated with {@code @PromptVariable},
 * it becomes a prompt template variable. Its value will be injected into prompt templates defined
 * via @{@link UserMessage}, @{@link SystemMessage} and {@link LlmService#systemMessageProvider(Function)}.
 * <p>
 * Example:
 * <pre>
 * {@code @UserMessage("The user name is {{name}}. He is a {{occupation}}.")}
 * String chat(@PromptVariable("name") String name, @PromptVariable("occupation") String occupation);
 * </pre>
 * <p>
 *
 * @see UserMessage
 * @see SystemMessage
 * @see PromptTemplate
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface PromptVariable {
    /**
     * Name of a variable (placeholder) in a prompt template.
     */
    String value();
}
