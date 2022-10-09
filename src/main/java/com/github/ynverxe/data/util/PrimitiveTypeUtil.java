package com.github.ynverxe.data.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("rawtypes")
public final class PrimitiveTypeUtil {

    public static final List<Class> WRAPPER_CLASSES = Collections.unmodifiableList(wrapperClasses());

    private static List<Class> wrapperClasses() {
        return Arrays.asList(
                Boolean.class,
                Character.class,
                Byte.class,
                Short.class,
                Integer.class,
                Long.class,
                Float.class,
                Double.class
        );
    }

    public static boolean isWrapper(Object obj) {
        if (obj == null) {
            return true;
        }

        return WRAPPER_CLASSES.contains(obj.getClass());
    }

    public static boolean isSerializable(Object obj) {
        return obj == null || obj.getClass().isPrimitive() || isWrapper(obj) || String.class == obj.getClass();
    }
}