package com.iamkaf.konfig.forge;

import com.iamkaf.konfig.impl.v1.KonfigConfigScreen;
import net.minecraftforge.common.MinecraftForge;

final class KonfigForgeClient {
    private KonfigForgeClient() {
    }

    static void init() {
        MinecraftForge.registerConfigScreen(KonfigConfigScreen::new);
    }
}
