package com.llmagent.mcp.client;

import com.llmagent.llm.chat.request.json.JsonObjectSchema;
import com.llmagent.llm.tool.ToolRequest;
import com.llmagent.llm.tool.ToolSpecification;
import com.llmagent.mcp.spec.McpException;
import com.llmagent.mcp.spec.McpSchema;
import com.llmagent.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class McpHelper {

    static McpSchema.CallToolRequest toMcpTooRequest(ToolRequest request) {
        return new McpSchema.CallToolRequest(request.name(), request.arguments());
    }

    static List<ToolSpecification> toToolSpecifications(McpSchema.ListToolsResult listToolsResult) {
        List<ToolSpecification> toolSpecifications = new ArrayList<>();
        for (McpSchema.Tool tool : listToolsResult.tools()) {
            toolSpecifications.add(ToolSpecification.builder().name(tool.name())
                    .description(tool.description())
                    .parameters(JsonUtil.fromJson(JsonUtil.toJson(tool.inputSchema()), JsonObjectSchema.class))
                    .build());
        }
        return toolSpecifications;
    }

    static String mapClientResultToString(McpSchema.CallToolResult result) {
        if (result == null) {
            throw new McpException("Tool execution returned null result");
        }

        // 检查是否是错误结果
        if (Boolean.TRUE.equals(result.isError())) {
            // 如果是错误，处理内容作为错误信息
            StringBuilder errorMessage = new StringBuilder("Tool execution failed: ");
            if (result.content() != null && !result.content().isEmpty()) {
                // 尝试从内容中提取文本错误信息
                String contentDetails = result.content().stream()
                        .map(content -> {
                            if (content instanceof McpSchema.TextContent textContent) {
                                return textContent.text();
                            } else {
                                // 对于非文本内容，可以简单标记一下
                                return "[" + content.type() + " content]";
                            }
                        })
                        .collect(Collectors.joining("\n")); // 用换行符分隔不同内容项

                if (!contentDetails.trim().isEmpty()) {
                    errorMessage.append(contentDetails);
                } else {
                    errorMessage.append("No detailed error message provided.");
                }
            } else {
                errorMessage.append("No error content provided.");
            }

            // 通常情况下，工具执行失败应该通过异常来向上层系统通知
            throw new McpException(errorMessage.toString().trim());

        } else {
            // 如果执行成功，处理内容作为结果
            if (result.content() == null || result.content().isEmpty()) {
                // 成功执行但没有返回内容
                return "Success";
            }

            // 将所有内容项转换为字符串，并拼接起来
            // 这里我们重点处理 TextContent，并标记其他类型的内容
            StringBuilder successContent = new StringBuilder();
            boolean first = true;
            for (McpSchema.Content content : result.content()) {
                if (!first) {
                    // 在不同内容项之间添加分隔符，比如换行
                    successContent.append("\n");
                }

                if (content instanceof McpSchema.TextContent textContent) {
                    successContent.append(textContent.text());
                } else if (content instanceof McpSchema.ImageContent) {
                    successContent.append("[Image content: ").append(content.type()).append("]"); // 可以根据 ImageContent 字段添加更多描述
                } else if (content instanceof McpSchema.EmbeddedResource) {
                    successContent.append("[Resource content: ").append(content.type()).append("]"); // 可以根据 EmbeddedResource 字段添加更多描述
                } else {
                    // 处理未知的内容类型（如果密封接口定义完整，理论上不会到这里）
                    successContent.append("[Unknown content type: ").append(content.type()).append("]");
                }
                first = false;
            }

            return successContent.toString().trim(); // trim() 移除可能的末尾换行符
        }
    }
}
