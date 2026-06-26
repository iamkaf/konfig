package com.iamkaf.konfig.impl.v1;

import org.jetbrains.annotations.ApiStatus;

//? if >=1.17 {
import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.copyDraftValue;
import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.parseDraft;
import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.sameValue;
import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.setRawValue;
import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.snapshotValue;
import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.stringListValue;
import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.stringValue;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApiStatus.Internal
final class KonfigScreenSession {
    private final List<EntryRef> entries;
    private final Map<ConfigValueImpl<?>, Object> drafts = new LinkedHashMap<ConfigValueImpl<?>, Object>();
    private final Map<ConfigValueImpl<?>, Object> sessionStartValues = new LinkedHashMap<ConfigValueImpl<?>, Object>();

    KonfigScreenSession(List<EntryRef> entries) {
        this.entries = entries;
        for (EntryRef entry : entries) {
            Object value = entry.value.get();
            this.setDraft(entry.value, value);
            if (entry.editable) {
                this.sessionStartValues.put(entry.value, snapshotValue(entry.value, value));
            }
        }
    }

    Object draft(ConfigValueImpl<?> value) {
        return this.drafts.get(value);
    }

    void setDraft(ConfigValueImpl<?> value, Object draft) {
        this.drafts.put(value, copyDraftValue(value, draft));
    }

    Object storedSnapshot(ConfigValueImpl<?> value) {
        return snapshotValue(value, value.get());
    }

    boolean readBoolean(ConfigValueImpl<?> value) {
        Object current = this.draft(value);
        if (current instanceof Boolean) {
            return ((Boolean) current).booleanValue();
        }
        return ((Boolean) value.get()).booleanValue();
    }

    Enum<?> currentEnum(ConfigValueImpl<?> value) {
        Object defaultValue = value.defaultValue();
        if (!(defaultValue instanceof Enum<?>)) {
            throw new IllegalStateException("Expected enum value for '" + value.path() + "'.");
        }

        Object current = this.draft(value);
        if (current != null && defaultValue.getClass().isInstance(current)) {
            return (Enum<?>) current;
        }

        return (Enum<?>) defaultValue;
    }

    Enum<?> nextEnum(ConfigValueImpl<?> value) {
        Enum<?> current = this.currentEnum(value);
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

    int currentColor(ConfigValueImpl<?> value) {
        Object current = this.draft(value);
        if (current instanceof Number) {
            return ((Number) current).intValue();
        }
        return ((Number) value.get()).intValue();
    }

    List<String> currentStringList(ConfigValueImpl<?> value) {
        Object current = this.draft(value);
        if (current instanceof List<?>) {
            return StringListValueHelper.mutableCopy(stringListValue(current, value.path()));
        }
        return StringListValueHelper.mutableCopy(stringListValue(value.get(), value.path()));
    }

    String currentDropdownValue(ConfigValueImpl<?> value) {
        List<String> options = value.dropdownOptions();
        Object current = this.draft(value);
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

    String currentStringValue(ConfigValueImpl<?> value) {
        Object current = this.draft(value);
        if (current instanceof String) {
            return (String) current;
        }
        return stringValue(value.get());
    }

    int currentInt(ConfigValueImpl<?> value) {
        Object current = this.draft(value);
        if (current instanceof Number) {
            return ((Number) current).intValue();
        }
        return ((Number) value.get()).intValue();
    }

    long currentLong(ConfigValueImpl<?> value) {
        Object current = this.draft(value);
        if (current instanceof Number) {
            return ((Number) current).longValue();
        }
        return ((Number) value.get()).longValue();
    }

    double currentDouble(ConfigValueImpl<?> value) {
        Object current = this.draft(value);
        if (current instanceof Number) {
            return ((Number) current).doubleValue();
        }
        return ((Number) value.get()).doubleValue();
    }

    void persist(EntryRef entry) {
        Object previousValue = entry.value.get();
        try {
            Object parsed = parseDraft(entry.value, this.draft(entry.value));
            if (sameValue(previousValue, parsed)) {
                return;
            }

            setRawValue(entry.value, parsed);
            entry.handle.save();
        } catch (RuntimeException exception) {
            setRawValue(entry.value, previousValue);
            throw exception;
        }
    }

    void resetAll() {
        Map<ConfigValueImpl<?>, Object> previousValues = new LinkedHashMap<ConfigValueImpl<?>, Object>();
        Set<ConfigHandleImpl> handles = new LinkedHashSet<ConfigHandleImpl>();
        try {
            for (EntryRef entry : this.entries) {
                if (!entry.editable) {
                    continue;
                }
                Object resetValue = snapshotValue(entry.value, this.sessionStartValues.get(entry.value));
                previousValues.put(entry.value, snapshotValue(entry.value, entry.value.get()));
                this.setDraft(entry.value, resetValue);
                setRawValue(entry.value, resetValue);
                handles.add(entry.handle);
            }

            for (ConfigHandleImpl handle : handles) {
                handle.save();
            }
        } catch (RuntimeException exception) {
            for (Map.Entry<ConfigValueImpl<?>, Object> previousValue : previousValues.entrySet()) {
                setRawValue(previousValue.getKey(), previousValue.getValue());
                this.setDraft(previousValue.getKey(), previousValue.getValue());
            }
            throw exception;
        }
    }

    void resetEntry(EntryRef entry) {
        Object previousValue = this.storedSnapshot(entry.value);
        try {
            Object resetValue = snapshotValue(entry.value, this.sessionStartValues.get(entry.value));
            this.setDraft(entry.value, resetValue);
            setRawValue(entry.value, resetValue);
            entry.handle.save();
        } catch (RuntimeException exception) {
            setRawValue(entry.value, previousValue);
            this.setDraft(entry.value, previousValue);
            throw exception;
        }
    }
}
//?}
