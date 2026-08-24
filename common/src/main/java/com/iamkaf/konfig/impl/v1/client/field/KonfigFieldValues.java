//? if >=1.17 {
// Modern config-screen field semantics only: 1.16.x keeps legacy loader-specific
// screens, so typed UI value parsing begins at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.field;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ConfigValue;
//? if >=1.21.11
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValue;
import com.iamkaf.konfig.impl.v1.config.model.ColorValueHelper;
import com.iamkaf.konfig.impl.v1.config.model.ConfigScreenValue;
import com.iamkaf.konfig.impl.v1.config.model.EntryKind;
import com.iamkaf.konfig.impl.v1.config.model.StringListValueHelper;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@ApiStatus.Internal
public final class KonfigFieldValues {
    private KonfigFieldValues() {
    }

    public static Object parseDraft(ConfigScreenValue<?> value, Object draft) {
        try {
            switch (value.kind()) {
                case BOOLEAN:
                    return parseBoolean(draft, value.path());
                case INTEGER:
                    return Integer.valueOf(Integer.parseInt(stringValue(draft).trim()));
                case LONG:
                    return Long.valueOf(Long.parseLong(stringValue(draft).trim()));
                case DOUBLE:
                    return Double.valueOf(Double.parseDouble(stringValue(draft).trim()));
                case STRING:
                    return stringValue(draft);
                case STRING_LIST:
                    return parseStringList(draft, value.path());
                case DROPDOWN:
                    return stringValue(draft).trim();
                case ENUM:
                    return parseEnum(value, draft);
                case COLOR_RGB:
                    return Integer.valueOf(parseColor(value, draft));
                case COLOR_ARGB:
                    return Integer.valueOf(parseColor(value, draft));
//? if >=1.21.11 {
                case FIELDSET:
                    if (draft instanceof FieldsetValue) {
                        return draft;
                    }
                    throw new IllegalArgumentException("Invalid fieldset for '" + value.path() + "'.");
//?}
                case CUSTOM:
                default:
                    return value.get();
            }
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("Invalid number for '" + value.path() + "'.");
        }
    }

    private static Boolean parseBoolean(Object draft, String path) {
        if (draft instanceof Boolean) {
            return (Boolean) draft;
        }

        String value = stringValue(draft).trim();
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Invalid boolean for '" + path + "' (expected true/false).");
    }

    private static Object parseEnum(ConfigScreenValue<?> value, Object draft) {
        Object defaultValue = value.defaultValue();
        if (!(defaultValue instanceof Enum<?>)) {
            return defaultValue;
        }

        Class<?> enumClass = defaultValue.getClass();
        if (enumClass.isInstance(draft)) {
            return draft;
        }

        String target = stringValue(draft);
        Object[] constants = enumClass.getEnumConstants();
        for (Object constant : constants) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(target)) {
                return constant;
            }
        }

        throw new IllegalArgumentException("Invalid value for '" + value.path() + "'.");
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseStringList(Object draft, String path) {
        if (draft instanceof List<?>) {
            return StringListValueHelper.immutableCopy((List<String>) draft, path);
        }
        throw new IllegalArgumentException("Invalid list for '" + path + "'.");
    }

    public static int parseColor(ConfigScreenValue<?> value, Object draft) {
        if (draft instanceof Number) {
            int encoded = ((Number) draft).intValue();
            if (value.kind() == EntryKind.COLOR_RGB) {
                return ColorValueHelper.requireRgb(encoded, value.path());
            }
            return encoded;
        }

        String raw = stringValue(draft);
        if (value.kind() == EntryKind.COLOR_ARGB) {
            return ColorValueHelper.parseArgb(raw, value.path());
        }
        return ColorValueHelper.parseRgb(raw, value.path());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void setRawValue(ConfigScreenValue<?> value, Object parsed) {
        ((ConfigValue) value).set(parsed);
    }

    public static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static boolean sameValue(Object left, Object right) {
        return left == right || (left != null && left.equals(right));
    }

    public static Object snapshotValue(ConfigScreenValue<?> value, Object currentValue) {
        if (value.kind() == EntryKind.STRING_LIST) {
            return StringListValueHelper.immutableCopy(stringListValue(currentValue, value.path()), value.path());
        }
        return currentValue;
    }

    public static Object copyDraftValue(ConfigScreenValue<?> value, Object currentValue) {
        if (value.kind() == EntryKind.STRING_LIST) {
            return StringListValueHelper.mutableCopy(stringListValue(currentValue, value.path()));
        }
        return currentValue;
    }

    @SuppressWarnings("unchecked")
    public static List<String> stringListValue(Object currentValue, String path) {
        if (currentValue == null) {
            return Collections.emptyList();
        }
        if (!(currentValue instanceof List<?>)) {
            throw new IllegalArgumentException("Expected list value for '" + path + "'.");
        }
        return (List<String>) currentValue;
    }

    public static double progressFor(double current, double min, double max) {
        double span = max - min;
        if (span <= 0.0D) {
            return 0.0D;
        }
        return Mth.clamp((current - min) / span, 0.0D, 1.0D);
    }

    public static int intFromProgress(double progress, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + (int) Math.round((max - min) * progress);
    }

    public static long longFromProgress(double progress, long min, long max) {
        if (max <= min) {
            return min;
        }
        return min + Math.round((max - min) * progress);
    }

    public static double doubleFromProgress(double progress, double min, double max) {
        if (max <= min) {
            return min;
        }
        return min + (max - min) * progress;
    }

    public static String formatDouble(double value) {
        String formatted = String.format(Locale.ROOT, "%.3f", value);
        while (formatted.contains(".") && (formatted.endsWith("0") || formatted.endsWith("."))) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    public static String normalizeHexInput(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        } else if (normalized.regionMatches(true, 0, "0x", 0, 2)) {
            normalized = normalized.substring(2);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    public static boolean isHexPrefix(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
//?}
