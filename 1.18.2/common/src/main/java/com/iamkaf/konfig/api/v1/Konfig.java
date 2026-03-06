package com.iamkaf.konfig.api.v1;

import com.iamkaf.konfig.impl.v1.ConfigBuilderImpl;
import com.iamkaf.konfig.impl.v1.KonfigManager;

import java.util.Collection;

public final class Konfig {
    private Konfig() {
    }

    public static ConfigBuilder builder(String modId, String name) {
        return new ConfigBuilderImpl(modId, name);
    }

    public static Collection<ConfigHandle> all() {
        return KonfigManager.get().allPublicHandles();
    }

    public static void reloadAll() {
        KonfigManager.get().reloadAll();
    }
}
