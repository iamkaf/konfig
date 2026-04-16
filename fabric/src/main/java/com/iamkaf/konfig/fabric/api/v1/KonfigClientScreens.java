//? if <=1.16.5 {
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
//?} else {
package com.iamkaf.konfig.fabric.api.v1;

import net.minecraft.client.gui.screens.Screen;

public final class KonfigClientScreens {
    private KonfigClientScreens() {
    }

    public static Screen create(Screen parent) {
        return com.iamkaf.konfig.api.v1.KonfigClientScreens.create(parent);
    }

    public static Screen create(String modId, Screen parent) {
        return com.iamkaf.konfig.api.v1.KonfigClientScreens.create(modId, parent);
    }

//? if >=26.1 {
    public static Screen create(String modId, String title, Screen parent) {
        return com.iamkaf.konfig.api.v1.KonfigClientScreens.create(modId, title, parent);
    }
//?}
}
//?}
