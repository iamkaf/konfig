//? if >=1.21.11 {
package com.iamkaf.konfig.api.v1.fieldset;

import org.jetbrains.annotations.ApiStatus;

/**
 * Describes who may edit a fieldset entry.
 */
@ApiStatus.Experimental
public enum FieldsetEntryOwnership {
    /** An entry supplied by the owning mod and shown as read-only. */
    BUILTIN(false),
    /** An entry owned and editable by the user. */
    USER(true);

    private final boolean editable;

    FieldsetEntryOwnership(boolean editable) {
        this.editable = editable;
    }

    /**
     * Returns whether a fieldset editor may change this entry.
     *
     * @return {@code true} for user-owned entries
     */
    public boolean editable() {
        return this.editable;
    }
}
//?}
