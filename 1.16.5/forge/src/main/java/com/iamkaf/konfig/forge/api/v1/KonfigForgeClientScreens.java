package com.iamkaf.konfig.forge.api.v1;

import com.iamkaf.konfig.forge.KonfigConfigScreen;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;

public final class KonfigForgeClientScreens {
    private KonfigForgeClientScreens() {
    }

    public static void register(String modId) {
        ModLoadingContext.get().registerExtensionPoint(
                ExtensionPoint.CONFIGGUIFACTORY,
                () -> (minecraft, parent) -> new KonfigConfigScreen(parent, modId)
        );
    }
}
