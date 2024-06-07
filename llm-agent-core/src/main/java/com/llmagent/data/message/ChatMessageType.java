package com.llmagent.data.message;

public enum ChatMessageType {
    SYSTEM(SystemMessage.class),

    USER(UserMessage.class);

    private final Class<? extends ChatMessage> messageClass;
    ChatMessageType(Class<? extends ChatMessage> messageClass) {
        this.messageClass = messageClass;
    }

    public Class<? extends ChatMessage> messageClass() {
        return messageClass;
    }
}
