package com.llmagent.llm.tool;

import com.llmagent.util.StringUtil;

import java.util.*;

public class SchemaProperty {
    /**
     * A property with key "type" and value "string".
     */
    public static final SchemaProperty STRING = type("string");

    /**
     * A property with key "type" and value "integer".
     */
    public static final SchemaProperty INTEGER = type("integer");

    /**
     * A property with key "type" and value "number".
     */
    public static final SchemaProperty NUMBER = type("number");

    /**
     * A property with key "type" and value "object".
     */
    public static final SchemaProperty OBJECT = type("object");

    /**
     * A property with key "type" and value "array".
     */
    public static final SchemaProperty ARRAY = type("array");

    /**
     * A property with key "type" and value "boolean".
     */
    public static final SchemaProperty BOOLEAN = type("boolean");

    /**
     * A property with key "type" and value "null".
     */
    public static final SchemaProperty NULL = type("null");

    private final String key;
    private final Object value;

    /**
     * Construct a property with key and value.
     *
     * @param key   the key.
     * @param value the value.
     */
    public SchemaProperty(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Get the key.
     *
     * @return the key.
     */
    public String key() {
        return key;
    }

    /**
     * Get the value.
     *
     * @return the value.
     */
    public Object value() {
        return value;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof SchemaProperty
                && equalTo((SchemaProperty) another);
    }

    /**
     * Utility method to compare two {@link SchemaProperty} instances.
     *
     * @param another the other instance.
     * @return true if the two instances are equal.
     */
    private boolean equalTo(SchemaProperty another) {
        if (!Objects.equals(key, another.key)) return false;

        if (value instanceof Object[] && another.value instanceof Object[]) {
            return Arrays.equals((Object[]) value, (Object[]) another.value);
        }

        return Objects.equals(value, another.value);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(key);
        int v = (value instanceof Object[]) ? Arrays.hashCode((Object[]) value) : Objects.hashCode(value);
        h += (h << 5) + v;
        return h;
    }

    @Override
    public String toString() {
        String valueString = (value instanceof Object[]) ? Arrays.toString((Object[]) value) : value.toString();
        return "SchemaProperty {"
                + " key = " + StringUtil.quoted(key)
                + ", value = " + valueString
                + " }";
    }

    /**
     * Construct a property with key and value.
     *
     * <p>Equivalent to {@code new SchemaProperty(key, value)}.
     *
     * @param key   the key.
     * @param value the value.
     * @return a property with key and value.
     */
    public static SchemaProperty from(String key, Object value) {
        return new SchemaProperty(key, value);
    }

    /**
     * Construct a property with key and value.
     *
     * <p>Equivalent to {@code new SchemaProperty(key, value)}.
     *
     * @param key   the key.
     * @param value the value.
     * @return a property with key and value.
     */
    public static SchemaProperty property(String key, Object value) {
        return from(key, value);
    }

    /**
     * Construct a property with key "type" and value.
     *
     * <p>Equivalent to {@code new SchemaProperty("type", value)}.
     *
     * @param value the value.
     * @return a property with key and value.
     */
    public static SchemaProperty type(String value) {
        return from("type", value);
    }

    /**
     * Construct a property with key "description" and value.
     *
     * <p>Equivalent to {@code new SchemaProperty("description", value)}.
     *
     * @param value the value.
     * @return a property with key and value.
     */
    public static SchemaProperty description(String value) {
        return from("description", value);
    }

    /**
     * Construct a property with key "enum" and value enumValues.
     *
     * @param enumValues enum values as strings. For example: {@code enums("CELSIUS", "FAHRENHEIT")}
     * @return a property with key "enum" and value enumValues
     */
    public static SchemaProperty enums(String... enumValues) {
        return from("enum", enumValues);
    }

    /**
     * Construct a property with key "enum" and value enumValues.
     *
     * <p>Verifies that each value is a java class.
     *
     * @param enumValues enum values. For example: {@code enums(TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT)}
     * @return a property with key "enum" and value enumValues
     */
    public static SchemaProperty enums(Object... enumValues) {
        List<String> enumNames = new ArrayList<>();
        for (Object enumValue : enumValues) {
            if (!enumValue.getClass().isEnum()) {
                throw new RuntimeException("Value " + enumValue.getClass().getName() + " should be enum");
            }
            enumNames.add(((Enum<?>) enumValue).name());
        }
        return from("enum", enumNames);
    }

    /**
     * Construct a property with key "enum" and all enum values taken from enumClass.
     *
     * @param enumClass enum class. For example: {@code enums(TemperatureUnit.class)}
     * @return a property with key "enum" and values taken from enumClass
     */
    public static SchemaProperty enums(Class<?> enumClass) {
        if (!enumClass.isEnum()) {
            throw new RuntimeException("Class " + enumClass.getName() + " should be enum");
        }
        return enums((Object[]) enumClass.getEnumConstants());
    }

    /**
     * Wraps the given type in a property with key "items".
     *
     * @param type the type
     * @return a property with key "items" and value type.
     */
    public static SchemaProperty items(SchemaProperty type) {
        return from("items", Collections.singletonMap(type.key, type.value));
    }
}
