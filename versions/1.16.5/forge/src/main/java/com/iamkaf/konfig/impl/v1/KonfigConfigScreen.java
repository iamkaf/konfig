package com.iamkaf.konfig.impl.v1;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public final class KonfigConfigScreen extends Screen {
    private final Screen parent;

    public KonfigConfigScreen(Screen parent) {
        this(parent, null);
    }

    public KonfigConfigScreen(Screen parent, String modIdFilter) {
        super(new StringTextComponent("Konfig"));
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
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(matrixStack);
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
        drawCenteredString(matrixStack, this.font, new StringTextComponent("Use the loader-specific config screen on 1.16.5."), this.width / 2, this.height / 2 + 4, 0xFFA0A0A0);
        super.render(matrixStack, mouseX, mouseY, partialTick);
    }
}
