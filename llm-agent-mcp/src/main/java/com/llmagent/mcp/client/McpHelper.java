package com.llmagent.mcp.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.llmagent.llm.chat.request.json.*;
import com.llmagent.llm.tool.ToolRequest;
import com.llmagent.llm.tool.ToolSpecification;
import com.llmagent.mcp.spec.McpException;
import com.llmagent.mcp.spec.McpSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class McpHelper {

    static McpSchema.CallToolRequest toMcpTooRequest(ToolRequest request) {
        return new McpSchema.CallToolRequest(request.name(), request.arguments());
    }

    /**
     * Converts the 'tools' element (inside the 'ListToolsResult' type in the MCP schema) into ${@link ToolSpecification}
     */
    static List<ToolSpecification> toToolSpecifications(McpSchema.ListToolsResult listToolsResult) {
        List<ToolSpecification> toolSpecifications = new ArrayList<>();
        for (McpSchema.Tool tool : listToolsResult.tools()) {
            toolSpecifications.add(ToolSpecification.builder().name(tool.name())
                    .description(tool.description())
                    .parameters((JsonObjectSchema) jsonNodeToJsonSchemaElement(convertToJsonNode(tool.inputSchema())))
                    .build());
        }
        return toolSpecifications;
    }

    static JsonNode convertToJsonNode(McpSchema.JsonSchema schema) {
        if (schema == null) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
            return objectMapper.valueToTree(schema);
        } catch (Exception e) {
            throw new IllegalArgumentException("can't convert JsonSchema into JsonNode: " + e.getMessage(), e);
        }
    }

    /**
     * Converts the 'inputSchema' element (inside the 'Tool' type in the MCP schema)
     * to a JsonSchemaElement object that describes the tool's arguments.
     */
    static JsonSchemaElement jsonNodeToJsonSchemaElement(JsonNode node) {
        if (node.get("type").getNodeType() != JsonNodeType.ARRAY) {
            String nodeType = node.get("type").asText();
            if (nodeType.equals("object")) {
                JsonObjectSchema.Builder builder = JsonObjectSchema.builder();
                JsonNode required = node.get("required");
                if (required != null) {
                    builder.required(toStringArray((ArrayNode) required));
                }
                if (node.has("additionalProperties")) {
                    builder.additionalProperties(
                            node.get("additionalProperties").asBoolean(false));
                }
                JsonNode description = node.get("description");
                if (description != null) {
                    builder.description(description.asText());
                }
                JsonNode properties = node.get("properties");
                if (properties != null) {
                    ObjectNode propertiesObject = (ObjectNode) properties;
                    for (Map.Entry<String, JsonNode> property : propertiesObject.properties()) {
                        builder.addProperty(property.getKey(), jsonNodeToJsonSchemaElement(property.getValue()));
                    }
                }
                return builder.build();
            } else if (nodeType.equals("string")) {
                if (node.has("enum")) {
                    JsonEnumSchema.Builder builder = JsonEnumSchema.builder();
                    if (node.has("description")) {
                        builder.description(node.get("description").asText());
                    }
                    builder.enumValues(toStringArray((ArrayNode) node.get("enum")));
                    return builder.build();
                } else {
                    JsonStringSchema.Builder builder = JsonStringSchema.builder();
                    if (node.has("description")) {
                        builder.description(node.get("description").asText());
                    }
                    return builder.build();
                }
            } else if (nodeType.equals("number")) {
                JsonNumberSchema.Builder builder = JsonNumberSchema.builder();
                if (node.has("description")) {
                    builder.description(node.get("description").asText());
                }
                return builder.build();
            } else if (nodeType.equals("integer")) {
                JsonIntegerSchema.Builder builder = JsonIntegerSchema.builder();
                if (node.has("description")) {
                    builder.description(node.get("description").asText());
                }
                return builder.build();
            } else if (nodeType.equals("boolean")) {
                JsonBooleanSchema.Builder builder = JsonBooleanSchema.builder();
                if (node.has("description")) {
                    builder.description(node.get("description").asText());
                }
                return builder.build();
            } else if (nodeType.equals("array")) {
                JsonArraySchema.Builder builder = JsonArraySchema.builder();
                if (node.has("description")) {
                    builder.description(node.get("description").asText());
                }
                builder.items(jsonNodeToJsonSchemaElement(node.get("items")));
                return builder.build();
            } else {
                throw new IllegalArgumentException("Unknown element type: " + nodeType);
            }
        } else {
            // this represents an array with multiple allowed types for items
            // for example:
            // "type": "array",
            //  "items": {
            //    "type": ["integer", "string", "null"]
            //  }
            //
            // and we transform this into
            //
            // "type": "array",
            // "items": {
            //   "anyOf": [
            //       {
            //           "type": "integer"
            //       },
            //       {
            //           "type": "string"
            //       },
            //       {
            //           "type": "null"
            //       }
            //   ]
            // }
            JsonAnyOfSchema.Builder anyOf = JsonAnyOfSchema.builder();
            JsonSchemaElement[] types = StreamSupport.stream(node.get("type").spliterator(), false)
                    .map(McpHelper::toTypeElement)
                    .toArray(JsonSchemaElement[]::new);
            anyOf.anyOf(types);
            return anyOf.build();
        }
    }

    private static JsonSchemaElement toTypeElement(JsonNode node) {
        if (!node.isTextual()) {
            throw new IllegalArgumentException(node + " is not a string");
        }
        switch (node.textValue()) {
            case "string":
                return JsonStringSchema.builder().build();
            case "number":
                return JsonNumberSchema.builder().build();
            case "integer":
                return JsonIntegerSchema.builder().build();
            case "boolean":
                return JsonBooleanSchema.builder().build();
            case "array":
                return JsonArraySchema.builder().build();
            case "object":
                return JsonObjectSchema.builder().build();
            case "null":
                return new JsonNullSchema();
            default:
                throw new IllegalArgumentException("Unsupported type: " + node.textValue());
        }
    }

    private static String[] toStringArray(ArrayNode jsonArray) {
        String[] result = new String[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            result[i] = jsonArray.get(i).asText();
        }
        return result;
    }

    static String mapClientResultToString(McpSchema.CallToolResult result) {
        if (result == null) {
            throw new McpException("Tool execution returned null result");
        }

        // check whether the result has an error
        if (Boolean.TRUE.equals(result.isError())) {
            StringBuilder errorMessage = new StringBuilder("Tool execution failed: ");
            if (result.content() != null && !result.content().isEmpty()) {
                // Try to extract text error information from the content
                String contentDetails = result.content().stream()
                        .map(content -> {
                            if (content instanceof McpSchema.TextContent textContent) {
                                return textContent.text();
                            } else {
                                return "[" + content.type() + " content]";
                            }
                        })
                        .collect(Collectors.joining("\n"));

                if (!contentDetails.trim().isEmpty()) {
                    errorMessage.append(contentDetails);
                } else {
                    errorMessage.append("No detailed error message provided.");
                }
            } else {
                errorMessage.append("No error content provided.");
            }

            // In general, tool execution failures should be notified to the upper through exceptions.
            throw new McpException(errorMessage.toString().trim());
        } else {
            if (result.content() == null || result.content().isEmpty()) {
                return "Success";
            }

            // Convert all content items to strings and concatenate them.
            // Here, we focus on processing TextContent and mark other types of content.
            StringBuilder successContent = new StringBuilder();
            boolean first = true;
            for (McpSchema.Content content : result.content()) {
                if (!first) {
                    successContent.append("\n");
                }

                if (content instanceof McpSchema.TextContent textContent) {
                    successContent.append(textContent.text());
                } else if (content instanceof McpSchema.ImageContent) {
                    successContent.append("[Image content: ").append(content.type()).append("]");
                } else if (content instanceof McpSchema.EmbeddedResource) {
                    successContent.append("[Resource content: ").append(content.type()).append("]");
                } else {
                    successContent.append("[Unknown content type: ").append(content.type()).append("]");
                }
                first = false;
            }

            return successContent.toString().trim();
        }
    }
}
