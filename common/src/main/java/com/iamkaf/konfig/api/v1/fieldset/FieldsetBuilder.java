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
        FieldsetSchema schema = new FieldsetSchema(this.fields, this.entryValidationRules);
        return FieldsetValue.of(schema, this.entries);
    }
}
//?}
