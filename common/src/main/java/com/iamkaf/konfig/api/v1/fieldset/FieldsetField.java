//? if >=1.21.11 {
package com.iamkaf.konfig.api.v1.fieldset;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A typed scalar declaration shared by every entry in a fieldset.
 *
 * @param <T> the Java value type
 */
@ApiStatus.Experimental
public final class FieldsetField<T> {
    private final String key;
    private final FieldsetFieldKind kind;
    private final T defaultValue;
    private final Class<?> valueType;
    private final Number minimum;
    private final Number maximum;
    private final List<String> options;
    private final ResourceKey<? extends Registry<?>> registryKey;
    private final List<ValidationRule<T>> validationRules;

    private FieldsetField(
            String key,
            FieldsetFieldKind kind,
            T defaultValue,
            Class<?> valueType,
            Number minimum,
            Number maximum,
            List<String> options,
            ResourceKey<? extends Registry<?>> registryKey,
            List<ValidationRule<T>> validationRules
    ) {
        this.key = requireKey(key);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.minimum = minimum;
        this.maximum = maximum;
        this.options = Collections.unmodifiableList(new ArrayList<String>(options));
        this.registryKey = registryKey;
        this.validationRules = Collections.unmodifiableList(new ArrayList<ValidationRule<T>>(validationRules));
        requireValueType(defaultValue);
    }

    public static FieldsetField<Boolean> bool(String key, boolean defaultValue) {
        return scalar(key, FieldsetFieldKind.BOOLEAN, Boolean.valueOf(defaultValue), Boolean.class);
    }

    public static FieldsetField<Integer> intRange(String key, int defaultValue, int minimum, int maximum) {
        if (minimum > maximum || defaultValue < minimum || defaultValue > maximum) {
            throw new IllegalArgumentException("Invalid integer range or default for field " + key);
        }
        return scalar(key, FieldsetFieldKind.INTEGER, Integer.valueOf(defaultValue), Integer.class)
                .withRange(Integer.valueOf(minimum), Integer.valueOf(maximum));
    }

    public static FieldsetField<Long> longRange(String key, long defaultValue, long minimum, long maximum) {
        if (minimum > maximum || defaultValue < minimum || defaultValue > maximum) {
            throw new IllegalArgumentException("Invalid long range or default for field " + key);
        }
        return scalar(key, FieldsetFieldKind.LONG, Long.valueOf(defaultValue), Long.class)
                .withRange(Long.valueOf(minimum), Long.valueOf(maximum));
    }

    public static FieldsetField<Double> doubleRange(String key, double defaultValue, double minimum, double maximum) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || !Double.isFinite(defaultValue)
                || minimum > maximum || defaultValue < minimum || defaultValue > maximum) {
            throw new IllegalArgumentException("Invalid double range or default for field " + key);
        }
        return scalar(key, FieldsetFieldKind.DOUBLE, Double.valueOf(defaultValue), Double.class)
                .withRange(Double.valueOf(minimum), Double.valueOf(maximum));
    }

    public static FieldsetField<String> string(String key, String defaultValue) {
        return scalar(key, FieldsetFieldKind.STRING, Objects.requireNonNull(defaultValue, "defaultValue"), String.class);
    }

    public static FieldsetField<Optional<String>> optionalString(String key) {
        return optionalString(key, Optional.empty());
    }

    public static FieldsetField<Optional<String>> optionalString(String key, Optional<String> defaultValue) {
        Objects.requireNonNull(defaultValue, "defaultValue");
        if (defaultValue.isPresent()) {
            Objects.requireNonNull(defaultValue.get(), "defaultValue value");
        }
        return new FieldsetField<Optional<String>>(
                key,
                FieldsetFieldKind.OPTIONAL_STRING,
                defaultValue,
                Optional.class,
                null,
                null,
                Collections.emptyList(),
                null,
                Collections.emptyList()
        );
    }

    public static FieldsetField<String> dropdown(String key, String defaultValue, List<String> options) {
        Objects.requireNonNull(defaultValue, "defaultValue");
        Objects.requireNonNull(options, "options");
        ArrayList<String> normalized = new ArrayList<String>();
        for (String option : options) {
            String value = requireNonBlank(option, "dropdown option");
            if (normalized.contains(value)) {
                throw new IllegalArgumentException("Duplicate dropdown option for field " + key + ": " + value);
            }
            normalized.add(value);
        }
        if (normalized.isEmpty() || !normalized.contains(defaultValue)) {
            throw new IllegalArgumentException("Dropdown default must be one of the options for field " + key);
        }
        FieldsetField<String> field = new FieldsetField<String>(
                key,
                FieldsetFieldKind.DROPDOWN,
                defaultValue,
                String.class,
                null,
                null,
                normalized,
                null,
                Collections.emptyList()
        );
        return field.validate(normalized::contains, "Must be one of: " + String.join(", ", normalized));
    }

    public static FieldsetField<String> registryString(
            String key,
            String defaultValue,
            ResourceKey<? extends Registry<?>> registryKey
    ) {
        return new FieldsetField<String>(
                key,
                FieldsetFieldKind.REGISTRY_STRING,
                Objects.requireNonNull(defaultValue, "defaultValue"),
                String.class,
                null,
                null,
                Collections.emptyList(),
                Objects.requireNonNull(registryKey, "registryKey"),
                Collections.emptyList()
        );
    }

    /**
     * Adds a semantic validation rule to this declaration.
     *
     * @param validator predicate that accepts valid values
     * @param message error shown when the predicate rejects a value
     * @return a new field declaration
     */
    public FieldsetField<T> validate(Predicate<T> validator, String message) {
        Objects.requireNonNull(validator, "validator");
        ArrayList<ValidationRule<T>> rules = new ArrayList<ValidationRule<T>>(this.validationRules);
        rules.add(new ValidationRule<T>(validator, requireNonBlank(message, "validation message")));
        return copy(this.minimum, this.maximum, rules);
    }

    public String key() {
        return this.key;
    }

    public FieldsetFieldKind kind() {
        return this.kind;
    }

    public T defaultValue() {
        return this.defaultValue;
    }

    public Optional<Number> minimum() {
        return Optional.ofNullable(this.minimum);
    }

    public Optional<Number> maximum() {
        return Optional.ofNullable(this.maximum);
    }

    public List<String> options() {
        return this.options;
    }

    public Optional<ResourceKey<? extends Registry<?>>> registryKey() {
        return Optional.ofNullable(this.registryKey);
    }

    public List<String> validationMessages(T value) {
        requireValueType(value);
        ArrayList<String> messages = new ArrayList<String>();
        for (ValidationRule<T> rule : this.validationRules) {
            if (!rule.validator.test(value)) {
                messages.add(rule.message);
            }
        }
        return Collections.unmodifiableList(messages);
    }

    void requireValueType(Object value) {
        Objects.requireNonNull(value, "value");
        if (!this.valueType.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Field " + this.key + " expects " + this.valueType.getSimpleName() + ", got " + value.getClass().getSimpleName()
            );
        }
        if (this.kind == FieldsetFieldKind.OPTIONAL_STRING) {
            Optional<?> optional = (Optional<?>) value;
            if (optional.isPresent() && !(optional.get() instanceof String)) {
                throw new IllegalArgumentException("Field " + this.key + " expects Optional<String>");
            }
        }
    }

    private FieldsetField<T> withRange(Number minimum, Number maximum) {
        Predicate<T> rangeValidator = value -> {
            Number number = (Number) value;
            if (this.kind == FieldsetFieldKind.DOUBLE) {
                double candidate = number.doubleValue();
                return Double.isFinite(candidate)
                        && Double.compare(candidate, minimum.doubleValue()) >= 0
                        && Double.compare(candidate, maximum.doubleValue()) <= 0;
            }
            long candidate = number.longValue();
            return candidate >= minimum.longValue() && candidate <= maximum.longValue();
        };
        ArrayList<ValidationRule<T>> rules = new ArrayList<ValidationRule<T>>(this.validationRules);
        rules.add(new ValidationRule<T>(rangeValidator, "Must be between " + minimum + " and " + maximum));
        return copy(minimum, maximum, rules);
    }

    private FieldsetField<T> copy(Number minimum, Number maximum, List<ValidationRule<T>> rules) {
        return new FieldsetField<T>(
                this.key,
                this.kind,
                this.defaultValue,
                this.valueType,
                minimum,
                maximum,
                this.options,
                this.registryKey,
                rules
        );
    }

    private static <T> FieldsetField<T> scalar(String key, FieldsetFieldKind kind, T defaultValue, Class<?> valueType) {
        return new FieldsetField<T>(
                key,
                kind,
                defaultValue,
                valueType,
                null,
                null,
                Collections.emptyList(),
                null,
                Collections.emptyList()
        );
    }

    private static String requireKey(String key) {
        String value = requireNonBlank(key, "field key");
        if (value.startsWith("_konfig_") || value.indexOf('.') >= 0) {
            throw new IllegalArgumentException("Field key is reserved or nested: " + value);
        }
        return value;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }

    private static final class ValidationRule<T> {
        private final Predicate<T> validator;
        private final String message;

        private ValidationRule(Predicate<T> validator, String message) {
            this.validator = validator;
            this.message = message;
        }
    }
}
//?}
