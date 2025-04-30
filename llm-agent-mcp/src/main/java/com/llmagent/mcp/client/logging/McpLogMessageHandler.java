package com.llmagent.mcp.client.logging;

import com.llmagent.mcp.spec.McpSchema;

/**
 * A handler that decides what to do with received log messages from an MCP server.
 */
public interface McpLogMessageHandler {

    void handleLogMessage(McpSchema.LoggingMessageNotification message);
}
