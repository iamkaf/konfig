package com.iamkaf.konfig.impl.v1;

import com.google.gson.JsonElement;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.RestartRequirement;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

final class ConfigValueImpl<T> implements ConfigValue<T> {
    private final String path;
    private final T defaultValue;
    private final EntryKind kind;
    private final Function<JsonElement, T> decoder;
    private final Function<T, JsonElement> encoder;
    private final Predicate<T> validator;
    private final String validationMessage;
    private final boolean sync;
    private final boolean clientOnly;
    private final boolean serverOnly;
    private final RestartRequirement restartRequirement;

    private volatile T localValue;
    private volatile T syncedValue;

    ConfigValueImpl(
            String path,
            T defaultValue,
            EntryKind kind,
            Function<JsonElement, T> decoder,
            Function<T, JsonElement> encoder,
            Predicate<T> validator,
            String validationMessage,
            boolean sync,
            boolean clientOnly,
            boolean serverOnly,
            RestartRequirement restartRequirement
    ) {
        this.path = path;
        this.defaultValue = defaultValue;
        this.kind = kind;
        this.decoder = decoder;
        this.encoder = encoder;
        this.validator = validator;
        this.validationMessage = validationMessage;
        this.sync = sync;
        this.clientOnly = clientOnly;
        this.serverOnly = serverOnly;
        this.restartRequirement = restartRequirement;
        this.localValue = defaultValue;
    }

    @Override
    public String path() {
        return this.path;
    }

    @Override
    public T defaultValue() {
        return this.defaultValue;
    }

    @Override
    public T get() {
        T overlay = this.syncedValue;
        return overlay != null ? overlay : this.localValue;
    }

    @Override
    public void set(T value) {
        setLocal(value);
    }

    void setLocal(T value) {
        T checked = validateOrThrow(value);
        this.localValue = checked;
    }

    void setSynced(T value) {
        this.syncedValue = validateOrFallback(value);
    }

    void clearSynced() {
        this.syncedValue = null;
    }

    T decodeOrFallback(JsonElement element) {
        try {
            if (element == null) {
                return this.defaultValue;
            }
            T decoded = this.decoder.apply(element);
            return validateOrFallback(decoded);
        } catch (Exception ignored) {
            return this.defaultValue;
        }
    }

    JsonElement encodeCurrent() {
        return this.encoder.apply(this.localValue);
    }

    boolean sync() {
        return this.sync;
    }

    boolean clientOnly() {
        return this.clientOnly;
    }

    boolean serverOnly() {
        return this.serverOnly;
    }

    RestartRequirement restartRequirement() {
        return this.restartRequirement;
    }

    EntryKind kind() {
        return this.kind;
    }

    private T validateOrFallback(T value) {
        if (value == null) {
            return this.defaultValue;
        }
        return this.validator.test(value) ? value : this.defaultValue;
    }

    private T validateOrThrow(T value) {
        Objects.requireNonNull(value, "Config value cannot be null for " + this.path);
        if (!this.validator.test(value)) {
            throw new IllegalArgumentException(this.validationMessage + " (" + this.path + ")");
        }
        return value;
    }
}
