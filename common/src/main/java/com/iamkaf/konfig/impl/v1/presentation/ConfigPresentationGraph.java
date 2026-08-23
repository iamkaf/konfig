package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigGraph;
import com.iamkaf.konfig.impl.v1.model.ConfigIdentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApiStatus.Internal
public final class ConfigPresentationGraph {
    private final ConfigGraph configs;
    private final List<ConfigPage> pages;
    private final List<ConfigTab> tabs;
    private final List<ConfigGroup> groups;
    private final List<ConfigDisplayNode> nodes;
    private final Map<ConfigPresentationIdentity, ConfigPage> pagesById;
    private final Map<ConfigPresentationIdentity, ConfigTab> tabsById;
    private final Map<ConfigPresentationIdentity, ConfigGroup> groupsById;
    private final Map<ConfigPresentationIdentity, ConfigDisplayNode> nodesById;
    private final ConfigSearchIndex searchIndex;

    ConfigPresentationGraph(
            ConfigGraph configs,
            List<ConfigPage> pages,
            List<ConfigTab> tabs,
            List<ConfigGroup> groups,
            List<ConfigDisplayNode> nodes
    ) {
        this.configs = Objects.requireNonNull(configs, "configs");
        this.pages = immutable(pages);
        this.tabs = immutable(tabs);
        this.groups = immutable(groups);
        this.nodes = immutable(nodes);
        this.pagesById = indexPages(this.pages);
        this.tabsById = indexTabs(this.tabs);
        this.groupsById = indexGroups(this.groups);
        this.nodesById = indexNodes(this.nodes);
        this.searchIndex = new ConfigSearchIndex(this.nodes);
    }

    public ConfigGraph configs() {
        return this.configs;
    }

    public List<ConfigPage> pages() {
        return this.pages;
    }

    public List<ConfigTab> tabs() {
        return this.tabs;
    }

    public List<ConfigGroup> groups() {
        return this.groups;
    }

    public List<ConfigDisplayNode> nodes() {
        return this.nodes;
    }

    public ConfigPage page(ConfigPresentationIdentity identity) {
        return this.pagesById.get(identity);
    }

    public ConfigTab tab(ConfigPresentationIdentity identity) {
        return this.tabsById.get(identity);
    }

    public ConfigGroup group(ConfigPresentationIdentity identity) {
        return this.groupsById.get(identity);
    }

    public ConfigDisplayNode node(ConfigPresentationIdentity identity) {
        return this.nodesById.get(identity);
    }

    public List<ConfigPage> pages(ConfigIdentity config) {
        List<ConfigPage> result = new ArrayList<ConfigPage>();
        for (ConfigPage page : this.pages) {
            if (page.identity().config().equals(config)) {
                result.add(page);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<ConfigTab> tabs(ConfigPresentationIdentity page) {
        List<ConfigTab> result = new ArrayList<ConfigTab>();
        for (ConfigTab tab : this.tabs) {
            if (tab.page().equals(page)) {
                result.add(tab);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<ConfigGroup> childGroups(ConfigPlacement placement) {
        Objects.requireNonNull(placement, "placement");
        List<ConfigGroup> result = new ArrayList<ConfigGroup>();
        for (ConfigGroup group : this.groups) {
            ConfigPlacement groupPlacement = group.placement();
            if (groupPlacement.page().equals(placement.page())
                    && Objects.equals(groupPlacement.tab(), placement.tab())
                    && Objects.equals(groupPlacement.group(), placement.group())) {
                result.add(group);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<ConfigDisplayNode> nodes(ConfigIdentity config, ConfigDisplayMode mode) {
        List<ConfigDisplayNode> result = new ArrayList<ConfigDisplayNode>();
        for (ConfigDisplayNode node : this.nodes) {
            if (node.identity().config().equals(config) && node.appearsIn(mode)) {
                result.add(node);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<ConfigDisplayNode> nodes(ConfigPlacement placement, ConfigDisplayMode mode) {
        Objects.requireNonNull(placement, "placement");
        List<ConfigDisplayNode> result = new ArrayList<ConfigDisplayNode>();
        for (ConfigDisplayNode node : this.nodes) {
            if (samePlacement(node.placement(), placement) && node.appearsIn(mode)) {
                result.add(node);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public ConfigSearchIndex searchIndex() {
        return this.searchIndex;
    }

    public List<ConfigDisplayNode> informationNodes(ConfigInformationContext context) {
        Objects.requireNonNull(context, "context");
        List<ConfigDisplayNode> result = new ArrayList<ConfigDisplayNode>();
        for (ConfigDisplayNode node : this.nodes) {
            if (context.equals(node.informationContext())) {
                result.add(node);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static boolean samePlacement(ConfigPlacement left, ConfigPlacement right) {
        return left.page().equals(right.page())
                && Objects.equals(left.tab(), right.tab())
                && Objects.equals(left.group(), right.group())
                && left.region() == right.region();
    }

    private static <T> List<T> immutable(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }

    private static Map<ConfigPresentationIdentity, ConfigPage> indexPages(List<ConfigPage> pages) {
        Map<ConfigPresentationIdentity, ConfigPage> result = new LinkedHashMap<ConfigPresentationIdentity, ConfigPage>();
        for (ConfigPage page : pages) {
            if (result.put(page.identity(), page) != null) {
                throw new IllegalArgumentException("Duplicate config page: " + page.identity());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<ConfigPresentationIdentity, ConfigTab> indexTabs(List<ConfigTab> tabs) {
        Map<ConfigPresentationIdentity, ConfigTab> result = new LinkedHashMap<ConfigPresentationIdentity, ConfigTab>();
        for (ConfigTab tab : tabs) {
            if (result.put(tab.identity(), tab) != null) {
                throw new IllegalArgumentException("Duplicate config tab: " + tab.identity());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<ConfigPresentationIdentity, ConfigGroup> indexGroups(List<ConfigGroup> groups) {
        Map<ConfigPresentationIdentity, ConfigGroup> result = new LinkedHashMap<ConfigPresentationIdentity, ConfigGroup>();
        for (ConfigGroup group : groups) {
            if (result.put(group.identity(), group) != null) {
                throw new IllegalArgumentException("Duplicate config group: " + group.identity());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<ConfigPresentationIdentity, ConfigDisplayNode> indexNodes(List<ConfigDisplayNode> nodes) {
        Map<ConfigPresentationIdentity, ConfigDisplayNode> result = new LinkedHashMap<ConfigPresentationIdentity, ConfigDisplayNode>();
        for (ConfigDisplayNode node : nodes) {
            if (result.put(node.identity(), node) != null) {
                throw new IllegalArgumentException("Duplicate config display node: " + node.identity());
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
