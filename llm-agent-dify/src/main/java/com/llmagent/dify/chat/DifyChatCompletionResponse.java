package com.llmagent.dify.chat;

import lombok.Data;

import java.util.Objects;

@Data
public class DifyChatCompletionResponse extends DifyMessage {

    private String mode;
    private DifyResponseMetadata metadata;

    public String answer() {
        return answer;
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(mode);
        h += (h << 5) + Objects.hashCode(metadata);
        return h;
    }
}
