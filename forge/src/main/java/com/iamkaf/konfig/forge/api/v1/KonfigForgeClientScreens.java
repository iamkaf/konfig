package com.iamkaf.konfig.forge.api.v1;

//? if >=1.17 {
import com.iamkaf.konfig.api.v1.KonfigClientScreens;
//?}
//? if >=26.1 {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.LoadingModList;
//?} elif >=1.19.1 {
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

public final class KonfigForgeClientScreens {
    private KonfigForgeClientScreens() {
    }

    public static void register(String modId) {
//? if >=26.1 {
        String displayName = LoadingModList.getMods().stream()
                .filter(info -> info.getModId().equals(modId))
                .findFirst()
                .map(info -> info.getDisplayName())
                .orElse(modId);
        MinecraftForge.registerConfigScreen(parent -> KonfigClientScreens.create(modId, displayName, parent));
//?} elif >=1.19.1 {
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
