//? if >=1.21.11 {
package com.iamkaf.konfig.api.v1.fieldset;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The ordered field declarations and entry-wide rules for a flat fieldset.
 */
@ApiStatus.Experimental
public final class FieldsetSchema {
    private final List<FieldsetField<?>> fields;
    private final Map<String, FieldsetField<?>> fieldsByKey;
    private final List<EntryValidationRule> entryValidationRules;

    FieldsetSchema(List<FieldsetField<?>> fields, List<EntryValidationRule> entryValidationRules) {
        if (fields.isEmpty()) {
            throw new IllegalStateException("A fieldset must declare at least one field");
        }

        ArrayList<FieldsetField<?>> ordered = new ArrayList<FieldsetField<?>>(fields.size());
        LinkedHashMap<String, FieldsetField<?>> byKey = new LinkedHashMap<String, FieldsetField<?>>();
        for (FieldsetField<?> field : fields) {
            Objects.requireNonNull(field, "field");
            if (byKey.put(field.key(), field) != null) {
                throw new IllegalStateException("Duplicate fieldset field key: " + field.key());
            }
            ordered.add(field);
        }
        this.fields = Collections.unmodifiableList(ordered);
        this.fieldsByKey = Collections.unmodifiableMap(byKey);
        this.entryValidationRules = Collections.unmodifiableList(new ArrayList<EntryValidationRule>(entryValidationRules));
    }

    public List<FieldsetField<?>> fields() {
        return this.fields;
    }

    public Optional<FieldsetField<?>> field(String key) {
        return Optional.ofNullable(this.fieldsByKey.get(key));
    }

    /**
     * Validates all declared fields and entry-wide rules for one entry.
     *
     * @param entry the entry to validate
     * @return every validation issue found
     */
    public FieldsetValidation validate(FieldsetEntry entry) {
        Objects.requireNonNull(entry, "entry");
        ArrayList<FieldsetValidationIssue> issues = new ArrayList<FieldsetValidationIssue>();
        for (String explicitKey : entry.explicitValues().keySet()) {
            if (!this.fieldsByKey.containsKey(explicitKey)) {
                issues.add(FieldsetValidationIssue.field(entry.identity(), explicitKey, "Unknown field"));
            }
        }
        for (FieldsetField<?> field : this.fields) {
            validateField(entry, field, issues);
        }
        for (EntryValidationRule rule : this.entryValidationRules) {
            if (!rule.validator.test(entry)) {
                issues.add(FieldsetValidationIssue.entry(entry.identity(), rule.message));
            }
        }
        return FieldsetValidation.of(issues);
    }

    void requireShape(FieldsetEntry entry) {
        for (Map.Entry<String, Object> value : entry.explicitValues().entrySet()) {
            FieldsetField<?> field = this.fieldsByKey.get(value.getKey());
            if (field == null) {
                throw new IllegalArgumentException(
                        "Unknown field " + value.getKey() + " in fieldset entry " + entry.identity()
                );
            }
            field.requireValueType(value.getValue());
        }
    }

    private static void validateField(
            FieldsetEntry entry,
            FieldsetField<?> field,
            List<FieldsetValidationIssue> issues
    ) {
        validateCapturedField(entry, field, issues);
    }

    private static <T> void validateCapturedField(
            FieldsetEntry entry,
            FieldsetField<T> field,
            List<FieldsetValidationIssue> issues
    ) {
        T value = entry.value(field);
        for (String message : field.validationMessages(value)) {
            issues.add(FieldsetValidationIssue.field(entry.identity(), field.key(), message));
        }
    }

    static final class EntryValidationRule {
        private final Predicate<FieldsetEntry> validator;
        private final String message;

        EntryValidationRule(Predicate<FieldsetEntry> validator, String message) {
            this.validator = validator;
            this.message = message;
        }
    }
}
//?}
