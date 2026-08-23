//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.state;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.value.ValueSemantics;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ApiStatus.Internal
public final class ConfigSessionField<T> {
    private final String id;
    private final T defaultValue;
    private final Supplier<T> storedValue;
    private final Supplier<T> effectiveValue;
    private final Consumer<T> writer;
    private final ValueSemantics<T> semantics;
    private final Supplier<ConfigPermission> permission;

    public ConfigSessionField(
            String id,
            T defaultValue,
            Supplier<T> storedValue,
            Supplier<T> effectiveValue,
            Consumer<T> writer,
            ValueSemantics<T> semantics,
            Supplier<ConfigPermission> permission
    ) {
        this.id = requireText(id, "id");
        this.semantics = Objects.requireNonNull(semantics, "semantics");
        T normalizedDefault = semantics.normalize(Objects.requireNonNull(defaultValue, "defaultValue"));
        ConfigValidation defaultValidation = semantics.validate(this.id, normalizedDefault);
        if (defaultValidation.hasErrors()) {
            throw new IllegalArgumentException("Invalid default value for config field '" + this.id + "': " + defaultValidation.issues());
        }
        this.defaultValue = semantics.copy(normalizedDefault);
        this.storedValue = Objects.requireNonNull(storedValue, "storedValue");
        this.effectiveValue = Objects.requireNonNull(effectiveValue, "effectiveValue");
        this.writer = Objects.requireNonNull(writer, "writer");
        this.permission = Objects.requireNonNull(permission, "permission");
    }

    public static <T> ConfigSessionField<T> local(
            String id,
            T defaultValue,
            Supplier<T> value,
            Consumer<T> writer,
            ValueSemantics<T> semantics
    ) {
        return new ConfigSessionField<>(id, defaultValue, value, value, writer, semantics, ConfigPermission::editablePermission);
    }

    public String id() {
        return this.id;
    }

    public T defaultValue() {
        return copy(this.defaultValue);
    }

    public T storedValue() {
        return copy(requireValue(this.storedValue.get(), "stored value"));
    }

    public T effectiveValue() {
        return copy(requireValue(this.effectiveValue.get(), "effective value"));
    }

    public ValueSemantics<T> semantics() {
        return this.semantics;
    }

    public ConfigPermission permission() {
        return Objects.requireNonNull(this.permission.get(), "permission result");
    }

    T copy(T value) {
        return this.semantics.copy(value);
    }

    void write(T value) {
        this.writer.accept(copy(value));
    }

    private T requireValue(T value, String role) {
        return Objects.requireNonNull(value, role + " for '" + this.id + "'");
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return normalized;
    }
}
//?}
