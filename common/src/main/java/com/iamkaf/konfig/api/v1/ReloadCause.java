package com.iamkaf.konfig.api.v1;

/**
 * Describes why a configuration was reloaded.
 */
public enum ReloadCause {
    /**
     * Reload caused by a watched file changing.
     */
    FILE_WATCH,
    /**
     * Reload caused by a command.
     */
    COMMAND,
    /**
     * Reload caused by server synchronization.
     */
    SERVER_SYNC,
    /**
     * Reload caused by direct API usage.
     */
    API_CALL
}
