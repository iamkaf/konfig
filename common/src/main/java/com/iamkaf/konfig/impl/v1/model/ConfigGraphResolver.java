package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.RestartRequirement;
import com.iamkaf.konfig.api.v1.SyncMode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ApiStatus.Internal
public final class ConfigGraphResolver {
    public ConfigGraph resolve(Collection<? extends ConfigSource> sources) {
        Objects.requireNonNull(sources, "sources");
        List<ConfigSource> orderedSources = new ArrayList<ConfigSource>(sources);
        Collections.sort(orderedSources, Comparator.comparingLong(ConfigSource::order)
                .thenComparing(source -> source.identity().value()));

        Map<ConfigIdentity, ConfigSource> identities = new LinkedHashMap<ConfigIdentity, ConfigSource>();
        Map<Path, ConfigIdentity> storageOwners = new LinkedHashMap<Path, ConfigIdentity>();
        List<ConfigDefinition> configs = new ArrayList<ConfigDefinition>(orderedSources.size());
        for (ConfigSource source : orderedSources) {
            ConfigIdentity identity = Objects.requireNonNull(source.identity(), "source identity");
            if (identities.put(identity, source) != null) {
                throw new IllegalStateException("Duplicate config identity: " + identity);
            }
            Path storagePath = Objects.requireNonNull(source.storagePath(), "storagePath for " + identity)
                    .toAbsolutePath()
                    .normalize();
            ConfigIdentity oldOwner = storageOwners.put(storagePath, identity);
            if (oldOwner != null) {
                throw new IllegalStateException(
                        "Config storage path " + storagePath + " is owned by both " + oldOwner + " and " + identity
                );
            }
            configs.add(resolveConfig(source, storagePath));
        }
        return new ConfigGraph(configs);
    }

    private ConfigDefinition resolveConfig(ConfigSource source, Path storagePath) {
        ConfigIdentity configIdentity = source.identity();
        List<FieldSource> fieldSources = new ArrayList<FieldSource>(source.fields());
        Collections.sort(fieldSources, Comparator.comparingLong(FieldSource::order)
                .thenComparing(FieldSource::path));

        Map<ConfigFieldIdentity, ConfigField> fieldsById = new LinkedHashMap<ConfigFieldIdentity, ConfigField>();
        Map<ConfigPath, SectionAccumulator> sections = new LinkedHashMap<ConfigPath, SectionAccumulator>();
        sections.put(ConfigPath.root(), new SectionAccumulator(ConfigPath.root(), Long.MIN_VALUE, source.label(), source.description()));

        for (SectionSource sectionSource : source.sections()) {
            ConfigPath path = ConfigPath.parse(sectionSource.path());
            addAncestors(sections, path, sectionSource.order());
            SectionAccumulator section = sections.get(path);
            section.order = Math.min(section.order, sectionSource.order());
            section.label = textOrEmpty(sectionSource.label());
            section.description = textOrEmpty(sectionSource.description());
        }

        for (FieldSource fieldSource : fieldSources) {
            ConfigPath path = ConfigPath.parse(fieldSource.path());
            if (path.isRoot()) {
                throw new IllegalStateException("Config field path cannot be empty for " + configIdentity);
            }
            ConfigFieldIdentity identity = new ConfigFieldIdentity(configIdentity, path);
            ConfigField field = new ConfigField(
                    identity,
                    fieldSource.order(),
                    fieldSource.kind(),
                    fieldSource.label(),
                    fieldSource.description(),
                    fieldSource.tooltip(),
                    fieldSource.persistent(),
                    fieldSource.synchronizedValue(),
                    fieldSource.clientOnly(),
                    fieldSource.serverOnly(),
                    fieldSource.restartRequirement(),
                    fieldSource.codecIdentity(),
                    fieldSource.aliases(),
                    fieldSource.tags()
            );
            if (fieldsById.put(identity, field) != null) {
                throw new IllegalStateException("Duplicate config field: " + identity);
            }

            ConfigPath sectionPath = path.parent();
            addAncestors(sections, sectionPath, fieldSource.order());
            SectionAccumulator section = sections.get(sectionPath);
            section.order = Math.min(section.order, fieldSource.order());
            section.fields.add(identity);
        }

        List<SectionAccumulator> orderedSections = new ArrayList<SectionAccumulator>(sections.values());
        Collections.sort(orderedSections, Comparator.comparingLong((SectionAccumulator section) -> section.order)
                .thenComparing(section -> section.path));
        List<ConfigSection> resolvedSections = new ArrayList<ConfigSection>(orderedSections.size());
        for (SectionAccumulator section : orderedSections) {
            resolvedSections.add(new ConfigSection(
                    new ConfigSectionIdentity(configIdentity, section.path),
                    section.order,
                    section.label,
                    section.description,
                    section.fields
            ));
        }

        return new ConfigDefinition(
                configIdentity,
                source.order(),
                source.scope(),
                source.syncMode(),
                source.schemaVersion(),
                storagePath,
                source.label(),
                source.description(),
                resolvedSections,
                new ArrayList<ConfigField>(fieldsById.values())
        );
    }

    private static void addAncestors(Map<ConfigPath, SectionAccumulator> sections, ConfigPath path, long order) {
        ConfigPath current = path;
        while (!current.isRoot()) {
            SectionAccumulator existing = sections.get(current);
            if (existing == null) {
                sections.put(current, new SectionAccumulator(current, order, ConfigText.empty(), ConfigText.empty()));
            } else {
                existing.order = Math.min(existing.order, order);
            }
            current = current.parent();
        }
    }

    private static ConfigText textOrEmpty(ConfigText text) {
        return text == null ? ConfigText.empty() : text;
    }

    private static final class SectionAccumulator {
        private final ConfigPath path;
        private long order;
        private ConfigText label;
        private ConfigText description;
        private final List<ConfigFieldIdentity> fields = new ArrayList<ConfigFieldIdentity>();

        private SectionAccumulator(ConfigPath path, long order, ConfigText label, ConfigText description) {
            this.path = path;
            this.order = order;
            this.label = textOrEmpty(label);
            this.description = textOrEmpty(description);
        }
    }

    public interface ConfigSource {
        ConfigIdentity identity();

        long order();

        ConfigScope scope();

        SyncMode syncMode();

        int schemaVersion();

        Path storagePath();

        default ConfigText label() {
            return ConfigText.empty();
        }

        default ConfigText description() {
            return ConfigText.empty();
        }

        default Collection<? extends SectionSource> sections() {
            return Collections.emptyList();
        }

        Collection<? extends FieldSource> fields();
    }

    public interface SectionSource {
        String path();

        long order();

        default ConfigText label() {
            return ConfigText.empty();
        }

        default ConfigText description() {
            return ConfigText.empty();
        }
    }

    public interface FieldSource {
        String path();

        long order();

        ConfigFieldKind kind();

        default ConfigText label() {
            return ConfigText.empty();
        }

        default ConfigText description() {
            return ConfigText.empty();
        }

        default ConfigText tooltip() {
            return ConfigText.empty();
        }

        default boolean persistent() {
            return true;
        }

        default boolean synchronizedValue() {
            return false;
        }

        default boolean clientOnly() {
            return false;
        }

        default boolean serverOnly() {
            return false;
        }

        default RestartRequirement restartRequirement() {
            return RestartRequirement.NONE;
        }

        default String codecIdentity() {
            return "";
        }

        default List<String> aliases() {
            return Collections.emptyList();
        }

        default Set<String> tags() {
            return Collections.emptySet();
        }
    }
}
