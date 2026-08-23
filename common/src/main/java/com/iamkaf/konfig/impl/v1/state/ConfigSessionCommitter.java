//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.state;

import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@ApiStatus.Internal
@FunctionalInterface
public interface ConfigSessionCommitter {
    ConfigCommitResult commit(CommitRequest request);

    record CommitRequest(String configId, long expectedRevision, Map<String, Object> values) {
        public CommitRequest {
            configId = requireText(configId, "configId");
            if (expectedRevision < 0L) {
                throw new IllegalArgumentException("expectedRevision must be non-negative");
            }
            values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }

        private static String requireText(String value, String name) {
            String normalized = Objects.requireNonNull(value, name).trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(name + " cannot be blank");
            }
            return normalized;
        }
    }
}
//?}
