package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigFieldIdentity;
import com.iamkaf.konfig.impl.v1.model.ConfigFieldKind;
import com.iamkaf.konfig.impl.v1.model.ConfigText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ApiStatus.Internal
public final class ConfigSearchMetadata {
    private final ConfigFieldIdentity field;
    private final ConfigText label;
    private final ConfigText description;
    private final ConfigText tooltip;
    private final List<ConfigText> locationLabels;
    private final ConfigFieldKind valueKind;
    private final Set<String> aliases;
    private final Set<String> tags;

    public ConfigSearchMetadata(
            ConfigFieldIdentity field,
            ConfigText label,
            ConfigText description,
            ConfigText tooltip,
            List<ConfigText> locationLabels,
            ConfigFieldKind valueKind,
            Set<String> aliases,
            Set<String> tags
    ) {
        this.field = field;
        this.label = label == null ? ConfigText.empty() : label;
        this.description = description == null ? ConfigText.empty() : description;
        this.tooltip = tooltip == null ? ConfigText.empty() : tooltip;
        this.locationLabels = locationLabels == null || locationLabels.isEmpty()
                ? Collections.<ConfigText>emptyList()
                : Collections.unmodifiableList(new ArrayList<ConfigText>(locationLabels));
        this.valueKind = valueKind;
        this.aliases = immutableSet(aliases);
        this.tags = immutableSet(tags);
    }

    public ConfigFieldIdentity field() {
        return this.field;
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

    public List<ConfigText> locationLabels() {
        return this.locationLabels;
    }

    public ConfigFieldKind valueKind() {
        return this.valueKind;
    }

    public Set<String> aliases() {
        return this.aliases;
    }

    public Set<String> tags() {
        return this.tags;
    }

    List<String> searchableText() {
        List<String> values = new ArrayList<String>();
        addText(values, this.label);
        addText(values, this.description);
        addText(values, this.tooltip);
        for (ConfigText locationLabel : this.locationLabels) {
            addText(values, locationLabel);
        }
        values.addAll(this.aliases);
        values.addAll(this.tags);
        if (this.field != null) {
            values.add(this.field.value());
            values.add(this.field.path().value());
        }
        if (this.valueKind != null) {
            values.add(this.valueKind.name());
        }
        return values;
    }

    private static void addText(List<String> values, ConfigText text) {
        values.addAll(text.translationKeys());
        if (!text.fallback().isEmpty()) {
            values.add(text.fallback());
        }
    }

    private static Set<String> immutableSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<String>(values));
    }
}
