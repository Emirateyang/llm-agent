package com.llmagent.mcp.client;

import com.llmagent.mcp.spec.McpSchema;
import com.llmagent.mcp.spec.McpTransport;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * client-side MCP transport
 */
public interface McpClientTransport extends McpTransport {

    /**
     * Creates a connection to the MCP server
     * This does NOT yet send the "initialize" message to negotiate capabilities.
     */
    Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler);
}
