//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

@ApiStatus.Internal
public interface ConfigStorage {
    ConfigStorageLoadResult load(String configId, Path path);

    /**
     * Replaces the stored document atomically. A failed save must leave the previous file readable.
     */
    ConfigStorageSaveResult save(String configId, Path path, ConfigStorageDocument document);
}
//?}
