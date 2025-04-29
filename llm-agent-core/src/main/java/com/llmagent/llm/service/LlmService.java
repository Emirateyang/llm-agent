package com.llmagent.llm.service;

import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.chat.StreamingChatLanguageModel;

/**
 * Agent Services is a high-level API to interact with {@link ChatLanguageModel} and {@link StreamingChatLanguageModel}.
 * <p>
 * You can define your own API (a Java interface with one or more methods),
 * and {@code LlmService} will provide an implementation for it, hiding all the complexity from you.
 * <p>
 */
public abstract class LlmService<T> {
}
