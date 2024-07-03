package com.llmagent.openai;

import com.llmagent.openai.client.OpenAiClient;

import java.util.function.Supplier;

public interface OpenAiClientBuilderFactory extends Supplier<OpenAiClient.Builder> {
}
