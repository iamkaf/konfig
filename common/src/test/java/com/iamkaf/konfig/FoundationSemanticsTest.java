//? if >=1.21.11 {
package com.iamkaf.konfig;

import com.iamkaf.konfig.impl.v1.state.ConfigChangeResult;
import com.iamkaf.konfig.impl.v1.state.ConfigCommitResult;
import com.iamkaf.konfig.impl.v1.state.ConfigMutation;
import com.iamkaf.konfig.impl.v1.state.ConfigSession;
import com.iamkaf.konfig.impl.v1.state.ConfigSessionField;
import com.iamkaf.konfig.impl.v1.storage.ConfigMigrationResult;
import com.iamkaf.konfig.impl.v1.storage.ConfigMigrationRunner;
import com.iamkaf.konfig.impl.v1.value.StandardValueSemantics;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FoundationSemanticsTest {
    @Test
    void migrationFailureReturnsTheUntouchedSourceDocument() {
        List<String> source = new ArrayList<String>(Arrays.asList("original"));
        Map<Integer, ConfigMigrationRunner.MigrationStep<List<String>>> steps = new LinkedHashMap<>();
        steps.put(0, (document, from, to) -> document.add("first"));
        steps.put(1, (document, from, to) -> {
            document.add("partial-second");
            throw new IllegalStateException("migration failed");
        });
        ConfigMigrationRunner<List<String>> runner = new ConfigMigrationRunner<>(ArrayList::new, steps);

        ConfigMigrationResult<List<String>> result = runner.migrate(source, 0, 2);

        assertTrue(result instanceof ConfigMigrationResult.Failed<?>);
        ConfigMigrationResult.Failed<?> failed = (ConfigMigrationResult.Failed<?>) result;
        assertSame(source, failed.document());
        assertEquals(Arrays.asList("original"), source);
        assertEquals(1, failed.failedFromVersion());
        assertEquals("migration failed", failed.cause().getMessage());
    }

    @Test
    void localSessionAppliesAllDraftsOrRollsEveryWriteBack() {
        AtomicReference<Integer> first = new AtomicReference<Integer>(1);
        AtomicReference<Integer> second = new AtomicReference<Integer>(2);
        AtomicBoolean failSave = new AtomicBoolean(true);
        AtomicInteger saveCalls = new AtomicInteger();
        ConfigSessionField<Integer> firstField = ConfigSessionField.local(
                "first",
                1,
                first::get,
                first::set,
                StandardValueSemantics.integer()
        );
        ConfigSessionField<Integer> secondField = ConfigSessionField.local(
                "second",
                2,
                second::get,
                second::set,
                StandardValueSemantics.integer()
        );
        ConfigSession session = ConfigSession.local("headless:atomic", 7L, Arrays.asList(firstField, secondField), () -> {
            saveCalls.incrementAndGet();
            if (failSave.get()) {
                throw new IllegalStateException("disk unavailable");
            }
        });
        session.mutate(new ConfigMutation.SetDraft("first", 10));
        session.mutate(new ConfigMutation.SetDraft("second", 20));

        ConfigChangeResult stale = session.apply(6L);
        assertEquals(ConfigChangeResult.Status.REJECTED_STALE, stale.status());
        assertEquals(0, saveCalls.get());
        assertEquals(1, first.get());
        assertEquals(2, second.get());

        ConfigChangeResult failed = session.apply(7L);
        assertEquals(ConfigChangeResult.Status.FAILED, failed.status());
        assertEquals(7L, session.revision());
        assertEquals(1, first.get());
        assertEquals(2, second.get());

        failSave.set(false);
        ConfigChangeResult accepted = session.apply(7L);
        assertEquals(ConfigChangeResult.Status.ACCEPTED, accepted.status());
        assertEquals(8L, session.revision());
        assertEquals(10, first.get());
        assertEquals(20, second.get());
        assertEquals(2, saveCalls.get());
    }

    @Test
    void pendingSessionBlocksLaterDraftsUntilTheAuthoritativeResultArrives() {
        AtomicReference<Integer> stored = new AtomicReference<Integer>(1);
        ConfigSessionField<Integer> field = ConfigSessionField.local(
                "value",
                1,
                stored::get,
                stored::set,
                StandardValueSemantics.integer()
        );
        ConfigSession session = new ConfigSession(
                "headless:pending",
                4L,
                List.of(field),
                request -> new ConfigCommitResult.Pending(19L)
        );

        assertEquals(
                ConfigChangeResult.Status.ACCEPTED,
                session.mutate(new ConfigMutation.SetDraft("value", 10)).status()
        );
        assertEquals(ConfigChangeResult.Status.PENDING, session.apply(4L).status());
        assertEquals(
                ConfigChangeResult.Status.PENDING,
                session.mutate(new ConfigMutation.SetDraft("value", 20)).status()
        );
        assertEquals(10, session.field("value").draftInput());

        stored.set(10);
        ConfigChangeResult completed = session.completePending(19L, new ConfigCommitResult.Accepted(5L));
        assertEquals(ConfigChangeResult.Status.ACCEPTED, completed.status());
        assertEquals(10, session.field("value").storedValue());
        assertEquals(10, session.field("value").draftInput());
    }
}
//?}
