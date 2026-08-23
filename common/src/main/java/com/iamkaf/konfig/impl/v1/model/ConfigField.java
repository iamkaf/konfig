package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.RestartRequirement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ApiStatus.Internal
public final class ConfigField {
    private final ConfigFieldIdentity identity;
    private final ConfigSectionIdentity section;
    private final long order;
    private final ConfigFieldKind kind;
    private final ConfigText label;
    private final ConfigText description;
    private final ConfigText tooltip;
    private final boolean persistent;
    private final boolean synchronizedValue;
    private final boolean clientOnly;
    private final boolean serverOnly;
    private final RestartRequirement restartRequirement;
    private final String codecIdentity;
    private final List<String> aliases;
    private final Set<String> tags;

    ConfigField(
            ConfigFieldIdentity identity,
            long order,
            ConfigFieldKind kind,
            ConfigText label,
            ConfigText description,
            ConfigText tooltip,
            boolean persistent,
            boolean synchronizedValue,
            boolean clientOnly,
            boolean serverOnly,
            RestartRequirement restartRequirement,
            String codecIdentity,
            List<String> aliases,
            Set<String> tags
    ) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.section = new ConfigSectionIdentity(identity.config(), identity.path().parent());
        this.order = order;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.label = label == null ? ConfigText.empty() : label;
        this.description = description == null ? ConfigText.empty() : description;
        this.tooltip = tooltip == null ? ConfigText.empty() : tooltip;
        this.persistent = persistent;
        this.synchronizedValue = synchronizedValue;
        this.clientOnly = clientOnly;
        this.serverOnly = serverOnly;
        if (clientOnly && serverOnly) {
            throw new IllegalArgumentException("A config field cannot be both client-only and server-only: " + identity);
        }
        this.restartRequirement = restartRequirement == null ? RestartRequirement.NONE : restartRequirement;
        this.codecIdentity = normalize(codecIdentity);
        this.aliases = immutableStrings(aliases);
        this.tags = immutableSet(tags);
    }

    public ConfigFieldIdentity identity() {
        return this.identity;
    }

    public ConfigSectionIdentity section() {
        return this.section;
    }

    public long order() {
        return this.order;
    }

    public ConfigFieldKind kind() {
        return this.kind;
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

    public boolean persistent() {
        return this.persistent;
    }

    public boolean synchronizedValue() {
        return this.synchronizedValue;
    }

    public boolean clientOnly() {
        return this.clientOnly;
    }

    public boolean serverOnly() {
        return this.serverOnly;
    }

    public RestartRequirement restartRequirement() {
        return this.restartRequirement;
    }

    public String codecIdentity() {
        return this.codecIdentity;
    }

    public List<String> aliases() {
        return this.aliases;
    }

    public Set<String> tags() {
        return this.tags;
    }

    public boolean isVisible(ConfigAccessContext context) {
        Objects.requireNonNull(context, "context");
        if (context.valueSource() == ConfigAccessContext.ValueSource.SYNCHRONIZED_SERVER) {
            return this.synchronizedValue && !this.clientOnly;
        }
        if (this.clientOnly) {
            return context.runtimeSide() == ConfigAccessContext.RuntimeSide.CLIENT;
        }
        if (this.serverOnly) {
            return context.runtimeSide() == ConfigAccessContext.RuntimeSide.SERVER;
        }
        return true;
    }

    public boolean isEditable(ConfigAccessContext context) {
        if (!isVisible(context)) {
            return false;
        }
        if (context.valueSource() == ConfigAccessContext.ValueSource.SYNCHRONIZED_SERVER) {
            return context.mayEditServerValues();
        }
        return true;
    }

    private static List<String> immutableStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<String>(new LinkedHashSet<String>(values)));
    }

    private static Set<String> immutableSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<String>(values));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
