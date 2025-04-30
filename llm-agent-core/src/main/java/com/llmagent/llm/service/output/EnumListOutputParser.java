package com.llmagent.llm.service.output;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class EnumListOutputParser<E extends Enum<E>> extends EnumCollectionOutputParser<E, List<E>> {
    EnumListOutputParser(Class<E> enumClass) {
        super(enumClass);
    }

    @Override
    Supplier<List<E>> emptyCollectionSupplier() {
        return ArrayList::new;
    }

    @Override
    Class<?> collectionType() {
        return List.class;
    }
}
