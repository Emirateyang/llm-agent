package com.llmagent.dify.chat;

import java.util.List;
import java.util.Map;

public class DifyChatRequestParameters {

    private final Map<String, Object> inputs;
    private final String responseMode;
    private final String conversationId;
    private final boolean autoGenerateName;
    private final boolean breakOnToolCalled;
    private final String user;
    private final List<DifyFileContent> files;

    private DifyChatRequestParameters(Builder builder) {
        this.inputs = builder.inputs;
        this.responseMode = builder.responseMode;
        this.conversationId = builder.conversationId;
        this.autoGenerateName = builder.autoGenerateName;
        this.user = builder.user;
        this.breakOnToolCalled = builder.breakOnToolCalled;
        this.files = builder.files;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public String getConversationId() {
        return conversationId;
    }

    public boolean hasAutoGenerateName() {
        return autoGenerateName;
    }

    public boolean hasBreakOnToolCalled() {
        return breakOnToolCalled;
    }

    public String getUser() {
        return user;
    }

    public List<DifyFileContent> getFiles() {
        return files;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Map<String, Object> inputs;
        private String responseMode;
        private String conversationId;
        private boolean autoGenerateName;
        private boolean breakOnToolCalled;
        private String user;
        private List<DifyFileContent> files;

        public Builder inputs(Map<String, Object> inputs) {
            this.inputs = inputs;
            return this;
        }

        public Builder responseMode(String responseMode) {
            this.responseMode = responseMode;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder autoGenerateName(boolean autoGenerateName) {
            this.autoGenerateName = autoGenerateName;
            return this;
        }

        public Builder breakOnToolCalled(boolean breakOnToolCalled) {
            this.breakOnToolCalled = breakOnToolCalled;
            return this;
        }
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder files(List<DifyFileContent> files) {
            this.files = files;
            return this;
        }

        public DifyChatRequestParameters build() {
            return new DifyChatRequestParameters(this);
        }
    }
}
