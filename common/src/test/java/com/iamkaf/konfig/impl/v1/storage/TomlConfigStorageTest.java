//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.storage;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TomlConfigStorageTest {
    @TempDir
    Path directory;

    @Test
    void loadAndSaveRetainRepresentableUnknownValuesAndComments() throws IOException {
        Path path = this.directory.resolve("retained.toml");
        Files.writeString(
                path,
                "# Keep this comment.\nunknown = 7\n\n[rules]\n# Keep this too.\nenabled = true\n\n[__konfig]\nversion = 3\n"
        );
        TomlConfigStorage storage = storage(defaults());

        ConfigStorageLoadResult.Loaded loaded = assertInstanceOf(
                ConfigStorageLoadResult.Loaded.class,
                storage.load("test:retained", path)
        );
        assertEquals(3, loaded.document().schemaVersion());
        assertEquals(7, loaded.document().values().get("unknown").getAsInt());
        assertEquals(true, loaded.document().values().get("rules.enabled").getAsBoolean());
        assertEquals("Keep this comment.", loaded.document().comments().get("unknown"));
        assertEquals("Keep this too.", loaded.document().comments().get("rules.enabled"));

        ConfigStorageDocument update = new ConfigStorageDocument(
                4,
                Map.of("rules.enabled", new JsonPrimitive(false)),
                Map.of(),
                ""
        );
        assertInstanceOf(ConfigStorageSaveResult.Saved.class, storage.save("test:retained", path, update));

        String saved = Files.readString(path);
        assertTrue(saved.contains("unknown = 7"));
        assertTrue(saved.contains("Keep this comment."));
        assertTrue(saved.contains("Keep this too."));
        ConfigStorageLoadResult.Loaded reloaded = assertInstanceOf(
                ConfigStorageLoadResult.Loaded.class,
                storage.load("test:retained", path)
        );
        assertEquals(4, reloaded.document().schemaVersion());
        assertEquals(false, reloaded.document().values().get("rules.enabled").getAsBoolean());
    }

    @Test
    void recoveryPreservesBrokenInputBeforeRestoringDefaults() throws IOException {
        Path path = this.directory.resolve("broken.toml");
        String broken = "enabled = [\n";
        Files.writeString(path, broken);
        ConfigStorageDocument defaults = defaults();
        TomlConfigStorage storage = storage(defaults);

        ConfigStorageLoadResult.Recovered recovered = assertInstanceOf(
                ConfigStorageLoadResult.Recovered.class,
                storage.load("test:broken", path)
        );
        assertEquals(defaults, recovered.defaults());
        assertEquals(broken, Files.readString(recovered.preservedFile()));
        assertTrue(Files.isRegularFile(path));

        ConfigStorageLoadResult.Loaded restored = assertInstanceOf(
                ConfigStorageLoadResult.Loaded.class,
                storage.load("test:broken", path)
        );
        assertEquals(defaults.schemaVersion(), restored.document().schemaVersion());
        assertEquals(true, restored.document().values().get("enabled").getAsBoolean());
    }

    @Test
    void missingAndFailPoliciesDoNotRewriteThePath() throws IOException {
        Path missingPath = this.directory.resolve("missing.toml");
        TomlConfigStorage storage = new TomlConfigStorage(defaults(), ConfigRecoveryPolicy.fail());
        assertInstanceOf(ConfigStorageLoadResult.Missing.class, storage.load("test:missing", missingPath));

        Path brokenPath = this.directory.resolve("failed.toml");
        String broken = "enabled = [\n";
        Files.writeString(brokenPath, broken);
        ConfigStorageLoadResult.Failed failed = assertInstanceOf(
                ConfigStorageLoadResult.Failed.class,
                storage.load("test:failed", brokenPath)
        );
        assertTrue(failed.preservedFile().isEmpty());
        assertEquals(broken, Files.readString(brokenPath));
    }

    private static TomlConfigStorage storage(ConfigStorageDocument defaults) {
        return new TomlConfigStorage(defaults, ConfigRecoveryPolicy.preserveBrokenFile());
    }

    private static ConfigStorageDocument defaults() {
        var values = new LinkedHashMap<String, com.google.gson.JsonElement>();
        values.put("enabled", new JsonPrimitive(true));
        return new ConfigStorageDocument(1, values, Map.of("enabled", "Default switch."), "Test config.");
    }
}
//?}
