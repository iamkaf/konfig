//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.value;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigFieldKind;
import com.iamkaf.konfig.impl.v1.state.ConfigValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@ApiStatus.Internal
public final class StandardValueSemantics {
    private StandardValueSemantics() {
    }

    public static ValueSemantics<Boolean> bool() {
        return scalar(ConfigFieldKind.BOOLEAN, input -> {
            if (input instanceof Boolean value) {
                return value;
            }
            String raw = stringValue(input).trim();
            if ("true".equalsIgnoreCase(raw)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(raw)) {
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("expected true or false");
        });
    }

    public static ValueSemantics<Integer> integer() {
        return scalar(ConfigFieldKind.INTEGER, input -> input instanceof Integer value
                ? value
                : Integer.valueOf(stringValue(input).trim()));
    }

    public static ValueSemantics<Long> longInteger() {
        return scalar(ConfigFieldKind.LONG, input -> input instanceof Long value
                ? value
                : Long.valueOf(stringValue(input).trim()));
    }

    public static ValueSemantics<Double> decimal() {
        return scalar(ConfigFieldKind.DOUBLE, input -> input instanceof Double value
                ? value
                : Double.valueOf(stringValue(input).trim()));
    }

    public static ValueSemantics<String> string() {
        return scalar(ConfigFieldKind.STRING, StandardValueSemantics::stringValue);
    }

    public static ValueSemantics<String> dropdown(Set<String> allowedValues) {
        Set<String> allowed = Set.copyOf(allowedValues);
        return validated(
                ConfigFieldKind.DROPDOWN,
                StandardValueSemantics::stringValue,
                UnaryOperator.identity(),
                allowed::contains,
                "not_allowed",
                "Expected one of " + allowed
        );
    }

    public static ValueSemantics<String> registryReference(Predicate<String> validator) {
        return validated(
                ConfigFieldKind.STRING,
                input -> stringValue(input).trim(),
                UnaryOperator.identity(),
                validator,
                "unknown_registry_entry",
                "Unknown registry entry"
        );
    }

    public static ValueSemantics<Integer> rgbColor() {
        return color(ConfigFieldKind.COLOR_RGB, 6, 0xFFFFFF);
    }

    public static ValueSemantics<Integer> argbColor() {
        return color(ConfigFieldKind.COLOR_ARGB, 8, 0xFFFFFFFF);
    }

    public static <E extends Enum<E>> ValueSemantics<E> enumValue(Class<E> enumType) {
        Objects.requireNonNull(enumType, "enumType");
        return scalar(ConfigFieldKind.ENUM, input -> {
            if (enumType.isInstance(input)) {
                return enumType.cast(input);
            }
            String requested = stringValue(input).trim();
            for (E constant : enumType.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(requested)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("unknown " + enumType.getSimpleName() + " value");
        });
    }

    public static ValueSemantics<List<String>> stringList() {
        return custom(
                ConfigFieldKind.STRING_LIST,
                input -> {
                    if (!(input instanceof List<?> values)) {
                        throw new IllegalArgumentException("expected a list of strings");
                    }
                    var result = new ArrayList<String>(values.size());
                    for (Object value : values) {
                        if (!(value instanceof String string)) {
                            throw new IllegalArgumentException("expected a list of strings");
                        }
                        result.add(string);
                    }
                    return List.copyOf(result);
                },
                List::copyOf,
                List::copyOf,
                (path, value) -> ConfigValidation.valid()
        );
    }

    public static <T> ValueSemantics<T> scalar(ConfigFieldKind kind, Function<Object, T> parser) {
        return custom(kind, parser, UnaryOperator.identity(), UnaryOperator.identity(), (path, value) -> ConfigValidation.valid());
    }

    public static <T> ValueSemantics<T> validated(
            ConfigFieldKind kind,
            Function<Object, T> parser,
            UnaryOperator<T> normalizer,
            Predicate<T> validator,
            String errorCode,
            String errorMessage
    ) {
        return custom(
                kind,
                parser,
                normalizer,
                UnaryOperator.identity(),
                (path, value) -> validator.test(value)
                        ? ConfigValidation.valid()
                        : ConfigValidation.error(path, errorCode, errorMessage)
        );
    }

    public static <T> ValueSemantics<T> custom(
            ConfigFieldKind kind,
            Function<Object, T> parser,
            UnaryOperator<T> normalizer,
            UnaryOperator<T> copier,
            Validator<T> validator
    ) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(normalizer, "normalizer");
        Objects.requireNonNull(copier, "copier");
        Objects.requireNonNull(validator, "validator");
        return new ValueSemantics<>() {
            @Override
            public ConfigFieldKind kind() {
                return kind;
            }

            @Override
            public T copy(T value) {
                return Objects.requireNonNull(copier.apply(Objects.requireNonNull(value, "value")), "copier result");
            }

            @Override
            public T normalize(T value) {
                return Objects.requireNonNull(normalizer.apply(Objects.requireNonNull(value, "value")), "normalizer result");
            }

            @Override
            public ConfigValidation validate(String path, T value) {
                return Objects.requireNonNull(validator.validate(path, value), "validator result");
            }

            @Override
            public ValueParseResult<T> parse(String path, Object input) {
                try {
                    T normalized = normalize(Objects.requireNonNull(parser.apply(input), "parser result"));
                    ConfigValidation validation = validate(path, normalized);
                    if (validation.hasErrors()) {
                        return new ValueParseResult.Rejected<>(validation);
                    }
                    return new ValueParseResult.Parsed<>(copy(normalized), validation);
                } catch (RuntimeException exception) {
                    String detail = exception.getMessage();
                    String message = "Invalid value for '" + path + "'";
                    if (detail != null && !detail.isBlank()) {
                        message += ": " + detail;
                    }
                    return new ValueParseResult.Rejected<>(ConfigValidation.error(path, "invalid_value", message));
                }
            }
        };
    }

    private static ValueSemantics<Integer> color(ConfigFieldKind kind, int digits, int mask) {
        return scalar(kind, input -> {
            if (input instanceof Number number) {
                return Integer.valueOf(number.intValue() & mask);
            }
            String normalized = normalizeHexInput(stringValue(input));
            if (normalized.length() != digits) {
                throw new IllegalArgumentException("expected " + digits + " hexadecimal digits");
            }
            return Integer.valueOf((int) Long.parseLong(normalized, 16));
        });
    }

    private static String normalizeHexInput(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        } else if (normalized.regionMatches(true, 0, "0x", 0, 2)) {
            normalized = normalized.substring(2);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @FunctionalInterface
    public interface Validator<T> {
        ConfigValidation validate(String path, T value);
    }
}
//?}
