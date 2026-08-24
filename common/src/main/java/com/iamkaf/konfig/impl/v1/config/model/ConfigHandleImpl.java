package com.iamkaf.konfig.impl.v1.config.model;

import org.jetbrains.annotations.ApiStatus;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import com.iamkaf.konfig.impl.v1.bootstrap.KonfigDebugConfig;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigListener;
import com.iamkaf.konfig.api.v1.ConfigMigration;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.ReloadCause;
import com.iamkaf.konfig.api.v1.SyncMode;
import com.iamkaf.konfig.impl.v1.bootstrap.RuntimeEnvironment;
import com.iamkaf.konfig.impl.v1.config.io.PathJson;
import com.iamkaf.konfig.impl.v1.config.io.PathToml;
import com.iamkaf.konfig.impl.v1.config.migration.ConfigMigrationContextImpl;
import com.iamkaf.konfig.impl.v1.config.migration.ConfigMigrationSupport;
import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
//? if >=1.21.11 {
import com.iamkaf.konfig.impl.v1.sync.ConfigEditTarget;
//?}

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApiStatus.Internal
public final class ConfigHandleImpl implements ConfigScreenHandle {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String modId;
    private final String name;
    private final ConfigScope scope;
    private final SyncMode syncMode;
    private final Path path;
    private final LinkedHashMap<String, ConfigValueImpl<?>> entries;
    private final LinkedHashMap<String, String> entryComments;
    private final LinkedHashMap<String, String> categoryComments;
    private final LinkedHashMap<String, TooltipText> entryTooltips;
    private final LinkedHashMap<String, String> categoryTooltips;
    private final List<InfoPanelItem> globalInfo;
    private final LinkedHashMap<String, List<InfoPanelItem>> categoryInfo;
    private final LinkedHashMap<String, List<InfoPanelItem>> entryInfo;
    private final String fileComment;
    private final int schemaVersion;
    private final LinkedHashMap<Integer, ConfigMigration> migrations;
    private final List<ConfigListener> listeners = new CopyOnWriteArrayList<ConfigListener>();
    private boolean newerSchemaReadOnly;
    private long revision;

    ConfigHandleImpl(
            String modId,
            String name,
            ConfigScope scope,
            SyncMode syncMode,
            Path path,
            LinkedHashMap<String, ConfigValueImpl<?>> entries,
            LinkedHashMap<String, String> entryComments,
            LinkedHashMap<String, String> categoryComments,
            LinkedHashMap<String, TooltipText> entryTooltips,
            LinkedHashMap<String, String> categoryTooltips,
            List<InfoPanelItem> globalInfo,
            LinkedHashMap<String, List<InfoPanelItem>> categoryInfo,
            LinkedHashMap<String, List<InfoPanelItem>> entryInfo,
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
        this.entryTooltips = entryTooltips;
        this.categoryTooltips = categoryTooltips;
        this.globalInfo = globalInfo == null ? Collections.emptyList() : Collections.unmodifiableList(globalInfo);
        this.categoryInfo = categoryInfo == null ? new LinkedHashMap<String, List<InfoPanelItem>>() : categoryInfo;
        this.entryInfo = entryInfo == null ? new LinkedHashMap<String, List<InfoPanelItem>>() : entryInfo;
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
    public synchronized void load() {
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
            try {
                Path preserved = PathToml.preserveBroken(this.path);
                configFound = false;
                Constants.LOG.warn(
                        "Failed reading config {}; preserved the broken file at {} and restored defaults.",
                        this.path.toAbsolutePath(),
                        preserved.toAbsolutePath(),
                        e
                );
            } catch (IOException preserveFailure) {
                throw new RuntimeException(
                        "Failed reading config " + this.path.toAbsolutePath()
                                + " and could not preserve the broken file",
                        preserveFailure
                );
            }
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
            write(root);
        }
        this.revision++;
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
    public synchronized void save() {
        requireWritable();
        write();
        this.revision++;
        notifyReload(ReloadCause.API_CALL);
    }

    private void write() {
        requireWritable();
        write(existingConfigForWrite());
    }

    private void write(CommentedConfig root) {
        requireWritable();
        try {
            Files.createDirectories(this.path.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed creating config directory for " + this.path, e);
        }

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
    public synchronized void reload() {
        load();
        notifyReload(ReloadCause.API_CALL);
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

    public String entryComment(String path) {
        return this.entryComments.getOrDefault(path, "");
    }

    public String categoryComment(String path) {
        return this.categoryComments.getOrDefault(path, "");
    }

    public String categoryTooltip(String path) {
        return this.categoryTooltips.getOrDefault(path, "");
    }

    public String fileComment() {
        return this.fileComment;
    }

    public int schemaVersion() {
        return this.schemaVersion;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Collection<ConfigScreenValue<?>> screenEntries() {
        return (Collection) screenValues();
    }

    public List<InfoPanelItem> globalInfo() {
        return this.globalInfo;
    }

    public List<InfoPanelItem> categoryInfo(String path) {
        List<InfoPanelItem> info = this.categoryInfo.get(path);
        return info == null ? Collections.emptyList() : info;
    }

    public List<InfoPanelItem> entryInfo(String path) {
        List<InfoPanelItem> info = this.entryInfo.get(path);
        return info == null ? Collections.emptyList() : info;
    }

    public String id() {
        return this.modId + ":" + this.name;
    }

    public String tooltip(String path, Function<String, String> translationResolver) {
        StringBuilder builder = new StringBuilder();
        String[] parts = path.split("\\.");
        StringBuilder categoryPath = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (categoryPath.length() > 0) {
                categoryPath.append('.');
            }
            categoryPath.append(parts[i]);
            appendComment(builder, this.categoryTooltips.get(categoryPath.toString()));
        }
        TooltipText entryTooltip = this.entryTooltips.get(path);
        appendComment(builder, entryTooltip == null ? "" : entryTooltip.resolve(translationResolver));
        return builder.toString();
    }

    public synchronized String snapshotJson() {
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

    public synchronized void applySyncSnapshot(String json) {
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

    public synchronized void clearSyncedValues() {
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
        this.newerSchemaReadOnly = false;
        if (!configFound) {
            ConfigMigrationSupport.writeSchemaVersion(root, this.schemaVersion);
            return true;
        }

        int fileVersion = ConfigMigrationSupport.readSchemaVersion(root);
        if (fileVersion > this.schemaVersion) {
            this.newerSchemaReadOnly = true;
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

    public synchronized long revision() {
        return this.revision;
    }

    public synchronized boolean newerSchemaReadOnly() {
        return this.newerSchemaReadOnly;
    }

//? if >=1.21.11 {
    ConfigEditTarget remoteEditTarget() {
        return new ConfigEditTarget() {
            @Override
            public String configId() {
                return ConfigHandleImpl.this.id();
            }

            @Override
            public long currentRevision() {
                return ConfigHandleImpl.this.revision();
            }

            @Override
            public String snapshotJson() {
                return ConfigHandleImpl.this.snapshotJson();
            }

            @Override
            public ApplyResult applyAtomic(long baseRevision, String completeDraftJson) {
                return ConfigHandleImpl.this.applyRemoteDraft(baseRevision, completeDraftJson);
            }
        };
    }

    private synchronized ConfigEditTarget.ApplyResult applyRemoteDraft(long baseRevision, String completeDraftJson) {
        if (baseRevision != this.revision) {
            return ConfigEditTarget.ApplyResult.stale(this.revision, snapshotJson());
        }
        if (this.newerSchemaReadOnly) {
            return ConfigEditTarget.ApplyResult.invalid(this.revision, "config uses a newer schema and is read-only");
        }

        JsonObject root;
        try {
            if (completeDraftJson == null) {
                throw new IllegalArgumentException("draft is missing");
            }
            root = new JsonParser().parse(completeDraftJson).getAsJsonObject();
        } catch (RuntimeException exception) {
            return ConfigEditTarget.ApplyResult.invalid(this.revision, "draft must be a JSON object");
        }

        List<ConfigValueImpl<?>> editable = this.entries.values().stream()
                .filter(ConfigValueImpl::persistent)
                .filter(ConfigValueImpl::sync)
                .filter(entry -> !entry.clientOnly())
                .collect(Collectors.toList());
        try {
            requireExactDraftShape(root, editable);
        } catch (IllegalArgumentException exception) {
            return ConfigEditTarget.ApplyResult.invalid(this.revision, exception.getMessage());
        }

        LinkedHashMap<ConfigValueImpl<?>, Object> decoded = new LinkedHashMap<ConfigValueImpl<?>, Object>();
        try {
            for (ConfigValueImpl<?> entry : editable) {
                decoded.put(entry, decodeRemoteValue(entry, PathJson.get(root, entry.path())));
            }
        } catch (RuntimeException exception) {
            return ConfigEditTarget.ApplyResult.invalid(this.revision, "draft contains an invalid value");
        }

        boolean changed = false;
        for (Map.Entry<ConfigValueImpl<?>, Object> value : decoded.entrySet()) {
            if (!Objects.equals(value.getKey().localValue(), value.getValue())) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            return ConfigEditTarget.ApplyResult.noOp(this.revision, snapshotJson());
        }

        LinkedHashMap<ConfigValueImpl<?>, Object> previous = new LinkedHashMap<ConfigValueImpl<?>, Object>();
        for (ConfigValueImpl<?> entry : editable) {
            previous.put(entry, entry.localValue());
        }
        try {
            decoded.forEach(ConfigHandleImpl::setUntypedLocal);
            write();
        } catch (RuntimeException exception) {
            previous.forEach(ConfigHandleImpl::setUntypedLocal);
            return ConfigEditTarget.ApplyResult.invalid(this.revision, "draft could not be persisted");
        }

        this.revision++;
        try {
            notifyReload(ReloadCause.API_CALL);
        } catch (RuntimeException exception) {
            Constants.LOG.warn("Config {} was saved but a reload listener failed", id(), exception);
        }
        return ConfigEditTarget.ApplyResult.accepted(this.revision, snapshotJson());
    }

    private static void requireExactDraftShape(JsonObject root, List<ConfigValueImpl<?>> entries) {
        DraftPath allowed = new DraftPath();
        for (ConfigValueImpl<?> entry : entries) {
            allowed.add(entry.path());
            if (PathJson.get(root, entry.path()) == null) {
                throw new IllegalArgumentException("draft is missing field " + entry.path());
            }
        }
        allowed.validate(root, "");
    }

    private static Object decodeRemoteValue(ConfigValueImpl<?> entry, com.google.gson.JsonElement element) {
        return entry.decodeStrict(element);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setUntypedLocal(ConfigValueImpl entry, Object value) {
        entry.setLocal(value);
    }

    private static final class DraftPath {
        private final Map<String, DraftPath> children = new LinkedHashMap<String, DraftPath>();
        private boolean terminal;

        private void add(String path) {
            DraftPath current = this;
            for (String segment : path.split("\\.")) {
                if (current.terminal) {
                    throw new IllegalStateException("Config path overlaps another value: " + path);
                }
                current = current.children.computeIfAbsent(segment, ignored -> new DraftPath());
            }
            if (!current.children.isEmpty()) {
                throw new IllegalStateException("Config path overlaps another value: " + path);
            }
            current.terminal = true;
        }

        private void validate(JsonObject object, String prefix) {
            for (Map.Entry<String, com.google.gson.JsonElement> member : object.entrySet()) {
                DraftPath next = this.children.get(member.getKey());
                String path = prefix.isEmpty() ? member.getKey() : prefix + "." + member.getKey();
                if (next == null) {
                    throw new IllegalArgumentException("draft contains unknown field " + path);
                }
                if (next.terminal) {
                    continue;
                }
                if (!member.getValue().isJsonObject()) {
                    throw new IllegalArgumentException("draft field " + path + " must be an object");
                }
                next.validate(member.getValue().getAsJsonObject(), path);
            }
        }
    }
//?}

    private void requireWritable() {
        if (this.newerSchemaReadOnly) {
            throw new IllegalStateException(
                    "Config " + id() + " is read-only because " + this.path.toAbsolutePath()
                            + " uses a newer schema version"
            );
        }
    }

    private CommentedConfig existingConfigForWrite() {
        if (!Files.exists(this.path)) {
            return TomlFormat.newConfig();
        }
        try {
            return PathToml.read(this.path);
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Refusing to overwrite unreadable config " + this.path.toAbsolutePath(),
                    exception
            );
        }
    }

    private static <T> void loadEntryFromToml(ConfigValueImpl<T> entry, CommentedConfig root) {
        entry.setLocal(entry.decodeOrFallback(PathToml.get(root, entry.path())));
    }

    private static <T> void syncEntryFromJson(ConfigValueImpl<T> entry, JsonObject root) {
        entry.setSynced(entry.decodeOrFallback(PathJson.get(root, entry.path())));
    }

    private void notifyReload(ReloadCause cause) {
        this.listeners.forEach(listener -> listener.onReload(this, cause));
        KonfigRuntime.configReloaded(this, cause);
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
