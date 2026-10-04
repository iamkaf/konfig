package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public interface ConfigStorageSaveResult {
    record Saved() implements ConfigStorageSaveResult {
    }

    record Failed(String message, Throwable cause) implements ConfigStorageSaveResult {
        public Failed {
            message = Objects.requireNonNull(message, "message");
            cause = Objects.requireNonNull(cause, "cause");
        }
    }
}
