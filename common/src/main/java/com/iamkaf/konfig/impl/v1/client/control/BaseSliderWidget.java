package com.iamkaf.konfig.impl.v1.client.control;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.text;

import com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenMetrics;
import net.minecraft.client.gui.components.AbstractSliderButton;
//? if >=1.21.9
import net.minecraft.client.input.MouseButtonEvent;
//? if >=1.21.9 && <1.21.11
import net.minecraft.client.input.KeyEvent;
//? if >=1.19.4 && <1.21.11 {
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
//?}
//? if >=1.19.4 && <=1.21.8
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Mth;

@ApiStatus.Internal
public abstract class BaseSliderWidget extends AbstractSliderButton {
//? if >=1.19.4 && <1.21.11
    private boolean keyboardEditing;

    protected BaseSliderWidget(double initialProgress) {
        super(0, 0, KonfigScreenMetrics.CONTROL_MIN_WIDTH, KonfigScreenMetrics.CONTROL_HEIGHT, text(""), initialProgress);
    }

    public final void syncToProgress(double progress) {
        this.value = Mth.clamp(progress, 0.0D, 1.0D);
        this.updateMessage();
    }

    /** Whether arrow keys currently edit the value, matching vanilla's keyboard-edit toggle. */
    protected final boolean keyboardEditing() {
//? if >=1.21.11 {
        return this.canChangeValue;
//?} elif >=1.19.4 {
        return this.keyboardEditing;
//?} else {
        // Vanilla has no keyboard-edit toggle before 1.19.4: a focused slider always takes arrow keys.
        return true;
//?}
    }

//? if >=1.19.4 && <1.21.11 {
    // AbstractSliderButton.canChangeValue is private before 1.21.11, so mirror its toggle here.
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.keyboardEditing = false;
            return;
        }
        InputType inputType = Minecraft.getInstance().getLastInputType();
        if (inputType == InputType.MOUSE || inputType == InputType.KEYBOARD_TAB) {
            this.keyboardEditing = true;
        }
    }

//? if >=1.21.9 {
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isSelection()) {
            this.keyboardEditing = !this.keyboardEditing;
        }
        return super.keyPressed(event);
    }
//?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_SPACE || keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            this.keyboardEditing = !this.keyboardEditing;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
//?}
//?}

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 && this.isActive() && this.isMouseOver(event.x(), event.y())) {
            return this.resetToDefault();
        }
        return super.mouseClicked(event, doubleClick);
    }
    //?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && this.isActive() && this.isMouseOver(mouseX, mouseY)) {
            return this.resetToDefault();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    //?}

    protected boolean resetToDefault() {
        return false;
    }
}
