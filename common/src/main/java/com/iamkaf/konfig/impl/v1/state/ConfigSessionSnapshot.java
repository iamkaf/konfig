//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.state;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public record ConfigSessionSnapshot(
        String configId,
        long revision,
        List<ConfigFieldState<?>> fields,
        boolean dirty,
        ConfigValidation validation,
        long pendingRequestId,
        boolean closed
) {
    public ConfigSessionSnapshot {
        configId = Objects.requireNonNull(configId, "configId");
        fields = List.copyOf(fields);
        validation = Objects.requireNonNull(validation, "validation");
        if (pendingRequestId < -1L) {
            throw new IllegalArgumentException("pendingRequestId must be -1 or non-negative");
        }
    }

    public boolean applyPending() {
        return this.pendingRequestId >= 0L;
    }
}
//?}
