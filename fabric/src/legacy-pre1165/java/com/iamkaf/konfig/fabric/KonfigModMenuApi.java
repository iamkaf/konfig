package com.iamkaf.konfig.fabric;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import com.iamkaf.konfig.fabric.api.v1.KonfigClientScreens;
import io.github.prospector.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Function;

@ApiStatus.Internal
public final class KonfigModMenuApi implements ModMenuApi {
    @Override
    public String getModId() {
        return Constants.MOD_ID;
    }

    @Override
    public Function<Screen, ? extends Screen> getConfigScreenFactory() {
        return parent -> KonfigClientScreens.create(Constants.MOD_ID, parent);
    }
}
