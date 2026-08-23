//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.value;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.config.model.ColorValueHelper;
import com.iamkaf.konfig.impl.v1.config.model.ConfigScreenValue;
import com.iamkaf.konfig.impl.v1.model.ConfigFieldKind;
import com.iamkaf.konfig.impl.v1.state.ConfigValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public final class ConfigValueSemantics<T> implements ValueSemantics<T> {
    private final ConfigScreenValue<T> value;
    private final ConfigFieldKind kind;

    public ConfigValueSemantics(ConfigScreenValue<T> value) {
        this.value = Objects.requireNonNull(value, "value");
        this.kind = switch (value.kind()) {
            case BOOLEAN -> ConfigFieldKind.BOOLEAN;
            case INTEGER -> ConfigFieldKind.INTEGER;
            case LONG -> ConfigFieldKind.LONG;
            case DOUBLE -> ConfigFieldKind.DOUBLE;
            case STRING -> ConfigFieldKind.STRING;
            case STRING_LIST -> ConfigFieldKind.STRING_LIST;
            case DROPDOWN -> ConfigFieldKind.DROPDOWN;
            case ENUM -> ConfigFieldKind.ENUM;
            case COLOR_RGB -> ConfigFieldKind.COLOR_RGB;
            case COLOR_ARGB -> ConfigFieldKind.COLOR_ARGB;
            case FIELDSET -> ConfigFieldKind.FIELDSET;
            default -> ConfigFieldKind.CUSTOM;
        };
    }

    @Override
    public ConfigFieldKind kind() {
        return this.kind;
    }

    @Override
    public T copy(T value) {
        return this.value.copyValue(value);
    }

    @Override
    public T normalize(T value) {
        return this.value.normalizeAndValidate(value);
    }

    @Override
    public ConfigValidation validate(String path, T value) {
        try {
            normalize(value);
            return ConfigValidation.valid();
        } catch (RuntimeException exception) {
            return invalid(path, exception);
        }
    }

    @Override
    public ValueParseResult<T> parse(String path, Object input) {
        try {
            T parsed = normalize(parseInput(input));
            return new ValueParseResult.Parsed<>(copy(parsed), ConfigValidation.valid());
        } catch (RuntimeException exception) {
            return new ValueParseResult.Rejected<>(invalid(path, exception));
        }
    }

    @SuppressWarnings("unchecked")
    private T parseInput(Object input) {
        Object parsed = switch (this.value.kind()) {
            case BOOLEAN -> parseBoolean(input);
            case INTEGER -> input instanceof Integer number ? number : Integer.valueOf(stringValue(input).trim());
            case LONG -> input instanceof Long number ? number : Long.valueOf(stringValue(input).trim());
            case DOUBLE -> input instanceof Double number ? number : Double.valueOf(stringValue(input).trim());
            case STRING, DROPDOWN -> stringValue(input);
            case STRING_LIST -> parseStringList(input);
            case ENUM -> parseEnum(input);
            case COLOR_RGB -> Integer.valueOf(parseColor(input, false));
            case COLOR_ARGB -> Integer.valueOf(parseColor(input, true));
            case FIELDSET, CUSTOM -> requireDefaultType(input);
            default -> throw new IllegalArgumentException("Value kind cannot be edited");
        };
        return (T) parsed;
    }

    private Boolean parseBoolean(Object input) {
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
    }

    private List<String> parseStringList(Object input) {
        if (!(input instanceof List<?> list)) {
            throw new IllegalArgumentException("expected a list of strings");
        }
        var result = new ArrayList<String>(list.size());
        for (Object item : list) {
            if (!(item instanceof String string)) {
                throw new IllegalArgumentException("expected a list of strings");
            }
            result.add(string);
        }
        return List.copyOf(result);
    }

    private Object parseEnum(Object input) {
        Object defaultValue = this.value.defaultValue();
        if (!(defaultValue instanceof Enum<?> defaultEnum)) {
            throw new IllegalStateException("enum config has a non-enum default");
        }
        if (defaultValue.getClass().isInstance(input)) {
            return input;
        }
        String requested = stringValue(input).trim();
        for (Object constant : defaultEnum.getDeclaringClass().getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(requested)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("unknown enum value");
    }

    private int parseColor(Object input, boolean alpha) {
        if (input instanceof Number number) {
            return alpha ? number.intValue() : ColorValueHelper.requireRgb(number.intValue(), this.value.path());
        }
        return alpha
                ? ColorValueHelper.parseArgb(stringValue(input), this.value.path())
                : ColorValueHelper.parseRgb(stringValue(input), this.value.path());
    }

    private Object requireDefaultType(Object input) {
        Object defaultValue = this.value.defaultValue();
        if (input == null || !defaultValue.getClass().isInstance(input)) {
            throw new IllegalArgumentException("expected " + defaultValue.getClass().getSimpleName());
        }
        return input;
    }

    private static ConfigValidation invalid(String path, RuntimeException exception) {
        String detail = exception.getMessage();
        String message = detail == null || detail.isBlank()
                ? "Invalid value for '" + path + "'"
                : detail;
        return ConfigValidation.error(path, "invalid_value", message);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
//?}
