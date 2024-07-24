package com.llmagent.dify;


import com.llmagent.dify.client.DifyClient;

import java.util.function.Supplier;

public interface DifyClientBuilderFactory extends Supplier<DifyClient.Builder> {
}
