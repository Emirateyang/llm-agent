package com.llmagent.mcp.spec;

import com.llmagent.mcp.spec.McpSchema.JSONRPCResponse.JSONRPCError;

public class McpException extends RuntimeException {

    private JSONRPCError jSONRpcError;

    public McpException(JSONRPCError jsonRpcError) {
        super(jsonRpcError.message());
        this.jSONRpcError = jsonRpcError;
    }

    public McpException(Object error) {
        super(error.toString());
    }

    public JSONRPCError getJSONRpcError() {
        return jSONRpcError;
    }
}
