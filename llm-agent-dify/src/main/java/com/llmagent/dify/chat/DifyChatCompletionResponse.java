package com.llmagent.dify.chat;

public class DifyChatCompletionResponse extends DifyMessage {
    private String mode;
    private DifyResponseMetadata metadata;

    public String answer() {
        return answer;
    }
}
