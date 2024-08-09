package com.llmagent.azure;

import com.llmagent.azure.chat.AzureAiChatModel;

import java.util.function.Supplier;

public interface AzureAiChatModelBuilderFactory extends Supplier<AzureAiChatModel.Builder> {
}
