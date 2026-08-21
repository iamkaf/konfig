package com.iamkaf.konfig.impl.v1.config.builder;

import org.jetbrains.annotations.ApiStatus;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.iamkaf.konfig.api.v1.*;
import com.iamkaf.konfig.impl.v1.bootstrap.RuntimeEnvironment;
import com.iamkaf.konfig.impl.v1.config.migration.ConfigMigrationSupport;
import com.iamkaf.konfig.impl.v1.config.model.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.config.model.ConfigValueImpl;
import com.iamkaf.konfig.impl.v1.config.model.ColorValueHelper;
import com.iamkaf.konfig.impl.v1.config.model.DropdownOptionMetadata;
import com.iamkaf.konfig.impl.v1.config.model.EntryKind;
import com.iamkaf.konfig.impl.v1.config.model.InfoPanelItem;
import com.iamkaf.konfig.impl.v1.config.model.KonfigModels;
import com.iamkaf.konfig.impl.v1.config.model.KonfigManager;
import com.iamkaf.konfig.impl.v1.config.model.StringListValueHelper;
import com.iamkaf.konfig.impl.v1.config.model.TooltipText;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} elif >=1.17 {
import net.minecraft.resources.ResourceLocation;
//?}

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

@ApiStatus.Internal
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
    private final LinkedHashMap<String, TooltipText> entryTooltips = new LinkedHashMap<String, TooltipText>();
    private final LinkedHashMap<String, String> categoryTooltips = new LinkedHashMap<String, String>();
    private List<InfoPanelItem> globalInfo = java.util.Collections.emptyList();
    private final LinkedHashMap<String, List<InfoPanelItem>> categoryInfo = new LinkedHashMap<String, List<InfoPanelItem>>();
    private final LinkedHashMap<String, List<InfoPanelItem>> entryInfo = new LinkedHashMap<String, List<InfoPanelItem>>();
    private final LinkedHashMap<Integer, ConfigMigration> migrations = new LinkedHashMap<Integer, ConfigMigration>();
    private int inlineDecorationIndex;

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
    public ConfigBuilder categoryTooltip(String tooltip) {
        if (this.categories.isEmpty()) {
            throw new IllegalStateException("No category to attach tooltip");
        }

        String path = currentCategoryPath();
        String normalized = normalizeComment(tooltip);
        if (isBlank(normalized)) {
            this.categoryTooltips.remove(path);
        } else {
            this.categoryTooltips.put(path, normalized);
        }
        return this;
    }

    @Override
    public ConfigBuilder info(Consumer<InfoPanelBuilder> builder) {
        this.globalInfo = buildInfo(builder);
        return this;
    }

    @Override
    public ConfigBuilder categoryInfo(Consumer<InfoPanelBuilder> builder) {
        if (this.categories.isEmpty()) {
            throw new IllegalStateException("No category to attach info");
        }

        String path = currentCategoryPath();
        List<InfoPanelItem> items = buildInfo(builder);
        if (items.isEmpty()) {
            this.categoryInfo.remove(path);
        } else {
            this.categoryInfo.put(path, items);
        }
        return this;
    }

    @Override
    public ConfigBuilder header(String text) {
        addDecoration(EntryKind.HEADER, text, null);
        return this;
    }

    @Override
    public ConfigBuilder headerKey(String translationKey) {
        addDecoration(EntryKind.HEADER, translationKey, null, null, true);
        return this;
    }

//? if >=1.21.11 {
    @Override
    public ConfigBuilder image(Identifier textureId) {
//?} elif >=1.17 {
    @Override
    public ConfigBuilder image(ResourceLocation textureId) {
//?} else {
    @Override
    public ConfigBuilder image(Object textureId) {
//?}
        return image(textureId, "", ImageOptions.defaults());
    }

//? if >=1.21.11 {
    @Override
    public ConfigBuilder image(Identifier textureId, ImageOptions options) {
//?} elif >=1.17 {
    @Override
    public ConfigBuilder image(ResourceLocation textureId, ImageOptions options) {
//?} else {
    @Override
    public ConfigBuilder image(Object textureId, ImageOptions options) {
//?}
        return image(textureId, "", options);
    }

//? if >=1.21.11 {
    @Override
    public ConfigBuilder image(Identifier textureId, String caption) {
//?} elif >=1.17 {
    @Override
    public ConfigBuilder image(ResourceLocation textureId, String caption) {
//?} else {
    @Override
    public ConfigBuilder image(Object textureId, String caption) {
//?}
        return image(textureId, caption, ImageOptions.defaults());
    }

//? if >=1.21.11 {
    @Override
    public ConfigBuilder image(Identifier textureId, String caption, ImageOptions options) {
//?} elif >=1.17 {
    @Override
    public ConfigBuilder image(ResourceLocation textureId, String caption, ImageOptions options) {
//?} else {
    @Override
    public ConfigBuilder image(Object textureId, String caption, ImageOptions options) {
//?}
        Objects.requireNonNull(textureId, "textureId");
        addDecoration(EntryKind.IMAGE, caption, textureId.toString(), options);
        return this;
    }

    @Override
    public ConfigBuilder inlineText(String text) {
        addDecoration(EntryKind.INLINE_TEXT, text, null);
        return this;
    }

    @Override
    public ConfigBuilder url(String label, String url) {
        if (isBlank(url)) {
            throw new IllegalArgumentException("url cannot be blank");
        }
        addDecoration(EntryKind.URL, label, url.trim());
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
    public ValueBuilder<String> dropdown(String key, String defaultValue, List<String> options) {
        String path = path(key);
        List<DropdownOptionMetadata> normalizedOptions = normalizeDropdownOptions(options);
        List<String> normalizedValues = dropdownOptionValues(normalizedOptions);
        String normalizedDefault = normalizeDropdownOption(defaultValue, "defaultValue");
        if (!normalizedValues.contains(normalizedDefault)) {
            throw new IllegalArgumentException("Default dropdown value must be one of the declared options for " + path);
        }

        return new ValueBuilderImpl<String>(
                this,
                path,
                normalizedDefault,
                EntryKind.DROPDOWN,
                JsonElement::getAsString,
                JsonPrimitive::new
        ).dropdownOptions(normalizedOptions)
                .canonicalize(value -> value == null ? null : value.trim())
                .validate(value -> value != null && normalizedValues.contains(value), "Dropdown value not one of declared options");
    }

    @Override
    public ValueBuilder<String> dropdown(String key, String defaultValue, Consumer<DropdownOptionsBuilder> options) {
        Objects.requireNonNull(options, "options");
        String path = path(key);
        DropdownOptionsBuilderImpl builder = new DropdownOptionsBuilderImpl();
        options.accept(builder);
        List<DropdownOptionMetadata> normalizedOptions = builder.build();
        List<String> normalizedValues = dropdownOptionValues(normalizedOptions);
        String normalizedDefault = normalizeDropdownOption(defaultValue, "defaultValue");
        if (!normalizedValues.contains(normalizedDefault)) {
            throw new IllegalArgumentException("Default dropdown value must be one of the declared options for " + path);
        }

        return new ValueBuilderImpl<String>(
                this,
                path,
                normalizedDefault,
                EntryKind.DROPDOWN,
                JsonElement::getAsString,
                JsonPrimitive::new
        ).dropdownOptions(normalizedOptions)
                .canonicalize(value -> value == null ? null : value.trim())
                .validate(value -> value != null && normalizedValues.contains(value), "Dropdown value not one of declared options");
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
        ConfigHandleImpl handle = KonfigModels.configHandle(
                this.modId,
                this.name,
                this.scope,
                this.syncMode,
                path,
                new LinkedHashMap<String, ConfigValueImpl<?>>(this.entries),
                new LinkedHashMap<String, String>(this.entryComments),
                new LinkedHashMap<String, String>(this.categoryComments),
                new LinkedHashMap<String, TooltipText>(this.entryTooltips),
                new LinkedHashMap<String, String>(this.categoryTooltips),
                this.globalInfo,
                new LinkedHashMap<String, List<InfoPanelItem>>(this.categoryInfo),
                new LinkedHashMap<String, List<InfoPanelItem>>(this.entryInfo),
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

    void addEntryTooltip(String path, TooltipText tooltip) {
        if (tooltip == null || tooltip.isEmpty()) {
            this.entryTooltips.remove(path);
        } else {
            this.entryTooltips.put(path, tooltip);
        }
    }

    void addEntryInfo(String path, List<InfoPanelItem> info) {
        if (info == null || info.isEmpty()) {
            this.entryInfo.remove(path);
        } else {
            this.entryInfo.put(path, info);
        }
    }

    static List<InfoPanelItem> buildInfo(Consumer<InfoPanelBuilder> builder) {
        Objects.requireNonNull(builder, "builder");
        InfoPanelBuilderImpl info = new InfoPanelBuilderImpl();
        builder.accept(info);
        return info.build();
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

    private void addDecoration(EntryKind kind, String label, String target) {
        addDecoration(kind, label, target, null, false);
    }

    private void addDecoration(EntryKind kind, String label, String target, ImageOptions imageOptions) {
        addDecoration(kind, label, target, imageOptions, false);
    }

    private void addDecoration(EntryKind kind, String label, String target, ImageOptions imageOptions, boolean labelTranslationKey) {
        String normalizedLabel = label == null ? "" : label.trim();
        if (kind != EntryKind.IMAGE && isBlank(normalizedLabel)) {
            throw new IllegalArgumentException("decoration text cannot be blank");
        }

        String prefix = currentCategoryPath();
        String path = (isBlank(prefix) ? "" : prefix + ".") + "__inline_" + String.format(Locale.ROOT, "%04d", ++this.inlineDecorationIndex);
        ConfigValueImpl<String> entry = KonfigModels.inlineDecorationValue(path, kind, normalizedLabel, target, imageOptions, labelTranslationKey);
        this.entries.put(path, entry);
        this.entryComments.remove(path);
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

    private static List<DropdownOptionMetadata> normalizeDropdownOptions(List<String> options) {
        Objects.requireNonNull(options, "options");
        LinkedHashSet<String> unique = new LinkedHashSet<String>();
        for (String option : options) {
            String normalized = normalizeDropdownOption(option, "dropdown option");
            if (!unique.add(normalized)) {
                throw new IllegalArgumentException("Duplicate dropdown option: " + normalized);
            }
        }
        if (unique.isEmpty()) {
            throw new IllegalArgumentException("Dropdown options cannot be empty");
        }
        List<DropdownOptionMetadata> result = new ArrayList<DropdownOptionMetadata>();
        for (String value : unique) {
            result.add(KonfigModels.dropdownOption(
                    value,
                    "",
                    false,
                    TooltipText.empty(),
                    java.util.Collections.emptyList()
            ));
        }
        return java.util.Collections.unmodifiableList(result);
    }

    static String normalizeDropdownOption(String option, String name) {
        String normalized = option == null ? "" : option.trim();
        return requireSimpleSegment(normalized, name);
    }

    private static List<String> dropdownOptionValues(List<DropdownOptionMetadata> options) {
        List<String> values = new ArrayList<String>();
        for (DropdownOptionMetadata option : options) {
            values.add(option.value());
        }
        return java.util.Collections.unmodifiableList(values);
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
