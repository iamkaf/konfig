package com.iamkaf.konfig.api.v1;

import java.util.function.Supplier;

/**
 * Runtime access to one typed config value.
 *
 * @param <T> the value type
 */
public interface ConfigValue<T> extends Supplier<T> {
    /**
     * Returns the dotted path for this value.
     *
     * @return the config path
     */
    String path();

    /**
     * Returns the default value.
     *
     * @return the default value
     */
    T defaultValue();

    /**
     * Returns the current value.
     *
     * @return the current value
     */
    T get();

    /**
     * Returns the local value without applying a synchronized server overlay.
     *
     * <p>Server-side consumers should use this when the same process also hosts a client,
     * since {@link #get()} intentionally exposes the effective synchronized value.</p>
     *
     * @return the local value
     */
    default T local() {
        return get();
    }

    /**
     * Sets the current value.
     *
     * @param value the new value
     */
    void set(T value);
}
