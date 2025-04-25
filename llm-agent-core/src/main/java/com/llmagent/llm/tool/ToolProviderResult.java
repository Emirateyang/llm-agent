package com.llmagent.llm.tool;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.llmagent.util.ObjectUtil.copyIfNotNull;

/**
 * Manage a collection of tools, where each tool is associated with an executor
 */
public class ToolProviderResult {
    private final Map<ToolSpecification, ToolExecutor> tools;

    public ToolProviderResult(Map<ToolSpecification, ToolExecutor> tools) {
        this.tools = copyIfNotNull(tools);
    }

    public Map<ToolSpecification, ToolExecutor> tools() {
        return tools;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();

        public Builder add(ToolSpecification tool, ToolExecutor executor) {
            tools.put(tool, executor);
            return this;
        }

        public Builder addAll(Map<ToolSpecification, ToolExecutor> tools) {
            this.tools.putAll(tools);
            return this;
        }

        public ToolProviderResult build() {
            return new ToolProviderResult(tools);
        }
    }
}
