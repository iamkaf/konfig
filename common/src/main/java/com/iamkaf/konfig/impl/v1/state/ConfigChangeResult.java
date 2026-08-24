//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.state;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public record ConfigChangeResult(
        Status status,
        long revision,
        List<String> changedFields,
        ConfigValidation validation,
        String message
) {
    public ConfigChangeResult {
        status = Objects.requireNonNull(status, "status");
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        changedFields = List.copyOf(changedFields);
        validation = Objects.requireNonNull(validation, "validation");
        message = message == null ? "" : message.trim();
    }

    public boolean accepted() {
        return this.status == Status.ACCEPTED || this.status == Status.NO_OP;
    }

    public enum Status {
        ACCEPTED,
        NO_OP,
        PENDING,
        REJECTED_VALIDATION,
        REJECTED_PERMISSION,
        REJECTED_STALE,
        REJECTED_CLOSED,
        FAILED
    }
}
//?}
