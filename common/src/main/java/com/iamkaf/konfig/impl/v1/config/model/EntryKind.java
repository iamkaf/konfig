package com.iamkaf.konfig.impl.v1.config.model;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public enum EntryKind {
    HEADER,
    IMAGE,
    INLINE_TEXT,
    URL,
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
    CUSTOM,
//? if >=1.21.11
    FIELDSET,
}
