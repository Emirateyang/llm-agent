package com.llmagent.dify.chat;

import lombok.Data;

@Data
public class DifyMessage {

    protected String event;
    protected String messageId;
    protected String conversationId;
    protected String answer;
    protected Integer createdAt;
}
