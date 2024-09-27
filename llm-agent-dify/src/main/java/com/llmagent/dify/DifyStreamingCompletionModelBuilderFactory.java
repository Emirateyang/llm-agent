package com.llmagent.dify;

import com.llmagent.dify.chat.DifyStreamingCompletionModel;

import java.util.function.Supplier;

public interface DifyStreamingCompletionModelBuilderFactory extends Supplier<DifyStreamingCompletionModel.DifyStreamingCompletionModelBuilder> {
}
