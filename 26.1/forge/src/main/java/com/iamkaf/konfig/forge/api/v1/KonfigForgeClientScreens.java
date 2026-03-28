package com.iamkaf.konfig.forge.api.v1;

import com.iamkaf.konfig.api.v1.KonfigClientScreens;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.LoadingModList;

public final class KonfigForgeClientScreens {
    private KonfigForgeClientScreens() {
    }

    public static void register(String modId) {
        String displayName = LoadingModList.getMods().stream()
                .filter(info -> info.getModId().equals(modId))
                .findFirst()
                .map(info -> info.getDisplayName())
                .orElse(modId);
        MinecraftForge.registerConfigScreen(parent -> KonfigClientScreens.create(modId, displayName, parent));
    }
}
