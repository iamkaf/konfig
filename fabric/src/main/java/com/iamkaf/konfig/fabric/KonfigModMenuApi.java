//? if >=1.18 {
package com.iamkaf.konfig.fabric;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.fabric.api.v1.KonfigClientScreens;
//? if >=26.1 {
import net.fabricmc.loader.api.FabricLoader;
//?}
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
// Modern Terraformers Mod Menu bridge. Legacy 1.16.5-1.17.1 has its own
// source-set copy because the API shape differs, but both delegate to the
// public Fabric screen factory.
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

//? if >=26.1 {
            String displayName = FabricLoader.getInstance()
                    .getModContainer(modId)
                    .map(container -> container.getMetadata().getName())
                    .orElse(modId);
            factories.putIfAbsent(modId, parent -> KonfigClientScreens.create(modId, displayName, parent));
//?} else {
            factories.putIfAbsent(modId, parent -> KonfigClientScreens.create(modId, parent));
//?}
        }

        return factories;
    }
}
//?} else {
package com.iamkaf.konfig.fabric;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
// Empty fallback staged only where this source set is present without Mod Menu.
final class KonfigModMenuApi {
    private KonfigModMenuApi() {
    }
}
//?}
