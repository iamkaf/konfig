package com.iamkaf.konfig.forge;

import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;

final class KonfigForgeClient {
    private KonfigForgeClient() {
    }

    static void init() {
        ModLoadingContext.get().registerExtensionPoint(
                ExtensionPoint.CONFIGGUIFACTORY,
                () -> (minecraft, parent) -> new KonfigConfigScreen(parent)
        );
    }
}
