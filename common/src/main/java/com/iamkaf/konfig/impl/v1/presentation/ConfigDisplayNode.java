package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigFieldIdentity;
import com.iamkaf.konfig.impl.v1.model.ConfigText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ApiStatus.Internal
public final class ConfigDisplayNode {
    public enum Kind {
        FIELD,
//? if >=1.21.11 {
        FIELDSET,
//?}
        HEADER,
        IMAGE,
        TEXT,
        LINK,
        ACTION,
        SEPARATOR,
        DIAGNOSTIC
    }

    private final ConfigPresentationIdentity identity;
    private final long order;
    private final Kind kind;
    private final ConfigFieldIdentity field;
    private final ConfigPlacement placement;
    private final Set<ConfigDisplayMode> displayModes;
    private final ConfigText label;
    private final ConfigText description;
    private final ConfigText tooltip;
    private final String target;
    private final ConfigImage image;
    private final ConfigSearchMetadata search;
    private final ConfigNavigationPath navigation;
    private final ConfigPresentationConditions conditions;
    private final List<ConfigDependencyExplanation> dependencyExplanations;
    private final ConfigNarration narration;
    private final ConfigLegacyFlattening legacyFlattening;
    private final ConfigInformationContext informationContext;

    ConfigDisplayNode(
            ConfigPresentationIdentity identity,
            long order,
            Kind kind,
            ConfigFieldIdentity field,
            ConfigPlacement placement,
            Set<ConfigDisplayMode> displayModes,
            ConfigText label,
            ConfigText description,
            ConfigText tooltip,
            String target,
            ConfigImage image,
            ConfigSearchMetadata search,
            ConfigNavigationPath navigation,
            ConfigPresentationConditions conditions,
            List<ConfigDependencyExplanation> dependencyExplanations,
            ConfigNarration narration,
            ConfigLegacyFlattening legacyFlattening,
            ConfigInformationContext informationContext
    ) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.order = order;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.field = field;
        this.placement = Objects.requireNonNull(placement, "placement");
        this.displayModes = immutableModes(displayModes);
        this.label = label == null ? ConfigText.empty() : label;
        this.description = description == null ? ConfigText.empty() : description;
        this.tooltip = tooltip == null ? ConfigText.empty() : tooltip;
        this.target = target == null ? "" : target.trim();
        this.image = image;
        this.search = Objects.requireNonNull(search, "search");
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.conditions = conditions == null ? ConfigPresentationConditions.always() : conditions;
        this.dependencyExplanations = dependencyExplanations == null || dependencyExplanations.isEmpty()
                ? Collections.<ConfigDependencyExplanation>emptyList()
                : Collections.unmodifiableList(new ArrayList<ConfigDependencyExplanation>(dependencyExplanations));
        this.narration = narration == null ? ConfigNarration.empty() : narration;
        this.legacyFlattening = Objects.requireNonNull(legacyFlattening, "legacyFlattening");
        this.informationContext = informationContext;

        boolean fieldNode = kind == Kind.FIELD;
//? if >=1.21.11 {
        fieldNode = fieldNode || kind == Kind.FIELDSET;
//?}
        if (fieldNode != (field != null)) {
            throw new IllegalArgumentException("Only field display nodes may reference a config field: " + identity);
        }
        if (kind == Kind.IMAGE && image == null) {
            throw new IllegalArgumentException("Image display node is missing image data: " + identity);
        }
        if ((kind == Kind.LINK || kind == Kind.ACTION) && this.target.isEmpty()) {
            throw new IllegalArgumentException(kind + " display node is missing a target: " + identity);
        }
    }

    public ConfigPresentationIdentity identity() {
        return this.identity;
    }

    public long order() {
        return this.order;
    }

    public Kind kind() {
        return this.kind;
    }

    public ConfigFieldIdentity field() {
        return this.field;
    }

    public ConfigPlacement placement() {
        return this.placement;
    }

    public Set<ConfigDisplayMode> displayModes() {
        return this.displayModes;
    }

    public ConfigText label() {
        return this.label;
    }

    public ConfigText description() {
        return this.description;
    }

    public ConfigText tooltip() {
        return this.tooltip;
    }

    public String target() {
        return this.target;
    }

    public ConfigImage image() {
        return this.image;
    }

    public ConfigSearchMetadata search() {
        return this.search;
    }

    public ConfigNavigationPath navigation() {
        return this.navigation;
    }

    public ConfigPresentationConditions conditions() {
        return this.conditions;
    }

    public List<ConfigDependencyExplanation> dependencyExplanations() {
        return this.dependencyExplanations;
    }

    public ConfigNarration narration() {
        return this.narration;
    }

    public ConfigLegacyFlattening legacyFlattening() {
        return this.legacyFlattening;
    }

    public ConfigInformationContext informationContext() {
        return this.informationContext;
    }

    public boolean appearsIn(ConfigDisplayMode mode) {
        return this.displayModes.contains(mode);
    }

    private static Set<ConfigDisplayMode> immutableModes(Set<ConfigDisplayMode> modes) {
        if (modes == null || modes.isEmpty()) {
            return Collections.unmodifiableSet(EnumSet.of(ConfigDisplayMode.DEFAULT));
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(modes));
    }
}
