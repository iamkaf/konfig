package com.iamkaf.konfig.fabric;

import com.iamkaf.konfig.fabric.api.v1.KonfigClientScreens;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

public final class KonfigLegacyModsScreen extends Screen {
    private final Screen parent;

    public KonfigLegacyModsScreen(Screen parent) {
        super(new TextComponent("Mods"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 100;
        int top = this.height / 4;

        this.addButton(new Button(left, top + 56, 200, 20, "Configure...", button ->
                this.minecraft.setScreen(KonfigClientScreens.create(this))
        ));
        this.addButton(new Button(left, this.height - 28, 200, 20, "Done", button -> this.onClose()));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        drawCenteredString(this.font, this.title.getString(), this.width / 2, 16, 0xFFFFFF);
        GuiComponent.fill(this.width / 2 - 110, this.height / 4 + 20, this.width / 2 + 110, this.height / 4 + 48, 0x66000000);
        drawString(this.font, "Konfig", this.width / 2 - 96, this.height / 4 + 28, 0xFFFFFF);
        drawString(this.font, "Shared config library", this.width / 2 - 96, this.height / 4 + 40, 0xA0A0A0);
        super.render(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
