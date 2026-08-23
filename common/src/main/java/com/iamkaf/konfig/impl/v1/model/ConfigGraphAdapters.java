package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ImageOptions;
import com.iamkaf.konfig.api.v1.RestartRequirement;
import com.iamkaf.konfig.impl.v1.config.model.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.config.model.ConfigValueImpl;
import com.iamkaf.konfig.impl.v1.config.model.EntryKind;
import com.iamkaf.konfig.impl.v1.config.model.InfoPanelItem;
import com.iamkaf.konfig.impl.v1.presentation.ConfigDependencyExplanation;
import com.iamkaf.konfig.impl.v1.presentation.ConfigDisplayMode;
import com.iamkaf.konfig.impl.v1.presentation.ConfigDisplayNode;
import com.iamkaf.konfig.impl.v1.presentation.ConfigImage;
import com.iamkaf.konfig.impl.v1.presentation.ConfigInformationContext;
import com.iamkaf.konfig.impl.v1.presentation.ConfigLegacyFlattening;
import com.iamkaf.konfig.impl.v1.presentation.ConfigNarration;
import com.iamkaf.konfig.impl.v1.presentation.ConfigPlacement;
import com.iamkaf.konfig.impl.v1.presentation.ConfigPresentationGraph;
import com.iamkaf.konfig.impl.v1.presentation.ConfigPresentationResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ApiStatus.Internal
public final class ConfigGraphAdapters {
    private ConfigGraphAdapters() {
    }

    public static ConfigGraph graph(Collection<ConfigHandleImpl> handles) {
        List<ConfigHandleImpl> snapshot = immutableHandles(handles);
        List<ConfigGraphResolver.ConfigSource> sources = new ArrayList<ConfigGraphResolver.ConfigSource>(snapshot.size());
        for (int index = 0; index < snapshot.size(); index++) {
            sources.add(new HandleGraphSource(snapshot.get(index), index));
        }
        return new ConfigGraphResolver().resolve(sources);
    }

    public static ConfigPresentationGraph presentationGraph(Collection<ConfigHandleImpl> handles) {
        List<ConfigHandleImpl> snapshot = immutableHandles(handles);
        return presentationGraph(graph(snapshot), snapshot);
    }

    public static ConfigPresentationGraph presentationGraph(ConfigGraph graph, Collection<ConfigHandleImpl> handles) {
        List<ConfigHandleImpl> snapshot = immutableHandles(handles);
        List<ConfigPresentationResolver.PresentationSource> sources = new ArrayList<ConfigPresentationResolver.PresentationSource>(
                snapshot.size()
        );
        for (ConfigHandleImpl handle : snapshot) {
            sources.add(new HandlePresentationSource(handle));
        }
        return new ConfigPresentationResolver().resolve(graph, sources);
    }

    private static List<ConfigHandleImpl> immutableHandles(Collection<ConfigHandleImpl> handles) {
        Objects.requireNonNull(handles, "handles");
        return Collections.unmodifiableList(new ArrayList<ConfigHandleImpl>(handles));
    }

    private static ConfigIdentity identity(ConfigHandleImpl handle) {
        return new ConfigIdentity(handle.modId(), handle.name());
    }

    private static ConfigText configLabel(ConfigHandleImpl handle) {
        return ConfigText.translated(
                java.util.Arrays.asList(
                        "konfig.config." + handle.modId() + "." + handle.name() + ".title",
                        "konfig.config." + handle.modId() + ".title",
                        handle.modId() + ".configuration.title"
                ),
                prettySegment(handle.name())
        );
    }

    private static ConfigText fieldLabel(ConfigHandleImpl handle, ConfigValueImpl<?> value) {
        String leaf = ConfigPath.parse(value.path()).lastSegment();
        return ConfigText.translated(
                java.util.Arrays.asList(
                        "konfig.config." + handle.modId() + "." + handle.name() + "." + value.path(),
                        handle.modId() + ".config." + leaf
                ),
                prettySegment(leaf)
        );
    }

    private static ConfigText inlineLabel(ConfigValueImpl<?> value) {
        if (value.inlineLabelTranslationKey()) {
            return ConfigText.translated(value.inlineLabel(), value.inlineLabel());
        }
        return ConfigText.literal(value.inlineLabel());
    }

    private static ConfigFieldKind fieldKind(EntryKind kind) {
        switch (kind) {
            case BOOLEAN:
                return ConfigFieldKind.BOOLEAN;
            case INTEGER:
                return ConfigFieldKind.INTEGER;
            case LONG:
                return ConfigFieldKind.LONG;
            case DOUBLE:
                return ConfigFieldKind.DOUBLE;
            case STRING:
                return ConfigFieldKind.STRING;
            case STRING_LIST:
                return ConfigFieldKind.STRING_LIST;
            case DROPDOWN:
                return ConfigFieldKind.DROPDOWN;
            case ENUM:
                return ConfigFieldKind.ENUM;
            case COLOR_RGB:
                return ConfigFieldKind.COLOR_RGB;
            case COLOR_ARGB:
                return ConfigFieldKind.COLOR_ARGB;
//? if >=1.21.11 {
            case FIELDSET:
                return ConfigFieldKind.FIELDSET;
//?}
            case CUSTOM:
                return ConfigFieldKind.CUSTOM;
            default:
                throw new IllegalArgumentException("Decoration entry is not a config field: " + kind);
        }
    }

    private static ConfigDisplayNode.Kind displayKind(EntryKind kind) {
        switch (kind) {
            case HEADER:
                return ConfigDisplayNode.Kind.HEADER;
            case IMAGE:
                return ConfigDisplayNode.Kind.IMAGE;
            case INLINE_TEXT:
                return ConfigDisplayNode.Kind.TEXT;
            case URL:
                return ConfigDisplayNode.Kind.LINK;
//? if >=1.21.11 {
            case FIELDSET:
                return ConfigDisplayNode.Kind.FIELDSET;
//?}
            default:
                return ConfigDisplayNode.Kind.FIELD;
        }
    }

    private static ConfigDisplayNode.Kind infoKind(EntryKind kind) {
        switch (kind) {
            case HEADER:
                return ConfigDisplayNode.Kind.HEADER;
            case IMAGE:
                return ConfigDisplayNode.Kind.IMAGE;
            case INLINE_TEXT:
                return ConfigDisplayNode.Kind.TEXT;
            case URL:
                return ConfigDisplayNode.Kind.LINK;
            default:
                throw new IllegalArgumentException("Unsupported information-panel item kind: " + kind);
        }
    }

    private static ConfigLegacyFlattening.Strategy legacyStrategy(ConfigDisplayNode.Kind kind) {
        switch (kind) {
            case FIELD:
//? if >=1.21.11 {
            case FIELDSET:
//?}
                return ConfigLegacyFlattening.Strategy.FIELD;
            case HEADER:
                return ConfigLegacyFlattening.Strategy.HEADER;
            case IMAGE:
                return ConfigLegacyFlattening.Strategy.IMAGE;
            case LINK:
            case ACTION:
                return ConfigLegacyFlattening.Strategy.LINK;
            case TEXT:
            case DIAGNOSTIC:
                return ConfigLegacyFlattening.Strategy.TEXT;
            case SEPARATOR:
            default:
                return ConfigLegacyFlattening.Strategy.IGNORE;
        }
    }

    private static ConfigImage image(String resource, ImageOptions options) {
        if (resource == null || resource.trim().isEmpty()) {
            return null;
        }
        ImageOptions resolved = options == null ? ImageOptions.defaults() : options;
        return new ConfigImage(
                resource,
                resolved.width(),
                resolved.height(),
                resolved.padding(),
                imageAlignment(resolved.align()),
                captionPosition(resolved.captionPosition())
        );
    }

    private static ConfigImage.Alignment imageAlignment(ImageOptions.Align alignment) {
        switch (alignment) {
            case CENTER:
                return ConfigImage.Alignment.CENTER;
            case RIGHT:
                return ConfigImage.Alignment.RIGHT;
            case LEFT:
            default:
                return ConfigImage.Alignment.LEFT;
        }
    }

    private static ConfigImage.CaptionPosition captionPosition(ImageOptions.CaptionPosition position) {
        switch (position) {
            case BELOW:
                return ConfigImage.CaptionPosition.BELOW;
            case NONE:
                return ConfigImage.CaptionPosition.NONE;
            case RIGHT:
            default:
                return ConfigImage.CaptionPosition.RIGHT;
        }
    }

    private static String categoryPath(String path) {
        return ConfigPath.parse(path).parent().value();
    }

    private static String groupKey(String categoryPath) {
        return categoryPath == null || categoryPath.isEmpty()
                ? ""
                : "group/" + categoryPath.replace('.', '/');
    }

    private static String stablePath(String path) {
        return path.replace('.', '/');
    }

    private static String combine(String first, String second) {
        String normalizedFirst = normalize(first);
        String normalizedSecond = normalize(second);
        if (normalizedFirst.isEmpty()) {
            return normalizedSecond;
        }
        if (normalizedSecond.isEmpty()) {
            return normalizedFirst;
        }
        return normalizedFirst + "\n\n" + normalizedSecond;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String prettySegment(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(raw.length());
        boolean capitalize = true;
        for (int index = 0; index < raw.length(); index++) {
            char character = raw.charAt(index);
            if (character == '_' || character == '-' || character == '.') {
                if (result.length() > 0 && result.charAt(result.length() - 1) != ' ') {
                    result.append(' ');
                }
                capitalize = true;
            } else if (capitalize) {
                result.append(Character.toUpperCase(character));
                capitalize = false;
            } else if (Character.isUpperCase(character) && index > 0 && Character.isLowerCase(raw.charAt(index - 1))) {
                result.append(' ').append(character);
            } else {
                result.append(Character.toLowerCase(character));
            }
        }
        return result.toString().trim();
    }

    private static final class HandleGraphSource implements ConfigGraphResolver.ConfigSource {
        private final ConfigHandleImpl handle;
        private final long order;
        private final List<ConfigGraphResolver.SectionSource> sections;
        private final List<ConfigGraphResolver.FieldSource> fields;

        private HandleGraphSource(ConfigHandleImpl handle, long order) {
            this.handle = handle;
            this.order = order;
            this.sections = sectionSources(handle);
            this.fields = fieldSources(handle);
        }

        @Override
        public ConfigIdentity identity() {
            return ConfigGraphAdapters.identity(this.handle);
        }

        @Override
        public long order() {
            return this.order;
        }

        @Override
        public com.iamkaf.konfig.api.v1.ConfigScope scope() {
            return this.handle.scope();
        }

        @Override
        public com.iamkaf.konfig.api.v1.SyncMode syncMode() {
            return this.handle.syncMode();
        }

        @Override
        public int schemaVersion() {
            return this.handle.schemaVersion();
        }

        @Override
        public java.nio.file.Path storagePath() {
            return this.handle.path();
        }

        @Override
        public ConfigText label() {
            return configLabel(this.handle);
        }

        @Override
        public ConfigText description() {
            return ConfigText.literal(this.handle.fileComment());
        }

        @Override
        public Collection<? extends ConfigGraphResolver.SectionSource> sections() {
            return this.sections;
        }

        @Override
        public Collection<? extends ConfigGraphResolver.FieldSource> fields() {
            return this.fields;
        }
    }

    private static List<ConfigGraphResolver.SectionSource> sectionSources(ConfigHandleImpl handle) {
        Map<String, Long> categories = new LinkedHashMap<String, Long>();
        long order = 0L;
        for (ConfigValueImpl<?> value : handle.screenValues()) {
            String category = categoryPath(value.path());
            while (!category.isEmpty()) {
                if (!categories.containsKey(category)) {
                    categories.put(category, Long.valueOf(order));
                }
                category = ConfigPath.parse(category).parent().value();
            }
            order++;
        }

        List<ConfigGraphResolver.SectionSource> result = new ArrayList<ConfigGraphResolver.SectionSource>(categories.size());
        for (Map.Entry<String, Long> category : categories.entrySet()) {
            result.add(new HandleSectionSource(handle, category.getKey(), category.getValue().longValue()));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<ConfigGraphResolver.FieldSource> fieldSources(ConfigHandleImpl handle) {
        List<ConfigGraphResolver.FieldSource> result = new ArrayList<ConfigGraphResolver.FieldSource>();
        long order = 0L;
        for (ConfigValueImpl<?> value : handle.screenValues()) {
            if (!value.isDecoration()) {
                result.add(new HandleFieldSource(handle, value, order));
            }
            order++;
        }
        return Collections.unmodifiableList(result);
    }

    private static final class HandleSectionSource implements ConfigGraphResolver.SectionSource {
        private final ConfigHandleImpl handle;
        private final String path;
        private final long order;

        private HandleSectionSource(ConfigHandleImpl handle, String path, long order) {
            this.handle = handle;
            this.path = path;
            this.order = order;
        }

        @Override
        public String path() {
            return this.path;
        }

        @Override
        public long order() {
            return this.order;
        }

        @Override
        public ConfigText label() {
            return ConfigText.literal(prettySegment(ConfigPath.parse(this.path).lastSegment()));
        }

        @Override
        public ConfigText description() {
            return ConfigText.literal(combine(this.handle.categoryComment(this.path), this.handle.categoryTooltip(this.path)));
        }
    }

    private static final class HandleFieldSource implements ConfigGraphResolver.FieldSource {
        private final ConfigHandleImpl handle;
        private final ConfigValueImpl<?> value;
        private final long order;

        private HandleFieldSource(ConfigHandleImpl handle, ConfigValueImpl<?> value, long order) {
            this.handle = handle;
            this.value = value;
            this.order = order;
        }

        @Override
        public String path() {
            return this.value.path();
        }

        @Override
        public long order() {
            return this.order;
        }

        @Override
        public ConfigFieldKind kind() {
            return fieldKind(this.value.kind());
        }

        @Override
        public ConfigText label() {
            return fieldLabel(this.handle, this.value);
        }

        @Override
        public ConfigText description() {
            return ConfigText.literal(this.handle.entryComment(this.value.path()));
        }

        @Override
        public ConfigText tooltip() {
            return ConfigText.literal(this.handle.tooltip(this.value.path(), key -> key));
        }

        @Override
        public boolean persistent() {
            return this.value.persistent();
        }

        @Override
        public boolean synchronizedValue() {
            return this.value.sync();
        }

        @Override
        public boolean clientOnly() {
            return this.value.clientOnly();
        }

        @Override
        public boolean serverOnly() {
            return this.value.serverOnly();
        }

        @Override
        public RestartRequirement restartRequirement() {
            return this.value.restartRequirement();
        }

        @Override
        public Set<String> tags() {
            Set<String> tags = new LinkedHashSet<String>();
            if (this.value.hasBoundRegistry()) {
                tags.add("registry");
            }
            if (!this.value.persistent()) {
                tags.add("virtual");
            }
            return tags;
        }
    }

    private static final class HandlePresentationSource implements ConfigPresentationResolver.PresentationSource {
        private final ConfigHandleImpl handle;
        private final List<ConfigPresentationResolver.NodeSource> nodes;

        private HandlePresentationSource(ConfigHandleImpl handle) {
            this.handle = handle;
            this.nodes = presentationNodes(handle);
        }

        @Override
        public ConfigIdentity config() {
            return identity(this.handle);
        }

        @Override
        public Collection<? extends ConfigPresentationResolver.NodeSource> nodes() {
            return this.nodes;
        }
    }

    private static List<ConfigPresentationResolver.NodeSource> presentationNodes(ConfigHandleImpl handle) {
        List<ConfigPresentationResolver.NodeSource> result = new ArrayList<ConfigPresentationResolver.NodeSource>();
        long order = 0L;
        for (ConfigValueImpl<?> value : handle.screenValues()) {
            result.add(new HandleDisplayNodeSource(handle, value, order++));
        }

        addInfoNodes(result, handle, "global", "", ConfigInformationContext.config(identity(handle)), handle.globalInfo());
        for (ConfigValueImpl<?> value : handle.screenValues()) {
            String category = categoryPath(value.path());
            if (!category.isEmpty()) {
                List<InfoPanelItem> categoryInfo = handle.categoryInfo(category);
                if (!categoryInfo.isEmpty() && !containsInfoPrefix(result, "info/category/" + stablePath(category) + '/')) {
                    addInfoNodes(
                            result,
                            handle,
                            "category/" + stablePath(category),
                            category,
                            ConfigInformationContext.section(new ConfigSectionIdentity(identity(handle), ConfigPath.parse(category))),
                            categoryInfo
                    );
                }
            }
            if (!value.isDecoration()) {
                addInfoNodes(
                        result,
                        handle,
                        "field/" + stablePath(value.path()),
                        category,
                        ConfigInformationContext.field(new ConfigFieldIdentity(identity(handle), ConfigPath.parse(value.path()))),
                        handle.entryInfo(value.path())
                );
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static boolean containsInfoPrefix(List<ConfigPresentationResolver.NodeSource> nodes, String prefix) {
        for (ConfigPresentationResolver.NodeSource node : nodes) {
            if (node.key().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static void addInfoNodes(
            List<ConfigPresentationResolver.NodeSource> result,
            ConfigHandleImpl handle,
            String ownerKey,
            String categoryPath,
            ConfigInformationContext context,
            List<InfoPanelItem> items
    ) {
        for (int index = 0; index < items.size(); index++) {
            result.add(new InfoDisplayNodeSource(
                    handle,
                    items.get(index),
                    "info/" + ownerKey + '/' + index,
                    categoryPath,
                    index,
                    context
            ));
        }
    }

    private static final class HandleDisplayNodeSource implements ConfigPresentationResolver.NodeSource {
        private final ConfigHandleImpl handle;
        private final ConfigValueImpl<?> value;
        private final long order;

        private HandleDisplayNodeSource(ConfigHandleImpl handle, ConfigValueImpl<?> value, long order) {
            this.handle = handle;
            this.value = value;
            this.order = order;
        }

        @Override
        public String key() {
            return (this.value.isDecoration() ? "decoration/" : "field/") + stablePath(this.value.path());
        }

        @Override
        public long order() {
            return this.order;
        }

        @Override
        public ConfigDisplayNode.Kind kind() {
            return displayKind(this.value.kind());
        }

        @Override
        public String fieldPath() {
            return this.value.isDecoration() ? "" : this.value.path();
        }

        @Override
        public String groupKey() {
            return ConfigGraphAdapters.groupKey(categoryPath(this.value.path()));
        }

        @Override
        public Set<ConfigDisplayMode> displayModes() {
            return EnumSet.allOf(ConfigDisplayMode.class);
        }

        @Override
        public ConfigText label() {
            return this.value.isDecoration() ? inlineLabel(this.value) : fieldLabel(this.handle, this.value);
        }

        @Override
        public ConfigText description() {
            return ConfigText.literal(this.handle.entryComment(this.value.path()));
        }

        @Override
        public ConfigText tooltip() {
            if (this.value.kind() == EntryKind.URL && !normalize(this.value.inlineTarget()).isEmpty()) {
                return ConfigText.literal(this.value.inlineTarget());
            }
            return ConfigText.literal(this.handle.tooltip(this.value.path(), key -> key));
        }

        @Override
        public String target() {
            return this.value.inlineTarget();
        }

        @Override
        public ConfigImage image() {
            return this.value.kind() == EntryKind.IMAGE
                    ? ConfigGraphAdapters.image(this.value.inlineTarget(), this.value.imageOptions())
                    : null;
        }

        @Override
        public List<ConfigDependencyExplanation> dependencyExplanations() {
            if (this.value.isDecoration() || this.value.restartRequirement() == RestartRequirement.NONE) {
                return Collections.emptyList();
            }
            ConfigFieldIdentity field = new ConfigFieldIdentity(identity(this.handle), ConfigPath.parse(this.value.path()));
            String message = this.value.restartRequirement() == RestartRequirement.GAME
                    ? "Requires restarting the game"
                    : "Requires reloading or reopening the world";
            return Collections.singletonList(new ConfigDependencyExplanation(
                    ConfigDependencyExplanation.Effect.RESTART_REQUIRED,
                    ConfigText.literal(message),
                    Collections.singletonList(field)
            ));
        }

        @Override
        public ConfigNarration narration() {
            return new ConfigNarration(label(), tooltip(), ConfigText.empty(), ConfigText.empty());
        }

        @Override
        public ConfigLegacyFlattening legacyFlattening() {
            return new ConfigLegacyFlattening(
                    ConfigPath.parse(categoryPath(this.value.path())),
                    legacyStrategy(kind()),
                    ConfigText.empty()
            );
        }
    }

    private static final class InfoDisplayNodeSource implements ConfigPresentationResolver.NodeSource {
        private final ConfigHandleImpl handle;
        private final InfoPanelItem item;
        private final String key;
        private final String categoryPath;
        private final long order;
        private final ConfigInformationContext context;

        private InfoDisplayNodeSource(
                ConfigHandleImpl handle,
                InfoPanelItem item,
                String key,
                String categoryPath,
                long order,
                ConfigInformationContext context
        ) {
            this.handle = handle;
            this.item = item;
            this.key = key;
            this.categoryPath = categoryPath;
            this.order = order;
            this.context = context;
        }

        @Override
        public String key() {
            return this.key;
        }

        @Override
        public long order() {
            return this.order;
        }

        @Override
        public ConfigDisplayNode.Kind kind() {
            return infoKind(this.item.kind);
        }

        @Override
        public String groupKey() {
            return ConfigGraphAdapters.groupKey(this.categoryPath);
        }

        @Override
        public ConfigPlacement.Region region() {
            return ConfigPlacement.Region.INFO_PANEL;
        }

        @Override
        public Set<ConfigDisplayMode> displayModes() {
            return EnumSet.allOf(ConfigDisplayMode.class);
        }

        @Override
        public ConfigText label() {
            return this.item.labelTranslationKey
                    ? ConfigText.translated(this.item.label, this.item.label)
                    : ConfigText.literal(this.item.label);
        }

        @Override
        public String target() {
            return this.item.target;
        }

        @Override
        public ConfigImage image() {
            return this.item.kind == EntryKind.IMAGE
                    ? ConfigGraphAdapters.image(this.item.target, this.item.imageOptions)
                    : null;
        }

        @Override
        public ConfigNarration narration() {
            return new ConfigNarration(label(), ConfigText.empty(), ConfigText.empty(), ConfigText.empty());
        }

        @Override
        public ConfigLegacyFlattening legacyFlattening() {
            return new ConfigLegacyFlattening(
                    ConfigPath.parse(this.categoryPath),
                    legacyStrategy(kind()),
                    ConfigText.empty()
            );
        }

        @Override
        public ConfigInformationContext informationContext() {
            return this.context;
        }
    }
}
