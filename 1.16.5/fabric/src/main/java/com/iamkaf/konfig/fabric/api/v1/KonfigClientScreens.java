package com.iamkaf.konfig.fabric.api.v1;

import com.iamkaf.konfig.fabric.KonfigConfigScreen;
import net.minecraft.client.gui.screens.Screen;

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
