package com.iamkaf.konfig.api.v1;

/**
 * Controls when a config handle synchronizes to connected clients.
 */
public enum SyncMode {
    /**
     * Do not synchronize this configuration.
     */
    NONE,
    /**
     * Synchronize when a client logs in.
     */
    LOGIN,
    /**
     * Synchronize on login and when the config reloads.
     */
    LOGIN_AND_RELOAD
}
