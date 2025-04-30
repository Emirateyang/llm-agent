package com.llmagent.llm.service;

public interface LlmServicesFactory {

    <T> LlmService<T> create(LlmServiceContext context);
}
