//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.*;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.util.Mth;

abstract class BaseSliderWidget extends AbstractSliderButton {
    BaseSliderWidget(double initialProgress) {
        super(0, 0, KonfigConfigScreen.CONTROL_MIN_WIDTH, KonfigConfigScreen.CONTROL_HEIGHT, text(""), initialProgress);
    }

    protected final void syncToProgress(double progress) {
        this.value = Mth.clamp(progress, 0.0D, 1.0D);
        this.updateMessage();
    }
}

final class IntegerSliderRow extends KonfigConfigRow {
    private final int min;
    private final int max;
    private final SliderWidget slider;

    IntegerSliderRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.min = entry.value.rangeMin().intValue();
        this.max = entry.value.rangeMax().intValue();
        this.slider = new SliderWidget();
    }

    @Override
    protected AbstractWidget control() {
        return this.slider;
    }

    @Override
    protected void syncFromDraft() {
        this.slider.syncToProgress(progressFor(this.currentValue(), this.min, this.max));
    }

    private int currentValue() {
        return this.host.currentInt(this.entry.value);
    }

    private void updateDraftFromSlider(double progress) {
        this.host.setDraft(this.entry.value, Integer.valueOf(intFromProgress(progress, this.min, this.max)));
    }

    private final class SliderWidget extends BaseSliderWidget {
        private SliderWidget() {
            super(progressFor(IntegerSliderRow.this.currentValue(), IntegerSliderRow.this.min, IntegerSliderRow.this.max));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(text(Integer.toString(IntegerSliderRow.this.currentValue())));
        }

        @Override
        protected void applyValue() {
            IntegerSliderRow.this.updateDraftFromSlider(this.value);
        }

//? if >=1.21.9 {
        @Override
        public void onRelease(MouseButtonEvent event) {
            Object previousValue = IntegerSliderRow.this.entry.value.get();
            super.onRelease(event);
            IntegerSliderRow.this.commitOrRevert(previousValue);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            int previousValue = IntegerSliderRow.this.currentValue();
            boolean handled = super.keyPressed(event);
            if (handled && previousValue != IntegerSliderRow.this.currentValue()) {
                IntegerSliderRow.this.commitOrRevert(Integer.valueOf(previousValue));
            }
            return handled;
        }
//?} else {
        @Override
        public void onRelease(double mouseX, double mouseY) {
            Object previousValue = IntegerSliderRow.this.entry.value.get();
            super.onRelease(mouseX, mouseY);
            IntegerSliderRow.this.commitOrRevert(previousValue);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            int previousValue = IntegerSliderRow.this.currentValue();
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (handled && previousValue != IntegerSliderRow.this.currentValue()) {
                IntegerSliderRow.this.commitOrRevert(Integer.valueOf(previousValue));
            }
            return handled;
        }
//?}
    }
}

final class LongSliderRow extends KonfigConfigRow {
    private final long min;
    private final long max;
    private final SliderWidget slider;

    LongSliderRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.min = entry.value.rangeMin().longValue();
        this.max = entry.value.rangeMax().longValue();
        this.slider = new SliderWidget();
    }

    @Override
    protected AbstractWidget control() {
        return this.slider;
    }

    @Override
    protected void syncFromDraft() {
        this.slider.syncToProgress(progressFor(this.currentValue(), this.min, this.max));
    }

    private long currentValue() {
        return this.host.currentLong(this.entry.value);
    }

    private void updateDraftFromSlider(double progress) {
        this.host.setDraft(this.entry.value, Long.valueOf(longFromProgress(progress, this.min, this.max)));
    }

    private final class SliderWidget extends BaseSliderWidget {
        private SliderWidget() {
            super(progressFor(LongSliderRow.this.currentValue(), LongSliderRow.this.min, LongSliderRow.this.max));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(text(Long.toString(LongSliderRow.this.currentValue())));
        }

        @Override
        protected void applyValue() {
            LongSliderRow.this.updateDraftFromSlider(this.value);
        }

//? if >=1.21.9 {
        @Override
        public void onRelease(MouseButtonEvent event) {
            Object previousValue = LongSliderRow.this.entry.value.get();
            super.onRelease(event);
            LongSliderRow.this.commitOrRevert(previousValue);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            long previousValue = LongSliderRow.this.currentValue();
            boolean handled = super.keyPressed(event);
            if (handled && previousValue != LongSliderRow.this.currentValue()) {
                LongSliderRow.this.commitOrRevert(Long.valueOf(previousValue));
            }
            return handled;
        }
//?} else {
        @Override
        public void onRelease(double mouseX, double mouseY) {
            Object previousValue = LongSliderRow.this.entry.value.get();
            super.onRelease(mouseX, mouseY);
            LongSliderRow.this.commitOrRevert(previousValue);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            long previousValue = LongSliderRow.this.currentValue();
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (handled && previousValue != LongSliderRow.this.currentValue()) {
                LongSliderRow.this.commitOrRevert(Long.valueOf(previousValue));
            }
            return handled;
        }
//?}
    }
}

final class DoubleSliderRow extends KonfigConfigRow {
    private final double min;
    private final double max;
    private final SliderWidget slider;

    DoubleSliderRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.min = entry.value.rangeMin().doubleValue();
        this.max = entry.value.rangeMax().doubleValue();
        this.slider = new SliderWidget();
    }

    @Override
    protected AbstractWidget control() {
        return this.slider;
    }

    @Override
    protected void syncFromDraft() {
        this.slider.syncToProgress(progressFor(this.currentValue(), this.min, this.max));
    }

    private double currentValue() {
        return this.host.currentDouble(this.entry.value);
    }

    private void updateDraftFromSlider(double progress) {
        this.host.setDraft(this.entry.value, Double.valueOf(doubleFromProgress(progress, this.min, this.max)));
    }

    private final class SliderWidget extends BaseSliderWidget {
        private SliderWidget() {
            super(progressFor(DoubleSliderRow.this.currentValue(), DoubleSliderRow.this.min, DoubleSliderRow.this.max));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(text(formatDouble(DoubleSliderRow.this.currentValue())));
        }

        @Override
        protected void applyValue() {
            DoubleSliderRow.this.updateDraftFromSlider(this.value);
        }

//? if >=1.21.9 {
        @Override
        public void onRelease(MouseButtonEvent event) {
            Object previousValue = DoubleSliderRow.this.entry.value.get();
            super.onRelease(event);
            DoubleSliderRow.this.commitOrRevert(previousValue);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            double previousValue = DoubleSliderRow.this.currentValue();
            boolean handled = super.keyPressed(event);
            if (handled && !sameValue(Double.valueOf(previousValue), Double.valueOf(DoubleSliderRow.this.currentValue()))) {
                DoubleSliderRow.this.commitOrRevert(Double.valueOf(previousValue));
            }
            return handled;
        }
//?} else {
        @Override
        public void onRelease(double mouseX, double mouseY) {
            Object previousValue = DoubleSliderRow.this.entry.value.get();
            super.onRelease(mouseX, mouseY);
            DoubleSliderRow.this.commitOrRevert(previousValue);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            double previousValue = DoubleSliderRow.this.currentValue();
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (handled && !sameValue(Double.valueOf(previousValue), Double.valueOf(DoubleSliderRow.this.currentValue()))) {
                DoubleSliderRow.this.commitOrRevert(Double.valueOf(previousValue));
            }
            return handled;
        }
//?}
    }
}
//?}
