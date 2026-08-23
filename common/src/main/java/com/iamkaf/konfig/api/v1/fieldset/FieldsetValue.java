//? if >=1.21.11 {
package com.iamkaf.konfig.api.v1.fieldset;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable ordered collection of structured fieldset entries.
 */
@ApiStatus.Experimental
public final class FieldsetValue {
    private final FieldsetSchema schema;
    private final List<FieldsetEntry> entries;

    private FieldsetValue(FieldsetSchema schema, List<FieldsetEntry> entries) {
        this.schema = Objects.requireNonNull(schema, "schema");
        ArrayList<FieldsetEntry> copied = new ArrayList<FieldsetEntry>(entries.size());
        LinkedHashSet<String> identities = new LinkedHashSet<String>();
        for (FieldsetEntry entry : entries) {
            Objects.requireNonNull(entry, "entry");
            this.schema.requireShape(entry);
            if (!identities.add(entry.identity())) {
                throw new IllegalArgumentException("Duplicate fieldset entry identity: " + entry.identity());
            }
            copied.add(entry);
        }
        this.entries = Collections.unmodifiableList(copied);
    }

    public static FieldsetValue empty(FieldsetSchema schema) {
        return new FieldsetValue(schema, Collections.emptyList());
    }

    public static FieldsetValue of(FieldsetSchema schema, List<FieldsetEntry> entries) {
        Objects.requireNonNull(entries, "entries");
        return new FieldsetValue(schema, entries);
    }

    public FieldsetSchema schema() {
        return this.schema;
    }

    public List<FieldsetEntry> entries() {
        return this.entries;
    }

    /**
     * Returns entries after applying keyed user replacements to builtins.
     *
     * <p>Without a declared key field this is the same list returned by
     * {@link #entries()}.</p>
     *
     * @return the entries shown by generated editors and search
     */
    public List<FieldsetEntry> visibleEntries() {
        Optional<FieldsetField<?>> keyField = this.schema.keyField();
        if (keyField.isEmpty()) {
            return this.entries;
        }

        LinkedHashSet<Object> userKeys = new LinkedHashSet<Object>();
        for (FieldsetEntry entry : this.entries) {
            if (entry.editable()) {
                userKeys.add(entryValue(entry, keyField.get()));
            }
        }
        if (userKeys.isEmpty()) {
            return this.entries;
        }

        ArrayList<FieldsetEntry> visible = new ArrayList<FieldsetEntry>(this.entries.size());
        for (FieldsetEntry entry : this.entries) {
            if (!entry.editable() && userKeys.contains(entryValue(entry, keyField.get()))) {
                continue;
            }
            visible.add(entry);
        }
        return visible.size() == this.entries.size()
                ? this.entries
                : Collections.unmodifiableList(visible);
    }

    public Optional<FieldsetEntry> entry(String identity) {
        int index = indexOf(identity);
        return index < 0 ? Optional.empty() : Optional.of(this.entries.get(index));
    }

    /**
     * Appends an entry. Both builtin defaults and user entries can be assembled this way.
     *
     * @param entry the entry to append
     * @return the changed value
     */
    public FieldsetValue add(FieldsetEntry entry) {
        Objects.requireNonNull(entry, "entry");
        if (indexOf(entry.identity()) >= 0) {
            throw new IllegalArgumentException("Duplicate fieldset entry identity: " + entry.identity());
        }
        ArrayList<FieldsetEntry> changed = mutableEntries();
        changed.add(entry);
        return new FieldsetValue(this.schema, changed);
    }

    /**
     * Replaces a user-owned entry without changing its stable identity.
     *
     * @param replacement the replacement entry
     * @return the changed value
     */
    public FieldsetValue replaceUserEntry(FieldsetEntry replacement) {
        Objects.requireNonNull(replacement, "replacement");
        int index = requireEntryIndex(replacement.identity());
        FieldsetEntry current = this.entries.get(index);
        requireEditable(current);
        if (replacement.ownership() != FieldsetEntryOwnership.USER) {
            throw new IllegalArgumentException("Replacement entry must remain user-owned: " + replacement.identity());
        }
        ArrayList<FieldsetEntry> changed = mutableEntries();
        changed.set(index, replacement);
        return new FieldsetValue(this.schema, changed);
    }

    /**
     * Copies any entry into a new user-owned entry.
     *
     * @param sourceIdentity identity of the entry to copy
     * @param newIdentity stable identity for the copy
     * @return the changed value
     */
    public FieldsetValue duplicateAsUser(String sourceIdentity, String newIdentity) {
        int sourceIndex = requireEntryIndex(sourceIdentity);
        if (indexOf(newIdentity) >= 0) {
            throw new IllegalArgumentException("Duplicate fieldset entry identity: " + newIdentity);
        }
        ArrayList<FieldsetEntry> changed = mutableEntries();
        changed.add(sourceIndex + 1, this.entries.get(sourceIndex).copyAsUser(newIdentity));
        return new FieldsetValue(this.schema, changed);
    }

    public FieldsetValue deleteUserEntry(String identity) {
        int index = requireEntryIndex(identity);
        requireEditable(this.entries.get(index));
        ArrayList<FieldsetEntry> changed = mutableEntries();
        changed.remove(index);
        return new FieldsetValue(this.schema, changed);
    }

    /**
     * Moves a user entry among the other user entries while builtin positions stay fixed.
     *
     * @param identity entry to move
     * @param newUserIndex zero-based position among user entries
     * @return the changed value
     */
    public FieldsetValue moveUserEntry(String identity, int newUserIndex) {
        int sourceIndex = requireEntryIndex(identity);
        FieldsetEntry source = this.entries.get(sourceIndex);
        requireEditable(source);

        ArrayList<FieldsetEntry> users = new ArrayList<FieldsetEntry>();
        for (FieldsetEntry entry : this.entries) {
            if (entry.editable()) {
                users.add(entry);
            }
        }
        if (newUserIndex < 0 || newUserIndex >= users.size()) {
            throw new IndexOutOfBoundsException("User entry index out of range: " + newUserIndex);
        }
        int oldUserIndex = users.indexOf(source);
        if (oldUserIndex == newUserIndex) {
            return this;
        }
        users.remove(oldUserIndex);
        users.add(newUserIndex, source);

        ArrayList<FieldsetEntry> changed = new ArrayList<FieldsetEntry>(this.entries.size());
        int userIndex = 0;
        for (FieldsetEntry entry : this.entries) {
            changed.add(entry.editable() ? users.get(userIndex++) : entry);
        }
        return new FieldsetValue(this.schema, changed);
    }

    /**
     * Finds entries whose identity or scalar field text contains the query.
     *
     * @param query case-insensitive search text
     * @return matching entries in fieldset order
     */
    public List<FieldsetEntry> search(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<FieldsetEntry> visible = this.visibleEntries();
        if (needle.isEmpty()) {
            return visible;
        }
        ArrayList<FieldsetEntry> matches = new ArrayList<FieldsetEntry>();
        for (FieldsetEntry entry : visible) {
            if (matches(entry, needle)) {
                matches.add(entry);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    public FieldsetValidation validate() {
        ArrayList<FieldsetValidationIssue> issues = new ArrayList<FieldsetValidationIssue>();
        for (FieldsetEntry entry : this.entries) {
            issues.addAll(this.schema.validate(entry).issues());
        }
        return FieldsetValidation.of(issues);
    }

    private boolean matches(FieldsetEntry entry, String needle) {
        if (entry.identity().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        if (entry.source()
                .map(source -> source.toLowerCase(Locale.ROOT).contains(needle))
                .orElse(false)) {
            return true;
        }
        for (FieldsetField<?> field : this.schema.fields()) {
            Object value = entryValue(entry, field);
            if (value instanceof Optional<?>) {
                Optional<?> optional = (Optional<?>) value;
                value = optional.isPresent() ? optional.get() : "";
            }
            if (String.valueOf(value).toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static Object entryValue(FieldsetEntry entry, FieldsetField<?> field) {
        return capturedEntryValue(entry, field);
    }

    private static <T> T capturedEntryValue(FieldsetEntry entry, FieldsetField<T> field) {
        return entry.value(field);
    }

    private int indexOf(String identity) {
        if (identity == null) {
            return -1;
        }
        for (int index = 0; index < this.entries.size(); index++) {
            if (this.entries.get(index).identity().equals(identity)) {
                return index;
            }
        }
        return -1;
    }

    private int requireEntryIndex(String identity) {
        int index = indexOf(identity);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown fieldset entry identity: " + identity);
        }
        return index;
    }

    private static void requireEditable(FieldsetEntry entry) {
        if (!entry.editable()) {
            throw new IllegalStateException("Builtin fieldset entry is read-only: " + entry.identity());
        }
    }

    private ArrayList<FieldsetEntry> mutableEntries() {
        return new ArrayList<FieldsetEntry>(this.entries);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FieldsetValue)) {
            return false;
        }
        FieldsetValue that = (FieldsetValue) other;
        return this.schema == that.schema && this.entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(this.schema) + this.entries.hashCode();
    }

    @Override
    public String toString() {
        return "FieldsetValue" + this.entries;
    }
}
//?}
