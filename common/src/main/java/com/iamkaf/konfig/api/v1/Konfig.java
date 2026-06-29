package com.iamkaf.konfig.api.v1;

import com.iamkaf.konfig.impl.v1.config.builder.ConfigBuilderImpl;
import com.iamkaf.konfig.impl.v1.config.model.KonfigManager;

import java.util.Collection;

/**
 * Entry point for creating and inspecting Konfig configuration handles.
 */
public final class Konfig {
    private Konfig() {
    }

    /**
     * Starts a builder for one mod-owned configuration.
     *
     * @param modId the owning mod id
     * @param name the logical config name
     * @return a new config builder
     */
    public static ConfigBuilder builder(String modId, String name) {
        return new ConfigBuilderImpl(modId, name);
    }

    /**
     * Returns all registered public config handles.
     *
     * @return registered config handles
     */
    public static Collection<ConfigHandle> all() {
        return KonfigManager.get().allPublicHandles();
    }

    /**
     * Reloads all registered config handles.
     */
    public static void reloadAll() {
        KonfigManager.get().reloadAll();
    }
}
