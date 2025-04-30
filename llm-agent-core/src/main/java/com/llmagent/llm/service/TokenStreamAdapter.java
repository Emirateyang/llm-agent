package com.llmagent.llm.service;

import com.llmagent.llm.output.TokenStream;

import java.lang.reflect.Type;

public interface TokenStreamAdapter {

    boolean canAdaptTokenStreamTo(Type type);

    Object adapt(TokenStream tokenStream);
}
