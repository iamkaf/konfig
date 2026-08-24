//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
public sealed interface ConfigStorageLoadResult permits ConfigStorageLoadResult.Loaded, ConfigStorageLoadResult.Missing, ConfigStorageLoadResult.Recovered, ConfigStorageLoadResult.Failed {
    record Loaded(ConfigStorageDocument document) implements ConfigStorageLoadResult {
        public Loaded {
            document = Objects.requireNonNull(document, "document");
        }
    }

    record Missing() implements ConfigStorageLoadResult {
    }

    record Recovered(ConfigStorageDocument defaults, Path preservedFile, Throwable originalFailure)
            implements ConfigStorageLoadResult {
        public Recovered {
            defaults = Objects.requireNonNull(defaults, "defaults");
            preservedFile = Objects.requireNonNull(preservedFile, "preservedFile");
            originalFailure = Objects.requireNonNull(originalFailure, "originalFailure");
        }
    }

    record Failed(String message, Throwable cause, Optional<Path> preservedFile) implements ConfigStorageLoadResult {
        public Failed {
            message = Objects.requireNonNull(message, "message");
            cause = Objects.requireNonNull(cause, "cause");
            preservedFile = Objects.requireNonNull(preservedFile, "preservedFile");
        }
    }
}
//?}
