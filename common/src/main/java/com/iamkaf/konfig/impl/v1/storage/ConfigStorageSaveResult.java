//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public sealed interface ConfigStorageSaveResult permits ConfigStorageSaveResult.Saved, ConfigStorageSaveResult.Failed {
    record Saved() implements ConfigStorageSaveResult {
    }

    record Failed(String message, Throwable cause) implements ConfigStorageSaveResult {
        public Failed {
            message = Objects.requireNonNull(message, "message");
            cause = Objects.requireNonNull(cause, "cause");
        }
    }
}
//?}
