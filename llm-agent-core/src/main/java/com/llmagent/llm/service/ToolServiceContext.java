package com.llmagent.llm.service;

import com.llmagent.llm.tool.ToolExecutor;
import com.llmagent.llm.tool.ToolSpecification;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ToolServiceContext {

    private final List<ToolSpecification> toolSpecifications;
    private final Map<String, ToolExecutor> toolExecutors;

    public ToolServiceContext(List<ToolSpecification> toolSpecifications, Map<String, ToolExecutor> toolExecutors) {
        this.toolSpecifications = toolSpecifications;
        this.toolExecutors = toolExecutors;
    }

    public List<ToolSpecification> toolSpecifications() {
        return toolSpecifications;
    }

    public Map<String, ToolExecutor> toolExecutors() {
        return toolExecutors;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ToolServiceContext) obj;
        return Objects.equals(this.toolSpecifications, that.toolSpecifications) &&
                Objects.equals(this.toolExecutors, that.toolExecutors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toolSpecifications, toolExecutors);
    }

    @Override
    public String toString() {
        return "ToolServiceContext[" +
                "toolSpecifications=" + toolSpecifications + ", " +
                "toolExecutors=" + toolExecutors + ']';
    }
}
