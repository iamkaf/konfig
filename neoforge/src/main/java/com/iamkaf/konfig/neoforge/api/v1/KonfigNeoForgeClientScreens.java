package com.iamkaf.konfig.neoforge.api.v1;

import com.iamkaf.konfig.api.v1.KonfigClientScreens;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * NeoForge helper for registering Konfig's generated config screen with a mod container.
 */
public final class KonfigNeoForgeClientScreens {
    private KonfigNeoForgeClientScreens() {
    }

    /**
     * Registers a Konfig config screen factory for a NeoForge mod.
     *
     * @param container the owning mod container
     * @param modId the mod id whose config screen should open
     */
    public static void register(ModContainer container, String modId) {
//? if >=26.1 {
        String displayName = container.getModInfo().getDisplayName();
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> KonfigClientScreens.create(modId, displayName, parent));
//?} else {
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> KonfigClientScreens.create(modId, parent));
//?}
    }
}
