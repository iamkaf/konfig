//? if >=1.21.11 {
package com.iamkaf.konfig.api.v1.fieldset;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Declarative presentation for browsing a large Fieldset by source profile.
 *
 * <p>Catalogs use normal Fieldset fields and controls. They do not expose
 * screen, widget, or renderer implementations.</p>
 */
@ApiStatus.Experimental
public final class FieldsetCatalog {
    private final String editableProfileLabel;
    private final String newEntryLabel;
    private final String overrideLabel;
    private final String duplicateLabel;
    private final String deleteLabel;
    private final FieldsetField<?> filterField;
    private final List<Section> sections;
    private final Function<FieldsetEntry, Optional<String>> warning;

    private FieldsetCatalog(Builder builder) {
        this.editableProfileLabel = builder.editableProfileLabel;
        this.newEntryLabel = builder.newEntryLabel;
        this.overrideLabel = builder.overrideLabel;
        this.duplicateLabel = builder.duplicateLabel;
        this.deleteLabel = builder.deleteLabel;
        this.filterField = builder.filterField;
        this.sections = Collections.unmodifiableList(new ArrayList<Section>(builder.sections));
        this.warning = builder.warning;
    }

    public static Builder create() {
        return new Builder();
    }

    public String editableProfileLabel() {
        return this.editableProfileLabel;
    }

    public String newEntryLabel() {
        return this.newEntryLabel;
    }

    public String overrideLabel() {
        return this.overrideLabel;
    }

    public String duplicateLabel() {
        return this.duplicateLabel;
    }

    public String deleteLabel() {
        return this.deleteLabel;
    }

    public Optional<FieldsetField<?>> filterField() {
        return Optional.ofNullable(this.filterField);
    }

    public List<Section> sections() {
        return this.sections;
    }

    /**
     * Returns a non-blocking status message for an entry, when one applies.
     *
     * @param entry the entry being presented
     * @return the status message
     */
    public Optional<String> warning(FieldsetEntry entry) {
        return this.warning.apply(Objects.requireNonNull(entry, "entry"));
    }

    void requireDeclaredFields(Map<String, FieldsetField<?>> fieldsByKey) {
        requireDeclared(this.filterField, fieldsByKey, "filter");
        LinkedHashSet<FieldsetField<?>> sectionFields = new LinkedHashSet<FieldsetField<?>>();
        for (Section section : this.sections) {
            for (FieldsetField<?> field : section.fields) {
                FieldsetField<?> declared = requireDeclared(field, fieldsByKey, "section");
                if (!sectionFields.add(declared)) {
                    throw new IllegalStateException("Duplicate Fieldset catalog section field: " + field.key());
                }
            }
        }
    }

    private static FieldsetField<?> requireDeclared(
            FieldsetField<?> field,
            Map<String, FieldsetField<?>> fieldsByKey,
            String role
    ) {
        if (field == null) {
            return null;
        }
        if (fieldsByKey.get(field.key()) != field) {
            throw new IllegalStateException("Fieldset catalog " + role + " field is not declared: " + field.key());
        }
        return field;
    }

    public static final class Builder {
        private String editableProfileLabel = "User Entries";
        private String newEntryLabel = "New Entry";
        private String overrideLabel = "Override";
        private String duplicateLabel = "Duplicate";
        private String deleteLabel = "Delete";
        private FieldsetField<?> filterField;
        private final List<Section> sections = new ArrayList<Section>();
        private Function<FieldsetEntry, Optional<String>> warning = ignored -> Optional.empty();

        private Builder() {
        }

        public Builder editableProfile(String label) {
            this.editableProfileLabel = requireLabel(label, "editable profile label");
            return this;
        }

        public Builder newEntryLabel(String label) {
            this.newEntryLabel = requireLabel(label, "new entry label");
            return this;
        }

        public Builder overrideLabel(String label) {
            this.overrideLabel = requireLabel(label, "override label");
            return this;
        }

        public Builder duplicateLabel(String label) {
            this.duplicateLabel = requireLabel(label, "duplicate label");
            return this;
        }

        public Builder deleteLabel(String label) {
            this.deleteLabel = requireLabel(label, "delete label");
            return this;
        }

        public Builder filter(FieldsetField<?> field) {
            this.filterField = Objects.requireNonNull(field, "field");
            return this;
        }

        public Builder section(String label, FieldsetField<?>... fields) {
            Objects.requireNonNull(fields, "fields");
            if (fields.length == 0) {
                throw new IllegalArgumentException("Fieldset catalog sections require at least one field");
            }
            ArrayList<FieldsetField<?>> declared = new ArrayList<FieldsetField<?>>(fields.length);
            for (FieldsetField<?> field : fields) {
                declared.add(Objects.requireNonNull(field, "field"));
            }
            this.sections.add(new Section(requireLabel(label, "section label"), declared));
            return this;
        }

        /** Adds a non-blocking status message to matching catalog entries. */
        public Builder warning(Function<FieldsetEntry, Optional<String>> warning) {
            this.warning = Objects.requireNonNull(warning, "warning");
            return this;
        }

        public FieldsetCatalog build() {
            return new FieldsetCatalog(this);
        }
    }

    public static final class Section {
        private final String label;
        private final List<FieldsetField<?>> fields;

        private Section(String label, List<FieldsetField<?>> fields) {
            this.label = label;
            this.fields = Collections.unmodifiableList(new ArrayList<FieldsetField<?>>(fields));
        }

        public String label() {
            return this.label;
        }

        public List<FieldsetField<?>> fields() {
            return this.fields;
        }
    }

    private static String requireLabel(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }
}
//?}
