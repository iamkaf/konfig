package com.iamkaf.konfig.forge.api.v1;

import com.iamkaf.konfig.api.v1.KonfigClientScreens;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class KonfigForgeClientScreens {
    private KonfigForgeClientScreens() {
    }

    public static void register(String modId) {
        String displayName = ModList.get()
                .getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
        MinecraftForge.registerConfigScreen(parent -> KonfigClientScreens.create(modId, displayName, parent));
    }
}
