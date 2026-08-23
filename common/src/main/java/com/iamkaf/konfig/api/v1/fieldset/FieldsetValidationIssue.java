//? if >=1.21.11 {
package com.iamkaf.konfig.api.v1.fieldset;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.Optional;

/**
 * One fieldset validation failure, scoped to an entry and optionally a field.
 */
@ApiStatus.Experimental
public final class FieldsetValidationIssue {
    private final String entryIdentity;
    private final String fieldKey;
    private final String message;

    private FieldsetValidationIssue(String entryIdentity, String fieldKey, String message) {
        this.entryIdentity = Objects.requireNonNull(entryIdentity, "entryIdentity");
        this.fieldKey = fieldKey;
        this.message = Objects.requireNonNull(message, "message");
    }

    public static FieldsetValidationIssue entry(String entryIdentity, String message) {
        return new FieldsetValidationIssue(entryIdentity, null, message);
    }

    public static FieldsetValidationIssue field(String entryIdentity, String fieldKey, String message) {
        return new FieldsetValidationIssue(entryIdentity, Objects.requireNonNull(fieldKey, "fieldKey"), message);
    }

    public String entryIdentity() {
        return this.entryIdentity;
    }

    public Optional<String> fieldKey() {
        return Optional.ofNullable(this.fieldKey);
    }

    public String message() {
        return this.message;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FieldsetValidationIssue)) {
            return false;
        }
        FieldsetValidationIssue that = (FieldsetValidationIssue) other;
        return this.entryIdentity.equals(that.entryIdentity)
                && Objects.equals(this.fieldKey, that.fieldKey)
                && this.message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.entryIdentity, this.fieldKey, this.message);
    }
}
//?}
