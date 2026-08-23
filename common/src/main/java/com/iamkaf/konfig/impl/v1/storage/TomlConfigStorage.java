//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.gson.JsonElement;
import com.iamkaf.konfig.impl.v1.config.io.PathToml;
import com.iamkaf.konfig.impl.v1.config.migration.ConfigMigrationSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
public final class TomlConfigStorage implements ConfigStorage {
    private final ConfigStorageDocument defaults;
    private final ConfigRecoveryPolicy recoveryPolicy;

    public TomlConfigStorage(ConfigStorageDocument defaults, ConfigRecoveryPolicy recoveryPolicy) {
        this.defaults = Objects.requireNonNull(defaults, "defaults").copy();
        this.recoveryPolicy = Objects.requireNonNull(recoveryPolicy, "recoveryPolicy");
    }

    @Override
    public ConfigStorageLoadResult load(String configId, Path path) {
        String normalizedId = requireConfigId(configId);
        Path normalizedPath = requirePath(path);
        if (Files.notExists(normalizedPath)) {
            return new ConfigStorageLoadResult.Missing();
        }
        if (!Files.isRegularFile(normalizedPath)) {
            IOException failure = new IOException("Config path is not a regular file: " + normalizedPath);
            return failedLoad(normalizedId, normalizedPath, failure, Optional.empty());
        }

        try {
            CommentedConfig root = PathToml.read(normalizedPath);
            return new ConfigStorageLoadResult.Loaded(readDocument(root));
        } catch (Exception failure) {
            return recover(normalizedId, normalizedPath, failure);
        }
    }

    @Override
    public ConfigStorageSaveResult save(
            String configId,
            Path path,
            ConfigStorageDocument document
    ) {
        String normalizedId = requireConfigId(configId);
        Path normalizedPath = requirePath(path);
        Objects.requireNonNull(document, "document");

        try {
            CommentedConfig root = Files.exists(normalizedPath)
                    ? PathToml.read(normalizedPath)
                    : TomlFormat.newConfig();
            ConfigMigrationSupport.writeSchemaVersion(root, document.schemaVersion());
            for (Map.Entry<String, JsonElement> entry : document.values().entrySet()) {
                String valuePath = ConfigMigrationSupport.requireUserPath(entry.getKey(), "stored value path");
                PathToml.put(root, valuePath, entry.getValue());
            }
            for (Map.Entry<String, String> entry : document.comments().entrySet()) {
                String commentPath = ConfigMigrationSupport.requireUserPath(entry.getKey(), "stored comment path");
                if (root.contains(commentPath)) {
                    PathToml.setComment(root, commentPath, entry.getValue());
                }
            }
            PathToml.write(normalizedPath, root, document.fileComment());
            return new ConfigStorageSaveResult.Saved();
        } catch (Exception failure) {
            return new ConfigStorageSaveResult.Failed(
                    "Failed saving config '" + normalizedId + "' at " + normalizedPath,
                    failure
            );
        }
    }

    private ConfigStorageLoadResult recover(String configId, Path path, Throwable failure) {
        ConfigRecoveryPolicy.Decision decision;
        try {
            decision = Objects.requireNonNull(
                    this.recoveryPolicy.decide(configId, path, failure),
                    "recovery policy decision"
            );
        } catch (Exception policyFailure) {
            policyFailure.addSuppressed(failure);
            return failedLoad(configId, path, policyFailure, Optional.empty());
        }
        if (decision == ConfigRecoveryPolicy.Decision.FAIL) {
            return failedLoad(configId, path, failure, Optional.empty());
        }

        Path preserved;
        try {
            preserved = PathToml.preserveBroken(path);
        } catch (Exception preserveFailure) {
            preserveFailure.addSuppressed(failure);
            return failedLoad(configId, path, preserveFailure, Optional.empty());
        }

        ConfigStorageSaveResult restored = save(configId, path, this.defaults);
        if (restored instanceof ConfigStorageSaveResult.Failed) {
            ConfigStorageSaveResult.Failed restoreFailure = (ConfigStorageSaveResult.Failed) restored;
            restoreFailure.cause().addSuppressed(failure);
            return new ConfigStorageLoadResult.Failed(
                    "Preserved broken config '" + configId + "' at " + preserved
                            + " but failed restoring defaults at " + path,
                    restoreFailure.cause(),
                    Optional.of(preserved)
            );
        }
        return new ConfigStorageLoadResult.Recovered(this.defaults.copy(), preserved, failure);
    }

    private ConfigStorageDocument readDocument(CommentedConfig root) {
        var values = new LinkedHashMap<String, JsonElement>();
        var comments = new LinkedHashMap<String, String>();
        collect(root, root, "", values, comments);
        values.remove(ConfigMigrationSupport.VERSION_PATH);
        comments.remove(ConfigMigrationSupport.VERSION_PATH);
        comments.remove(ConfigMigrationSupport.METADATA_ROOT);
        return new ConfigStorageDocument(
                ConfigMigrationSupport.readSchemaVersion(root),
                values,
                comments,
                ""
        );
    }

    private static void collect(
            CommentedConfig root,
            UnmodifiableConfig current,
            String prefix,
            Map<String, JsonElement> values,
            Map<String, String> comments
    ) {
        for (UnmodifiableConfig.Entry entry : current.entrySet()) {
            String segment = entry.getKey();
            if (!isRepresentableSegment(segment)) {
                continue;
            }
            String path = prefix.isEmpty() ? segment : prefix + '.' + segment;
            String comment = root.getComment(path);
            if (comment != null) {
                comments.put(path, comment.trim());
            }

            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig) {
                collect(root, (UnmodifiableConfig) value, path, values, comments);
                continue;
            }
            JsonElement json = PathToml.get(root, path);
            if (json != null) {
                values.put(path, json);
            }
        }
    }

    private static boolean isRepresentableSegment(String segment) {
        return segment != null && !segment.isEmpty() && segment.indexOf('.') < 0;
    }

    private static ConfigStorageLoadResult.Failed failedLoad(
            String configId,
            Path path,
            Throwable failure,
            Optional<Path> preservedFile
    ) {
        return new ConfigStorageLoadResult.Failed(
                "Failed loading config '" + configId + "' at " + path,
                failure,
                preservedFile
        );
    }

    private static String requireConfigId(String configId) {
        String normalized = Objects.requireNonNull(configId, "configId").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("configId cannot be blank");
        }
        return normalized;
    }

    private static Path requirePath(Path path) {
        return Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }
}
//?}
