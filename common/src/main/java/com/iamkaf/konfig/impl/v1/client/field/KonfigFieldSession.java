//? if >=1.17 {
// Modern config-screen field semantics only: 1.16.x keeps legacy loader-specific
// screens, so save/reset field sessions begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.field;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.field.KonfigFieldValues.setRawValue;
import static com.iamkaf.konfig.impl.v1.client.field.KonfigFieldValues.snapshotValue;

import com.iamkaf.konfig.impl.v1.client.screen.EntryRef;
import com.iamkaf.konfig.impl.v1.config.model.ConfigScreenHandle;
import com.iamkaf.konfig.impl.v1.config.model.ConfigScreenValue;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApiStatus.Internal
public final class KonfigFieldSession {
    private final List<EntryRef> entries;
    private final Map<ConfigScreenValue<?>, KonfigField> fields = new LinkedHashMap<ConfigScreenValue<?>, KonfigField>();

    public KonfigFieldSession(List<EntryRef> entries) {
        this.entries = entries;
        for (EntryRef entry : entries) {
            this.fields.put(entry.value, new KonfigField(entry));
        }
    }

    public KonfigField field(EntryRef entry) {
        return this.field(entry.value);
    }

    public KonfigField field(ConfigScreenValue<?> value) {
        KonfigField field = this.fields.get(value);
        if (field == null) {
            throw new IllegalArgumentException("Unknown config field '" + value.path() + "'.");
        }
        return field;
    }

    public void resetAll() {
        Map<KonfigField, Object> previousValues = new LinkedHashMap<KonfigField, Object>();
        Set<ConfigScreenHandle> handles = new LinkedHashSet<ConfigScreenHandle>();
        try {
            for (EntryRef entry : this.entries) {
                if (!entry.editable) {
                    continue;
                }
                KonfigField field = this.field(entry);
                Object resetValue = field.sessionStartValue();
                previousValues.put(field, snapshotValue(entry.value, entry.value.get()));
                field.setDraft(resetValue);
                setRawValue(entry.value, resetValue);
                handles.add(entry.handle);
            }

            for (ConfigScreenHandle handle : handles) {
                handle.save();
            }
        } catch (RuntimeException exception) {
            for (Map.Entry<KonfigField, Object> previousValue : previousValues.entrySet()) {
                previousValue.getKey().restoreStoredValue(previousValue.getValue());
            }
            throw exception;
        }
    }
}
//?}
