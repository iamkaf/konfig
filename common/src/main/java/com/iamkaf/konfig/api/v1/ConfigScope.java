package com.iamkaf.konfig.api.v1;

/**
 * Logical ownership side for a configuration file.
 */
public enum ConfigScope {
    /**
     * Client-only configuration.
     */
    CLIENT,
    /**
     * Shared configuration used by both logical sides.
     */
    COMMON,
    /**
     * Server-owned configuration.
     */
    SERVER
}
