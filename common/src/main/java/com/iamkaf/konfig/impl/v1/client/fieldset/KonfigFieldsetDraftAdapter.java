//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntry;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetField;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetFieldKind;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValidationIssue;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValue;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
final class KonfigFieldsetDraftAdapter implements KonfigFieldsetUiAdapter<FieldsetEntry, FieldsetField<?>> {
    private final KonfigFieldsetDraftSession session;
    private final KonfigFieldsetAccess access;

    KonfigFieldsetDraftAdapter(KonfigFieldsetDraftSession session, KonfigFieldsetAccess access) {
        this.session = Objects.requireNonNull(session, "session");
        this.access = Objects.requireNonNull(access, "access");
    }

    @Override
    public List<FieldsetEntry> entries() {
        return this.session.draft().entries();
    }

    @Override
    public String entryId(FieldsetEntry entry) {
        return entry.identity();
    }

    @Override
    public Component entryLabel(FieldsetEntry entry) {
        for (FieldsetField<?> field : this.session.draft().schema().fields()) {
            if (field.kind() != FieldsetFieldKind.STRING
                    && field.kind() != FieldsetFieldKind.OPTIONAL_STRING
                    && field.kind() != FieldsetFieldKind.DROPDOWN
                    && field.kind() != FieldsetFieldKind.REGISTRY_STRING) {
                continue;
            }
            String value = displayValue(read(entry, field));
            if (!value.isBlank()) {
                return Component.literal(value);
            }
        }
        int index = this.entries().indexOf(entry);
        return Component.literal(index < 0 ? "Entry" : "Entry " + (index + 1));
    }

    @Override
    public Component entrySummary(FieldsetEntry entry) {
        List<String> parts = new ArrayList<>();
        for (FieldsetField<?> field : this.session.draft().schema().fields()) {
            String value = displayValue(read(entry, field));
            if (value.isBlank() || value.equals(this.entryLabel(entry).getString())) {
                continue;
            }
            parts.add(pretty(field.key()) + ": " + value);
            if (parts.size() == 2) {
                break;
            }
        }
        return Component.literal(String.join(", ", parts));
    }

    @Override
    public List<String> entrySearchTerms(FieldsetEntry entry) {
        List<String> terms = new ArrayList<>();
        terms.add(entry.identity());
        for (FieldsetField<?> field : this.session.draft().schema().fields()) {
            terms.add(displayValue(read(entry, field)));
        }
        return List.copyOf(terms);
    }

    @Override
    public KonfigFieldsetAccess fieldsetAccess() {
        return this.access;
    }

    @Override
    public KonfigFieldsetAccess entryAccess(FieldsetEntry entry) {
        if (!this.access.canEdit()) {
            return this.access;
        }
        return entry.editable() ? KonfigFieldsetAccess.editable() : KonfigFieldsetAccess.builtinReadOnly();
    }

    @Override
    public FieldsetEntry createEntry() {
        return FieldsetEntry.newUser();
    }

    @Override
    public FieldsetEntry duplicateEntry(FieldsetEntry entry) {
        FieldsetEntry copy = FieldsetEntry.newUser();
        FieldsetValue duplicated = this.session.draft().duplicateAsUser(entry.identity(), copy.identity());
        return duplicated.entry(copy.identity()).orElseThrow();
    }

    @Override
    public KonfigFieldsetEditResult replaceEntries(List<FieldsetEntry> entries) {
        if (!this.access.canEdit()) {
            return KonfigFieldsetEditResult.readOnly(this.access.reason());
        }
        try {
            FieldsetValue next = FieldsetValue.of(this.session.draft().schema(), entries);
            return this.session.update(next)
                    ? KonfigFieldsetEditResult.applied()
                    : KonfigFieldsetEditResult.noChange();
        } catch (RuntimeException exception) {
            return KonfigFieldsetEditResult.invalid(Component.literal(message(exception)));
        }
    }

    @Override
    public List<FieldsetField<?>> fields(FieldsetEntry entry) {
        return this.session.draft().schema().fields();
    }

    @Override
    public Component fieldLabel(FieldsetField<?> field) {
        return Component.literal(pretty(field.key()));
    }

    @Override
    public Component fieldDescription(FieldsetField<?> field) {
        return Component.empty();
    }

    @Override
    public String fieldPath(FieldsetField<?> field) {
        return field.key();
    }

    @Override
    public KonfigFieldsetValueBinding<Object> bind(FieldsetEntry entry, FieldsetField<?> field) {
        return new FieldBinding(this, entry.identity(), field);
    }

    @Override
    public KonfigFieldsetValidation validation() {
        List<KonfigFieldsetValidation.Issue> issues = new ArrayList<>();
        for (FieldsetValidationIssue issue : this.session.draft().validate().issues()) {
            issues.add(KonfigFieldsetValidation.Issue.fieldError(
                    issue.entryIdentity(),
                    issue.fieldKey().orElse(""),
                    Component.literal(issue.message())
            ));
        }
        return issues.isEmpty() ? KonfigFieldsetValidation.valid() : new KonfigFieldsetValidation(issues);
    }

    private FieldsetEntry requireEntry(String identity) {
        return this.session.draft().entry(identity)
                .orElseThrow(() -> new IllegalStateException("Unknown fieldset entry '" + identity + "'."));
    }

    private KonfigFieldsetEditResult set(String identity, FieldsetField<?> field, Object value) {
        FieldsetEntry entry = this.requireEntry(identity);
        KonfigFieldsetAccess entryAccess = this.entryAccess(entry);
        if (!entryAccess.canEdit()) {
            return KonfigFieldsetEditResult.readOnly(entryAccess.reason());
        }
        try {
            FieldsetEntry changed = entry.withScalar(field, value);
            FieldsetValue next = this.session.draft().replaceUserEntry(changed);
            return this.session.update(next)
                    ? KonfigFieldsetEditResult.applied()
                    : KonfigFieldsetEditResult.noChange();
        } catch (RuntimeException exception) {
            return KonfigFieldsetEditResult.invalid(Component.literal(message(exception)));
        }
    }

    private static Object read(FieldsetEntry entry, FieldsetField<?> field) {
        return readCaptured(entry, field);
    }

    private static <T> T readCaptured(FieldsetEntry entry, FieldsetField<T> field) {
        return entry.value(field);
    }

    private static String displayValue(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.map(String::valueOf).orElse("");
        }
        return String.valueOf(value);
    }

    private static String pretty(String key) {
        StringBuilder result = new StringBuilder();
        boolean capitalize = true;
        for (int index = 0; index < key.length(); index++) {
            char character = key.charAt(index);
            if (character == '_' || character == '-' || character == '.') {
                if (!result.isEmpty() && result.charAt(result.length() - 1) != ' ') {
                    result.append(' ');
                }
                capitalize = true;
            } else if (capitalize) {
                result.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                result.append(character);
            }
        }
        return result.toString().trim();
    }

    private static String message(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "The value could not be edited." : message;
    }

    private static final class FieldBinding implements KonfigFieldsetValueBinding<Object> {
        private final KonfigFieldsetDraftAdapter adapter;
        private final String entryId;
        private final FieldsetField<?> field;

        private FieldBinding(KonfigFieldsetDraftAdapter adapter, String entryId, FieldsetField<?> field) {
            this.adapter = adapter;
            this.entryId = entryId;
            this.field = field;
        }

        @Override
        public Object draft() {
            return read(this.adapter.requireEntry(this.entryId), this.field);
        }

        @Override
        public boolean dirty() {
            Optional<FieldsetEntry> original = this.adapter.session.original().entry(this.entryId);
            return original.isEmpty() || !Objects.equals(read(original.get(), this.field), this.draft());
        }

        @Override
        public KonfigFieldsetAccess access() {
            return this.adapter.entryAccess(this.adapter.requireEntry(this.entryId));
        }

        @Override
        public KonfigFieldsetValidation validation() {
            return this.adapter.validation().forField(this.entryId, this.field.key());
        }

        @Override
        public KonfigFieldsetEditResult setDraft(Object value) {
            return this.adapter.set(this.entryId, this.field, value);
        }

        @Override
        public KonfigFieldsetEditResult reset() {
            Object resetValue = this.adapter.session.original().entry(this.entryId)
                    .map(entry -> read(entry, this.field))
                    .orElse(this.field.defaultValue());
            return this.setDraft(resetValue);
        }
    }
}
//?}
