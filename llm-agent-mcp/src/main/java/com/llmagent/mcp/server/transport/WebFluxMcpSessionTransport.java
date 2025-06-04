package com.llmagent.mcp.server.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmagent.mcp.server.McpServerTransport;
import com.llmagent.mcp.server.ServerSentEvent;
import com.llmagent.mcp.spec.McpSchema;
import reactor.core.Exceptions;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class WebFluxMcpSessionTransport implements McpServerTransport {

    private final FluxSink<ServerSentEvent<?>> sink;
    private final ObjectMapper objectMapper;
    public static final String MESSAGE_EVENT_TYPE = "message";

    @Override
    public void close() {
        sink.complete();
    }

    public WebFluxMcpSessionTransport(FluxSink<ServerSentEvent<?>> sink) {
        this.sink = sink;
        this.objectMapper = new ObjectMapper();
    }


    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(sink::complete);
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
        return Mono.fromSupplier(() -> {
            try {
                return objectMapper.writeValueAsString(message);
            }
            catch (IOException e) {
                throw Exceptions.propagate(e);
            }
        }).doOnNext(jsonText -> {
            ServerSentEvent<Object> event = ServerSentEvent.builder()
                    .event(MESSAGE_EVENT_TYPE)
                    .data(jsonText)
                    .build();
            sink.next(event);
        }).doOnError(e -> {
            // TODO log with sessionid
            Throwable exception = Exceptions.unwrap(e);
            sink.error(exception);
        }).then();
    }

    @Override
    public void checkHealth() {

    }

    @Override
    public <T> T unmarshal(Object data, TypeReference<T> typeReference) {
        return objectMapper.convertValue(data, typeReference);
    }
}
