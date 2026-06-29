package com.iamkaf.konfig.impl.v1.client.legacy;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.impl.v1.config.model.ColorValueHelper;
import com.iamkaf.konfig.impl.v1.config.model.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.config.model.ConfigValueImpl;
import com.iamkaf.konfig.impl.v1.config.model.EntryKind;
import com.iamkaf.konfig.impl.v1.config.model.StringListValueHelper;
import com.iamkaf.konfig.impl.v1.client.toast.KonfigToastSupport;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApiStatus.Internal
public final class LegacyDraftSession {
    private final Map<ConfigValueImpl<?>, Object> drafts = new LinkedHashMap<ConfigValueImpl<?>, Object>();
    private final Map<ConfigValueImpl<?>, Object> sessionStartValues = new LinkedHashMap<ConfigValueImpl<?>, Object>();

    public LegacyDraftSession(Collection<LegacyConfigEntry> entries) {
        for (LegacyConfigEntry entry : entries) {
            Object value = entry.value().get();
            this.drafts.put(entry.value(), copyDraftValue(entry.value(), value));
            if (entry.editable()) {
                this.sessionStartValues.put(entry.value(), snapshotValue(entry.value(), value));
            }
        }
    }

    public Object draft(ConfigValueImpl<?> value) {
        return this.drafts.get(value);
    }

    public void draft(ConfigValueImpl<?> value, Object draft) {
        this.drafts.put(value, draft);
    }

    public Object sessionStartValue(ConfigValueImpl<?> value) {
        return this.sessionStartValues.get(value);
    }

    public boolean persist(LegacyConfigEntry entry) {
        Object previousValue = entry.value().get();
        try {
            Object parsed = parseDraft(entry.value(), this.drafts.get(entry.value()));
            if (sameValue(previousValue, parsed)) {
                return true;
            }

            setRawValue(entry.value(), parsed);
            entry.handle().save();
            return true;
        } catch (Exception exception) {
            setRawValue(entry.value(), previousValue);
            KonfigToastSupport.saveFailed(exceptionMessage(exception));
            return false;
        }
    }

    public boolean reset(Collection<LegacyConfigEntry> entries) {
        Map<ConfigValueImpl<?>, Object> previousValues = new LinkedHashMap<ConfigValueImpl<?>, Object>();
        Set<ConfigHandleImpl> handles = new LinkedHashSet<ConfigHandleImpl>();
        try {
            for (LegacyConfigEntry entry : entries) {
                if (!entry.editable()) {
                    continue;
                }
                Object resetValue = snapshotValue(entry.value(), this.sessionStartValues.get(entry.value()));
                previousValues.put(entry.value(), snapshotValue(entry.value(), entry.value().get()));
                this.drafts.put(entry.value(), copyDraftValue(entry.value(), resetValue));
                setRawValue(entry.value(), resetValue);
                handles.add(entry.handle());
            }

            for (ConfigHandleImpl handle : handles) {
                handle.save();
            }
            return true;
        } catch (Exception exception) {
            for (Map.Entry<ConfigValueImpl<?>, Object> previousValue : previousValues.entrySet()) {
                setRawValue(previousValue.getKey(), previousValue.getValue());
                this.drafts.put(previousValue.getKey(), copyDraftValue(previousValue.getKey(), previousValue.getValue()));
            }
            KonfigToastSupport.resetFailed(exceptionMessage(exception));
            return false;
        }
    }

    public boolean readBoolean(ConfigValueImpl<?> value) {
        Object current = this.drafts.get(value);
        if (current instanceof Boolean) {
            return ((Boolean) current).booleanValue();
        }
        return ((Boolean) value.get()).booleanValue();
    }

    public Enum<?> currentEnum(ConfigValueImpl<?> value) {
        Object defaultValue = value.defaultValue();
        if (!(defaultValue instanceof Enum<?>)) {
            throw new IllegalStateException("Expected enum value for '" + value.path() + "'.");
        }

        Object current = this.drafts.get(value);
        if (current != null && defaultValue.getClass().isInstance(current)) {
            return (Enum<?>) current;
        }

        return (Enum<?>) defaultValue;
    }

    public Enum<?> cycleEnum(ConfigValueImpl<?> value) {
        Enum<?> current = currentEnum(value);
        Object[] constants = current.getDeclaringClass().getEnumConstants();

        int index = 0;
        for (int i = 0; i < constants.length; i++) {
            if (constants[i] == current) {
                index = i;
                break;
            }
        }

        return (Enum<?>) constants[(index + 1) % constants.length];
    }

    public int currentColor(ConfigValueImpl<?> value) {
        Object current = this.drafts.get(value);
        if (current instanceof Number) {
            return ((Number) current).intValue();
        }
        return ((Number) value.get()).intValue();
    }

    public List<String> currentStringList(ConfigValueImpl<?> value) {
        Object current = this.drafts.get(value);
        if (current instanceof List<?>) {
            return StringListValueHelper.mutableCopy(stringListValue(current, value.path()));
        }
        return StringListValueHelper.mutableCopy(stringListValue(value.get(), value.path()));
    }

    public String currentDropdownValue(ConfigValueImpl<?> value) {
        List<String> options = value.dropdownOptions();
        Object current = this.drafts.get(value);
        if (current instanceof String) {
            String normalized = ((String) current).trim();
            if (options.contains(normalized)) {
                return normalized;
            }
        }

        Object stored = value.get();
        if (stored instanceof String) {
            String normalized = ((String) stored).trim();
            if (options.contains(normalized)) {
                return normalized;
            }
        }

        Object defaultValue = value.defaultValue();
        if (defaultValue instanceof String) {
            String normalized = ((String) defaultValue).trim();
            if (options.contains(normalized)) {
                return normalized;
            }
        }

        return options.isEmpty() ? "" : options.get(0);
    }

    public void revertDraft(ConfigValueImpl<?> value, Object previousValue) {
        this.drafts.put(value, copyDraftValue(value, previousValue));
    }

    public static Object parseDraft(ConfigValueImpl<?> value, Object draft) {
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
                case CUSTOM:
                default:
                    return value.get();
            }
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("Invalid number for '" + value.path() + "'.");
        }
    }

    public static String exceptionMessage(Exception exception) {
        return exception.getMessage() == null ? "" : exception.getMessage();
    }

    public static Object snapshotValue(ConfigValueImpl<?> value, Object currentValue) {
        if (value.kind() == EntryKind.STRING_LIST) {
            return StringListValueHelper.immutableCopy(stringListValue(currentValue, value.path()), value.path());
        }
        return currentValue;
    }

    public static Object copyDraftValue(ConfigValueImpl<?> value, Object currentValue) {
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

    private static Object parseEnum(ConfigValueImpl<?> value, Object draft) {
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

    public static int parseColor(ConfigValueImpl<?> value, Object draft) {
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
    public static void setRawValue(ConfigValueImpl<?> value, Object parsed) {
        ((ConfigValue) value).set(parsed);
    }

    public static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static boolean sameValue(Object left, Object right) {
        return left == right || (left != null && left.equals(right));
    }
}
