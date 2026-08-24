package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public enum ConfigFieldKind {
    BOOLEAN,
    INTEGER,
    LONG,
    DOUBLE,
    STRING,
    STRING_LIST,
    DROPDOWN,
    ENUM,
    COLOR_RGB,
    COLOR_ARGB,
//? if >=1.21.11 {
    FIELDSET,
//?}
    CUSTOM
}
