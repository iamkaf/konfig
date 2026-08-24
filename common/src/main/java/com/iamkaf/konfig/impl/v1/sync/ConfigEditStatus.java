package com.iamkaf.konfig.impl.v1.sync;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public enum ConfigEditStatus {
    ACCEPTED,
    STALE,
    UNAUTHORIZED,
    INVALID,
    NO_OP,
    UNKNOWN_CONFIG,
    TOO_LARGE,
    READ_ONLY,
    UNSUPPORTED
}
