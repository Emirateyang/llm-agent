package com.llmagent.llm.tool;

import com.llmagent.llm.chat.request.json.JsonObjectSchema;
import com.llmagent.llm.chat.request.json.JsonSchemaElement;
import com.llmagent.llm.chat.request.json.JsonSchemaElementHelper;
import com.llmagent.util.JsonSchemaElementUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static com.llmagent.llm.tool.SchemaProperty.*;
import static com.llmagent.util.StringUtil.isNullOrBlank;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class ToolSpecifications {
    private ToolSpecifications() {
    }

    /**
     * Returns {@link ToolSpecification}s for all methods annotated with @{@link Tool} within the specified class.
     *
     * @param classWithTools the class.
     * @return the {@link ToolSpecification}s.
     */
    public static List<ToolSpecification> toolSpecificationsFrom(Class<?> classWithTools) {
        List<ToolSpecification> toolSpecifications = stream(classWithTools.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Tool.class))
                .map(ToolSpecifications::toolSpecificationFrom)
                .collect(toList());
        validateSpecifications(toolSpecifications);
        return toolSpecifications;
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
     * Validates all the {@link ToolSpecification}s. The validation checks for duplicate method names.
     * Throws {@link IllegalArgumentException} if validation fails
     *
     * @param toolSpecifications list of ToolSpecification to be validated.
     */
    public static void validateSpecifications(List<ToolSpecification> toolSpecifications) throws IllegalArgumentException {

        // Checks for duplicates methods
        Set<String> names = new HashSet<>();
        for (ToolSpecification toolSpecification : toolSpecifications) {
            if (!names.add(toolSpecification.name())) {
                throw new IllegalArgumentException(String.format("Tool names must be unique. The tool '%s' appears several times", toolSpecification.name()));
            }
        }
    }

    /**
     * Returns the {@link ToolSpecification} for the given method annotated with @{@link Tool}.
     *
     * @param method the method.
     * @return the {@link ToolSpecification}.
     */
    public static ToolSpecification toolSpecificationFrom(Method method) {

        Tool annotation = method.getAnnotation(Tool.class);

        String name = isNullOrBlank(annotation.name()) ? method.getName() : annotation.name();

        String description = String.join("\n", annotation.description());
        if (description.isEmpty()) {
            description = null;
        }

        JsonObjectSchema parameters = parametersFrom(method.getParameters());

        return ToolSpecification.builder()
                .name(name)
                .description(description)
                .parameters(parameters)
                .build();
    }

    private static JsonObjectSchema parametersFrom(Parameter[] parameters) {

        Map<String, JsonSchemaElement> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        Map<Class<?>, JsonSchemaElementUtil.VisitedClassMetadata> visited = new LinkedHashMap<>();

        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(ToolMemoryId.class)) {
                continue;
            }

            boolean isRequired = Optional.ofNullable(parameter.getAnnotation(P.class))
                    .map(P::required)
                    .orElse(true);

            properties.put(parameter.getName(), jsonSchemaElementFrom(parameter, visited));
            if (isRequired) {
                required.add(parameter.getName());
            }
        }

        Map<String, JsonSchemaElement> definitions = new LinkedHashMap<>();
        visited.forEach((clazz, visitedClassMetadata) -> {
            if (visitedClassMetadata.recursionDetected) {
                definitions.put(visitedClassMetadata.reference, visitedClassMetadata.jsonSchemaElement);
            }
        });

        if (properties.isEmpty()) {
            return null;
        }

        return JsonObjectSchema.builder()
                .addProperties(properties)
                .required(required)
                .definitions(definitions.isEmpty() ? null : definitions)
                .build();
    }

    private static JsonSchemaElement jsonSchemaElementFrom(Parameter parameter,
                                                           Map<Class<?>, JsonSchemaElementUtil.VisitedClassMetadata> visited) {
        P annotation = parameter.getAnnotation(P.class);
        String description = annotation == null ? null : annotation.value();
        return JsonSchemaElementUtil.jsonSchemaElementFrom(
                parameter.getType(),
                parameter.getParameterizedType(),
                description,
                true,
                visited
        );
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
