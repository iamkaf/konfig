package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.RestartRequirement;
import com.iamkaf.konfig.impl.v1.model.ConfigDefinition;
import com.iamkaf.konfig.impl.v1.model.ConfigField;
import com.iamkaf.konfig.impl.v1.model.ConfigFieldIdentity;
import com.iamkaf.konfig.impl.v1.model.ConfigFieldKind;
import com.iamkaf.konfig.impl.v1.model.ConfigGraph;
import com.iamkaf.konfig.impl.v1.model.ConfigIdentity;
import com.iamkaf.konfig.impl.v1.model.ConfigPath;
import com.iamkaf.konfig.impl.v1.model.ConfigSection;
import com.iamkaf.konfig.impl.v1.model.ConfigText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ApiStatus.Internal
public final class ConfigPresentationResolver {
    public static final String DEFAULT_PAGE_KEY = "page/main";
    public static final String DEFAULT_TAB_KEY = "tab/general";

    public ConfigPresentationGraph resolve(ConfigGraph graph) {
        return resolve(graph, Collections.<PresentationSource>emptyList());
    }

    public ConfigPresentationGraph resolve(ConfigGraph graph, Collection<? extends PresentationSource> sources) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(sources, "sources");

        Map<ConfigIdentity, PresentationSource> sourcesByConfig = indexSources(graph, sources);
        List<ConfigPage> pages = new ArrayList<ConfigPage>();
        List<ConfigTab> tabs = new ArrayList<ConfigTab>();
        List<ConfigGroup> groups = new ArrayList<ConfigGroup>();
        List<ConfigDisplayNode> nodes = new ArrayList<ConfigDisplayNode>();

        for (ConfigDefinition config : graph.configs()) {
            PresentationSource source = sourcesByConfig.get(config.identity());
            ResolvedConfigPresentation resolved = resolveConfig(graph, config, source);
            pages.addAll(resolved.pages);
            tabs.addAll(resolved.tabs);
            groups.addAll(resolved.groups);
            nodes.addAll(resolved.nodes);
        }

        return new ConfigPresentationGraph(graph, pages, tabs, groups, nodes);
    }

    private static Map<ConfigIdentity, PresentationSource> indexSources(
            ConfigGraph graph,
            Collection<? extends PresentationSource> sources
    ) {
        Map<ConfigIdentity, PresentationSource> result = new LinkedHashMap<ConfigIdentity, PresentationSource>();
        for (PresentationSource source : sources) {
            ConfigIdentity identity = Objects.requireNonNull(source.config(), "presentation source config");
            if (graph.config(identity) == null) {
                throw new IllegalStateException("Presentation source references an unknown config: " + identity);
            }
            if (result.put(identity, source) != null) {
                throw new IllegalStateException("Duplicate presentation source for config: " + identity);
            }
        }
        return result;
    }

    private static ResolvedConfigPresentation resolveConfig(
            ConfigGraph graph,
            ConfigDefinition config,
            PresentationSource source
    ) {
        ConfigIdentity configId = config.identity();
        Map<ConfigPresentationIdentity, ConfigPage> pages = new LinkedHashMap<ConfigPresentationIdentity, ConfigPage>();
        Map<ConfigPresentationIdentity, ConfigTab> tabs = new LinkedHashMap<ConfigPresentationIdentity, ConfigTab>();
        Map<ConfigPresentationIdentity, ConfigGroup> groups = new LinkedHashMap<ConfigPresentationIdentity, ConfigGroup>();

        if (source != null) {
            for (PageSource page : source.pages()) {
                ConfigPresentationIdentity identity = identity(configId, page.key());
                putUnique(pages, identity, new ConfigPage(identity, page.order(), page.label(), page.description()), "page");
            }
        }
        ConfigPresentationIdentity defaultPage = identity(configId, DEFAULT_PAGE_KEY);
        pages.putIfAbsent(defaultPage, new ConfigPage(defaultPage, Long.MIN_VALUE, config.label(), config.description()));

        if (source != null) {
            for (TabSource tab : source.tabs()) {
                ConfigPresentationIdentity identity = identity(configId, tab.key());
                ConfigPresentationIdentity page = identity(configId, keyOrDefault(tab.pageKey(), DEFAULT_PAGE_KEY));
                putUnique(tabs, identity, new ConfigTab(identity, page, tab.order(), tab.label(), tab.description()), "tab");
            }
        }
        ConfigPresentationIdentity defaultTab = identity(configId, DEFAULT_TAB_KEY);
        tabs.putIfAbsent(defaultTab, new ConfigTab(defaultTab, defaultPage, Long.MIN_VALUE, config.label(), config.description()));

        addDefaultSectionGroups(config, defaultPage, defaultTab, groups);
        if (source != null) {
            for (GroupSource group : source.groups()) {
                ConfigPresentationIdentity identity = identity(configId, group.key());
                ConfigPresentationIdentity page = identity(configId, keyOrDefault(group.pageKey(), DEFAULT_PAGE_KEY));
                ConfigPresentationIdentity tab = optionalIdentity(configId, keyOrDefault(group.tabKey(), DEFAULT_TAB_KEY));
                ConfigPresentationIdentity parent = optionalIdentity(configId, group.parentGroupKey());
                ConfigPlacement placement = new ConfigPlacement(page, tab, parent, ConfigPlacement.Region.BODY);
                groups.put(identity, new ConfigGroup(
                        identity,
                        placement,
                        group.order(),
                        group.label(),
                        group.description(),
                        group.collapsible(),
                        group.expandedByDefault()
                ));
            }
        }

        validateContainers(configId, pages, tabs, groups);
        List<ConfigDisplayNode> nodes = resolveNodes(graph, config, source, pages, tabs, groups, defaultPage, defaultTab);

        List<ConfigPage> orderedPages = new ArrayList<ConfigPage>(pages.values());
        List<ConfigTab> orderedTabs = new ArrayList<ConfigTab>(tabs.values());
        List<ConfigGroup> orderedGroups = new ArrayList<ConfigGroup>(groups.values());
        Collections.sort(orderedPages, Comparator.comparingLong(ConfigPage::order).thenComparing(page -> page.identity().key()));
        Collections.sort(orderedTabs, Comparator.comparingLong(ConfigTab::order).thenComparing(tab -> tab.identity().key()));
        Collections.sort(orderedGroups, Comparator.comparingLong(ConfigGroup::order).thenComparing(group -> group.identity().key()));
        Collections.sort(nodes, Comparator.comparingLong(ConfigDisplayNode::order).thenComparing(node -> node.identity().key()));
        return new ResolvedConfigPresentation(orderedPages, orderedTabs, orderedGroups, nodes);
    }

    private static void addDefaultSectionGroups(
            ConfigDefinition config,
            ConfigPresentationIdentity page,
            ConfigPresentationIdentity tab,
            Map<ConfigPresentationIdentity, ConfigGroup> groups
    ) {
        for (ConfigSection section : config.sections()) {
            ConfigPath path = section.identity().path();
            if (path.isRoot()) {
                continue;
            }
            ConfigPresentationIdentity identity = identity(config.identity(), sectionGroupKey(path));
            ConfigPath parentPath = path.parent();
            ConfigPresentationIdentity parent = parentPath.isRoot()
                    ? null
                    : identity(config.identity(), sectionGroupKey(parentPath));
            ConfigText label = section.label().isEmpty()
                    ? ConfigText.literal(prettySegment(path.lastSegment()))
                    : section.label();
            groups.put(identity, new ConfigGroup(
                    identity,
                    new ConfigPlacement(page, tab, parent, ConfigPlacement.Region.BODY),
                    section.order(),
                    label,
                    section.description(),
                    false,
                    true
            ));
        }
    }

    private static List<ConfigDisplayNode> resolveNodes(
            ConfigGraph graph,
            ConfigDefinition config,
            PresentationSource source,
            Map<ConfigPresentationIdentity, ConfigPage> pages,
            Map<ConfigPresentationIdentity, ConfigTab> tabs,
            Map<ConfigPresentationIdentity, ConfigGroup> groups,
            ConfigPresentationIdentity defaultPage,
            ConfigPresentationIdentity defaultTab
    ) {
        Map<ConfigPresentationIdentity, ConfigDisplayNode> nodes = new LinkedHashMap<ConfigPresentationIdentity, ConfigDisplayNode>();
        Set<ConfigFieldIdentity> displayedFields = new LinkedHashSet<ConfigFieldIdentity>();
        if (source != null) {
            for (NodeSource nodeSource : source.nodes()) {
                ConfigPresentationIdentity identity = identity(config.identity(), nodeSource.key());
                ConfigField field = resolveField(graph, config.identity(), nodeSource.fieldPath());
                ConfigPlacement placement = resolvePlacement(config.identity(), nodeSource, defaultPage, defaultTab);
                validatePlacement(identity, placement, pages, tabs, groups);
                ConfigDisplayNode node = createNode(config, field, identity, placement, nodeSource, pages, tabs, groups);
                putUnique(nodes, identity, node, "display node");
                if (field != null && !displayedFields.add(field.identity())) {
                    throw new IllegalStateException("Config field has more than one display node: " + field.identity());
                }
            }
        }

        for (ConfigField field : config.fields()) {
            if (displayedFields.contains(field.identity())) {
                continue;
            }
            ConfigPresentationIdentity group = field.section().path().isRoot()
                    ? null
                    : identity(config.identity(), sectionGroupKey(field.section().path()));
            ConfigPlacement placement = new ConfigPlacement(defaultPage, defaultTab, group, ConfigPlacement.Region.BODY);
            ConfigPresentationIdentity identity = identity(config.identity(), "field/" + field.identity().path().value());
            ConfigDisplayNode.Kind kind = ConfigDisplayNode.Kind.FIELD;
//? if >=1.21.11 {
            if (field.kind() == ConfigFieldKind.FIELDSET) {
                kind = ConfigDisplayNode.Kind.FIELDSET;
            }
//?}
            ConfigDisplayNode node = createDefaultFieldNode(config, field, identity, placement, kind, pages, tabs, groups);
            putUnique(nodes, identity, node, "display node");
        }
        return new ArrayList<ConfigDisplayNode>(nodes.values());
    }

    private static ConfigDisplayNode createNode(
            ConfigDefinition config,
            ConfigField field,
            ConfigPresentationIdentity identity,
            ConfigPlacement placement,
            NodeSource source,
            Map<ConfigPresentationIdentity, ConfigPage> pages,
            Map<ConfigPresentationIdentity, ConfigTab> tabs,
            Map<ConfigPresentationIdentity, ConfigGroup> groups
    ) {
        boolean fieldKind = source.kind() == ConfigDisplayNode.Kind.FIELD;
//? if >=1.21.11 {
        fieldKind = fieldKind || source.kind() == ConfigDisplayNode.Kind.FIELDSET;
//?}
        if (fieldKind != (field != null)) {
            throw new IllegalStateException("Display node field reference does not match its kind: " + identity);
        }
//? if >=1.21.11 {
        if (field != null && (source.kind() == ConfigDisplayNode.Kind.FIELDSET) != (field.kind() == ConfigFieldKind.FIELDSET)) {
            throw new IllegalStateException("Fieldset display kind does not match config field kind: " + identity);
        }
//?}
        ConfigSearchMetadata search = source.search();
        if (search == null) {
            search = defaultSearch(field, source.label(), source.description(), source.tooltip(), locationLabels(placement, pages, tabs, groups));
        }
        ConfigNavigationPath navigation = navigation(identity, placement, config, pages, tabs, groups);
        ConfigLegacyFlattening legacy = source.legacyFlattening();
        if (legacy == null) {
            ConfigPath category = field == null ? ConfigPath.root() : field.section().path();
            legacy = new ConfigLegacyFlattening(category, defaultLegacyStrategy(source.kind()), ConfigText.empty());
        }
        return new ConfigDisplayNode(
                identity,
                source.order(),
                source.kind(),
                field == null ? null : field.identity(),
                placement,
                source.displayModes(),
                source.label(),
                source.description(),
                source.tooltip(),
                source.target(),
                source.image(),
                search,
                navigation,
                source.conditions(),
                source.dependencyExplanations(),
                source.narration(),
                legacy,
                source.informationContext()
        );
    }

    private static ConfigDisplayNode createDefaultFieldNode(
            ConfigDefinition config,
            ConfigField field,
            ConfigPresentationIdentity identity,
            ConfigPlacement placement,
            ConfigDisplayNode.Kind kind,
            Map<ConfigPresentationIdentity, ConfigPage> pages,
            Map<ConfigPresentationIdentity, ConfigTab> tabs,
            Map<ConfigPresentationIdentity, ConfigGroup> groups
    ) {
        List<ConfigText> locations = locationLabels(placement, pages, tabs, groups);
        ConfigSearchMetadata search = defaultSearch(field, field.label(), field.description(), field.tooltip(), locations);
        List<ConfigDependencyExplanation> explanations = new ArrayList<ConfigDependencyExplanation>();
        if (field.restartRequirement() != RestartRequirement.NONE) {
            explanations.add(new ConfigDependencyExplanation(
                    ConfigDependencyExplanation.Effect.RESTART_REQUIRED,
                    ConfigText.literal(restartFallback(field.restartRequirement())),
                    Collections.singletonList(field.identity())
            ));
        }
        return new ConfigDisplayNode(
                identity,
                field.order(),
                kind,
                field.identity(),
                placement,
                EnumSet.allOf(ConfigDisplayMode.class),
                field.label(),
                field.description(),
                field.tooltip(),
                "",
                null,
                search,
                navigation(identity, placement, config, pages, tabs, groups),
                ConfigPresentationConditions.always(),
                explanations,
                new ConfigNarration(field.label(), field.description(), ConfigText.empty(), ConfigText.empty()),
                new ConfigLegacyFlattening(field.section().path(), ConfigLegacyFlattening.Strategy.FIELD, ConfigText.empty()),
                null
        );
    }

    private static ConfigSearchMetadata defaultSearch(
            ConfigField field,
            ConfigText label,
            ConfigText description,
            ConfigText tooltip,
            List<ConfigText> locations
    ) {
        return new ConfigSearchMetadata(
                field == null ? null : field.identity(),
                label,
                description,
                tooltip,
                locations,
                field == null ? null : field.kind(),
                field == null ? Collections.<String>emptySet() : new LinkedHashSet<String>(field.aliases()),
                field == null ? Collections.<String>emptySet() : searchTags(field)
        );
    }

    private static Set<String> searchTags(ConfigField field) {
        Set<String> result = new LinkedHashSet<String>(field.tags());
        result.add(field.kind().name().toLowerCase(java.util.Locale.ROOT));
        if (field.synchronizedValue()) {
            result.add("synchronized");
            result.add("permission-controlled");
        }
        if (field.clientOnly()) {
            result.add("client-only");
        }
        if (field.serverOnly()) {
            result.add("server-only");
        }
        if (field.restartRequirement() != RestartRequirement.NONE) {
            result.add("restart-" + field.restartRequirement().name().toLowerCase(java.util.Locale.ROOT));
        }
        return result;
    }

    private static ConfigNavigationPath navigation(
            ConfigPresentationIdentity target,
            ConfigPlacement placement,
            ConfigDefinition config,
            Map<ConfigPresentationIdentity, ConfigPage> pages,
            Map<ConfigPresentationIdentity, ConfigTab> tabs,
            Map<ConfigPresentationIdentity, ConfigGroup> groups
    ) {
        List<ConfigNavigationPath.Breadcrumb> breadcrumbs = new ArrayList<ConfigNavigationPath.Breadcrumb>();
        ConfigPresentationIdentity configBreadcrumb = identity(config.identity(), "config");
        breadcrumbs.add(new ConfigNavigationPath.Breadcrumb(ConfigNavigationPath.Kind.CONFIG, configBreadcrumb, config.label()));
        ConfigPage page = pages.get(placement.page());
        breadcrumbs.add(new ConfigNavigationPath.Breadcrumb(
                ConfigNavigationPath.Kind.PAGE,
                placement.page(),
                page == null ? ConfigText.empty() : page.label()
        ));
        if (placement.tab() != null) {
            ConfigTab tab = tabs.get(placement.tab());
            breadcrumbs.add(new ConfigNavigationPath.Breadcrumb(
                    ConfigNavigationPath.Kind.TAB,
                    placement.tab(),
                    tab == null ? ConfigText.empty() : tab.label()
            ));
        }
        if (placement.group() != null) {
            addGroupBreadcrumbs(breadcrumbs, placement.group(), groups, new HashSet<ConfigPresentationIdentity>());
        }
        return new ConfigNavigationPath(breadcrumbs, target);
    }

    private static void addGroupBreadcrumbs(
            List<ConfigNavigationPath.Breadcrumb> breadcrumbs,
            ConfigPresentationIdentity groupId,
            Map<ConfigPresentationIdentity, ConfigGroup> groups,
            Set<ConfigPresentationIdentity> visited
    ) {
        if (!visited.add(groupId)) {
            throw new IllegalStateException("Presentation group cycle at " + groupId);
        }
        ConfigGroup group = groups.get(groupId);
        if (group == null) {
            return;
        }
        if (group.placement().group() != null) {
            addGroupBreadcrumbs(breadcrumbs, group.placement().group(), groups, visited);
        }
        breadcrumbs.add(new ConfigNavigationPath.Breadcrumb(ConfigNavigationPath.Kind.GROUP, group.identity(), group.label()));
    }

    private static List<ConfigText> locationLabels(
            ConfigPlacement placement,
            Map<ConfigPresentationIdentity, ConfigPage> pages,
            Map<ConfigPresentationIdentity, ConfigTab> tabs,
            Map<ConfigPresentationIdentity, ConfigGroup> groups
    ) {
        List<ConfigText> result = new ArrayList<ConfigText>();
        ConfigPage page = pages.get(placement.page());
        if (page != null) {
            result.add(page.label());
        }
        ConfigTab tab = tabs.get(placement.tab());
        if (tab != null) {
            result.add(tab.label());
        }
        ConfigGroup group = groups.get(placement.group());
        if (group != null) {
            result.add(group.label());
        }
        return result;
    }

    private static void validateContainers(
            ConfigIdentity config,
            Map<ConfigPresentationIdentity, ConfigPage> pages,
            Map<ConfigPresentationIdentity, ConfigTab> tabs,
            Map<ConfigPresentationIdentity, ConfigGroup> groups
    ) {
        for (ConfigTab tab : tabs.values()) {
            if (!pages.containsKey(tab.page())) {
                throw new IllegalStateException("Config tab references an unknown page: " + tab.identity());
            }
        }
        for (ConfigGroup group : groups.values()) {
            validatePlacement(group.identity(), group.placement(), pages, tabs, groups);
            if (group.identity().equals(group.placement().group())) {
                throw new IllegalStateException("Config group cannot contain itself: " + group.identity());
            }
            addGroupBreadcrumbs(new ArrayList<ConfigNavigationPath.Breadcrumb>(), group.identity(), groups, new HashSet<ConfigPresentationIdentity>());
        }
    }

    private static void validatePlacement(
            ConfigPresentationIdentity owner,
            ConfigPlacement placement,
            Map<ConfigPresentationIdentity, ConfigPage> pages,
            Map<ConfigPresentationIdentity, ConfigTab> tabs,
            Map<ConfigPresentationIdentity, ConfigGroup> groups
    ) {
        if (!pages.containsKey(placement.page())) {
            throw new IllegalStateException(owner + " references an unknown page: " + placement.page());
        }
        if (placement.tab() != null) {
            ConfigTab tab = tabs.get(placement.tab());
            if (tab == null) {
                throw new IllegalStateException(owner + " references an unknown tab: " + placement.tab());
            }
            if (!tab.page().equals(placement.page())) {
                throw new IllegalStateException(owner + " places a tab on the wrong page: " + placement.tab());
            }
        }
        if (placement.group() != null) {
            ConfigGroup group = groups.get(placement.group());
            if (group == null) {
                throw new IllegalStateException(owner + " references an unknown group: " + placement.group());
            }
            if (!group.placement().page().equals(placement.page()) || !Objects.equals(group.placement().tab(), placement.tab())) {
                throw new IllegalStateException(owner + " places a group outside its page or tab: " + placement.group());
            }
        }
    }

    private static ConfigPlacement resolvePlacement(
            ConfigIdentity config,
            NodeSource source,
            ConfigPresentationIdentity defaultPage,
            ConfigPresentationIdentity defaultTab
    ) {
        ConfigPresentationIdentity page = optionalIdentity(config, source.pageKey());
        ConfigPresentationIdentity tab = optionalIdentity(config, source.tabKey());
        ConfigPresentationIdentity group = optionalIdentity(config, source.groupKey());
        return new ConfigPlacement(
                page == null ? defaultPage : page,
                tab == null ? defaultTab : tab,
                group,
                source.region()
        );
    }

    private static ConfigField resolveField(ConfigGraph graph, ConfigIdentity config, String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        ConfigField field = graph.field(new ConfigFieldIdentity(config, ConfigPath.parse(path)));
        if (field == null) {
            throw new IllegalStateException("Presentation node references an unknown field: " + config + '/' + path);
        }
        return field;
    }

    private static ConfigLegacyFlattening.Strategy defaultLegacyStrategy(ConfigDisplayNode.Kind kind) {
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

    private static String restartFallback(RestartRequirement requirement) {
        return requirement == RestartRequirement.GAME
                ? "Requires restarting the game"
                : "Requires reloading or reopening the world";
    }

    private static String sectionGroupKey(ConfigPath path) {
        return "group/" + path.value().replace('.', '/');
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
            } else {
                result.append(character);
            }
        }
        return result.toString().trim();
    }

    private static ConfigPresentationIdentity identity(ConfigIdentity config, String key) {
        return new ConfigPresentationIdentity(config, key);
    }

    private static ConfigPresentationIdentity optionalIdentity(ConfigIdentity config, String key) {
        return key == null || key.trim().isEmpty() ? null : identity(config, key);
    }

    private static String keyOrDefault(String key, String fallback) {
        return key == null || key.trim().isEmpty() ? fallback : key;
    }

    private static <T> void putUnique(
            Map<ConfigPresentationIdentity, T> values,
            ConfigPresentationIdentity identity,
            T value,
            String kind
    ) {
        if (values.put(identity, value) != null) {
            throw new IllegalStateException("Duplicate config " + kind + ": " + identity);
        }
    }

    private static final class ResolvedConfigPresentation {
        private final List<ConfigPage> pages;
        private final List<ConfigTab> tabs;
        private final List<ConfigGroup> groups;
        private final List<ConfigDisplayNode> nodes;

        private ResolvedConfigPresentation(
                List<ConfigPage> pages,
                List<ConfigTab> tabs,
                List<ConfigGroup> groups,
                List<ConfigDisplayNode> nodes
        ) {
            this.pages = pages;
            this.tabs = tabs;
            this.groups = groups;
            this.nodes = nodes;
        }
    }

    public interface PresentationSource {
        ConfigIdentity config();

        default Collection<? extends PageSource> pages() {
            return Collections.emptyList();
        }

        default Collection<? extends TabSource> tabs() {
            return Collections.emptyList();
        }

        default Collection<? extends GroupSource> groups() {
            return Collections.emptyList();
        }

        default Collection<? extends NodeSource> nodes() {
            return Collections.emptyList();
        }
    }

    public interface PageSource {
        String key();

        long order();

        default ConfigText label() {
            return ConfigText.empty();
        }

        default ConfigText description() {
            return ConfigText.empty();
        }
    }

    public interface TabSource {
        String key();

        long order();

        default String pageKey() {
            return DEFAULT_PAGE_KEY;
        }

        default ConfigText label() {
            return ConfigText.empty();
        }

        default ConfigText description() {
            return ConfigText.empty();
        }
    }

    public interface GroupSource {
        String key();

        long order();

        default String pageKey() {
            return DEFAULT_PAGE_KEY;
        }

        default String tabKey() {
            return DEFAULT_TAB_KEY;
        }

        default String parentGroupKey() {
            return "";
        }

        default ConfigText label() {
            return ConfigText.empty();
        }

        default ConfigText description() {
            return ConfigText.empty();
        }

        default boolean collapsible() {
            return false;
        }

        default boolean expandedByDefault() {
            return true;
        }
    }

    public interface NodeSource {
        String key();

        long order();

        ConfigDisplayNode.Kind kind();

        default String fieldPath() {
            return "";
        }

        default String pageKey() {
            return DEFAULT_PAGE_KEY;
        }

        default String tabKey() {
            return DEFAULT_TAB_KEY;
        }

        default String groupKey() {
            return "";
        }

        default ConfigPlacement.Region region() {
            return ConfigPlacement.Region.BODY;
        }

        default Set<ConfigDisplayMode> displayModes() {
            return EnumSet.of(ConfigDisplayMode.DEFAULT);
        }

        default ConfigText label() {
            return ConfigText.empty();
        }

        default ConfigText description() {
            return ConfigText.empty();
        }

        default ConfigText tooltip() {
            return ConfigText.empty();
        }

        default String target() {
            return "";
        }

        default ConfigImage image() {
            return null;
        }

        default ConfigSearchMetadata search() {
            return null;
        }

        default ConfigPresentationConditions conditions() {
            return ConfigPresentationConditions.always();
        }

        default List<ConfigDependencyExplanation> dependencyExplanations() {
            return Collections.emptyList();
        }

        default ConfigNarration narration() {
            return ConfigNarration.empty();
        }

        default ConfigLegacyFlattening legacyFlattening() {
            return null;
        }

        default ConfigInformationContext informationContext() {
            return null;
        }
    }
}
