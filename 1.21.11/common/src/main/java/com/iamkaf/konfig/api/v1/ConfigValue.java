package com.iamkaf.konfig.api.v1;

import java.util.function.Supplier;

public interface ConfigValue<T> extends Supplier<T> {
    String path();

    T defaultValue();

    T get();

    void set(T value);
}
