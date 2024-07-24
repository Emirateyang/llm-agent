package com.llmagent.dify.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DifyMessageRequest {

    private Map<String, Object> inputs;
    private String responseMode;
    private String conversationId;
    private String user;
    private List<DifyFileContent> files;
    // 默认 true。 若设置为 false，则可通过调用会话重命名接口并设置 auto_generate 为 true 实现异步生成标题
    private boolean autoGenerateName = true;

    private DifyMessageRequest(Builder builder) {
        this.inputs = builder.inputs;
        this.responseMode = builder.responseMode;
        this.conversationId = builder.conversationId;
        this.files = builder.files;
        this.autoGenerateName = builder.autoGenerateName;
        this.user = builder.user;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Map<String, Object> inputs;
        private String responseMode;
        private String conversationId;
        private String user;
        private List<DifyFileContent> files;
        // 默认 true。 若设置为 false，则可通过调用会话重命名接口并设置 auto_generate 为 true 实现异步生成标题
        private boolean autoGenerateName = true;

        private Builder() {
        }

        public Builder from(DifyMessageRequest instance) {
            inputs(instance.inputs);
            responseMode(instance.responseMode);
            conversationId(instance.conversationId);
            autoGenerateName(instance.autoGenerateName);
            user(instance.user);
            files(instance.files);
            return this;
        }

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


        public Builder autoGenerateName(Boolean autoGenerateName) {
            this.autoGenerateName = autoGenerateName;
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


        public DifyMessageRequest build() {
            return new DifyMessageRequest(this);
        }
    }

}
