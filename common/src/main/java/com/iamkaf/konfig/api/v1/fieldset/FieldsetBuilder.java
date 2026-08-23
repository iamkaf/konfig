//? if >=1.21.11 {
package com.iamkaf.konfig.api.v1.fieldset;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Builds a flat repeatable fieldset declaration and its default entries.
 *
 * <p>This bridge API is experimental while Konfig's broader developer API is designed.</p>
 */
@ApiStatus.Experimental
public final class FieldsetBuilder {
    private final List<FieldsetField<?>> fields = new ArrayList<FieldsetField<?>>();
    private final List<FieldsetEntry> entries = new ArrayList<FieldsetEntry>();
    private final List<FieldsetSchema.EntryValidationRule> entryValidationRules =
            new ArrayList<FieldsetSchema.EntryValidationRule>();
    private FieldsetField<?> titleField;
    private FieldsetField<?> iconField;
    private FieldsetField<?> keyField;
    private final List<FieldsetField<?>> summaryFields = new ArrayList<FieldsetField<?>>();

    private FieldsetBuilder() {
    }

    public static FieldsetBuilder create() {
        return new FieldsetBuilder();
    }

    public FieldsetBuilder field(FieldsetField<?> field) {
        this.fields.add(Objects.requireNonNull(field, "field"));
        return this;
    }

    public FieldsetBuilder entry(FieldsetEntry entry) {
        this.entries.add(Objects.requireNonNull(entry, "entry"));
        return this;
    }

    /**
     * Chooses the field shown as each entry's title in generated editors.
     *
     * @param field a field declared by this builder
     * @return this builder
     */
    public FieldsetBuilder title(FieldsetField<?> field) {
        this.titleField = Objects.requireNonNull(field, "field");
        return this;
    }

    /**
     * Chooses the registry-backed field rendered as each entry's icon.
     *
     * @param field a registry string field declared by this builder
     * @return this builder
     */
    public FieldsetBuilder icon(FieldsetField<String> field) {
        Objects.requireNonNull(field, "field");
        if (field.kind() != FieldsetFieldKind.REGISTRY_STRING || field.registryKey().isEmpty()) {
            throw new IllegalArgumentException("Fieldset icons require a registry string field");
        }
        this.iconField = field;
        return this;
    }

    /**
     * Chooses the field that identifies replacement entries in generated views.
     *
     * <p>When a user entry has the same key value as a builtin entry, generated
     * editors show the user entry in its place. The builtin remains part of the
     * stored value and becomes visible again when the user entry is deleted.</p>
     *
     * @param field a field declared by this builder
     * @return this builder
     */
    public <T> FieldsetBuilder key(FieldsetField<T> field) {
        this.keyField = Objects.requireNonNull(field, "field");
        return this;
    }

    /**
     * Chooses the fields shown beneath each entry's title in generated editors.
     *
     * @param fields fields declared by this builder
     * @return this builder
     */
    public FieldsetBuilder summary(FieldsetField<?>... fields) {
        Objects.requireNonNull(fields, "fields");
        for (FieldsetField<?> field : fields) {
            this.summaryFields.add(Objects.requireNonNull(field, "field"));
        }
        return this;
    }

    /**
     * Adds validation that can compare several fields in the same entry.
     *
     * @param validator predicate that accepts valid entries
     * @param message error shown when the predicate rejects an entry
     * @return this builder
     */
    public FieldsetBuilder validate(Predicate<FieldsetEntry> validator, String message) {
        Objects.requireNonNull(validator, "validator");
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("validation message cannot be blank");
        }
        this.entryValidationRules.add(new FieldsetSchema.EntryValidationRule(validator, message.trim()));
        return this;
    }

    public FieldsetValue build() {
        FieldsetSchema schema = new FieldsetSchema(
                this.fields,
                this.entryValidationRules,
                this.titleField,
                this.iconField,
                this.keyField,
                this.summaryFields
        );
        return FieldsetValue.of(schema, this.entries);
    }
}
//?}
