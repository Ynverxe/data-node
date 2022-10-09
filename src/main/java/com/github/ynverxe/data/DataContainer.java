package com.github.ynverxe.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("ConstantConditions, unchecked")
public interface DataContainer<K> {

    @Nullable Object get(K key);

    boolean has(K key);

    boolean identify(K key, Class<?> expectedType);

    default @Nullable DataNode getNode(K key) {
        Object found = get(key);

        return (DataNode) found;
    }

    default <T> @Nullable List<T> getList(K key) {
        Object found = get(key);

        if (found instanceof List) {
            return (List<T>) found;
        }

        return null;
    }

    default @Nullable Number getNumber(K key, Number def) {
        Object found = get(key);

        if (found != null) {
            return (Number) found;
        }

        return def;
    }

    default @Nullable Number getNumber(K key) {
        return getNumber(key, null);
    }

    default @Nullable String getString(K key, String def) {
        Object found = get(key);

        if (found != null) {
            return (String) found;
        }

        return def;
    }

    default @Nullable String getString(K key) {
        return getString(key, null);
    }

    default @Nullable Boolean getBoolean(K key, Boolean def) {
        Object found = get(key);

        if (found != null) {
            return (Boolean) found;
        }

        return def;
    }

    default @NotNull Boolean getBoolean(K key) {
        return getBoolean(key, false);
    }

    default @NotNull Integer getInt(K key, Integer def) {
        return getNumber(key, def).intValue();
    }

    default @NotNull Integer getInt(K key) {
        return getInt(key, 0);
    }

    default @NotNull Double getDouble(K key, Double def) {
        return getNumber(key, def).doubleValue();
    }

    default @NotNull Double getDouble(K key) {
        return getDouble(key, 0.0);
    }

    default @NotNull Float getFloat(K key, Float def) {
        return getNumber(key, def).floatValue();
    }

    default @NotNull Float getFloat(K key) {
        return getFloat(key, 0f);
    }

    default @NotNull Long getLong(K key, Long def) {
        return getNumber(key, def).longValue();
    }

    default @NotNull Long getLong(K key) {
        return getLong(key, (long) 0);
    }

    default @NotNull Byte getByte(K key, Byte def) {
        return getNumber(key, def).byteValue();
    }

    default @NotNull Byte getByte(K key) {
        return getByte(key, (byte) 0);
    }
}