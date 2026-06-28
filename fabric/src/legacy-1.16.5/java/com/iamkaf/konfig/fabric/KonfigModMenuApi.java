package com.iamkaf.konfig.fabric;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.fabric.api.v1.KonfigClientScreens;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
// Terraformers Mod Menu bridge for Fabric 1.16.5 through 1.17.1. It stays in a
// legacy source set because the modern Mod Menu bridge is only compiled on 1.18+.
public final class KonfigModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> KonfigClientScreens.create(Constants.MOD_ID, parent);
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<String, ConfigScreenFactory<?>>();

        for (ConfigHandle handle : Konfig.all()) {
            String modId = handle.modId();
            if (Constants.MOD_ID.equals(modId)) {
                continue;
            }

            factories.putIfAbsent(modId, parent -> KonfigClientScreens.create(modId, parent));
        }

        return factories;
    }
}
