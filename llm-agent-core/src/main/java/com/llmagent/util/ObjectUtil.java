package com.llmagent.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

public class ObjectUtil {

    public static <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static <T> T getOrDefault(T value, Supplier<T> defaultValueSupplier) {
        return value != null ? value : defaultValueSupplier.get();
    }

    /**
     * Is the given string {@code null} or empty ("")?
     * @param string The string to check.
     * @return true if the string is {@code null} or empty.
     */
    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /**
     * Is the collection {@code null} or empty?
     * @param collection The collection to check.
     * @return {@code true} if the collection is {@code null} or {@link Collection#isEmpty()}, otherwise {@code false}.
     */
    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Is the given string not {@code null} and not empty ("")?
     * @param string The string to check.
     * @return true if the given string is not {@code null} and not empty ("")?
     */
    public static boolean isNotNullOrEmpty(String string) {
        return !isNullOrEmpty(string);
    }

    /**
     * Returns an (unmodifiable) copy of the provided map.
     * Returns <code>null</code> if the provided map is <code>null</code>.
     *
     * @param map The map to copy.
     * @return The copy of the provided map.
     */
    public static <K,V> Map<K,V> copyIfNotNull(Map<K,V> map) {
        if (map == null) {
            return null;
        }

        return unmodifiableMap(map);
    }

    /**
     * Returns an (unmodifiable) copy of the provided list.
     * Returns <code>null</code> if the provided list is <code>null</code>.
     *
     * @param list The list to copy.
     * @param <T>  Generic type of the list.
     * @return The copy of the provided list.
     */
    public static <T> List<T> copyIfNotNull(List<T> list) {
        if (list == null) {
            return null;
        }

        return unmodifiableList(list);
    }

    public static boolean isJsonInteger(Class<?> type) {
        return type == byte.class
                || type == Byte.class
                || type == short.class
                || type == Short.class
                || type == int.class
                || type == Integer.class
                || type == long.class
                || type == Long.class
                || type == BigInteger.class;
    }

    public static boolean isJsonNumber(Class<?> type) {
        return type == float.class
                || type == Float.class
                || type == double.class
                || type == Double.class
                || type == BigDecimal.class;
    }

    public static boolean isJsonBoolean(Class<?> type) {
        return type == boolean.class || type == Boolean.class;
    }

    public static boolean isJsonString(Class<?> type) {
        return type == String.class
                || type == char.class
                || type == Character.class
                || CharSequence.class.isAssignableFrom(type)
                || type == UUID.class;
    }

    public static boolean isJsonArray(Class<?> type) {
        return type.isArray() || Iterable.class.isAssignableFrom(type);
    }

    /**
     * Generates a UUID from a hash of the given input string.
     * @param input The input string.
     * @return A UUID.
     */
    public static String generateUUIDFrom(String input) {
        byte[] hashBytes = getSha256Instance().digest(input.getBytes(UTF_8));
        String hexFormat = HexFormat.of().formatHex(hashBytes);
        return UUID.nameUUIDFromBytes(hexFormat.getBytes(UTF_8)).toString();
    }

    /**
     * Internal method to get an SHA-256 instance of {@link MessageDigest}.
     * @return a {@link MessageDigest}.
     */
    private static MessageDigest getSha256Instance() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
