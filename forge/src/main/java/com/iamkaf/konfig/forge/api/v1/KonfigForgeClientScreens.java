package com.iamkaf.konfig.forge.api.v1;

import com.iamkaf.konfig.api.v1.KonfigClientScreens;
import net.minecraftforge.common.MinecraftForge;

public final class KonfigForgeClientScreens {
    private KonfigForgeClientScreens() {
    }

    public static void register(String modId) {
        MinecraftForge.registerConfigScreen(parent -> KonfigClientScreens.create(modId, parent));
    }
}
