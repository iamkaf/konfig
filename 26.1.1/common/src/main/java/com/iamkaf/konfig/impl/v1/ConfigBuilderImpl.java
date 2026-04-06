package com.iamkaf.konfig.impl.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.iamkaf.konfig.api.v1.*;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ConfigBuilderImpl implements ConfigBuilder {
    private final String modId;
    private final String name;

    private ConfigScope scope = ConfigScope.COMMON;
    private SyncMode syncMode = SyncMode.LOGIN;
    private String fileName;
    private String fileComment = "";
    private int schemaVersion;

    private final ArrayDeque<String> categories = new ArrayDeque<String>();
    private final LinkedHashMap<String, ConfigValueImpl<?>> entries = new LinkedHashMap<String, ConfigValueImpl<?>>();
    private final LinkedHashMap<String, String> entryComments = new LinkedHashMap<String, String>();
    private final LinkedHashMap<String, String> categoryComments = new LinkedHashMap<String, String>();
    private final LinkedHashMap<Integer, ConfigMigration> migrations = new LinkedHashMap<Integer, ConfigMigration>();

    public ConfigBuilderImpl(String modId, String name) {
        this.modId = requireSimpleSegment(modId, "modId");
        this.name = requireSimpleSegment(name, "name");
        this.fileName = this.name + ".toml";
    }

    @Override
    public ConfigBuilder scope(ConfigScope scope) {
        this.scope = scope == null ? ConfigScope.COMMON : scope;
        return this;
    }

    @Override
    public ConfigBuilder syncMode(SyncMode mode) {
        this.syncMode = mode == null ? SyncMode.NONE : mode;
        return this;
    }

    @Override
    public ConfigBuilder fileName(String fileName) {
        if (!isBlank(fileName)) {
            this.fileName = normalizeFileName(fileName);
        }
        return this;
    }

    @Override
    public ConfigBuilder schemaVersion(int version) {
        ConfigMigrationSupport.validateSchemaVersion(version);
        this.schemaVersion = version;
        return this;
    }

    @Override
    public ConfigBuilder migrate(int fromVersion, ConfigMigration migration) {
        ConfigMigrationSupport.validateSchemaVersion(fromVersion);
        Objects.requireNonNull(migration, "migration");
        if (this.migrations.put(Integer.valueOf(fromVersion), migration) != null) {
            throw new IllegalStateException("Duplicate config migration from version " + fromVersion);
        }
        return this;
    }

    @Override
    public ConfigBuilder push(String category) {
        String segment = requireSimpleSegment(category, "category");
        if (this.categories.isEmpty() && ConfigMigrationSupport.isReservedRootSegment(segment)) {
            throw new IllegalArgumentException("category uses reserved Konfig metadata root: " + segment);
        }
        this.categories.push(segment);
        return this;
    }

    @Override
    public ConfigBuilder pop() {
        if (this.categories.isEmpty()) {
            throw new IllegalStateException("No category to pop");
        }
        this.categories.pop();
        return this;
    }

    @Override
    public ConfigBuilder comment(String comment) {
        this.fileComment = normalizeComment(comment);
        return this;
    }

    @Override
    public ConfigBuilder categoryComment(String comment) {
        if (this.categories.isEmpty()) {
            throw new IllegalStateException("No category to comment");
        }

        String path = currentCategoryPath();
        String normalized = normalizeComment(comment);
        if (isBlank(normalized)) {
            this.categoryComments.remove(path);
        } else {
            this.categoryComments.put(path, normalized);
        }
        return this;
    }

    @Override
    public ValueBuilder<Boolean> bool(String key, boolean defaultValue) {
        String path = path(key);
        return new ValueBuilderImpl<Boolean>(
                this,
                path,
                Boolean.valueOf(defaultValue),
                EntryKind.BOOLEAN,
                JsonElement::getAsBoolean,
                JsonPrimitive::new
        );
    }

    @Override
    public ValueBuilder<Integer> intRange(String key, int defaultValue, int min, int max) {
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("Default integer out of range");
        }
        String path = path(key);
        return new ValueBuilderImpl<Integer>(
                this,
                path,
                Integer.valueOf(defaultValue),
                EntryKind.INTEGER,
                JsonElement::getAsInt,
                JsonPrimitive::new
        ).range(Integer.valueOf(min), Integer.valueOf(max))
                .validate(value -> value != null && value.intValue() >= min && value.intValue() <= max, "Integer out of range");
    }

    @Override
    public ValueBuilder<Long> longRange(String key, long defaultValue, long min, long max) {
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("Default long out of range");
        }
        String path = path(key);
        return new ValueBuilderImpl<Long>(
                this,
                path,
                Long.valueOf(defaultValue),
                EntryKind.LONG,
                JsonElement::getAsLong,
                JsonPrimitive::new
        ).range(Long.valueOf(min), Long.valueOf(max))
                .validate(value -> value != null && value.longValue() >= min && value.longValue() <= max, "Long out of range");
    }

    @Override
    public ValueBuilder<Double> doubleRange(String key, double defaultValue, double min, double max) {
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("Default double out of range");
        }
        String path = path(key);
        return new ValueBuilderImpl<Double>(
                this,
                path,
                Double.valueOf(defaultValue),
                EntryKind.DOUBLE,
                JsonElement::getAsDouble,
                JsonPrimitive::new
        ).range(Double.valueOf(min), Double.valueOf(max))
                .validate(value -> value != null && value.doubleValue() >= min && value.doubleValue() <= max, "Double out of range");
    }

    @Override
    public StringValueBuilder string(String key, String defaultValue, int minLen, int maxLen) {
        Objects.requireNonNull(defaultValue, "defaultValue");
        if (defaultValue.length() < minLen || defaultValue.length() > maxLen) {
            throw new IllegalArgumentException("Default string length out of range");
        }
        String path = path(key);
        return new StringValueBuilderImpl(this, path, defaultValue)
                .validate(value -> value != null && value.length() >= minLen && value.length() <= maxLen, "String length out of range");
    }

    @Override
    public StringListValueBuilder stringList(String key, List<String> defaultValue) {
        Objects.requireNonNull(defaultValue, "defaultValue");
        String path = path(key);
        return new StringListValueBuilderImpl(
                this,
                path,
                StringListValueHelper.immutableCopy(defaultValue, path)
        ).canonicalize(value -> StringListValueHelper.immutableCopy(value, path));
    }

    @Override
    public <E extends Enum<E>> ValueBuilder<E> enumValue(String key, E defaultValue) {
        Objects.requireNonNull(defaultValue, "defaultValue");
        final Class<E> enumClass = (Class<E>) defaultValue.getDeclaringClass();
        String path = path(key);
        return new ValueBuilderImpl<E>(
                this,
                path,
                defaultValue,
                EntryKind.ENUM,
                json -> Enum.valueOf(enumClass, json.getAsString()),
                value -> new JsonPrimitive(value.name())
        );
    }

    @Override
    public ValueBuilder<Integer> colorRgb(String key, int defaultValue) {
        String path = path(key);
        ColorValueHelper.requireRgb(defaultValue, path);
        return new ValueBuilderImpl<Integer>(
                this,
                path,
                Integer.valueOf(defaultValue),
                EntryKind.COLOR_RGB,
                json -> Integer.valueOf(ColorValueHelper.parseRgb(json.getAsString(), path)),
                value -> new JsonPrimitive(ColorValueHelper.formatRgb(value.intValue()))
        ).canonicalize(value -> Integer.valueOf(ColorValueHelper.requireRgb(value.intValue(), path)));
    }

    @Override
    public ValueBuilder<Integer> colorArgb(String key, int defaultValue) {
        String path = path(key);
        return new ValueBuilderImpl<Integer>(
                this,
                path,
                Integer.valueOf(defaultValue),
                EntryKind.COLOR_ARGB,
                json -> Integer.valueOf(ColorValueHelper.parseArgb(json.getAsString(), path)),
                value -> new JsonPrimitive(ColorValueHelper.formatArgb(value.intValue()))
        );
    }

    @Override
    public <T> ValueBuilder<T> custom(String key, T defaultValue, KonfigCodec<T> codec) {
        Objects.requireNonNull(codec, "codec");
        String path = path(key);
        return new ValueBuilderImpl<T>(
                this,
                path,
                defaultValue,
                EntryKind.CUSTOM,
                json -> {
                    try {
                        return codec.decode(new KonfigNode(json));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                value -> {
                    try {
                        return codec.encode(value).json();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @Override
    public ConfigHandle build() {
        for (Integer fromVersion : this.migrations.keySet()) {
            if (fromVersion.intValue() >= this.schemaVersion) {
                throw new IllegalStateException(
                        "Migration from version " + fromVersion + " is unreachable for schema version " + this.schemaVersion
                );
            }
        }

        Path path = RuntimeEnvironment.configDirectory()
                .resolve(this.modId)
                .resolve(normalizeFileName(this.fileName));
        ConfigHandleImpl handle = new ConfigHandleImpl(
                this.modId,
                this.name,
                this.scope,
                this.syncMode,
                path,
                new LinkedHashMap<String, ConfigValueImpl<?>>(this.entries),
                new LinkedHashMap<String, String>(this.entryComments),
                new LinkedHashMap<String, String>(this.categoryComments),
                this.fileComment,
                this.schemaVersion,
                new LinkedHashMap<Integer, ConfigMigration>(this.migrations)
        );
        KonfigManager.get().register(handle);
        handle.load();
        return handle;
    }

    void addEntry(String path, ConfigValueImpl<?> value, String comment) {
        if (this.entries.containsKey(path)) {
            throw new IllegalStateException("Duplicate config key: " + path);
        }
        this.entries.put(path, value);
        if (isBlank(comment)) {
            this.entryComments.remove(path);
        } else {
            this.entryComments.put(path, normalizeComment(comment));
        }
    }

    private String path(String key) {
        String leaf = requireSimpleSegment(key, "key");
        String prefix = currentCategoryPath();
        if (isBlank(prefix) && ConfigMigrationSupport.isReservedRootSegment(leaf)) {
            throw new IllegalArgumentException("key uses reserved Konfig metadata root: " + leaf);
        }
        return isBlank(prefix) ? leaf : prefix + '.' + leaf;
    }

    private String currentCategoryPath() {
        if (this.categories.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        String[] stack = this.categories.toArray(new String[0]);
        for (int i = stack.length - 1; i >= 0; i--) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(stack[i]);
        }
        return builder.toString();
    }

    private static String normalizeFileName(String fileName) {
        String normalized = fileName == null ? "" : fileName.trim();
        if (isBlank(normalized)) {
            throw new IllegalArgumentException("fileName cannot be blank");
        }
        if (normalized.indexOf('/') >= 0 || normalized.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("fileName must not include path separators: " + normalized);
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".toml")) {
            return normalized;
        }
        if (normalized.indexOf('.') >= 0) {
            throw new IllegalArgumentException("Konfig only writes TOML files: " + normalized);
        }
        return normalized + ".toml";
    }

    private static String normalizeComment(String comment) {
        return isBlank(comment) ? "" : comment.trim();
    }

    private static String requireSimpleSegment(String value, String name) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        if (value.indexOf('.') >= 0 || value.indexOf('/') >= 0 || value.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(name + " contains unsupported characters: " + value);
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
