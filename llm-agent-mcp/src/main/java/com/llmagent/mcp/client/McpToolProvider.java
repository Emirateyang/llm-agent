package com.llmagent.mcp.client;

import com.llmagent.exception.McpRetrieveToolException;
import com.llmagent.llm.tool.ToolProvider;
import com.llmagent.llm.tool.ToolProviderRequest;
import com.llmagent.llm.tool.ToolProviderResult;
import com.llmagent.llm.tool.ToolSpecification;
import com.llmagent.mcp.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.llmagent.mcp.client.McpHelper.*;
import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.ValidationUtil.ensureNotNull;

public class McpToolProvider implements ToolProvider {
    private static final Logger log = LoggerFactory.getLogger(McpToolProvider.class);
    private final List<McpClient> mcpClients;
    private final boolean failAll;

    private McpToolProvider(Builder builder) {
        this.mcpClients = new ArrayList<>(builder.mcpClients);
        this.failAll = getOrDefault(builder.failAll, false);
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult.Builder builder = ToolProviderResult.builder();
        for (McpClient mcpClient : mcpClients) {
            try {
                McpSchema.ListToolsResult listToolsResult = mcpClient.initialize().then(mcpClient.listTools()).block();
                ensureNotNull(listToolsResult, "Failed to retrieve tools from MCP server");
                List<ToolSpecification> toolSpecifications = toToolSpecifications(listToolsResult);
                for (ToolSpecification toolSpecification : toolSpecifications) {
                    builder.add(
                        toolSpecification, (toolRequest, memoryId) -> {
                            McpSchema.CallToolResult callResult = mcpClient.executeTool(toMcpTooRequest(toolRequest)).block();
                            return mapClientResultToString(callResult);
                        });
                }
            } catch (Exception e) {
                if (failAll) {
                    throw new McpRetrieveToolException("Failed to retrieve tools from MCP server", e);
                } else {
                    log.warn("Failed to retrieve tools from MCP server", e);
                }
            }
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<McpClient> mcpClients;
        private Boolean failAll;

        /**
         * The list of MCP clients to use for tools.
         */
        public McpToolProvider.Builder mcpClients(List<McpClient> mcpClients) {
            this.mcpClients = mcpClients;
            return this;
        }

        /**
         * If this is true, then the tool provider will throw an exception if it fails to list tools from any of the servers.
         * If this is false (default), then the tool provider will ignore the error and continue with the next server.
         */
        public McpToolProvider.Builder failAll(boolean failAll) {
            this.failAll = failAll;
            return this;
        }

        public McpToolProvider build() {
            return new McpToolProvider(this);
        }
    }
}
