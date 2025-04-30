package com.llmagent.llm.service;

import com.llmagent.data.message.ToolMessage;
import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.chat.StreamingChatLanguageModel;
import com.llmagent.llm.memory.ChatMemory;
import com.llmagent.llm.memory.ChatMemoryProvider;
import com.llmagent.llm.memory.MemoryId;
import com.llmagent.llm.output.TokenStream;
import com.llmagent.llm.tool.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.llmagent.exception.IllegalConfigurationException.illegalConfiguration;
import static com.llmagent.util.ServiceHelper.loadFactories;
import static java.util.Arrays.asList;

/**
 * LLM Services is a high-level API to interact with {@link ChatLanguageModel} and {@link StreamingChatLanguageModel}.
 * <p>
 * You can define your own API (a Java interface with one or more methods),
 * and {@code LlmService} will provide an implementation for it, hiding all the complexity from you.
 * <p>
 */
public abstract class LlmService<T> {

    protected final LlmServiceContext context;

    protected LlmService(LlmServiceContext context) {
        this.context = context;
    }

    /**
     * Creates an LLM Service (an implementation of the provided interface), that is backed by the provided chat model.
     * This convenience method can be used to create simple LLM Services.
     * For more complex cases, please use {@link #builder}.
     *
     * @param llmService The class of the interface to be implemented.
     * @param chatModel The chat model to be used under the hood.
     * @return An instance of the provided interface, implementing all its defined methods.
     */
    public static <T> T create(Class<T> llmService, ChatLanguageModel chatModel) {
        return builder(llmService).chatModel(chatModel).build();
    }

    /**
     * Begins the construction of an LLm Service.
     *
     * @param llmService The class of the interface to be implemented.
     * @return builder
     */
    public static <T> LlmService<T> builder(Class<T> llmService) {
        LlmServiceContext context = new LlmServiceContext(llmService);
        for (LlmServicesFactory factory : loadFactories(LlmServicesFactory.class)) {
            return factory.create(context);
        }
        return new DefaultLlmService<>(context);
    }

    /**
     * Configures chat model that will be used under the hood of the LLM Service.
     * <p>
     * Either {@link ChatLanguageModel} or {@link StreamingChatLanguageModel} should be configured,
     * but not both at the same time.
     *
     * @param chatModel Chat model that will be used under the hood of the AI Service.
     * @return builder
     */
    public LlmService<T> chatModel(ChatLanguageModel chatModel) {
        context.chatModel = chatModel;
        return this;
    }

    /**
     * Configures streaming chat model that will be used under the hood of the LLM Service.
     * The methods of the AI Service must return a {@link TokenStream} type.
     * <p>
     * Either {@link ChatLanguageModel} or {@link StreamingChatLanguageModel} should be configured,
     * but not both at the same time.
     *
     * @param streamingChatModel Streaming chat model that will be used under the hood of the AI Service.
     * @return builder
     */
    public LlmService<T> streamingChatModel(StreamingChatLanguageModel streamingChatModel) {
        context.streamingChatModel = streamingChatModel;
        return this;
    }

    /**
     * Configures the system message provider, which provides a system message to be used each time an AI service is invoked.
     * <br>
     * When both {@code @SystemMessage} and the system message provider are configured,
     * {@code @SystemMessage} takes precedence.
     *
     * @param systemMessageProvider A {@link Function} that accepts a chat memory ID
     *                              (a value of a method parameter annotated with @{@link MemoryId})
     *                              and returns a system message to be used.
     *                              If there is no parameter annotated with {@code @MemoryId},
     *                              the value of memory ID is "default".
     *                              The returned {@link String} can be either a complete system message
     *                              or a system message template containing unresolved template variables (e.g. "{{name}}"),
     *                              which will be resolved using the values of method parameters annotated with @{@link V}.
     * @return builder
     */
    public LlmService<T> systemMessageProvider(Function<Object, String> systemMessageProvider) {
        context.systemMessageProvider = systemMessageProvider.andThen(Optional::ofNullable);
        return this;
    }

    /**
     * Configures the chat memory that will be used to preserve conversation history between method calls.
     * <p>
     * Unless a {@link ChatMemory} or {@link ChatMemoryProvider} is configured, all method calls will be independent of each other.
     * In other words, the LLM will not remember the conversation from the previous method calls.
     * <p>
     * The same {@link ChatMemory} instance will be used for every method call.
     * <p>
     * If you want to have a separate {@link ChatMemory} for each user/conversation, configure {@link #chatMemoryProvider} instead.
     * <p>
     * Either a {@link ChatMemory} or a {@link ChatMemoryProvider} can be configured, but not both simultaneously.
     *
     * @param chatMemory An instance of chat memory to be used by the AI Service.
     * @return builder
     */
    public LlmService<T> chatMemory(ChatMemory chatMemory) {
        context.initChatMemories(chatMemory);
        return this;
    }

    /**
     * Configures the chat memory provider, which provides a dedicated instance of {@link ChatMemory} for each user/conversation.
     * To distinguish between users/conversations, one of the method's arguments should be a memory ID (of any data type)
     * annotated with {@link MemoryId}.
     * For each new (previously unseen) memoryId, an instance of {@link ChatMemory} will be automatically obtained
     * by invoking {@link ChatMemoryProvider#get(Object id)}.
     *
     * If you prefer to use the same (shared) {@link ChatMemory} for all users/conversations, configure a {@link #chatMemory} instead.
     * <p>
     * Either a {@link ChatMemory} or a {@link ChatMemoryProvider} can be configured, but not both simultaneously.
     *
     * @param chatMemoryProvider The provider of a {@link ChatMemory} for each new user/conversation.
     * @return builder
     */
    public LlmService<T> chatMemoryProvider(ChatMemoryProvider chatMemoryProvider) {
        context.initChatMemories(chatMemoryProvider);
        return this;
    }

    /**
     * Configures the tools that the LLM can use.
     *
     * @param objectsWithTools One or more objects whose methods are annotated with {@link Tool}.
     *                         All these tools (methods annotated with {@link Tool}) will be accessible to the LLM.
     *                         Note that inherited methods are ignored.
     * @return builder
     * @see Tool
     */
    public LlmService<T> tools(Object... objectsWithTools) {
        return tools(asList(objectsWithTools));
    }

    /**
     * Configures the tools that the LLM can use.
     *
     * @param objectsWithTools A list of objects whose methods are annotated with {@link Tool}.
     *                         All these tools (methods annotated with {@link Tool}) are accessible to the LLM.
     *                         Note that inherited methods are ignored.
     * @return builder
     * @see Tool
     */
    public LlmService<T> tools(Collection<Object> objectsWithTools) {
        context.toolService.tools(objectsWithTools);
        return this;
    }

    /**
     * Configures the tool provider that the LLM can use
     *
     * @param toolProvider Decides which tools the LLM could use to handle the request
     * @return builder
     */
    public LlmService<T> toolProvider(ToolProvider toolProvider) {
        context.toolService.toolProvider(toolProvider);
        return this;
    }

    /**
     * Configures the tools that the LLM can use.
     *
     * @param tools A map of {@link ToolSpecification} to {@link ToolExecutor} entries.
     *              This method of configuring tools is useful when tools must be configured programmatically.
     *              Otherwise, it is recommended to use the {@link Tool}-annotated java methods
     *              and configure tools with the {@link #tools(Object...)} and {@link #tools(Collection)} methods.
     * @return builder
     */
    public LlmService<T> tools(Map<ToolSpecification, ToolExecutor> tools) {
        context.toolService.tools(tools);
        return this;
    }

    public LlmService<T> maxSequentialToolsInvocations(int maxSequentialToolsInvocations) {
        context.toolService.maxSequentialToolsInvocations(maxSequentialToolsInvocations);
        return this;
    }

    /**
     * Configures the strategy to be used when the LLM hallucinates a tool name (i.e., attempts to call a nonexistent tool).
     *
     * @param hallucinatedToolNameStrategy A Function from {@link ToolRequest} to {@link ToolMessage} defining
     *                                  the response provided to the LLM when it hallucinates a tool name.
     * @return builder
     */
    public LlmService<T> hallucinatedToolNameStrategy(
            Function<ToolRequest, ToolMessage> hallucinatedToolNameStrategy) {
        context.toolService.hallucinatedToolNameStrategy(hallucinatedToolNameStrategy);
        return this;
    }

    /**
     * Constructs and returns the LLM Service.
     *
     * @return An instance of the LLM Service implementing the specified interface.
     */
    public abstract T build();

    protected void performBasicValidation() {
        if (context.chatModel == null && context.streamingChatModel == null) {
            throw illegalConfiguration("Please specify either chatLanguageModel or streamingLanguageChatModel");
        }
    }
}
