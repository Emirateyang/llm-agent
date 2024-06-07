package com.llmagent.data.message;

public interface Content {
    /**
     * Returns the type of content.
     *
     * <p>Can be used to cast the content to the correct type.</p>
     *
     * @return The type of content.
     */
    ContentType type();
}
