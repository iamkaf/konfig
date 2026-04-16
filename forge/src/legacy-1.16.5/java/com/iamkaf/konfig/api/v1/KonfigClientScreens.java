package com.iamkaf.konfig.api.v1;

import com.iamkaf.konfig.forge.KonfigConfigScreen;
import net.minecraft.client.gui.screen.Screen;

public final class KonfigClientScreens {
    private KonfigClientScreens() {
    }

    public static Screen create(Screen parent) {
        return new KonfigConfigScreen(parent);
    }

    public static Screen create(String modId, Screen parent) {
        return new KonfigConfigScreen(parent, modId);
    }
}
