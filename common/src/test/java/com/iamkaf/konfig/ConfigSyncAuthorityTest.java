package com.iamkaf.konfig;

import com.iamkaf.konfig.impl.v1.sync.ConfigEditRequest;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditResult;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditSnapshot;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditStatus;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditTarget;
import com.iamkaf.konfig.impl.v1.sync.ConfigSyncAuthority;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ConfigSyncAuthorityTest {
    @Test
    void acceptedEditReturnsTheNewAuthoritativeSnapshot() {
        ConfigSyncAuthority authority = new ConfigSyncAuthority();
        MutableTarget target = new MutableTarget("headless:rules", 3L, "{\"value\":1}");
        authority.register(target);

        ConfigEditResult result = authority.apply(
                new ConfigEditRequest(7L, target.configId(), 3L, "{\"value\":2}"),
                true,
                true
        );

        assertEquals(ConfigEditStatus.ACCEPTED, result.status());
        assertEquals(4L, result.revision());
        assertEquals("{\"value\":2}", result.snapshotJson());
        assertEquals(1, target.applyCount);
    }

    @Test
    void staleEditReturnsTheCurrentRevisionWithoutMutation() {
        ConfigSyncAuthority authority = new ConfigSyncAuthority();
        MutableTarget target = new MutableTarget("headless:rules", 4L, "{\"value\":2}");
        authority.register(target);

        ConfigEditResult result = authority.apply(
                new ConfigEditRequest(8L, target.configId(), 3L, "{\"value\":3}"),
                true,
                true
        );

        assertEquals(ConfigEditStatus.STALE, result.status());
        assertEquals(4L, result.revision());
        assertEquals("{\"value\":2}", result.snapshotJson());
        assertEquals("{\"value\":2}", target.snapshotJson());
    }

    @Test
    void unauthorizedAndUnnegotiatedEditsNeverReachTheTarget() {
        ConfigSyncAuthority authority = new ConfigSyncAuthority();
        MutableTarget target = new MutableTarget("headless:rules", 0L, "{}");
        authority.register(target);
        ConfigEditRequest request = new ConfigEditRequest(1L, target.configId(), 0L, "{\"value\":1}");

        assertEquals(ConfigEditStatus.UNSUPPORTED, authority.apply(request, false, true).status());
        assertEquals(ConfigEditStatus.UNAUTHORIZED, authority.apply(request, true, false).status());
        assertEquals(0, target.applyCount);
    }

    @Test
    void invalidRequestMetadataAndConfigIdsAreRejected() {
        ConfigSyncAuthority authority = new ConfigSyncAuthority();

        assertEquals(
                ConfigEditStatus.INVALID,
                authority.apply(new ConfigEditRequest(-1L, "headless:rules", 0L, "{}"), true, true).status()
        );
        assertEquals(
                ConfigEditStatus.INVALID,
                authority.apply(new ConfigEditRequest(1L, "headless:rules", -1L, "{}"), true, true).status()
        );
        assertEquals(
                ConfigEditStatus.INVALID,
                authority.apply(new ConfigEditRequest(1L, "Not A Config", 0L, "{}"), true, true).status()
        );
    }

    @Test
    void unknownConfigIsRejectedAfterCapabilityAndPermissionChecks() {
        ConfigSyncAuthority authority = new ConfigSyncAuthority();
        ConfigEditResult result = authority.apply(
                new ConfigEditRequest(1L, "headless:missing", 0L, "{}"),
                true,
                true
        );

        assertEquals(ConfigEditStatus.UNKNOWN_CONFIG, result.status());
        assertEquals(-1L, result.revision());
    }

    @Test
    void oversizedDraftIsRejectedBeforeTargetLookup() {
        ConfigSyncAuthority authority = new ConfigSyncAuthority();
        String oversized = "x".repeat(ConfigSyncAuthority.MAX_JSON_LENGTH + 1);

        ConfigEditResult result = authority.apply(
                new ConfigEditRequest(1L, "headless:missing", 0L, oversized),
                true,
                true
        );

        assertEquals(ConfigEditStatus.TOO_LARGE, result.status());
    }

    @Test
    void noOpPreservesRevisionAndReturnsAuthoritativeSnapshot() {
        ConfigSyncAuthority authority = new ConfigSyncAuthority();
        MutableTarget target = new MutableTarget("headless:rules", 5L, "{\"value\":2}");
        authority.register(target);

        ConfigEditResult result = authority.apply(
                new ConfigEditRequest(9L, target.configId(), 5L, target.snapshotJson()),
                true,
                true
        );

        assertEquals(ConfigEditStatus.NO_OP, result.status());
        assertEquals(5L, result.revision());
        assertEquals(target.snapshotJson(), result.snapshotJson());
    }

    @Test
    void duplicateTargetRegistrationIsRejectedButReregisteringTheSameTargetIsSafe() {
        ConfigSyncAuthority authority = new ConfigSyncAuthority();
        MutableTarget first = new MutableTarget("headless:rules", 0L, "{}");
        MutableTarget second = new MutableTarget("headless:rules", 0L, "{}");

        authority.register(first);
        authority.register(first);
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> authority.register(second));
        assertEquals("Remote config target already registered: headless:rules", failure.getMessage());
        assertEquals(first.snapshotJson(), authority.snapshot("headless:rules").jsonPayload());
    }

    @Test
    void thrownTargetApplyBecomesInvalidWithoutChangingItsSnapshot() {
        ConfigSyncAuthority authority = new ConfigSyncAuthority();
        MutableTarget target = new MutableTarget("headless:rules", 2L, "{\"value\":1}");
        target.throwOnApply = true;
        authority.register(target);

        ConfigEditResult result = authority.apply(
                new ConfigEditRequest(4L, target.configId(), 2L, "{\"value\":2}"),
                true,
                true
        );

        assertEquals(ConfigEditStatus.INVALID, result.status());
        assertEquals(2L, result.revision());
        assertEquals("{\"value\":1}", target.snapshotJson());
    }

    @Test
    void snapshotsAreSortedByConfigIdentity() {
        ConfigSyncAuthority authority = new ConfigSyncAuthority();
        authority.register(new MutableTarget("headless:zeta", 2L, "{\"z\":1}"));
        authority.register(new MutableTarget("headless:alpha", 1L, "{\"a\":1}"));

        List<ConfigEditSnapshot> snapshots = authority.snapshots();
        assertEquals("headless:alpha", snapshots.get(0).configId());
        assertEquals("headless:zeta", snapshots.get(1).configId());
    }

    private static final class MutableTarget implements ConfigEditTarget {
        private final String configId;
        private long revision;
        private String snapshot;
        private int applyCount;
        private boolean throwOnApply;

        private MutableTarget(String configId, long revision, String snapshot) {
            this.configId = configId;
            this.revision = revision;
            this.snapshot = snapshot;
        }

        @Override
        public String configId() {
            return this.configId;
        }

        @Override
        public long currentRevision() {
            return this.revision;
        }

        @Override
        public String snapshotJson() {
            return this.snapshot;
        }

        @Override
        public ApplyResult applyAtomic(long baseRevision, String completeDraftJson) {
            this.applyCount++;
            if (this.throwOnApply) {
                throw new IllegalStateException("apply failed");
            }
            if (baseRevision != this.revision) {
                return ApplyResult.stale(this.revision, this.snapshot);
            }
            if (this.snapshot.equals(completeDraftJson)) {
                return ApplyResult.noOp(this.revision, this.snapshot);
            }
            this.snapshot = completeDraftJson;
            this.revision++;
            return ApplyResult.accepted(this.revision, this.snapshot);
        }
    }
}
