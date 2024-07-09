package com.llmagent.data;

import java.io.Serializable;
import java.util.*;

import com.llmagent.vector.store.EmbeddingStore;

import static com.llmagent.exception.Exceptions.illegalArgument;
import static com.llmagent.exception.Exceptions.runtime;

public class Metadata implements Serializable {

    private static final Set<Class<?>> SUPPORTED_VALUE_TYPES = new LinkedHashSet<>();

    static {
        SUPPORTED_VALUE_TYPES.add(String.class);

        SUPPORTED_VALUE_TYPES.add(int.class);
        SUPPORTED_VALUE_TYPES.add(Integer.class);

        SUPPORTED_VALUE_TYPES.add(long.class);
        SUPPORTED_VALUE_TYPES.add(Long.class);

        SUPPORTED_VALUE_TYPES.add(float.class);
        SUPPORTED_VALUE_TYPES.add(Float.class);

        SUPPORTED_VALUE_TYPES.add(double.class);
        SUPPORTED_VALUE_TYPES.add(Double.class);
    }

    private final Map<String, Object> metadata;

    /**
     * Construct a Metadata object with an empty map of key-value pairs.
     */
    public Metadata() {
        this(new HashMap<>());
    }

    /**
     * Constructs a Metadata object from a map of key-value pairs.
     *
     * @param metadata the map of key-value pairs; must not be {@code null}. {@code null} values are not permitted.
     *                 Supported value types: {@link String}, {@link Integer}, {@link Long}, {@link Float}, {@link Double}
     */
    public Metadata(Map<String, ?> metadata) {
        metadata.forEach((key, value) -> {
            if (!SUPPORTED_VALUE_TYPES.contains(value.getClass())) {
                throw illegalArgument("The metadata key '%s' has the value '%s', which is of the unsupported type '%s'. " +
                                "Currently, the supported types are: %s",
                        key, value, value.getClass().getName(), SUPPORTED_VALUE_TYPES
                );
            }
        });
        this.metadata = new HashMap<>(metadata);
    }

    /**
     * Returns the value associated with the given key.
     *
     * @param key the key
     * @return the value associated with the given key, or {@code null} if the key is not present.
     * @deprecated as of 0.31.0, use {@link #getString(String)}, {@link #getInteger(String)}, {@link #getLong(String)},
     * {@link #getFloat(String)}, {@link #getDouble(String)} instead.
     */
    @Deprecated
    public String get(String key) {
        Object value = metadata.get(key);
        if (value != null) {
            return value.toString();
        } else {
            return null;
        }
    }

    /**
     * Returns the {@code String} value associated with the given key.
     *
     * @param key the key
     * @return the {@code String} value associated with the given key, or {@code null} if the key is not present.
     * @throws RuntimeException if the value is not of type String
     */
    public String getString(String key) {
        if (!containsKey(key)) {
            return null;
        }

        Object value = metadata.get(key);
        if (value instanceof String) {
            return (String) value;
        }

        throw runtime("Metadata entry with the key '%s' has a value of '%s' and type '%s'. " +
                "It cannot be returned as a String.", key, value, value.getClass().getName());
    }

    /**
     * Returns the {@code Integer} value associated with the given key.
     * <br>
     * Some {@link EmbeddingStore} implementations (still) store {@code Metadata} values as {@code String}s.
     * In this case, the {@code String} value will be parsed into an {@code Integer} when this method is called.
     * <br>
     * Some {@link EmbeddingStore} implementations store {@code Metadata} key-value pairs as JSON.
     * In this case, type information is lost when serializing to JSON and then deserializing back from JSON.
     * JSON libraries can, for example, serialize an {@code Integer} and then deserialize it as a {@code Long}.
     * Or serialize a {@code Float} and then deserialize it as a {@code Double}, and so on.
     * In such cases, the actual value will be cast to an {@code Integer} when this method is called.
     *
     * @param key the key
     * @return the {@link Integer} value associated with the given key, or {@code null} if the key is not present.
     * @throws RuntimeException if the value is not {@link Number}
     */
    public Integer getInteger(String key) {
        if (!containsKey(key)) {
            return null;
        }

        Object value = metadata.get(key);
        if (value instanceof String) {
            return Integer.parseInt(value.toString());
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        throw runtime("Metadata entry with the key '%s' has a value of '%s' and type '%s'. " +
                "It cannot be returned as an Integer.", key, value, value.getClass().getName());
    }

    /**
     * Returns the {@code Long} value associated with the given key.
     * <br>
     * Some {@link EmbeddingStore} implementations (still) store {@code Metadata} values as {@code String}s.
     * In this case, the {@code String} value will be parsed into an {@code Long} when this method is called.
     * <br>
     * Some {@link EmbeddingStore} implementations store {@code Metadata} key-value pairs as JSON.
     * In this case, type information is lost when serializing to JSON and then deserializing back from JSON.
     * JSON libraries can, for example, serialize an {@code Integer} and then deserialize it as a {@code Long}.
     * Or serialize a {@code Float} and then deserialize it as a {@code Double}, and so on.
     * In such cases, the actual value will be cast to a {@code Long} when this method is called.
     *
     * @param key the key
     * @return the {@code Long} value associated with the given key, or {@code null} if the key is not present.
     * @throws RuntimeException if the value is not {@link Number}
     */
    public Long getLong(String key) {
        if (!containsKey(key)) {
            return null;
        }

        Object value = metadata.get(key);
        if (value instanceof String) {
            return Long.parseLong(value.toString());
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        throw runtime("Metadata entry with the key '%s' has a value of '%s' and type '%s'. " +
                "It cannot be returned as a Long.", key, value, value.getClass().getName());
    }

    /**
     * Returns the {@code Float} value associated with the given key.
     * <br>
     * Some {@link EmbeddingStore} implementations (still) store {@code Metadata} values as {@code String}s.
     * In this case, the {@code String} value will be parsed into a {@code Float} when this method is called.
     * <br>
     * Some {@link EmbeddingStore} implementations store {@code Metadata} key-value pairs as JSON.
     * In this case, type information is lost when serializing to JSON and then deserializing back from JSON.
     * JSON libraries can, for example, serialize an {@code Integer} and then deserialize it as a {@code Long}.
     * Or serialize a {@code Float} and then deserialize it as a {@code Double}, and so on.
     * In such cases, the actual value will be cast to a {@code Float} when this method is called.
     *
     * @param key the key
     * @return the {@code Float} value associated with the given key, or {@code null} if the key is not present.
     * @throws RuntimeException if the value is not {@link Number}
     */
    public Float getFloat(String key) {
        if (!containsKey(key)) {
            return null;
        }

        Object value = metadata.get(key);
        if (value instanceof String) {
            return Float.parseFloat(value.toString());
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        }

        throw runtime("Metadata entry with the key '%s' has a value of '%s' and type '%s'. " +
                "It cannot be returned as a Float.", key, value, value.getClass().getName());
    }

    /**
     * Returns the {@code Double} value associated with the given key.
     * <br>
     * Some {@link EmbeddingStore} implementations (still) store {@code Metadata} values as {@code String}s.
     * In this case, the {@code String} value will be parsed into a {@code Double} when this method is called.
     * <br>
     * Some {@link EmbeddingStore} implementations store {@code Metadata} key-value pairs as JSON.
     * In this case, type information is lost when serializing to JSON and then deserializing back from JSON.
     * JSON libraries can, for example, serialize an {@code Integer} and then deserialize it as a {@code Long}.
     * Or serialize a {@code Float} and then deserialize it as a {@code Double}, and so on.
     * In such cases, the actual value will be cast to a {@code Double} when this method is called.
     *
     * @param key the key
     * @return the {@code Double} value associated with the given key, or {@code null} if the key is not present.
     * @throws RuntimeException if the value is not {@link Number}
     */
    public Double getDouble(String key) {
        if (!containsKey(key)) {
            return null;
        }

        Object value = metadata.get(key);
        if (value instanceof String) {
            return Double.parseDouble(value.toString());
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        throw runtime("Metadata entry with the key '%s' has a value of '%s' and type '%s'. " +
                "It cannot be returned as a Double.", key, value, value.getClass().getName());
    }

    /**
     * Check whether this {@code Metadata} contains a given key.
     *
     * @param key the key
     * @return {@code true} if this metadata contains a given key; {@code false} otherwise.
     */
    public boolean containsKey(String key) {
        return metadata.containsKey(key);
    }

    /**
     * Adds a key-value pair to the metadata.
     *
     * @param key   the key
     * @param value the value
     * @return {@code this}
     * @deprecated as of 0.31.0, use {@link #put(String, String)}, {@link #put(String, int)}, {@link #put(String, long)},
     * {@link #put(String, float)}, {@link #put(String, double)} instead.
     */
    @Deprecated
    public Metadata add(String key, Object value) {
        return put(key, value.toString());
    }

    /**
     * Adds a key-value pair to the metadata.
     *
     * @param key   the key
     * @param value the value
     * @return {@code this}
     * @deprecated as of 0.31.0, use {@link #put(String, String)}, {@link #put(String, int)}, {@link #put(String, long)},
     * {@link #put(String, float)}, {@link #put(String, double)} instead.
     */
    @Deprecated
    public Metadata add(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Adds a key-value pair to the metadata.
     *
     * @param key   the key
     * @param value the value
     * @return {@code this}
     */
    public Metadata put(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Adds a key-value pair to the metadata.
     *
     * @param key   the key
     * @param value the value
     * @return {@code this}
     */
    public Metadata put(String key, int value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Adds a key-value pair to the metadata.
     *
     * @param key   the key
     * @param value the value
     * @return {@code this}
     */
    public Metadata put(String key, long value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Adds a key-value pair to the metadata.
     *
     * @param key   the key
     * @param value the value
     * @return {@code this}
     */
    public Metadata put(String key, float value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Adds a key-value pair to the metadata.
     *
     * @param key   the key
     * @param value the value
     * @return {@code this}
     */
    public Metadata put(String key, double value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Removes the given key from the metadata.
     *
     * @param key the key
     * @return {@code this}
     */
    public Metadata remove(String key) {
        this.metadata.remove(key);
        return this;
    }

    /**
     * Copies the metadata.
     *
     * @return a copy of this Metadata object.
     */
    public Metadata copy() {
        return new Metadata(metadata);
    }

    /**
     * Get a copy of the metadata as a map of key-value pairs.
     *
     * @return the metadata as a map of key-value pairs.
     */
    public Map<String, Object> toMap() {
        return new HashMap<>(metadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metadata that = (Metadata) o;
        return Objects.equals(this.metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata);
    }

    @Override
    public String toString() {
        return "Metadata {" +
                " metadata = " + metadata +
                " }";
    }

    /**
     * Constructs a Metadata object from a single key-value pair.
     *
     * @param key   the key
     * @param value the value
     * @return a Metadata object
     */
    public static Metadata from(String key, String value) {
        return new Metadata().put(key, value);
    }

    /**
     * @param key   the key
     * @param value the value
     * @return a Metadata object
     * @deprecated Use {@link #from(String, String)} instead
     */
    @Deprecated
    public static Metadata from(String key, Object value) {
        return new Metadata().add(key, value);
    }

    /**
     * Constructs a Metadata object from a map of key-value pairs.
     *
     * @param metadata the map of key-value pairs
     * @return a Metadata object
     */
    public static Metadata from(Map<String, ?> metadata) {
        return new Metadata(metadata);
    }

    /**
     * Constructs a Metadata object from a single key-value pair.
     *
     * @param key   the key
     * @param value the value
     * @return a Metadata object
     */
    public static Metadata metadata(String key, String value) {
        return from(key, value);
    }

    /**
     * @param key   the key
     * @param value the value
     * @return a Metadata object
     * @deprecated Use {@link #metadata(String, String)} instead
     */
    @Deprecated
    public static Metadata metadata(String key, Object value) {
        return from(key, value);
    }
}
