//? if >=1.21.11 {
package com.iamkaf.konfig.api.v1.fieldset;

import org.jetbrains.annotations.ApiStatus;

/**
 * Scalar controls supported inside a fieldset entry.
 */
@ApiStatus.Experimental
public enum FieldsetFieldKind {
    BOOLEAN,
    INTEGER,
    LONG,
    DOUBLE,
    STRING,
    OPTIONAL_STRING,
    DROPDOWN,
    REGISTRY_STRING
}
//?}
