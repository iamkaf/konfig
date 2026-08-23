//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.fieldset.FieldsetValue;

import java.util.Objects;

@ApiStatus.Internal
final class KonfigFieldsetDraftSession {
    private FieldsetValue original;
    private FieldsetValue draft;

    KonfigFieldsetDraftSession(FieldsetValue value) {
        this.original = Objects.requireNonNull(value, "value");
        this.draft = value;
    }

    FieldsetValue original() {
        return this.original;
    }

    FieldsetValue draft() {
        return this.draft;
    }

    boolean update(FieldsetValue value) {
        FieldsetValue next = Objects.requireNonNull(value, "value");
        if (this.draft.equals(next)) {
            return false;
        }
        this.draft = next;
        return true;
    }

    boolean dirty() {
        return !this.original.equals(this.draft);
    }

    void markPersisted() {
        this.original = this.draft;
    }

    void adoptPersisted(FieldsetValue value) {
        FieldsetValue persisted = Objects.requireNonNull(value, "value");
        this.original = persisted;
        this.draft = persisted;
    }

    void restorePersisted() {
        this.draft = this.original;
    }
}
//?}
