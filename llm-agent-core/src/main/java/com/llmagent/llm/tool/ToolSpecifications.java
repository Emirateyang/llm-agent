package com.llmagent.llm.tool;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.llmagent.llm.tool.SchemaProperty.*;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class ToolSpecifications {
    private ToolSpecifications() {
    }

    /**
     * Returns {@link ToolSpecification}s for all methods annotated with @{@link Tool}
     * within the class of the specified object.
     *
     * @param objectWithTools the object.
     * @return the {@link ToolSpecification}s.
     */
    public static List<ToolSpecification> toolSpecificationsFrom(Object objectWithTools) {
        return toolSpecificationsFrom(objectWithTools.getClass());
    }


    /**
     * Convert a {@link Parameter} to a {@link SchemaProperty}.
     *
     * @param parameter the parameter.
     * @return the {@link SchemaProperty}.
     */
    static Iterable<SchemaProperty> toJsonSchemaProperties(Parameter parameter) {

        Class<?> type = parameter.getType();

        P annotation = parameter.getAnnotation(P.class);
        SchemaProperty description = annotation == null ? null : description(annotation.value());

        if (type == String.class) {
            return removeNulls(STRING, description);
        }

        if (isBoolean(type)) {
            return removeNulls(BOOLEAN, description);
        }

        if (isInteger(type)) {
            return removeNulls(INTEGER, description);
        }

        if (isNumber(type)) {
            return removeNulls(NUMBER, description);
        }

        if (type.isArray()) {
            return removeNulls(ARRAY, arrayTypeFrom(type.getComponentType()), description);
        }
        if (Collection.class.isAssignableFrom(type)) {
            return removeNulls(ARRAY, arrayTypeFrom(parameter.getParameterizedType()), description);
        }

        if (type.isEnum()) {
            return removeNulls(STRING, enums((Class<?>) type), description);
        }

        return removeNulls(OBJECT, description); // TODO provide internals
    }

    private static SchemaProperty arrayTypeFrom(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length == 1) {
                return arrayTypeFrom((Class<?>) actualTypeArguments[0]);
            }
        }
        return items(SchemaProperty.OBJECT);
    }

    // TODO put constraints on min and max?
    private static boolean isNumber(Class<?> type) {
        return type == float.class || type == Float.class
                || type == double.class || type == Double.class
                || type == BigDecimal.class;
    }

    private static boolean isInteger(Class<?> type) {
        return type == byte.class || type == Byte.class
                || type == short.class || type == Short.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == BigInteger.class;
    }

    private static boolean isBoolean(Class<?> type) {
        return type == boolean.class || type == Boolean.class;
    }

    private static SchemaProperty arrayTypeFrom(Class<?> clazz) {
        if (clazz == String.class) {
            return items(STRING);
        }
        if (isBoolean(clazz)) {
            return items(BOOLEAN);
        }
        if (isInteger(clazz)) {
            return items(INTEGER);
        }
        if (isNumber(clazz)) {
            return items(SchemaProperty.NUMBER);
        }
        return items(SchemaProperty.OBJECT);
    }

    /**
     * Remove nulls from the given array.
     *
     * @param items the array
     * @return an iterable of the non-null items.
     */
    static Iterable<SchemaProperty> removeNulls(SchemaProperty... items) {
        return stream(items)
                .filter(Objects::nonNull)
                .collect(toList());
    }
}
