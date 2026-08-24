//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

@ApiStatus.Internal
@FunctionalInterface
public interface ConfigRecoveryPolicy {
    Decision decide(String configId, Path path, Throwable failure);

    enum Decision {
        FAIL,
        PRESERVE_BROKEN_FILE_AND_RESTORE_DEFAULTS
    }

    static ConfigRecoveryPolicy fail() {
        return (configId, path, failure) -> Decision.FAIL;
    }

    static ConfigRecoveryPolicy preserveBrokenFile() {
        return (configId, path, failure) -> Decision.PRESERVE_BROKEN_FILE_AND_RESTORE_DEFAULTS;
    }
}
//?}
