package com.llmagent.dify.chat;

import java.util.List;

public class DifyStreamingChatCompletionResponse extends DifyMessage {

    private String taskId;

    private String id;
    // event: agent_thought only

    // agent_thought在消息中的位置，如第一轮迭代position为1
    private Integer position;
    private String thought;
    private String observation;
    private String tool;
    private String toolInput;
    private List<String> messageFiles;

    // event: message_file only
    private String type;
    private String belongsTo;
    private String url;

    // event: error only
    private Integer status;
    private String code;
    private String message;

    private DifyResponseMetadata metadata;

}
