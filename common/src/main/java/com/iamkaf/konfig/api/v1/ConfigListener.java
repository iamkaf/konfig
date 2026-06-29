package com.iamkaf.konfig.api.v1;

/**
 * Listener for configuration lifecycle events.
 */
public interface ConfigListener {
    /**
     * Called after a config handle loads from disk.
     *
     * @param handle the loaded handle
     */
    default void onLoad(ConfigHandle handle) {
    }

    /**
     * Called after a config handle reloads.
     *
     * @param handle the reloaded handle
     * @param cause the reload cause
     */
    default void onReload(ConfigHandle handle, ReloadCause cause) {
    }

    /**
     * Called when a config handle is unloaded.
     *
     * @param handle the unloaded handle
     */
    default void onUnload(ConfigHandle handle) {
    }
}
