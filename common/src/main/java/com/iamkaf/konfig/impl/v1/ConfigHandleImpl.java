package com.iamkaf.konfig.impl.v1;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigDebugConfig;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigListener;
import com.iamkaf.konfig.api.v1.ConfigMigration;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.ReloadCause;
import com.iamkaf.konfig.api.v1.SyncMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public final class ConfigHandleImpl implements ConfigHandle {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String modId;
    private final String name;
    private final ConfigScope scope;
    private final SyncMode syncMode;
    private final Path path;
    private final LinkedHashMap<String, ConfigValueImpl<?>> entries;
    private final LinkedHashMap<String, String> entryComments;
    private final LinkedHashMap<String, String> categoryComments;
    private final String fileComment;
    private final int schemaVersion;
    private final LinkedHashMap<Integer, ConfigMigration> migrations;
    private final List<ConfigListener> listeners = new CopyOnWriteArrayList<ConfigListener>();

    ConfigHandleImpl(
            String modId,
            String name,
            ConfigScope scope,
            SyncMode syncMode,
            Path path,
            LinkedHashMap<String, ConfigValueImpl<?>> entries,
            LinkedHashMap<String, String> entryComments,
            LinkedHashMap<String, String> categoryComments,
            String fileComment,
            int schemaVersion,
            LinkedHashMap<Integer, ConfigMigration> migrations
    ) {
        this.modId = modId;
        this.name = name;
        this.scope = scope;
        this.syncMode = syncMode;
        this.path = path;
        this.entries = entries;
        this.entryComments = entryComments;
        this.categoryComments = categoryComments;
        this.fileComment = fileComment == null ? "" : fileComment;
        this.schemaVersion = schemaVersion;
        this.migrations = migrations;
    }

    @Override
    public String modId() {
        return this.modId;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public ConfigScope scope() {
        return this.scope;
    }

    @Override
    public SyncMode syncMode() {
        return this.syncMode;
    }

    @Override
    public Path path() {
        return this.path;
    }

    @Override
    public void load() {
        CommentedConfig root = TomlFormat.newConfig();
        boolean configFound = Files.exists(this.path);
        boolean loadedFromDisk = false;
        boolean shouldPersist = true;
        try {
            if (configFound) {
                root = PathToml.read(this.path);
                loadedFromDisk = true;
            }
        } catch (Exception e) {
            Constants.LOG.warn("Failed reading config {}, using defaults.", this.path.toAbsolutePath(), e);
        }

        try {
            shouldPersist = migrate(root, loadedFromDisk);
        } catch (Exception e) {
            throw new RuntimeException("Failed migrating config " + id() + " at " + this.path.toAbsolutePath(), e);
        }

        for (ConfigValueImpl<?> entry : this.entries.values()) {
            if (!entry.persistent()) {
                continue;
            }
            if (!shouldLoadOnThisSide(entry)) {
                continue;
            }
            loadEntryFromToml(entry, root);
        }

        if (shouldPersist) {
            save();
        }
        if (KonfigDebugConfig.enabled()) {
            if (configFound) {
                Constants.LOG.info("[Konfig/Debug] config found for {} at {}", id(), this.path.toAbsolutePath());
            } else {
                Constants.LOG.info("[Konfig/Debug] config not found, created defaults for {} at {}", id(), this.path.toAbsolutePath());
            }
        }
        this.listeners.forEach(listener -> listener.onLoad(this));
    }

    @Override
    public void save() {
        try {
            Files.createDirectories(this.path.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed creating config directory for " + this.path, e);
        }

        CommentedConfig root = TomlFormat.newConfig();
        ConfigMigrationSupport.writeSchemaVersion(root, this.schemaVersion);
        for (ConfigValueImpl<?> entry : this.entries.values()) {
            if (!entry.persistent()) {
                continue;
            }
            if (!shouldLoadOnThisSide(entry)) {
                continue;
            }
            PathToml.put(root, entry.path(), entry.encodeCurrent());
            String entryComment = this.entryComments.get(entry.path());
            if (entryComment != null && root.contains(entry.path())) {
                PathToml.setComment(root, entry.path(), entryComment);
            }
        }

        for (Map.Entry<String, String> categoryComment : this.categoryComments.entrySet()) {
            if (root.contains(categoryComment.getKey())) {
                PathToml.setComment(root, categoryComment.getKey(), categoryComment.getValue());
            }
        }

        try {
            PathToml.write(this.path, root, this.fileComment);
        } catch (IOException e) {
            throw new RuntimeException("Failed writing config " + this.path, e);
        }
    }

    @Override
    public void reload() {
        load();
        this.listeners.forEach(listener -> listener.onReload(this, ReloadCause.API_CALL));
    }

    @Override
    public void addListener(ConfigListener listener) {
        this.listeners.add(listener);
    }

    public Collection<ConfigValue<?>> values() {
        return Collections.unmodifiableList(this.entries.values().stream()
                .filter(ConfigValueImpl::persistent)
                .collect(Collectors.toList()));
    }

    public Collection<ConfigValueImpl<?>> screenValues() {
        return Collections.unmodifiableCollection(this.entries.values());
    }

    public String id() {
        return this.modId + ":" + this.name;
    }

    public String tooltip(String path) {
        StringBuilder builder = new StringBuilder();
        String[] parts = path.split("\\.");
        StringBuilder categoryPath = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (categoryPath.length() > 0) {
                categoryPath.append('.');
            }
            categoryPath.append(parts[i]);
            appendComment(builder, this.categoryComments.get(categoryPath.toString()));
        }
        appendComment(builder, this.entryComments.get(path));
        return builder.toString();
    }

    public String snapshotJson() {
        JsonObject root = new JsonObject();
        for (ConfigValueImpl<?> entry : this.entries.values()) {
            if (!entry.persistent()) {
                continue;
            }
            if (entry.sync() && !entry.clientOnly()) {
                PathJson.put(root, entry.path(), entry.encodeCurrent());
            }
        }
        return GSON.toJson(root);
    }

    public void applySyncSnapshot(String json) {
        JsonObject root;
        try {
            root = new JsonParser().parse(json).getAsJsonObject();
        } catch (Exception e) {
            Constants.LOG.warn("Ignoring invalid sync payload for {}", id(), e);
            return;
        }

        for (ConfigValueImpl<?> entry : this.entries.values()) {
            if (!entry.persistent()) {
                continue;
            }
            if (!entry.sync() || entry.clientOnly()) {
                continue;
            }
            syncEntryFromJson(entry, root);
        }

        this.listeners.forEach(listener -> listener.onReload(this, ReloadCause.SERVER_SYNC));
    }

    public void clearSyncedValues() {
        this.entries.values().forEach(ConfigValueImpl::clearSynced);
        this.listeners.forEach(listener -> listener.onUnload(this));
    }

    private boolean shouldLoadOnThisSide(ConfigValueImpl<?> entry) {
        if (entry.clientOnly() && !RuntimeEnvironment.isClient()) {
            return false;
        }
        if (entry.serverOnly() && RuntimeEnvironment.isClient()) {
            return false;
        }
        return true;
    }

    private boolean migrate(CommentedConfig root, boolean configFound) throws Exception {
        if (!configFound) {
            ConfigMigrationSupport.writeSchemaVersion(root, this.schemaVersion);
            return true;
        }

        int fileVersion = ConfigMigrationSupport.readSchemaVersion(root);
        if (fileVersion > this.schemaVersion) {
            Constants.LOG.warn(
                    "Config {} at {} uses newer schema v{} than supported v{}; loading without automatic rewrite.",
                    id(),
                    this.path.toAbsolutePath(),
                    Integer.valueOf(fileVersion),
                    Integer.valueOf(this.schemaVersion)
            );
            return false;
        }

        int currentVersion = fileVersion;
        while (currentVersion < this.schemaVersion) {
            ConfigMigration migration = this.migrations.get(Integer.valueOf(currentVersion));
            if (migration == null) {
                throw new IllegalStateException(
                        "Missing config migration for " + id() + " from version " + currentVersion + " to " + (currentVersion + 1)
                );
            }
            if (KonfigDebugConfig.enabled()) {
                Constants.LOG.info(
                        "[Konfig/Debug] migrating {} from schema v{} to v{}",
                        id(),
                        Integer.valueOf(currentVersion),
                        Integer.valueOf(currentVersion + 1)
                );
            }
            migration.migrate(new ConfigMigrationContextImpl(this.modId, this.name, currentVersion, currentVersion + 1, root));
            currentVersion++;
            ConfigMigrationSupport.writeSchemaVersion(root, currentVersion);
        }

        if (currentVersion == this.schemaVersion) {
            ConfigMigrationSupport.writeSchemaVersion(root, currentVersion);
        }
        return true;
    }

    private static <T> void loadEntryFromToml(ConfigValueImpl<T> entry, CommentedConfig root) {
        entry.setLocal(entry.decodeOrFallback(PathToml.get(root, entry.path())));
    }

    private static <T> void syncEntryFromJson(ConfigValueImpl<T> entry, JsonObject root) {
        entry.setSynced(entry.decodeOrFallback(PathJson.get(root, entry.path())));
    }

    private static void appendComment(StringBuilder builder, String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n').append('\n');
        }
        builder.append(comment.trim());
    }
}
