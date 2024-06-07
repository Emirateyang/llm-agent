package com.llmagent.data.message;

public enum ContentType {
    /**
     * Text content.
     */
    TEXT(TextContent.class),
    /**
     * Image content.
     */
    IMAGE(ImageContent.class);

    private final Class<? extends Content> contentClass;

    ContentType(Class<? extends Content> contentClass) {
        this.contentClass = contentClass;
    }

    /**
     * Returns the class of the content type.
     * @return The class of the content type.
     */
    public Class<? extends Content> getContentClass() {
        return contentClass;
    }
}
