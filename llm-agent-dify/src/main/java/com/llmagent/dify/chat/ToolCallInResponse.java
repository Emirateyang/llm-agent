package com.llmagent.dify.chat;

import lombok.Data;

import java.util.Map;

@Data
public class ToolCallInResponse {
    private String name;
    private Map<String, Object> params;
}
