package com.iamkaf.konfig.impl.v1;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

public final class KonfigConfigScreen extends Screen {
    private final Screen parent;

    public KonfigConfigScreen(Screen parent) {
        this(parent, null);
    }

    public KonfigConfigScreen(Screen parent, String modIdFilter) {
        super(new TextComponent("Konfig"));
        this.parent = parent;
    }

    @Override
    protected void init() {
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
        drawCenteredString(poseStack, this.font, new TextComponent("Use the loader-specific config screen on 1.16.5."), this.width / 2, this.height / 2 + 4, 0xFFA0A0A0);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }
}
