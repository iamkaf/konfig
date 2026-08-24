package com.iamkaf.konfig.impl.v1.sync;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public interface ConfigEditTarget {
    String configId();

    long currentRevision();

    String snapshotJson();

    ApplyResult applyAtomic(long baseRevision, String completeDraftJson);

    final class ApplyResult {
        private final ConfigEditStatus status;
        private final long revision;
        private final String snapshotJson;
        private final String detail;

        private ApplyResult(ConfigEditStatus status, long revision, String snapshotJson, String detail) {
            if (status != ConfigEditStatus.ACCEPTED
                    && status != ConfigEditStatus.STALE
                    && status != ConfigEditStatus.INVALID
                    && status != ConfigEditStatus.NO_OP) {
                throw new IllegalArgumentException("Target cannot return transport status " + status);
            }
            this.status = Objects.requireNonNull(status, "status");
            this.revision = revision;
            this.snapshotJson = snapshotJson == null ? "" : snapshotJson;
            this.detail = detail == null ? "" : detail;
        }

        public static ApplyResult accepted(long revision, String snapshotJson) {
            return new ApplyResult(ConfigEditStatus.ACCEPTED, revision, snapshotJson, "");
        }

        public static ApplyResult stale(long revision, String snapshotJson) {
            return new ApplyResult(ConfigEditStatus.STALE, revision, snapshotJson, "stale revision");
        }

        public static ApplyResult invalid(long revision, String detail) {
            return new ApplyResult(ConfigEditStatus.INVALID, revision, "", detail);
        }

        public static ApplyResult noOp(long revision, String snapshotJson) {
            return new ApplyResult(ConfigEditStatus.NO_OP, revision, snapshotJson, "no changes");
        }

        public ConfigEditStatus status() {
            return this.status;
        }

        public long revision() {
            return this.revision;
        }

        public String snapshotJson() {
            return this.snapshotJson;
        }

        public String detail() {
            return this.detail;
        }
    }
}
