package com.iamkaf.konfig.fabric;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.fabric.api.v1.KonfigClientScreens;
import io.github.prospector.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Function;

public final class KonfigModMenuApi implements ModMenuApi {
    @Override
    public String getModId() {
        return Constants.MOD_ID;
    }

    @Override
    public Function<Screen, ? extends Screen> getConfigScreenFactory() {
        return KonfigClientScreens::create;
    }
}
