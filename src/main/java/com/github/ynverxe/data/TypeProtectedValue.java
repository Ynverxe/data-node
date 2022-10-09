package com.github.ynverxe.data;

class TypeProtectedValue {
    private final Class<?> requiredValueClass;
    private final boolean nullable;
    private Object value;

    public TypeProtectedValue(Class<?> requiredValueClass, boolean nullable, Object value) {
        this.requiredValueClass = requiredValueClass;
        this.nullable = nullable;
        this.value = value;
    }

    public boolean offer(Object value) {
        return value == null && nullable || requiredValueClass.isInstance(value);
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public TypeProtectedValue clone(Object newValue) {
        return new TypeProtectedValue(requiredValueClass, nullable, newValue);
    }
}