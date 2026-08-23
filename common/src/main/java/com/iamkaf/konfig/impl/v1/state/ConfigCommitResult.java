//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.state;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public sealed interface ConfigCommitResult permits ConfigCommitResult.Accepted, ConfigCommitResult.NoOp, ConfigCommitResult.Pending, ConfigCommitResult.Stale, ConfigCommitResult.Rejected, ConfigCommitResult.Failed {
    record Accepted(long revision) implements ConfigCommitResult {
        public Accepted {
            requireRevision(revision);
        }
    }

    record NoOp(long revision) implements ConfigCommitResult {
        public NoOp {
            requireRevision(revision);
        }
    }

    record Pending(long requestId) implements ConfigCommitResult {
        public Pending {
            if (requestId < 0L) {
                throw new IllegalArgumentException("requestId must be non-negative");
            }
        }
    }

    record Stale(long currentRevision, String message) implements ConfigCommitResult {
        public Stale {
            requireRevision(currentRevision);
            message = requireMessage(message);
        }
    }

    record Rejected(Reason reason, ConfigValidation validation, String message) implements ConfigCommitResult {
        public Rejected {
            reason = Objects.requireNonNull(reason, "reason");
            validation = Objects.requireNonNull(validation, "validation");
            message = requireMessage(message);
        }
    }

    enum Reason {
        VALIDATION,
        PERMISSION,
        READ_ONLY,
        UNSUPPORTED
    }

    record Failed(String message, Throwable cause) implements ConfigCommitResult {
        public Failed {
            message = requireMessage(message);
            cause = Objects.requireNonNull(cause, "cause");
        }
    }

    private static void requireRevision(long revision) {
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
    }

    private static String requireMessage(String message) {
        String normalized = Objects.requireNonNull(message, "message").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("message cannot be blank");
        }
        return normalized;
    }
}
//?}
