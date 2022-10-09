package com.github.ynverxe.data;

import com.github.ynverxe.data.util.PrimitiveTypeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * A functional node that contains data that can be serializable.
 */
@SuppressWarnings("unchecked, rawtypes, unused")
public class DataNode implements Cloneable, FunctionalDataContainer<String> {

    public static final DataNode EMPTY = new DataNode(new EmptyMap<>());
    private final static Object NONEXISTENT_PATH = new Object();

    private static final Function<Object, Object> SEARCH_RESULT_MAPPER = o -> {
        if (o == NONEXISTENT_PATH) return null;

        return o instanceof TypeProtectedValue ? ((TypeProtectedValue) o).getValue() : o;
    };
    private static final Function<Object, Boolean> SEARCH_RESULT_PREDICATE = o -> o != NONEXISTENT_PATH;
    private static final Function<Object, Object> LAZY_SEARCH_RESULT_MAPPER = o -> o != NONEXISTENT_PATH ? o : null;

    private final Map<String, Object> data;

    /**
     * Only for internal uses.
     * @param data - The map with the data
     */
    private DataNode(Map<String, Object> data) {
        this.data = data;
    }

    /**
     * Creates a new node.
     */
    public DataNode() {
        this(new HashMap<>());
    }

    /**
     * Clear the node content.
     *
     * Not all entries can be cleared, if the value is being held by
     * {@link TypeProtectedValue} and it not accept null values, the
     * value not will be cleared.
     *
     * Also, if the entry value is a {@link TypeProtectedValue}, the entry
     * will not be removed.
     *
     * @return A map with the entries that were cleared.
     */
    public @NotNull Map<String, Object> clear() {
        Map<String, Object> objectMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof TypeProtectedValue) {
                TypeProtectedValue typeProtectedValue = (TypeProtectedValue) value;

                if (!typeProtectedValue.offer(null)) continue;

                typeProtectedValue.setValue(null);
            } else {
                entry.setValue(null);
            }

            objectMap.put(entry.getKey(), value);
        }

        return objectMap;
    }

    /**
     * Puts a new value into the node.
     *
     * If any stored value with the same path, and it is held by
     * {@link TypeProtectedValue} and this not accepts values of the provided
     * value type, the operation will be cancelled.
     *
     * @param path - The value path
     * @param value - The value to store
     * @throws IllegalArgumentException - If the value is not serializable
     *
     * @return true, if the value has successfully added to the node, false if otherwise.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean put(@NotNull String path, @Nullable Object value) throws IllegalArgumentException {
        if (value instanceof Fragmented) {
            value = ((Fragmented) value).defragment();
        }

        Object realValue = value instanceof TypeProtectedValue ? ((TypeProtectedValue) value).getValue() : value;

        if (!isSerializable(realValue)) {
            throw new IllegalArgumentException(value + " is not apt for serialization");
        }

        Object found = valueSearch(path, LAZY_SEARCH_RESULT_MAPPER);

        if (found instanceof TypeProtectedValue) {
            if (!((TypeProtectedValue) found).offer(realValue)) return false;
        }

        return internalAdd(path, found, value);
    }

    /**
     * Finds a stored list with using a path and append values.
     *
     * @param path - The list path
     * @param values - The values to append
     * @return true if the values was correctly added to list, false otherwise.
     */
    @SuppressWarnings("rawtypes, unchecked")
    public boolean addToList(@NotNull String path, boolean storeIfAbsent, @Nullable Object... values) {
        if (values == null) values = new Object[0];

        List list = getList(path);

        if (storeIfAbsent && list == null) {
            list = new ArrayList();
        }

        if (list != null) {
            list.addAll(Arrays.asList(values));
            return put(path, list);
        }

        return false;
    }

    /**
     * Finds a stored list with using a path and remove values.
     *
     * @param path - The list path
     * @param values - The values to remove
     * @return true if the values was correctly removed from list, false otherwise.
     */
    @SuppressWarnings("rawtypes, unchecked")
    public boolean removeFromList(@NotNull String path, boolean storeIfAbsent, @NotNull Object... values) {
        if (values == null) values = new Object[0];

        List list = getList(path);

        if (storeIfAbsent && list == null) {
            list = new ArrayList();
        }

        if (list != null) {
            list.removeAll(Arrays.asList(values));
            return put(path, list);
        }

        return false;
    }

    @Override
    public String toString() {
        return data.toString();
    }

    /**
     * Clone this node.
     *
     * @return A new node with the data cloned from the current node.
     */
    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public DataNode clone() {
        Map<String, Object> objectMap = new HashMap<>();

        data.forEach((k, v) -> objectMap.put(k, tryNormalize(v)));

        return new DataNode(objectMap);
    }

    /**
     * Finds a stored value using the provided path.
     *
     * @param path - The value path
     * @return The value if exists, otherwise null.
     */
    @Override
    public @Nullable Object get(@NotNull String path) {
        Object found = valueSearch(path, SEARCH_RESULT_MAPPER);

        return found instanceof List ? Collections.unmodifiableList((List<?>) found) : found;
    }

    /**
     * Check if the provided path exists.
     *
     * @param path - The path
     * @return true if the entire path exists, false if not.
     */
    @Override
    public boolean has(@NotNull String path) {
        return valueSearch(path, SEARCH_RESULT_PREDICATE);
    }

    /**
     * Identify the type of any value in the current node.
     *
     * @param path - The value path
     * @param expectedType - The expected value type
     * @return true if the found value is an instance of the expected type.
     */
    @Override
    public boolean identify(String path, Class<?> expectedType) {
        return expectedType.isInstance(data.get(path));
    }

    /**
     * Consume another node.
     *
     * @param dataNode - The node that will be consumed.
     */
    public void consume(DataNode dataNode) {
        dataNode.data.forEach(this::put);
    }

    /**
     * Simplify the stored values of the current node, ideal for serialization.
     *
     * @return A map with the simplified values.
     */
    public Map<String, Object> simplify() {
        Map<String, Object> objectMap = new HashMap<>();

        data.forEach((k, v) -> objectMap.put(k, trySimplify(v)));

        return objectMap;
    }

    /**
     * Handles the internal value add event.
     *
     * @param path - The value path
     * @param previous - The previous value associated with the path
     * @param value    - The mutated value
     */
    protected void handleInternalValueAdd(String path, Object previous, Object value) {}

    /**
     * Protects a value to guarantee that the value of the specified path
     * is always of a specific type or never null.
     *
     * @param path - The value path
     * @param clazz - The value specify class
     * @param value - The value
     * @param nullable - Define if the value can be null
     * @param <S> - The value type
     * @param <T> - The children type
     */
    protected final <S, T extends S> void protectValue(String path, Class<S> clazz, T value, boolean nullable) {
        put(path, new TypeProtectedValue(clazz, nullable, value));
    }

    private <T> T valueSearch(String path, Function<Object, T> mapper) {
        int i = path.indexOf(".");

        if (i == -1) {
            Object found = data.get(path);

            return mapper.apply(found);
        } else {
            checkPath(i, path);

            String nodeKey = path.substring(0, i);
            String restOfPath = path.substring(i + 1);

            Object found = get(nodeKey);

            if (found instanceof DataNode) {
                return ((DataNode) found).valueSearch(restOfPath, mapper);
            }
        }

        return mapper.apply(NONEXISTENT_PATH);
    }

    private boolean performValueAdd(String key, Object previous, Object value) {
        if (previous instanceof TypeProtectedValue) {
            TypeProtectedValue typeProtectedValue = (TypeProtectedValue) previous;

            if (!typeProtectedValue.offer(value)) return false;

            typeProtectedValue.setValue(value);
        } else {
            data.put(key, value);
        }

        handleInternalValueAdd(key, previous, value);
        return true;
    }

    private boolean internalAdd(
            String key,
            Object previous,
            Object object
    ) {
        if ("".equals(key))
            throw new IllegalArgumentException("Empty key");

        object = tryNormalize(object);

        int i = key.indexOf(".");

        DataNode dataNode = this;

        if (i != -1) {
            checkPath(i, key);

            while ((i = key.indexOf(".")) != -1) {
                String nodePath = key.substring(0, i);
                key = key.substring(i + 1);

                Object found = dataNode.valueSearch(nodePath, LAZY_SEARCH_RESULT_MAPPER);

                if (found instanceof DataNode) {
                    dataNode = (DataNode) found;
                } else {
                    DataNode newNode = new DataNode();

                    if (!dataNode.performValueAdd(nodePath, found, newNode)) {
                        return false;
                    }

                    dataNode = newNode;
                }
            }

            if (dataNode == this)
                return false;
        }

        return dataNode.performValueAdd(key, previous, object);
    }

    private static void checkPath(int j, String key) {
        if (j == 0 || j + 1 >= key.length())
            throw new IllegalArgumentException("Malformed key path");
    }

    private static Object trySimplify(Object obj) {
        if (obj instanceof Collection) {
            List list = new ArrayList();

            for (Object o : (Collection) obj) {
                list.add(trySimplify(o));
            }

            return list;
        } else if (obj instanceof DataNode) {
            return ((DataNode) obj).simplify();
        } else if (obj instanceof TypeProtectedValue) {
            return trySimplify(((TypeProtectedValue) obj).getValue());
        }

        return obj;
    }

    private static Object tryNormalize(Object obj) {
        if (obj instanceof Collection) {
            List list = new ArrayList();

            for (Object o : (Collection) obj) {
                if (!isSerializable(o)) {
                    throw new IllegalStateException(o + " is not a serializable type");
                }

                list.add(tryNormalize(o));
            }

            return list;
        } else if (obj instanceof Map) {
            return fromMap((Map<String, ?>) obj);
        } else if (obj instanceof DataNode) {
            return ((DataNode) obj).clone();
        } else if (obj instanceof TypeProtectedValue) {
            TypeProtectedValue typeProtectedValue = (TypeProtectedValue) obj;
            Object realValue = typeProtectedValue.getValue();

            return typeProtectedValue.clone(tryNormalize(realValue));
        }

        return obj;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isSerializable(Object o) {
        if (o instanceof Collection) {
            for (Object element : (Collection<?>) o) {
                if (!isSerializable(element)) return false;
            }
        } else if (o instanceof Map) {
            for (Map.Entry<String, ?> entry : ((Map<String, ?>) o).entrySet()) {
                if (!isSerializable(entry.getValue())) return false;
            }
        } else {
            return o instanceof DataNode || PrimitiveTypeUtil.isSerializable(o);
        }

        return true;
    }

    /**
     * @param map - The data map
     * @return a new node that contains the provided data.
     */
    public static @NotNull DataNode fromMap(Map<String, ?> map) {
        DataNode dataNode = new DataNode();
        map.forEach(dataNode::put);

        return dataNode;
    }
}