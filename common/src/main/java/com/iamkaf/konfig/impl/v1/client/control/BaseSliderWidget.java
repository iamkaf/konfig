//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.control;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.text;

import com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenMetrics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

@ApiStatus.Internal
public abstract class BaseSliderWidget extends AbstractSliderButton {
    protected BaseSliderWidget(double initialProgress) {
        super(0, 0, KonfigScreenMetrics.CONTROL_MIN_WIDTH, KonfigScreenMetrics.CONTROL_HEIGHT, text(""), initialProgress);
    }

    public final void syncToProgress(double progress) {
        this.value = Mth.clamp(progress, 0.0D, 1.0D);
        this.updateMessage();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 && this.isActive() && this.isMouseOver(event.x(), event.y())) {
            return this.resetToDefault();
        }
        return super.mouseClicked(event, doubleClick);
    }

    protected boolean resetToDefault() {
        return false;
    }
}
//?}
