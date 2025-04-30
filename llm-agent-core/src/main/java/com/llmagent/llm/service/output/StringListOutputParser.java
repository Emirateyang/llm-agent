package com.llmagent.llm.service.output;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class StringListOutputParser extends StringCollectionOutputParser<List<String>> {

    @Override
    Supplier<List<String>> emptyCollectionSupplier() {
        return ArrayList::new;
    }

    @Override
    Class<?> collectionType() {
        return List.class;
    }
}
