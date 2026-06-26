package com.iamkaf.konfig.impl.v1;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

@ApiStatus.Internal
public final class KonfigConfigScreen extends Screen {
    private final Screen parent;
    private final String screenTitle;

    public KonfigConfigScreen(Screen parent) {
        this(parent, null, null);
    }

    public KonfigConfigScreen(Screen parent, String modIdFilter) {
        this(parent, modIdFilter, null);
    }

    public KonfigConfigScreen(Screen parent, String modIdFilter, String screenTitle) {
        super(new StringTextComponent(defaultScreenTitle(modIdFilter, screenTitle)));
        this.parent = parent;
        this.screenTitle = screenTitle;
    }

    private static String defaultScreenTitle(String modIdFilter, String screenTitle) {
        if (!isBlank(screenTitle)) {
            return screenTitle;
        }
        if (!isBlank(modIdFilter)) {
            return prettySegment(modIdFilter);
        }
        return "Configurations";
    }

    private static String prettySegment(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(raw.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < raw.length(); i++) {
            char character = raw.charAt(i);
            if (character == '_' || character == '-' || character == '.') {
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) != ' ') {
                    builder.append(' ');
                }
                capitalizeNext = true;
                continue;
            }

            if (capitalizeNext) {
                builder.append(Character.toUpperCase(character));
                capitalizeNext = false;
            } else if (Character.isUpperCase(character) && i > 0 && Character.isLowerCase(raw.charAt(i - 1))) {
                builder.append(' ').append(character);
            } else {
                builder.append(Character.toLowerCase(character));
            }
        }
        if (builder.length() > 0) {
            builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
        }
        return builder.toString().trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
