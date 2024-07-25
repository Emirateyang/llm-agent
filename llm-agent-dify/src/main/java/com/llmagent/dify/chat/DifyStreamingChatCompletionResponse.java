package com.llmagent.dify.chat;

import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
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

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(taskId);
        h += (h << 5) + Objects.hashCode(id);
        h += (h << 5) + Objects.hashCode(position);
        h += (h << 5) + Objects.hashCode(thought);
        h += (h << 5) + Objects.hashCode(observation);
        h += (h << 5) + Objects.hashCode(tool);
        h += (h << 5) + Objects.hashCode(toolInput);
        h += (h << 5) + Objects.hashCode(messageFiles);
        h += (h << 5) + Objects.hashCode(type);
        h += (h << 5) + Objects.hashCode(belongsTo);
        h += (h << 5) + Objects.hashCode(message);
        h += (h << 5) + Objects.hashCode(metadata);
        return h;
    }
}
