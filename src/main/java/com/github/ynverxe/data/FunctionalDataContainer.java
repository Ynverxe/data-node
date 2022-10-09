package com.github.ynverxe.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("unchecked, rawtypes")
public interface FunctionalDataContainer<K> extends DataContainer<K> {

    default <E, T> @NotNull Optional<T> map(K key, Class<E> expected, Function<E, T> mappingFunction) {
        Object found = get(key);

        if (!expected.isInstance(found))
            return Optional.empty();

        return Optional.ofNullable(mappingFunction.apply((E) found));
    }

    default <E, T, X extends Exception> @NotNull T map(K key, Class<E> expected, Function<E, T> mappingFunction, Function<K, X> throwableFunction) throws X {
        Object found = get(key);

        if (!expected.isInstance(found))
            throw throwableFunction.apply(key);

        return mappingFunction.apply((E) found);
    }

    default <E, T, X extends Exception> @NotNull T mapOrThrow(K key, Class<E> expected, Function<E, T> mappingFunction, Function<String, X> throwableFunction) throws X {
        Object found = get(key);

        if (!expected.isInstance(found))
            throw throwableFunction.apply((found != null ? found.getClass() : null) + ", expected: " + expected + " (key=" + key + ")");

        return mappingFunction.apply((E) found);
    }

    default <E, T> @Nullable List<T> mapList(K key, Class<E> expectedElementType, Function<E, T> mappingFunction) {
        List list = getList(key);

        if (list == null)
            return null;

        List mappedElements = new ArrayList(list.size());

        for (Object o : list) {
            if (expectedElementType.isInstance(o)) {
                mappedElements.add(mappingFunction.apply((E) o));
            }
        }

        return mappedElements;
    }

    default <E> @Nullable List<E> listOf(K key, Class<E> expectedElementType) {
        return mapList(key, expectedElementType, e -> e);
    }

    default <E> @NotNull Optional<List<E>> optionalListOf(K key, Class<E> expectedElementType) {
        return Optional.ofNullable(listOf(key, expectedElementType));
    }

    default <T> @NotNull Optional<T> optionalGet(K key) {
        return Optional.ofNullable((T) get(key));
    }

    default <T> @NotNull Optional<T> optionalGet(K key, Class<T> expected) {
        Object found = get(key);

        if (expected.isInstance(found)) {
            return Optional.of((T) found);
        }

        return Optional.empty();
    }
}