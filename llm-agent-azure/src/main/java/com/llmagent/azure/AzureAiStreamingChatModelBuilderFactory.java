package com.llmagent.azure;

import com.llmagent.azure.chat.AzureAiStreamingChatModel;

import java.util.function.Supplier;

public interface AzureAiStreamingChatModelBuilderFactory extends Supplier<AzureAiStreamingChatModel.Builder> {
}
