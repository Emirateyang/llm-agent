package com.llmagent.mcp.spec;

import reactor.core.publisher.Mono;

/**
 * Functional interface for handling incoming JSON-RPC requests.
 * Implementations should process the request parameters and return a response.
 *
 * @param <T> Response type
 */
@FunctionalInterface
public interface RequestHandler<T> {

    /**
     * Handles an incoming request with the given parameters.
     * @param params The request parameters
     * @return A Mono containing the response object
     */
    Mono<T> handle(Object params);
}
