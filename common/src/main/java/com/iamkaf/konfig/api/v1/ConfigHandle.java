package com.iamkaf.konfig.api.v1;

import java.nio.file.Path;

/**
 * Runtime handle for a registered Konfig configuration.
 */
public interface ConfigHandle {
    /**
     * Returns the owning mod id.
     *
     * @return the owning mod id
     */
    String modId();

    /**
     * Returns the logical config name.
     *
     * @return the config name
     */
    String name();

    /**
     * Returns the scope that owns this configuration.
     *
     * @return the config scope
     */
    ConfigScope scope();

    /**
     * Returns the configured synchronization mode.
     *
     * @return the sync mode
     */
    SyncMode syncMode();

    /**
     * Returns the resolved file path for this configuration.
     *
     * @return the config file path
     */
    Path path();

    /**
     * Loads values from disk, creating defaults when needed.
     */
    void load();

    /**
     * Saves the current values to disk.
     */
    void save();

    /**
     * Reloads values from disk and notifies listeners.
     */
    void reload();

    /**
     * Adds a lifecycle listener to this handle.
     *
     * @param listener the listener to add
     */
    void addListener(ConfigListener listener);
}
