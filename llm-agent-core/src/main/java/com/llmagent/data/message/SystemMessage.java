package com.llmagent.data.message;

import com.llmagent.data.Role;
import com.llmagent.util.StringUtil;

import java.util.Objects;

import static com.llmagent.data.message.ChatMessageType.SYSTEM;

public class SystemMessage implements ChatMessage {

    private final Role role = Role.SYSTEM;

    private final String content;

    /**
     * Creates a new system message.
     * @param content the message text.
     */
    public SystemMessage(String content) {
        this.content = content;
    }

    /**
     * Returns the message text.
     * @return the message text.
     */
    public String content() {
        return content;
    }

    public Role role() {
        return role;
    }

    @Override
    public ChatMessageType type() {
        return SYSTEM;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemMessage that = (SystemMessage) o;
        return Objects.equals(this.content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "SystemMessage {" +
                " content = " + StringUtil.quoted(content) +
                " }";
    }

    /**
     * Creates a new system message.
     * @param text the message text.
     * @return the system message.
     */
    public static SystemMessage from(String text) {
        return new SystemMessage(text);
    }

    /**
     * Creates a new system message.
     * @param text the message text.
     * @return the system message.
     */
    public static SystemMessage systemMessage(String text) {
        return from(text);
    }
}
