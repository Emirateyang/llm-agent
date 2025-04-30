package com.llmagent.mcp.spec;

import reactor.core.publisher.Mono;

/**
 * Functional interface for handling incoming JSON-RPC notifications.
 * Implementations should process the notification parameters without returning a response.
 */
@FunctionalInterface
public interface NotificationHandler {

    /**
     * Handles an incoming notification with the given parameters.
     * @param params The notification parameters
     * @return A Mono that completes when the notification is processed
     */
    Mono<Void> handle(Object params);
}
