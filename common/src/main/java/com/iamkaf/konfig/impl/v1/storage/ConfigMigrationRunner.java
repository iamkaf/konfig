//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

@ApiStatus.Internal
public final class ConfigMigrationRunner<D> {
    private final UnaryOperator<D> copier;
    private final Map<Integer, MigrationStep<D>> steps;

    public ConfigMigrationRunner(UnaryOperator<D> copier, Map<Integer, MigrationStep<D>> steps) {
        this.copier = Objects.requireNonNull(copier, "copier");
        this.steps = new LinkedHashMap<>(steps);
    }

    public ConfigMigrationResult<D> migrate(D source, int storedVersion, int targetVersion) {
        Objects.requireNonNull(source, "source");
        requireVersion(storedVersion, "storedVersion");
        requireVersion(targetVersion, "targetVersion");
        if (storedVersion > targetVersion) {
            return new ConfigMigrationResult.NewerSchema<>(source, storedVersion, targetVersion);
        }
        if (storedVersion == targetVersion) {
            return new ConfigMigrationResult.Current<>(source, storedVersion);
        }

        D working = Objects.requireNonNull(this.copier.apply(source), "copier result");
        var applied = new ArrayList<Integer>();
        for (int version = storedVersion; version < targetVersion; version++) {
            MigrationStep<D> step = this.steps.get(Integer.valueOf(version));
            if (step == null) {
                return new ConfigMigrationResult.MissingStep<>(source, version, targetVersion);
            }
            try {
                step.migrate(working, version, version + 1);
                applied.add(Integer.valueOf(version));
            } catch (Exception exception) {
                return new ConfigMigrationResult.Failed<>(source, version, targetVersion, exception);
            }
        }
        return new ConfigMigrationResult.Migrated<>(working, storedVersion, targetVersion, applied);
    }

    private static void requireVersion(int version, String name) {
        if (version < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    @FunctionalInterface
    public interface MigrationStep<D> {
        void migrate(D document, int fromVersion, int toVersion) throws Exception;
    }
}
//?}
