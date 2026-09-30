package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
public interface ConfigStorageLoadResult {
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
