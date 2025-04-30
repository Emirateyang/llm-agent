package com.llmagent.mcp.client;

import com.llmagent.mcp.spec.McpSchema;
import reactor.core.publisher.Mono;


public interface McpClient extends AutoCloseable {

    /**
     * The initialization phase MUST be the first interaction between client and server.
     *@see <a href=
     * "https://github.com/modelcontextprotocol/specification/blob/main/docs/specification/basic/lifecycle.md#initialization">MCP
     * Initialization Spec</a>
     */
    Mono<McpSchema.InitializeResult> initialize();

    /**
     * Gracefully closes the client connection.
     * @return a {@link Mono <Void>} that completes when the connection has been closed.
     */
    Mono<Void> closeGracefully();

    /**
     * Obtains a list of tools from the MCP server.
     */
    Mono<McpSchema.ListToolsResult> listTools();

    /**
     * Executes a tool on the MCP server and returns the result as a String.
     */
    Mono<McpSchema.CallToolResult> executeTool(McpSchema.CallToolRequest request);

    /**
     * Obtains the current list of resources available on the MCP server.
     */
    Mono<McpSchema.ListResourcesResult> listResources();

    /**
     * Obtains the current list of resource templates (dynamic resources) available on the MCP server.
     */
    Mono<McpSchema.ListResourceTemplatesResult> listResourceTemplates();

    /**
     * Retrieves the contents of the resource with a known resource.
     * This also works for dynamic resources (templates).
     */
    Mono<McpSchema.ReadResourceResult> readResource(McpSchema.Resource resource);

    /**
     * Obtain a list of prompts available on the MCP server.
     */
    Mono<McpSchema.ListPromptsResult> listPrompts();

    /**
     * Render the contents of a prompt.
     */
    Mono<McpSchema.GetPromptResult> getPrompt(McpSchema.GetPromptRequest getPromptRequest);

    /**
     * Performs a health check that returns normally if the MCP server is reachable and
     * properly responding to ping requests. If this method throws an exception,
     * the health of this MCP client is considered degraded.
     */
    void checkHealth();
}
