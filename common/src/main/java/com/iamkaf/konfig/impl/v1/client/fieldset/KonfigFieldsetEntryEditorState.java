//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
public final class KonfigFieldsetEntryEditorState<E, F> {
    private final KonfigFieldsetUiAdapter<E, F> adapter;
    private final String entryId;
    private final KonfigFieldsetValidation validation;

    public KonfigFieldsetEntryEditorState(KonfigFieldsetUiAdapter<E, F> adapter, String entryId) {
        this(adapter, entryId, adapter.validation());
    }

    public KonfigFieldsetEntryEditorState(
            KonfigFieldsetUiAdapter<E, F> adapter,
            String entryId,
            KonfigFieldsetValidation validation
    ) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.entryId = Objects.requireNonNull(entryId, "entryId");
        this.validation = Objects.requireNonNull(validation, "validation");
    }

    public String entryId() {
        return this.entryId;
    }

    public boolean exists() {
        return this.entry().isPresent();
    }

    public Component label() {
        return this.entry().map(this.adapter::entryLabel).orElse(Component.empty());
    }

    public Component summary() {
        return this.entry().map(this.adapter::entrySummary).orElse(Component.empty());
    }

    public KonfigFieldsetAccess access() {
        KonfigFieldsetAccess fieldsetAccess = this.adapter.fieldsetAccess();
        if (!fieldsetAccess.canEdit()) {
            return fieldsetAccess;
        }
        return this.entry()
                .map(this.adapter::entryAccess)
                .orElseGet(() -> KonfigFieldsetAccess.readOnly(Component.literal("This entry no longer exists.")));
    }

    public KonfigFieldsetValidation validation() {
        return this.validation.forEntry(this.entryId);
    }

    public List<FieldState<F>> fields() {
        Optional<E> entry = this.entry();
        if (entry.isEmpty()) {
            return List.of();
        }

        E resolvedEntry = entry.get();
        List<FieldState<F>> fields = new ArrayList<>();
        for (F field : this.adapter.fields(resolvedEntry)) {
            String fieldPath = this.adapter.fieldPath(field);
            fields.add(new FieldState<>(
                    field,
                    this.adapter.fieldLabel(field),
                    this.adapter.fieldDescription(field),
                    fieldPath,
                    this.adapter.bind(resolvedEntry, field),
                    this.validation.forField(this.entryId, fieldPath)
            ));
        }
        return List.copyOf(fields);
    }

    private Optional<E> entry() {
        for (E entry : this.adapter.entries()) {
            if (this.adapter.entryId(entry).equals(this.entryId)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public record FieldState<F>(
            F field,
            Component label,
            Component description,
            String path,
            KonfigFieldsetValueBinding<Object> value,
            KonfigFieldsetValidation validation
    ) {
        public FieldState {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(validation, "validation");
        }
    }
}
//?}
