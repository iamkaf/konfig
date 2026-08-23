package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.SyncMode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApiStatus.Internal
public final class ConfigDefinition {
    private final ConfigIdentity identity;
    private final long order;
    private final ConfigScope scope;
    private final SyncMode syncMode;
    private final int schemaVersion;
    private final Path storagePath;
    private final ConfigText label;
    private final ConfigText description;
    private final List<ConfigSection> sections;
    private final List<ConfigField> fields;
    private final Map<ConfigSectionIdentity, ConfigSection> sectionsById;
    private final Map<ConfigFieldIdentity, ConfigField> fieldsById;

    ConfigDefinition(
            ConfigIdentity identity,
            long order,
            ConfigScope scope,
            SyncMode syncMode,
            int schemaVersion,
            Path storagePath,
            ConfigText label,
            ConfigText description,
            List<ConfigSection> sections,
            List<ConfigField> fields
    ) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.order = order;
        this.scope = Objects.requireNonNull(scope, "scope");
        this.syncMode = Objects.requireNonNull(syncMode, "syncMode");
        if (schemaVersion < 0) {
            throw new IllegalArgumentException("schemaVersion cannot be negative for " + identity);
        }
        this.schemaVersion = schemaVersion;
        this.storagePath = Objects.requireNonNull(storagePath, "storagePath").toAbsolutePath().normalize();
        this.label = label == null ? ConfigText.empty() : label;
        this.description = description == null ? ConfigText.empty() : description;
        this.sections = Collections.unmodifiableList(new ArrayList<ConfigSection>(sections));
        this.fields = Collections.unmodifiableList(new ArrayList<ConfigField>(fields));
        this.sectionsById = indexSections(this.sections);
        this.fieldsById = indexFields(this.fields);
    }

    public ConfigIdentity identity() {
        return this.identity;
    }

    public long order() {
        return this.order;
    }

    public ConfigScope scope() {
        return this.scope;
    }

    public SyncMode syncMode() {
        return this.syncMode;
    }

    public int schemaVersion() {
        return this.schemaVersion;
    }

    public Path storagePath() {
        return this.storagePath;
    }

    public ConfigText label() {
        return this.label;
    }

    public ConfigText description() {
        return this.description;
    }

    public List<ConfigSection> sections() {
        return this.sections;
    }

    public List<ConfigField> fields() {
        return this.fields;
    }

    public ConfigSection section(ConfigSectionIdentity identity) {
        return this.sectionsById.get(identity);
    }

    public ConfigSection section(ConfigPath path) {
        return section(new ConfigSectionIdentity(this.identity, path));
    }

    public ConfigField field(ConfigFieldIdentity identity) {
        return this.fieldsById.get(identity);
    }

    public ConfigField field(ConfigPath path) {
        return field(new ConfigFieldIdentity(this.identity, path));
    }

    public List<ConfigField> fields(ConfigSectionIdentity section) {
        ConfigSection match = this.sectionsById.get(section);
        if (match == null) {
            return Collections.emptyList();
        }
        List<ConfigField> result = new ArrayList<ConfigField>(match.fields().size());
        for (ConfigFieldIdentity field : match.fields()) {
            result.add(this.fieldsById.get(field));
        }
        return Collections.unmodifiableList(result);
    }

    public List<ConfigField> fieldsUnder(ConfigSectionIdentity section) {
        if (!this.sectionsById.containsKey(section)) {
            return Collections.emptyList();
        }
        List<ConfigField> result = new ArrayList<ConfigField>();
        List<String> prefix = section.path().segments();
        for (ConfigField field : this.fields) {
            List<String> candidate = field.identity().path().parent().segments();
            if (candidate.size() >= prefix.size() && candidate.subList(0, prefix.size()).equals(prefix)) {
                result.add(field);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static Map<ConfigSectionIdentity, ConfigSection> indexSections(List<ConfigSection> sections) {
        Map<ConfigSectionIdentity, ConfigSection> result = new LinkedHashMap<ConfigSectionIdentity, ConfigSection>();
        for (ConfigSection section : sections) {
            if (result.put(section.identity(), section) != null) {
                throw new IllegalArgumentException("Duplicate config section: " + section.identity());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<ConfigFieldIdentity, ConfigField> indexFields(List<ConfigField> fields) {
        Map<ConfigFieldIdentity, ConfigField> result = new LinkedHashMap<ConfigFieldIdentity, ConfigField>();
        for (ConfigField field : fields) {
            if (result.put(field.identity(), field) != null) {
                throw new IllegalArgumentException("Duplicate config field: " + field.identity());
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
