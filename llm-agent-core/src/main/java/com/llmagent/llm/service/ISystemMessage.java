package com.llmagent.llm.service;

import com.llmagent.exception.IllegalConfigurationException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies either a complete system message (prompt) or a system message template to be used
 * each time an LLM service is invoked.
 * <br>
 * An example:
 * <pre>
 * interface Assistant {
 *
 *     {@code @ISystemMessage}("You are a helpful assistant")
 *     String chat(String userMessage);
 * }
 * </pre>
 * The system message can contain template variables,
 * which will be resolved with values from method parameters annotated with @{@link PromptVariable}.
 * <br>
 * An example:
 * <pre>
 * interface Assistant {
 *
 *     {@code @ISystemMessage}("You are a {{characteristic}} assistant")
 *     String chat(@IUserMessage String userMessage, @PromptVariable("characteristic") String characteristic);
 * }
 * </pre>
 * When both {@code @ISystemMessage} and {@link LlmService#systemMessageProvider(Function)} are configured,
 * {@code @SystemMessage} takes precedence.
 */
@Target({ElementType.TYPE, METHOD})
@Retention(RUNTIME)
public @interface ISystemMessage {

    /**
     * Prompt template can be defined in one line or multiple lines.
     * If the template is defined in multiple lines, the lines will be joined with a delimiter defined below.
     */
    String[] value() default "";

    String delimiter() default "\n";

    /**
     * The resource from which to read the prompt template.
     * If no resource is specified, the prompt template is taken from {@link #value()}.
     * If the resource is not found, an {@link IllegalConfigurationException} is thrown.
     * <p>
     * The resource will be read by calling {@link Class#getResourceAsStream(String)}
     * on the AI Service class (interface).
     */
    String fromResource() default "";
}
