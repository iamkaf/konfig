package com.iamkaf.konfig;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigListener;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.KonfigCodec;
import com.iamkaf.konfig.api.v1.KonfigNode;
import com.iamkaf.konfig.api.v1.ReloadCause;
import com.iamkaf.konfig.api.v1.SyncMode;
import com.iamkaf.konfig.impl.v1.bootstrap.RuntimeEnvironment;
import com.iamkaf.konfig.impl.v1.config.io.PathToml;
import com.iamkaf.konfig.impl.v1.config.migration.ConfigMigrationSupport;
import com.iamkaf.konfig.impl.v1.config.model.ConfigHandleImpl;
//? if >=1.21.11 {
import com.iamkaf.konfig.impl.v1.config.model.ConfigScreenValue;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditRequest;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditResult;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditStatus;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
//?}
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
//? if >=1.21.11 {
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
//?}

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HeadlessKonfigRegressionTest {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    @TempDir
    Path configDirectory;

    @BeforeEach
    void useIsolatedServerConfigDirectory() {
        RuntimeEnvironment.initialize(this.configDirectory, false);
    }

    @Test
    void registrationRetainsIdentityOwnershipAndResolvedPath() {
        int registeredBefore = Konfig.all().size();
        String name = uniqueName("identity");

        ConfigHandle handle = Konfig.builder("headless", name)
                .scope(ConfigScope.SERVER)
                .syncMode(SyncMode.LOGIN_AND_RELOAD)
                .fileName("owned")
                .build();

        assertEquals("headless", handle.modId());
        assertEquals(name, handle.name());
        assertEquals(ConfigScope.SERVER, handle.scope());
        assertEquals(SyncMode.LOGIN_AND_RELOAD, handle.syncMode());
        assertEquals(this.configDirectory.resolve("headless").resolve("owned.toml"), handle.path());
        assertTrue(Konfig.all().contains(handle));
        assertEquals(registeredBefore + 1, Konfig.all().size());
    }

    @Test
    void registrationOrderMatchesDeclarationOrder() {
        int registeredBefore = Konfig.all().size();
        ConfigHandle first = Konfig.builder("headless", uniqueName("order")).build();
        ConfigHandle second = Konfig.builder("headless", uniqueName("order")).build();
        ConfigHandle third = Konfig.builder("headless", uniqueName("order")).build();

        List<ConfigHandle> registered = new ArrayList<ConfigHandle>(Konfig.all());
        assertEquals(
                Arrays.asList(first, second, third),
                registered.subList(registeredBefore, registeredBefore + 3)
        );
    }

    @Test
    void duplicateRegistrationAndUnsafeOwnedPathsAreRejected() {
        String name = uniqueName("duplicate");
        Konfig.builder("headless", name).build();

        IllegalStateException duplicate = assertThrows(
                IllegalStateException.class,
                () -> Konfig.builder("headless", name).build()
        );
        assertEquals("Config already registered: headless:" + name, duplicate.getMessage());

        String firstPathOwner = uniqueName("pathowner");
        String secondPathOwner = uniqueName("pathowner");
        Konfig.builder("headless", firstPathOwner).fileName("shared-path").build();
        IllegalStateException duplicatePath = assertThrows(
                IllegalStateException.class,
                () -> Konfig.builder("headless", secondPathOwner).fileName("shared-path").build()
        );
        assertTrue(duplicatePath.getMessage().contains("Config file already owned by headless:" + firstPathOwner));

        assertThrows(IllegalArgumentException.class, () -> Konfig.builder("../outside", uniqueName("mod")));
        assertThrows(
                IllegalArgumentException.class,
                () -> Konfig.builder("headless", uniqueName("file")).fileName("../outside.toml")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> Konfig.builder("headless", uniqueName("extension")).fileName("config.json")
        );
    }

    @Test
    void nestedCategoriesOwnStableDottedValuePaths() {
        ConfigBuilder builder = Konfig.builder("headless", uniqueName("paths"));
        ConfigValue<Boolean> nested = builder
                .push("rules")
                .push("diamond")
                .bool("enabled", true)
                .build();
        builder.pop().pop();

        assertEquals("rules.diamond.enabled", nested.path());
        assertThrows(IllegalStateException.class, builder::pop);
        assertThrows(IllegalArgumentException.class, () -> builder.push("bad.segment"));
        assertThrows(IllegalArgumentException.class, () -> builder.bool("__konfig", true));
    }

    @Test
    void persistentTypesRoundTripThroughToml() throws IOException {
        ConfigBuilder builder = Konfig.builder("headless", uniqueName("roundtrip"));
        ConfigValue<Boolean> enabled = builder.bool("enabled", false).build();
        ConfigValue<Integer> count = builder.intRange("count", 1, 0, 20).build();
        ConfigValue<Long> distance = builder.longRange("distance", 2L, 0L, 100L).build();
        ConfigValue<Double> ratio = builder.doubleRange("ratio", 0.5D, 0.0D, 1.0D).build();
        ConfigValue<String> label = builder.string("label", "default", 1, 32).build();
        ConfigValue<List<String>> tags = builder.stringList("tags", Arrays.asList("one")).build();
        ConfigValue<TestMode> mode = builder.enumValue("mode", TestMode.FIRST).build();
        ConfigValue<TestPoint> point = builder.custom("point", new TestPoint(0, 0), pointCodec()).build();
        ConfigHandle handle = builder.build();

        enabled.set(true);
        count.set(12);
        distance.set(80L);
        ratio.set(0.75D);
        label.set("saved");
        tags.set(Arrays.asList("two", "three"));
        mode.set(TestMode.SECOND);
        point.set(new TestPoint(4, 9));
        handle.save();

        enabled.set(false);
        count.set(1);
        distance.set(2L);
        ratio.set(0.5D);
        label.set("changed");
        tags.set(Arrays.asList("changed"));
        mode.set(TestMode.FIRST);
        point.set(new TestPoint(1, 1));
        handle.reload();

        assertEquals(true, enabled.get());
        assertEquals(12, count.get());
        assertEquals(80L, distance.get());
        assertEquals(0.75D, ratio.get());
        assertEquals("saved", label.get());
        assertEquals(Arrays.asList("two", "three"), tags.get());
        assertEquals(TestMode.SECOND, mode.get());
        assertEquals(new TestPoint(4, 9), point.get());
        assertTrue(Files.readString(handle.path()).contains("version = 0"));
    }

    @Test
    void validationRejectsApiWritesAndFallsBackWhileLoading() throws IOException {
        ConfigBuilder builder = Konfig.builder("headless", uniqueName("validation"));
        ConfigValue<Integer> count = builder.intRange("count", 4, 0, 10)
                .validate(value -> value.intValue() % 2 == 0, "Count must be even")
                .build();
        ConfigHandle handle = builder.build();

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> count.set(3));
        assertTrue(failure.getMessage().contains("Count must be even"));
        assertTrue(failure.getMessage().contains("count"));
        assertEquals(4, count.get());

        Files.writeString(handle.path(), "count = 99\n\n[__konfig]\nversion = 0\n");
        handle.reload();
        assertEquals(4, count.get());
    }

    @Test
    void migrationsRunInOrderAndPersistTheTargetSchema() throws IOException {
        String name = uniqueName("migration");
        Path path = this.configDirectory.resolve("headless").resolve(name + ".toml");
        Files.createDirectories(path.getParent());
        Files.writeString(path, "old_count = 7\n\n[__konfig]\nversion = 0\n");

        List<String> migrations = new ArrayList<String>();
        ConfigBuilder builder = Konfig.builder("headless", name)
                .schemaVersion(2)
                .migrate(0, context -> {
                    assertEquals("headless", context.modId());
                    assertEquals(name, context.name());
                    assertEquals(0, context.fromVersion());
                    assertEquals(1, context.toVersion());
                    assertTrue(context.rename("old_count", "rules.count"));
                    migrations.add("0->1");
                })
                .migrate(1, context -> {
                    context.set("rules.count", context.intValue("rules.count").intValue() + 1);
                    migrations.add("1->2");
                });
        ConfigValue<Integer> count = builder.push("rules").intRange("count", 0, 0, 20).build();
        builder.pop();
        ConfigHandle handle = builder.build();

        assertEquals(Arrays.asList("0->1", "1->2"), migrations);
        assertEquals(8, count.get());
        CommentedConfig stored = PathToml.read(handle.path());
        assertEquals(2, ConfigMigrationSupport.readSchemaVersion(stored));
        assertEquals(8, PathToml.get(stored, "rules.count").getAsInt());
        assertFalse(stored.contains("old_count"));
    }

    @Test
    void missingMigrationStopsRegistrationLoad() throws IOException {
        String name = uniqueName("missingmigration");
        Path path = this.configDirectory.resolve("headless").resolve(name + ".toml");
        Files.createDirectories(path.getParent());
        Files.writeString(path, "value = true\n\n[__konfig]\nversion = 0\n");

        ConfigBuilder builder = Konfig.builder("headless", name).schemaVersion(1);
        builder.bool("value", false).build();
        RuntimeException failure = assertThrows(RuntimeException.class, builder::build);
        assertTrue(failure.getMessage().contains("Failed migrating config headless:" + name));
        assertTrue(failure.getCause().getMessage().contains("Missing config migration"));
    }

    @Test
    void migrationFailureLeavesTheStoredFileUntouched() throws IOException {
        String name = uniqueName("failedmigration");
        Path path = this.configDirectory.resolve("headless").resolve(name + ".toml");
        Files.createDirectories(path.getParent());
        String original = "value = 3\n\n[__konfig]\nversion = 0\n";
        Files.writeString(path, original);

        ConfigBuilder builder = Konfig.builder("headless", name)
                .schemaVersion(1)
                .migrate(0, context -> {
                    context.set("value", 9);
                    throw new IllegalStateException("migration stopped");
                });
        builder.intRange("value", 0, 0, 10).build();

        RuntimeException failure = assertThrows(RuntimeException.class, builder::build);
        assertTrue(failure.getCause().getMessage().contains("migration stopped"));
        assertEquals(original, Files.readString(path));
    }

    @Test
    void corruptFilesArePreservedBeforeDefaultsAreRestored() throws IOException {
        String name = uniqueName("corrupt");
        Path path = this.configDirectory.resolve("headless").resolve(name + ".toml");
        Files.createDirectories(path.getParent());
        String broken = "enabled = [\n";
        Files.writeString(path, broken);

        ConfigBuilder builder = Konfig.builder("headless", name).schemaVersion(2);
        ConfigValue<Boolean> enabled = builder.bool("enabled", true).build();
        ConfigHandle handle = builder.build();

        assertEquals(true, enabled.get());
        assertTrue(Files.isRegularFile(path.resolveSibling(path.getFileName() + ".broken")));
        assertEquals(broken, Files.readString(path.resolveSibling(path.getFileName() + ".broken")));
        assertTrue(PathToml.read(handle.path()).contains("enabled"));
    }

    @Test
    void newerSchemaRemainsReadOnlyAfterLoading() throws IOException {
        String name = uniqueName("newerschema");
        Path path = this.configDirectory.resolve("headless").resolve(name + ".toml");
        Files.createDirectories(path.getParent());
        String newer = "enabled = true\n\n[__konfig]\nversion = 99\n";
        Files.writeString(path, newer);

        ConfigBuilder builder = Konfig.builder("headless", name).schemaVersion(1);
        ConfigValue<Boolean> enabled = builder.bool("enabled", false).build();
        ConfigHandleImpl handle = (ConfigHandleImpl) builder.build();

        assertTrue(handle.newerSchemaReadOnly());
        assertEquals(true, enabled.get());
        enabled.set(false);
        assertThrows(IllegalStateException.class, handle::save);
        assertEquals(newer, Files.readString(path));
    }

    @Test
    void savingRetainsUnknownTomlValuesAndComments() throws IOException {
        String name = uniqueName("retention");
        Path path = this.configDirectory.resolve("headless").resolve(name + ".toml");
        Files.createDirectories(path.getParent());
        Files.writeString(path, "# retained root comment\nunknown = 7\n# retained value comment\nenabled = true\n\n[__konfig]\nversion = 0\n");

        ConfigBuilder builder = Konfig.builder("headless", name);
        ConfigValue<Boolean> enabled = builder.bool("enabled", false).build();
        ConfigHandle handle = builder.build();
        enabled.set(false);
        handle.save();

        String stored = Files.readString(path);
        assertTrue(stored.contains("unknown = 7"));
        assertTrue(stored.contains("retained root comment"));
        assertTrue(stored.contains("retained value comment"));
    }

//? if >=1.21.11 {
    @Test
    void remoteScreenViewDoesNotReplaceTheStoredValue() {
        AtomicReference<String> remote = new AtomicReference<>("server");
        AtomicBoolean available = new AtomicBoolean();
        ConfigBuilder builder = Konfig.builder("headless", uniqueName("remote_view"));
        ConfigValue<String> value = builder.string("value", "local", 1, 20)
                .remoteScreenView(remote::get, available::get)
                .build();
        builder.build();

        @SuppressWarnings("unchecked")
        ConfigScreenValue<String> screenValue = (ConfigScreenValue<String>) value;
        assertFalse(screenValue.remoteScreenViewAvailable());
        assertEquals("local", value.get());

        available.set(true);
        assertTrue(screenValue.remoteScreenViewAvailable());
        assertEquals("server", screenValue.remoteScreenValue());
        assertEquals("local", value.get());

        value.set("changed locally");
        assertEquals("server", screenValue.remoteScreenValue());
        assertEquals("changed locally", value.get());
    }

    @Test
    void registeredRemoteTargetAppliesCompleteDraftAtomically() throws IOException {
        String name = uniqueName("remote");
        ConfigBuilder builder = Konfig.builder("headless", name)
                .scope(ConfigScope.SERVER)
                .syncMode(SyncMode.LOGIN);
        ConfigValue<Integer> count = builder.push("rules").intRange("count", 2, 0, 20).sync(true).build();
        ConfigValue<Boolean> enabled = builder.bool("enabled", false).sync(true).build();
        builder.pop();
        ConfigHandleImpl handle = (ConfigHandleImpl) builder.build();
        long baseRevision = handle.revision();

        ConfigEditResult accepted = KonfigSync.authority().apply(
                new ConfigEditRequest(1L, handle.id(), baseRevision, "{\"rules\":{\"count\":7,\"enabled\":true}}"),
                true,
                true
        );
        assertEquals(ConfigEditStatus.ACCEPTED, accepted.status());
        assertEquals(baseRevision + 1L, accepted.revision());
        assertEquals(7, count.get());
        assertEquals(true, enabled.get());
        assertEquals(7, PathToml.get(PathToml.read(handle.path()), "rules.count").getAsInt());

        ConfigEditResult stale = KonfigSync.authority().apply(
                new ConfigEditRequest(2L, handle.id(), baseRevision, "{\"rules\":{\"count\":8,\"enabled\":false}}"),
                true,
                true
        );
        assertEquals(ConfigEditStatus.STALE, stale.status());
        assertEquals(7, count.get());

        ConfigEditResult incomplete = KonfigSync.authority().apply(
                new ConfigEditRequest(3L, handle.id(), handle.revision(), "{\"rules\":{\"count\":8}}"),
                true,
                true
        );
        assertEquals(ConfigEditStatus.INVALID, incomplete.status());
        assertEquals(7, count.get());
    }
//?}

    @Test
    void synchronizedValuesOverlayLocalStateUntilDisconnect() {
        ConfigBuilder builder = Konfig.builder("headless", uniqueName("sync"));
        ConfigValue<Integer> synced = builder.push("rules").intRange("count", 2, 0, 20).sync(true).build();
        ConfigValue<String> clientOnly = builder.string("client_label", "local", 1, 20)
                .sync(true)
                .clientOnly()
                .build();
        ConfigValue<Boolean> unsynced = builder.bool("unsynced", false).build();
        builder.pop();
        ConfigHandleImpl handle = (ConfigHandleImpl) builder.build();

        synced.set(5);
        clientOnly.set("private");
        unsynced.set(true);
        String snapshot = handle.snapshotJson();
        assertTrue(snapshot.contains("\"count\": 5"));
        assertFalse(snapshot.contains("client_label"));
        assertFalse(snapshot.contains("unsynced"));

        handle.applySyncSnapshot("{\"rules\":{\"count\":9}}");
        assertEquals(9, synced.get());
        assertEquals("private", clientOnly.get());

        synced.set(6);
        assertEquals(9, synced.get());
        handle.clearSyncedValues();
        assertEquals(6, synced.get());
    }

    @Test
    void entrySideOwnershipControlsHeadlessPersistence() throws IOException {
        String serverName = uniqueName("serverside");
        ConfigHandle serverHandle = sideOwnedConfig(serverName);
        CommentedConfig serverToml = PathToml.read(serverHandle.path());
        assertTrue(serverToml.contains("shared"));
        assertTrue(serverToml.contains("server"));
        assertFalse(serverToml.contains("client"));

        Path clientDirectory = this.configDirectory.resolve("client-runtime");
        RuntimeEnvironment.initialize(clientDirectory, true);
        ConfigHandle clientHandle = sideOwnedConfig(uniqueName("clientside"));
        CommentedConfig clientToml = PathToml.read(clientHandle.path());
        assertTrue(clientToml.contains("shared"));
        assertTrue(clientToml.contains("client"));
        assertFalse(clientToml.contains("server"));
    }

    @Test
    void nullScopeAndSyncModeUseDocumentedFallbacks() {
        ConfigHandle defaults = Konfig.builder("headless", uniqueName("defaults")).build();
        ConfigHandle explicitNulls = Konfig.builder("headless", uniqueName("nulls"))
                .scope(null)
                .syncMode(null)
                .build();

        assertEquals(ConfigScope.COMMON, defaults.scope());
        assertEquals(SyncMode.LOGIN, defaults.syncMode());
        assertEquals(ConfigScope.COMMON, explicitNulls.scope());
        assertEquals(SyncMode.NONE, explicitNulls.syncMode());
    }

    @Test
    void listenersObserveLoadReloadSyncAndUnloadInOrder() {
        ConfigBuilder builder = Konfig.builder("headless", uniqueName("listeners"));
        builder.bool("enabled", false).sync(true).build();
        ConfigHandleImpl handle = (ConfigHandleImpl) builder.build();
        List<String> events = new ArrayList<String>();
        handle.addListener(new ConfigListener() {
            @Override
            public void onLoad(ConfigHandle loaded) {
                assertSame(handle, loaded);
                events.add("load");
            }

            @Override
            public void onReload(ConfigHandle reloaded, ReloadCause cause) {
                assertSame(handle, reloaded);
                events.add("reload:" + cause.name());
            }

            @Override
            public void onUnload(ConfigHandle unloaded) {
                assertSame(handle, unloaded);
                events.add("unload");
            }
        });

        handle.load();
        handle.save();
        handle.reload();
        handle.applySyncSnapshot("{\"enabled\":true}");
        handle.clearSyncedValues();

        assertEquals(
                Arrays.asList(
                        "load",
                        "reload:API_CALL",
                        "load",
                        "reload:API_CALL",
                        "reload:SERVER_SYNC",
                        "unload"
                ),
                events
        );
    }

    private ConfigHandle sideOwnedConfig(String name) {
        ConfigBuilder builder = Konfig.builder("headless", name).scope(ConfigScope.COMMON);
        builder.bool("shared", true).build();
        builder.bool("client", true).clientOnly().build();
        builder.bool("server", true).serverOnly().build();
        return builder.build();
    }

    private static KonfigCodec<TestPoint> pointCodec() {
        return new KonfigCodec<TestPoint>() {
            @Override
            public TestPoint decode(KonfigNode node) {
                String[] parts = node.json().getAsString().split(",", -1);
                return new TestPoint(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }

            @Override
            public KonfigNode encode(TestPoint value) {
                return new KonfigNode(new JsonPrimitive(value.x + "," + value.y));
            }
        };
    }

    private static String uniqueName(String prefix) {
        return prefix + NEXT_ID.incrementAndGet();
    }

    private enum TestMode {
        FIRST,
        SECOND
    }

    private static final class TestPoint {
        private final int x;
        private final int y;

        private TestPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof TestPoint)) {
                return false;
            }
            TestPoint point = (TestPoint) other;
            return this.x == point.x && this.y == point.y;
        }

        @Override
        public int hashCode() {
            return 31 * this.x + this.y;
        }
    }
}
