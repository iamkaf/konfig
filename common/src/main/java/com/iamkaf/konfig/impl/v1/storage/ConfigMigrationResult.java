//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public sealed interface ConfigMigrationResult<D> permits ConfigMigrationResult.Current, ConfigMigrationResult.Migrated, ConfigMigrationResult.NewerSchema, ConfigMigrationResult.MissingStep, ConfigMigrationResult.Failed {
    D document();

    record Current<D>(D document, int version) implements ConfigMigrationResult<D> {
        public Current {
            document = Objects.requireNonNull(document, "document");
        }
    }

    record Migrated<D>(D document, int fromVersion, int toVersion, List<Integer> appliedFromVersions)
            implements ConfigMigrationResult<D> {
        public Migrated {
            document = Objects.requireNonNull(document, "document");
            appliedFromVersions = List.copyOf(appliedFromVersions);
        }
    }

    record NewerSchema<D>(D document, int storedVersion, int supportedVersion) implements ConfigMigrationResult<D> {
        public NewerSchema {
            document = Objects.requireNonNull(document, "document");
        }
    }

    record MissingStep<D>(D document, int missingFromVersion, int targetVersion) implements ConfigMigrationResult<D> {
        public MissingStep {
            document = Objects.requireNonNull(document, "document");
        }
    }

    record Failed<D>(D document, int failedFromVersion, int targetVersion, Throwable cause)
            implements ConfigMigrationResult<D> {
        public Failed {
            document = Objects.requireNonNull(document, "document");
            cause = Objects.requireNonNull(cause, "cause");
        }
    }
}
//?}
