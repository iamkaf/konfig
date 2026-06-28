//? if >=1.17 {
// Modern config-screen field semantics only: 1.16.x keeps legacy loader-specific
// screens, so typed UI value state begins at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.field;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.field.KonfigFieldValues.copyDraftValue;
import static com.iamkaf.konfig.impl.v1.client.field.KonfigFieldValues.parseDraft;
import static com.iamkaf.konfig.impl.v1.client.field.KonfigFieldValues.sameValue;
import static com.iamkaf.konfig.impl.v1.client.field.KonfigFieldValues.setRawValue;
import static com.iamkaf.konfig.impl.v1.client.field.KonfigFieldValues.snapshotValue;
import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.text;
import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.translate;
import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.translatedDropdownOption;
import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.translatedDropdownValue;
import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.translatedEnumValue;

import com.iamkaf.konfig.impl.v1.client.screen.EntryRef;
import com.iamkaf.konfig.impl.v1.config.model.ColorValueHelper;
import com.iamkaf.konfig.impl.v1.config.model.ConfigScreenValue;
import com.iamkaf.konfig.impl.v1.config.model.DropdownOptionMetadata;
import com.iamkaf.konfig.impl.v1.config.model.EntryKind;
import com.iamkaf.konfig.impl.v1.config.model.StringListValueHelper;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

@ApiStatus.Internal
public final class KonfigField {
    private final EntryRef entry;
    private final Object sessionStartValue;
    private Object draft;

    KonfigField(EntryRef entry) {
        this.entry = entry;
        Object value = entry.value.get();
        this.draft = copyDraftValue(entry.value, value);
        this.sessionStartValue = entry.editable ? snapshotValue(entry.value, value) : null;
    }

    public EntryRef entry() {
        return this.entry;
    }

    public ConfigScreenValue<?> value() {
        return this.entry.value;
    }

    public Object draft() {
        return this.draft;
    }

    public Object storedSnapshot() {
        return snapshotValue(this.entry.value, this.entry.value.get());
    }

    public void setDraft(Object draft) {
        this.draft = copyDraftValue(this.entry.value, draft);
    }

    public void validateDraft(Object draft) {
        parseDraft(this.entry.value, draft);
    }

    public boolean booleanValue() {
        Object current = this.draft;
        if (current instanceof Boolean) {
            return ((Boolean) current).booleanValue();
        }
        return ((Boolean) this.entry.value.get()).booleanValue();
    }

    public void setBoolean(boolean value) {
        this.setDraft(Boolean.valueOf(value));
    }

    public Enum<?> enumValue() {
        Object defaultValue = this.entry.value.defaultValue();
        if (!(defaultValue instanceof Enum<?>)) {
            throw new IllegalStateException("Expected enum value for '" + this.entry.value.path() + "'.");
        }

        Object current = this.draft;
        if (current != null && defaultValue.getClass().isInstance(current)) {
            return (Enum<?>) current;
        }

        return (Enum<?>) defaultValue;
    }

    public Enum<?> nextEnumValue() {
        Enum<?> current = this.enumValue();
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

    public int colorValue() {
        Object current = this.draft;
        if (current instanceof Number) {
            return ((Number) current).intValue();
        }
        return ((Number) this.entry.value.get()).intValue();
    }

    public void setColor(int value) {
        this.setDraft(Integer.valueOf(value));
    }

    public List<String> stringListValue() {
        Object current = this.draft;
        if (current instanceof List<?>) {
            return StringListValueHelper.mutableCopy(KonfigFieldValues.stringListValue(current, this.entry.value.path()));
        }
        return StringListValueHelper.mutableCopy(KonfigFieldValues.stringListValue(this.entry.value.get(), this.entry.value.path()));
    }

    public String dropdownValue() {
        List<String> options = this.entry.value.dropdownOptions();
        Object current = this.draft;
        if (current instanceof String) {
            String normalized = ((String) current).trim();
            if (options.contains(normalized)) {
                return normalized;
            }
        }

        Object stored = this.entry.value.get();
        if (stored instanceof String) {
            String normalized = ((String) stored).trim();
            if (options.contains(normalized)) {
                return normalized;
            }
        }

        Object defaultValue = this.entry.value.defaultValue();
        if (defaultValue instanceof String) {
            String normalized = ((String) defaultValue).trim();
            if (options.contains(normalized)) {
                return normalized;
            }
        }

        return options.isEmpty() ? "" : options.get(0);
    }

    public void setDropdownValue(String value) {
        this.setDraft(value);
    }

    public DropdownOptionMetadata dropdownOption(int index) {
        List<DropdownOptionMetadata> options = this.entry.value.dropdownOptionMetadata();
        return index >= 0 && index < options.size() ? options.get(index) : null;
    }

    public DropdownOptionMetadata currentDropdownOption() {
        return this.entry.value.dropdownOption(this.dropdownValue());
    }

    public String stringValue() {
        Object current = this.draft;
        if (current instanceof String) {
            return (String) current;
        }
        return KonfigFieldValues.stringValue(this.entry.value.get());
    }

    public int intValue() {
        Object current = this.draft;
        if (current instanceof Number) {
            return ((Number) current).intValue();
        }
        return ((Number) this.entry.value.get()).intValue();
    }

    public long longValue() {
        Object current = this.draft;
        if (current instanceof Number) {
            return ((Number) current).longValue();
        }
        return ((Number) this.entry.value.get()).longValue();
    }

    public double doubleValue() {
        Object current = this.draft;
        if (current instanceof Number) {
            return ((Number) current).doubleValue();
        }
        return ((Number) this.entry.value.get()).doubleValue();
    }

    public Component booleanText() {
        return CommonComponents.optionStatus(this.booleanValue());
    }

    public Component enumText() {
        return translatedEnumValue(this.entry, this.enumValue());
    }

    public Component colorText() {
        int color = this.colorValue();
        if (this.entry.value.kind() == EntryKind.COLOR_ARGB) {
            return text(ColorValueHelper.formatArgb(color));
        }
        return text(ColorValueHelper.formatRgb(color));
    }

    public Component stringListText() {
        List<String> values = this.stringListValue();
        if (values.isEmpty()) {
            return translate("konfig.screen.list.empty");
        }
        if (values.size() == 1) {
            return text(values.get(0));
        }
        if (values.size() == 2) {
            return text(values.get(0) + ", " + values.get(1));
        }
        return translate("konfig.screen.list.summary", values.get(0), Integer.valueOf(values.size() - 1));
    }

    public Component dropdownText(String option) {
        DropdownOptionMetadata metadata = this.entry.value.dropdownOption(option);
        return metadata == null ? translatedDropdownValue(this.entry, option) : translatedDropdownOption(this.entry, metadata);
    }

    public void persist() {
        Object previousValue = this.entry.value.get();
        try {
            Object parsed = parseDraft(this.entry.value, this.draft);
            if (sameValue(previousValue, parsed)) {
                return;
            }

            setRawValue(this.entry.value, parsed);
            this.entry.handle.save();
        } catch (RuntimeException exception) {
            setRawValue(this.entry.value, previousValue);
            throw exception;
        }
    }

    public void resetToSessionStart() {
        Object previousValue = this.storedSnapshot();
        try {
            Object resetValue = snapshotValue(this.entry.value, this.sessionStartValue);
            this.setDraft(resetValue);
            setRawValue(this.entry.value, resetValue);
            this.entry.handle.save();
        } catch (RuntimeException exception) {
            setRawValue(this.entry.value, previousValue);
            this.setDraft(previousValue);
            throw exception;
        }
    }

    void restoreStoredValue(Object storedValue) {
        setRawValue(this.entry.value, storedValue);
        this.setDraft(storedValue);
    }

    Object sessionStartValue() {
        return snapshotValue(this.entry.value, this.sessionStartValue);
    }
}
//?}
