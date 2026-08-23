package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApiStatus.Internal
public final class ConfigGraph {
    private final List<ConfigDefinition> configs;
    private final Map<ConfigIdentity, ConfigDefinition> configsById;
    private final Map<ConfigFieldIdentity, ConfigField> fieldsById;

    ConfigGraph(List<ConfigDefinition> configs) {
        this.configs = Collections.unmodifiableList(new ArrayList<ConfigDefinition>(configs));
        Map<ConfigIdentity, ConfigDefinition> configIndex = new LinkedHashMap<ConfigIdentity, ConfigDefinition>();
        Map<ConfigFieldIdentity, ConfigField> fieldIndex = new LinkedHashMap<ConfigFieldIdentity, ConfigField>();
        for (ConfigDefinition config : configs) {
            if (configIndex.put(config.identity(), config) != null) {
                throw new IllegalArgumentException("Duplicate config identity: " + config.identity());
            }
            for (ConfigField field : config.fields()) {
                if (fieldIndex.put(field.identity(), field) != null) {
                    throw new IllegalArgumentException("Duplicate field identity: " + field.identity());
                }
            }
        }
        this.configsById = Collections.unmodifiableMap(configIndex);
        this.fieldsById = Collections.unmodifiableMap(fieldIndex);
    }

    public List<ConfigDefinition> configs() {
        return this.configs;
    }

    public ConfigDefinition config(ConfigIdentity identity) {
        return this.configsById.get(Objects.requireNonNull(identity, "identity"));
    }

    public ConfigField field(ConfigFieldIdentity identity) {
        return this.fieldsById.get(Objects.requireNonNull(identity, "identity"));
    }

    public List<ConfigField> fields(ConfigIdentity config) {
        ConfigDefinition definition = config(config);
        return definition == null ? Collections.<ConfigField>emptyList() : definition.fields();
    }

    public List<ConfigField> fields() {
        return Collections.unmodifiableList(new ArrayList<ConfigField>(this.fieldsById.values()));
    }

    public List<ConfigField> visibleFields(ConfigAccessContext context) {
        return selectFields(context, false);
    }

    public List<ConfigField> editableFields(ConfigAccessContext context) {
        return selectFields(context, true);
    }

    private List<ConfigField> selectFields(ConfigAccessContext context, boolean editable) {
        Objects.requireNonNull(context, "context");
        List<ConfigField> result = new ArrayList<ConfigField>();
        for (ConfigField field : this.fieldsById.values()) {
            if (editable ? field.isEditable(context) : field.isVisible(context)) {
                result.add(field);
            }
        }
        return Collections.unmodifiableList(result);
    }
}
