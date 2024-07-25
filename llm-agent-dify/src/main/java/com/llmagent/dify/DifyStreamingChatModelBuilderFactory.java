package com.llmagent.dify;

import com.llmagent.dify.chat.DifyStreamingChatModel;

import java.util.function.Supplier;

public interface DifyStreamingChatModelBuilderFactory extends Supplier<DifyStreamingChatModel.DifyStreamingChatModelBuilder> {
}
