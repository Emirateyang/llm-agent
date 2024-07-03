package com.llmagent.openai.client;

import java.util.Collections;

/**
 * for azure openai
 */
public class ApiKeyHeaderInjector extends GenericHeaderInjector {

    ApiKeyHeaderInjector(String apiKey) {
        super(Collections.singletonMap("api-key", apiKey));
    }
}
