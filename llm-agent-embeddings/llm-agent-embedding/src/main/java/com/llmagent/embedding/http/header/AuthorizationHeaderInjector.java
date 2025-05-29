package com.llmagent.embedding.http.header;

import java.util.Map;

/**
 * 将认证信息注入到request的header中.
 */
public class AuthorizationHeaderInjector extends GenericHeaderInjector {
    public AuthorizationHeaderInjector(String apiKey) {
        super(Map.of(
                "Authorization", "Bearer " + apiKey,
                "Content-Type", "application/json"
        ));
    }
}
