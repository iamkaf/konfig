//? if <=1.16.5 {
package com.iamkaf.konfig.fabric.api.v1;

import com.iamkaf.konfig.fabric.KonfigConfigScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fabric-facing factory for Konfig client config screens.
 */
public final class KonfigClientScreens {
    private KonfigClientScreens() {
    }

    /**
     * Creates a screen listing all registered configurations.
     *
     * @param parent the screen to return to when closed
     * @return a new config screen
     */
    public static Screen create(Screen parent) {
        return new KonfigConfigScreen(parent);
    }

    /**
     * Creates a screen filtered to one mod id.
     *
     * @param modId the mod id to show
     * @param parent the screen to return to when closed
     * @return a new config screen
     */
    public static Screen create(String modId, Screen parent) {
        return new KonfigConfigScreen(parent, modId);
    }

    /**
     * Creates a screen filtered to one mod id with an explicit title.
     *
     * @param modId the mod id to show
     * @param title the visible screen title
     * @param parent the screen to return to when closed
     * @return a new config screen
     */
    public static Screen create(String modId, String title, Screen parent) {
        return new KonfigConfigScreen(parent, modId, title);
    }
}
//?} else {
package com.iamkaf.konfig.fabric.api.v1;

import net.minecraft.client.gui.screens.Screen;

/**
 * Fabric-facing factory for Konfig client config screens.
 */
public final class KonfigClientScreens {
    private KonfigClientScreens() {
    }

    /**
     * Creates a screen listing all registered configurations.
     *
     * @param parent the screen to return to when closed
     * @return a new config screen
     */
    public static Screen create(Screen parent) {
        return com.iamkaf.konfig.api.v1.KonfigClientScreens.create(parent);
    }

    /**
     * Creates a screen filtered to one mod id.
     *
     * @param modId the mod id to show
     * @param parent the screen to return to when closed
     * @return a new config screen
     */
    public static Screen create(String modId, Screen parent) {
        return com.iamkaf.konfig.api.v1.KonfigClientScreens.create(modId, parent);
    }

    /**
     * Creates a screen filtered to one mod id with an explicit title.
     *
     * @param modId the mod id to show
     * @param title the visible screen title
     * @param parent the screen to return to when closed
     * @return a new config screen
     */
    public static Screen create(String modId, String title, Screen parent) {
        return com.iamkaf.konfig.api.v1.KonfigClientScreens.create(modId, title, parent);
    }
}
//?}
