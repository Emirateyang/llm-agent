package com.llmagent.embedding.dashscope.client;

import java.util.Map;

/**
 * 将认证信息注入到request的header中.
 */
public class AuthorizationHeaderInjector extends GenericHeaderInjector {
    AuthorizationHeaderInjector(String apiKey) {
        super(Map.of(
                "Authorization", "Bearer " + apiKey,
                "Content-Type", "application/json"
        ));
    }
}
