package com.llmagent.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.*;

public class ObjectUtil {

    public static <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static <T> T getOrDefault(T value, Supplier<T> defaultValueSupplier) {
        return value != null ? value : defaultValueSupplier.get();
    }

    /**
     * Is the map object {@code null} or empty?
     * @param map The iterable object to check.
     * @return {@code true} if the map object is {@code null} or empty map, otherwise {@code false}.
     * */
    public static boolean isNullOrEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
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

    /**
     * Returns an (unmodifiable) copy of the provided set.
     * Returns <code>null</code> if the provided set is <code>null</code>.
     *
     * @param set The set to copy.
     * @param <T>  Generic type of the set.
     * @return The copy of the provided set.
     */
    public static <T> Set<T> copyIfNotNull(Set<T> set) {
        if (set == null) {
            return null;
        }

        return unmodifiableSet(set);
    }

    /**
     * Returns an (unmodifiable) copy of the provided list.
     * Returns an empty list if the provided list is <code>null</code>.
     *
     * @param list The list to copy.
     * @param <T>  Generic type of the list.
     * @return The copy of the provided list or an empty list.
     */
    public static <T> List<T> copy(List<T> list) {
        if (list == null) {
            return List.of();
        }

        return unmodifiableList(list);
    }

    /**
     * Returns an (unmodifiable) copy of the provided map.
     * Returns an empty map if the provided map is <code>null</code>.
     *
     * @param map The map to copy.
     * @return The copy of the provided map or an empty map.
     */
    public static <K,V> Map<K,V> copy(Map<K,V> map) {
        if (map == null) {
            return Map.of();
        }

        return unmodifiableMap(map);
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

    /**
     * Determine if the given objects are equal, returning {@code true} if
     * both are {@code null} or {@code false} if only one is {@code null}.
     * <p>Compares arrays with {@code Arrays.equals}, performing an equality
     * check based on the array elements rather than the array reference.
     * @param o1 first Object to compare
     * @param o2 second Object to compare
     * @return whether the given objects are equal
     * @see Object#equals(Object)
     * @see java.util.Arrays#equals
     */
    public static boolean nullSafeEquals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1.equals(o2)) {
            return true;
        }
        if (o1.getClass().isArray() && o2.getClass().isArray()) {
            return arrayEquals(o1, o2);
        }
        return false;
    }

    /**
     * Compare the given arrays with {@code Arrays.equals}, performing an equality
     * check based on the array elements rather than the array reference.
     * @param o1 first array to compare
     * @param o2 second array to compare
     * @return whether the given objects are equal
     * @see #nullSafeEquals(Object, Object)
     * @see java.util.Arrays#equals
     */
    private static boolean arrayEquals(Object o1, Object o2) {
        if (o1 instanceof Object[] objects1 && o2 instanceof Object[] objects2) {
            return Arrays.equals(objects1, objects2);
        }
        if (o1 instanceof boolean[] booleans1 && o2 instanceof boolean[] booleans2) {
            return Arrays.equals(booleans1, booleans2);
        }
        if (o1 instanceof byte[] bytes1 && o2 instanceof byte[] bytes2) {
            return Arrays.equals(bytes1, bytes2);
        }
        if (o1 instanceof char[] chars1 && o2 instanceof char[] chars2) {
            return Arrays.equals(chars1, chars2);
        }
        if (o1 instanceof double[] doubles1 && o2 instanceof double[] doubles2) {
            return Arrays.equals(doubles1, doubles2);
        }
        if (o1 instanceof float[] floats1 && o2 instanceof float[] floats2) {
            return Arrays.equals(floats1, floats2);
        }
        if (o1 instanceof int[] ints1 && o2 instanceof int[] ints2) {
            return Arrays.equals(ints1, ints2);
        }
        if (o1 instanceof long[] longs1 && o2 instanceof long[] longs2) {
            return Arrays.equals(longs1, longs2);
        }
        if (o1 instanceof short[] shorts1 && o2 instanceof short[] shorts2) {
            return Arrays.equals(shorts1, shorts2);
        }
        return false;
    }

    /**
     * Return a hash code for the given elements, delegating to
     * {@link #nullSafeHashCode(Object)} for each element. Contrary
     * to {@link Objects#hash(Object...)}, this method can handle an
     * element that is an array.
     * @param elements the elements to be hashed
     * @return a hash value of the elements
     * @since 6.1
     */
    public static int nullSafeHash(Object ... elements) {
        if (elements == null) {
            return 0;
        }
        int result = 1;
        for (Object element : elements) {
            result = 31 * result + nullSafeHashCode(element);
        }
        return result;
    }

    /**
     * Return a hash code for the given object, typically the value of
     * {@link Object#hashCode()}. If the object is an array, this method
     * will delegate to one of the {@code Arrays.hashCode} methods. If
     * the object is {@code null}, this method returns {@code 0}.
     * @see Object#hashCode()
     * @see Arrays
     */
    public static int nullSafeHashCode(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj.getClass().isArray()) {
            if (obj instanceof Object[] objects) {
                return Arrays.hashCode(objects);
            }
            if (obj instanceof boolean[] booleans) {
                return Arrays.hashCode(booleans);
            }
            if (obj instanceof byte[] bytes) {
                return Arrays.hashCode(bytes);
            }
            if (obj instanceof char[] chars) {
                return Arrays.hashCode(chars);
            }
            if (obj instanceof double[] doubles) {
                return Arrays.hashCode(doubles);
            }
            if (obj instanceof float[] floats) {
                return Arrays.hashCode(floats);
            }
            if (obj instanceof int[] ints) {
                return Arrays.hashCode(ints);
            }
            if (obj instanceof long[] longs) {
                return Arrays.hashCode(longs);
            }
            if (obj instanceof short[] shorts) {
                return Arrays.hashCode(shorts);
            }
        }
        return obj.hashCode();
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If {@code array} is {@code null}, this method returns 0.
     * @deprecated as of 6.1 in favor of {@link Arrays#hashCode(Object[])}
     */
    @Deprecated(since = "6.1")
    public static int nullSafeHashCode(Object [] array) {
        return Arrays.hashCode(array);
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If {@code array} is {@code null}, this method returns 0.
     * @deprecated as of 6.1 in favor of {@link Arrays#hashCode(boolean[])}
     */
    @Deprecated(since = "6.1")
    public static int nullSafeHashCode(boolean [] array) {
        return Arrays.hashCode(array);
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If {@code array} is {@code null}, this method returns 0.
     * @deprecated as of 6.1 in favor of {@link Arrays#hashCode(byte[])}
     */
    @Deprecated(since = "6.1")
    public static int nullSafeHashCode(byte [] array) {
        return Arrays.hashCode(array);
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If {@code array} is {@code null}, this method returns 0.
     * @deprecated as of 6.1 in favor of {@link Arrays#hashCode(char[])}
     */
    @Deprecated(since = "6.1")
    public static int nullSafeHashCode(char [] array) {
        return Arrays.hashCode(array);
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If {@code array} is {@code null}, this method returns 0.
     * @deprecated as of 6.1 in favor of {@link Arrays#hashCode(double[])}
     */
    @Deprecated(since = "6.1")
    public static int nullSafeHashCode(double [] array) {
        return Arrays.hashCode(array);
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If {@code array} is {@code null}, this method returns 0.
     * @deprecated as of 6.1 in favor of {@link Arrays#hashCode(float[])}
     */
    @Deprecated(since = "6.1")
    public static int nullSafeHashCode(float [] array) {
        return Arrays.hashCode(array);
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If {@code array} is {@code null}, this method returns 0.
     * @deprecated as of 6.1 in favor of {@link Arrays#hashCode(int[])}
     */
    @Deprecated(since = "6.1")
    public static int nullSafeHashCode(int [] array) {
        return Arrays.hashCode(array);
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If {@code array} is {@code null}, this method returns 0.
     * @deprecated as of 6.1 in favor of {@link Arrays#hashCode(long[])}
     */
    @Deprecated(since = "6.1")
    public static int nullSafeHashCode(long [] array) {
        return Arrays.hashCode(array);
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If {@code array} is {@code null}, this method returns 0.
     * @deprecated as of 6.1 in favor of {@link Arrays#hashCode(short[])}
     */
    @Deprecated(since = "6.1")
    public static int nullSafeHashCode(short [] array) {
        return Arrays.hashCode(array);
    }

}
