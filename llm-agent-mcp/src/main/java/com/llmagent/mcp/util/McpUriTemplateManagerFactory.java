package com.llmagent.mcp.util;

/**
 * Factory interface for creating instances of {@link McpUriTemplateManager}.
 *
 */
public interface McpUriTemplateManagerFactory {

	/**
	 * Creates a new instance of {@link McpUriTemplateManager} with the specified URI
	 * template.
	 * @param uriTemplate The URI template to be used for variable extraction
	 * @return A new instance of {@link McpUriTemplateManager}
	 * @throws IllegalArgumentException if the URI template is null or empty
	 */
	McpUriTemplateManager create(String uriTemplate);

}
