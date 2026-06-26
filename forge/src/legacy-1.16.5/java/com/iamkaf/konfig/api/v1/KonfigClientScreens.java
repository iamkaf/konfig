package com.iamkaf.konfig.api.v1;

import com.iamkaf.konfig.forge.KonfigConfigScreen;
import net.minecraft.client.gui.screen.Screen;

/**
 * Factory for Konfig's generated client config screens on legacy Forge.
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
