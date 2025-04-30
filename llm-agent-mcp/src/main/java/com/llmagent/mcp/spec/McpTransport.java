package com.llmagent.mcp.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import reactor.core.publisher.Mono;

/**
 * Defines the asynchronous transport layer for the Model Context Protocol (MCP).
 *
 * <p>
 * The McpTransport interface provides the foundation for implementing custom transport
 * mechanisms in the Model Context Protocol. It handles the bidirectional communication
 * between the client and server components, supporting asynchronous message exchange
 * using JSON-RPC format.
 * </p>
 */
public interface McpTransport {

    /**
     * Closes the transport connection and releases any associated resources.
     * <p>
     * This method ensures proper cleanup of resources when the transport is no longer needed.
     * It should handle the graceful shutdown of any active connections.
     * </p>
     */
    default void close() {
        this.closeGracefully().subscribe();
    }

    /**
     * Closes the transport connection and releases any associated resources asynchronously.
     * @return a {@link Mono<Void>} that completes when the connection has been closed.
     */
    Mono<Void> closeGracefully();

    /**
     * Sends a message to the peer asynchronously.
     * Messages are sent in JSON-RPC format as specified by the MCP protocol.
     */
    Mono<Void> sendMessage(McpSchema.JSONRPCMessage message);

    /**
     * Performs transport-specific health checks, if applicable. This is called
     * by `McpClient.checkHealth()` as the first check before performing a check
     * by sending a 'ping' over the MCP protocol. The purpose is that the
     * transport may have some specific and faster ways to detect that it is broken,
     * like for example, the STDIO transport can fail the check if it detects
     * that the server subprocess isn't alive anymore.
     */
    void checkHealth();

    /**
     * Unmarshal the given data into an object of the specified type.
     * @param <T> the type of the object to unmarshal
     * @param data the data to unmarshal
     * @param typeReference the type reference for the object to unmarshal
     * @return the unmarshalled object
     */
    <T> T unmarshal(Object data, TypeReference<T> typeReference);
}
