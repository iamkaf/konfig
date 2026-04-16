package com.iamkaf.konfig.fabric.mixin;

import com.iamkaf.konfig.fabric.KonfigLegacyModsScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
abstract class KonfigLegacyTitleScreenMixin extends Screen {
    protected KonfigLegacyTitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void konfig$addLegacyModsButton(CallbackInfo ci) {
        if (!FabricLoader.getInstance().isModLoaded("teakit")) {
            return;
        }

        for (AbstractWidget widget : this.buttons) {
            if (widget.getMessage() != null && widget.getMessage().contains("Mods")) {
                return;
            }
        }

        int y = this.height / 4 + 48 + 24 * 3;
        this.addButton(new Button(this.width / 2 - 100, y, 200, 20, "Mods", button ->
                this.minecraft.setScreen(new KonfigLegacyModsScreen((Screen) (Object) this))
        ));
    }
}
