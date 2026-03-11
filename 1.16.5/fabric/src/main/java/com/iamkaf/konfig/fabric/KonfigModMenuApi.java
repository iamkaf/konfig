package com.iamkaf.konfig.fabric;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.fabric.api.v1.KonfigClientScreens;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import java.util.LinkedHashMap;
import java.util.Map;

public final class KonfigModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return KonfigClientScreens::create;
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
