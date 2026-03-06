package com.iamkaf.konfig.api.v1;

import java.nio.file.Path;

public interface ConfigHandle {
    String modId();

    String name();

    ConfigScope scope();

    SyncMode syncMode();

    Path path();

    void load();

    void save();

    void reload();

    void addListener(ConfigListener listener);
}
