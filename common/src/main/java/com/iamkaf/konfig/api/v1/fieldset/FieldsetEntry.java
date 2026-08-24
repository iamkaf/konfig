//? if >=1.21.11 {
package com.iamkaf.konfig.api.v1.fieldset;

import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One immutable, stably identified row in a fieldset value.
 */
@ApiStatus.Experimental
public final class FieldsetEntry {
    private final String identity;
    private final FieldsetEntryOwnership ownership;
    private final String source;
    private final Map<String, Object> values;

    private FieldsetEntry(
            String identity,
            FieldsetEntryOwnership ownership,
            String source,
            Map<String, Object> values
    ) {
        this.identity = requireIdentity(identity);
        this.ownership = Objects.requireNonNull(ownership, "ownership");
        this.source = requireSource(ownership, source);
        this.values = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(values));
    }

    public static FieldsetEntry builtin(String identity) {
        return new FieldsetEntry(identity, FieldsetEntryOwnership.BUILTIN, null, Collections.emptyMap());
    }

    /**
     * Creates a read-only entry with the source shown by generated editors.
     *
     * @param identity stable internal identity
     * @param source owner or compatibility source, such as {@code Bonded}
     * @return a new empty builtin entry
     */
    public static FieldsetEntry builtin(String identity, String source) {
        return new FieldsetEntry(identity, FieldsetEntryOwnership.BUILTIN, source, Collections.emptyMap());
    }

    public static FieldsetEntry user(String identity) {
        return new FieldsetEntry(identity, FieldsetEntryOwnership.USER, null, Collections.emptyMap());
    }

    /**
     * Creates a user entry with a generated stable identity.
     *
     * @return a new empty user entry
     */
    public static FieldsetEntry newUser() {
        return user(UUID.randomUUID().toString());
    }

    public String identity() {
        return this.identity;
    }

    public FieldsetEntryOwnership ownership() {
        return this.ownership;
    }

    public Optional<String> source() {
        return Optional.ofNullable(this.source);
    }

    public boolean editable() {
        return this.ownership.editable();
    }

    /**
     * Reads this entry's value or the field declaration's default.
     *
     * @param field the typed field declaration
     * @param <T> the field value type
     * @return the stored or default value
     */
    public <T> T value(FieldsetField<T> field) {
        Objects.requireNonNull(field, "field");
        Object value = this.values.get(field.key());
        if (value == null) {
            return field.defaultValue();
        }
        field.requireValueType(value);
        @SuppressWarnings("unchecked")
        T typed = (T) value;
        return typed;
    }

    /**
     * Returns a copy with one field changed.
     *
     * <p>This method also supports draft values that fail semantic validation. It rejects
     * only values of the wrong Java type. Call {@link FieldsetSchema#validate(FieldsetEntry)}
     * before saving.</p>
     *
     * @param field the field to change
     * @param value the new typed value
     * @param <T> the field value type
     * @return the changed entry
     */
    public <T> FieldsetEntry with(FieldsetField<T> field, T value) {
        return withScalar(field, value);
    }

    /**
     * Changes a scalar after runtime type checking.
     *
     * <p>This is the heterogeneous edit boundary used by generated editors that iterate
     * {@link FieldsetSchema#fields()}.</p>
     *
     * @param field the field to change
     * @param value the new scalar value
     * @return the changed entry
     */
    public FieldsetEntry withScalar(FieldsetField<?> field, Object value) {
        Objects.requireNonNull(field, "field");
        field.requireValueType(value);
        LinkedHashMap<String, Object> changed = new LinkedHashMap<String, Object>(this.values);
        if (field.kind() == FieldsetFieldKind.OPTIONAL_STRING && !((Optional<?>) value).isPresent()) {
            changed.remove(field.key());
        } else {
            changed.put(field.key(), value);
        }
        return new FieldsetEntry(this.identity, this.ownership, this.source, changed);
    }

    Map<String, Object> explicitValues() {
        return this.values;
    }

    FieldsetEntry copyAsUser(String identity) {
        return new FieldsetEntry(identity, FieldsetEntryOwnership.USER, null, this.values);
    }

    static FieldsetEntry decoded(String identity, FieldsetEntryOwnership ownership, Map<String, Object> values) {
        return new FieldsetEntry(identity, ownership, null, values);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FieldsetEntry)) {
            return false;
        }
        FieldsetEntry that = (FieldsetEntry) other;
        return this.identity.equals(that.identity)
                && this.ownership == that.ownership
                && Objects.equals(this.source, that.source)
                && this.values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.identity, this.ownership, this.source, this.values);
    }

    @Override
    public String toString() {
        if (this.source == null) {
            return "FieldsetEntry{" + this.identity + ", " + this.ownership + ", " + this.values + '}';
        }
        return "FieldsetEntry{" + this.identity + ", " + this.ownership + ", " + this.source + ", " + this.values + '}';
    }

    private static String requireIdentity(String identity) {
        if (identity == null || identity.trim().isEmpty()) {
            throw new IllegalArgumentException("Fieldset entry identity cannot be blank");
        }
        return identity.trim();
    }

    private static String requireSource(FieldsetEntryOwnership ownership, String source) {
        if (source == null) {
            return null;
        }
        if (ownership != FieldsetEntryOwnership.BUILTIN) {
            throw new IllegalArgumentException("Only builtin fieldset entries can declare a source");
        }
        if (source.trim().isEmpty()) {
            throw new IllegalArgumentException("Fieldset entry source cannot be blank");
        }
        return source.trim();
    }
}
//?}
