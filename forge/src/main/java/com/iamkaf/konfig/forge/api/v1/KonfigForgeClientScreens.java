package com.iamkaf.konfig.forge.api.v1;

//? if >=1.17 {
import com.iamkaf.konfig.api.v1.KonfigClientScreens;
//?}
//? if >=26.1 {
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
//?} elif >=1.19 {
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
//?} elif >=1.18 {
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.fml.ModLoadingContext;
//?} elif >=1.17 {
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fmlclient.ConfigGuiHandler;
//?} else {
import com.iamkaf.konfig.forge.KonfigConfigScreen;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
//?}

/**
 * Forge helper for registering Konfig's generated config screen with the mod list.
 */
public final class KonfigForgeClientScreens {
    private KonfigForgeClientScreens() {
    }

    /**
     * Registers a Konfig config screen factory for a Forge mod.
     *
     * @param modId the mod id whose config screen should open
     */
    public static void register(String modId) {
//? if >=26.1 {
        var container = ModList.getModContainerById(modId)
                .orElseThrow(() -> new IllegalArgumentException("No Forge mod container found for " + modId));
        String displayName = container.getModInfo().getDisplayName();
        container.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> KonfigClientScreens.create(modId, displayName, parent)
                )
        );
//?} elif >=1.19 {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> KonfigClientScreens.create(modId, parent))
        );
//?} elif >=1.18 {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((minecraft, parent) -> KonfigClientScreens.create(modId, parent))
        );
//?} elif >=1.17 {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((minecraft, parent) -> KonfigClientScreens.create(modId, parent))
        );
//?} else {
        ModLoadingContext.get().registerExtensionPoint(
                ExtensionPoint.CONFIGGUIFACTORY,
                () -> (minecraft, parent) -> new KonfigConfigScreen(parent, modId)
        );
//?}
    }
}
