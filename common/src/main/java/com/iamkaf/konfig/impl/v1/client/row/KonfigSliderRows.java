//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.row;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.field.KonfigFieldValues.*;
import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.text;

import com.iamkaf.konfig.impl.v1.client.control.BaseSliderWidget;
import com.iamkaf.konfig.impl.v1.client.screen.EntryRef;
import com.iamkaf.konfig.impl.v1.client.screen.KonfigRowHost;
import net.minecraft.client.gui.components.AbstractWidget;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

@ApiStatus.Internal
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
        return this.field().intValue();
    }

    private void updateDraftFromSlider(double progress) {
        this.field().setDraft(Integer.valueOf(intFromProgress(progress, this.min, this.max)));
    }

    private void stepValue(int direction) {
        int previousValue = this.currentValue();
        int nextValue = stepInt(previousValue, direction, this.min, this.max);
        if (nextValue == previousValue) {
            return;
        }
        this.field().setDraft(Integer.valueOf(nextValue));
        this.slider.syncToProgress(progressFor(nextValue, this.min, this.max));
        this.commitOrRevert(Integer.valueOf(previousValue));
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

        @Override
        protected boolean resetToDefault() {
            IntegerSliderRow.this.resetToDefault();
            return true;
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
            if (this.canChangeValue && (event.isLeft() || event.isRight())) {
                IntegerSliderRow.this.stepValue(event.isLeft() ? -1 : 1);
                return true;
            }
            return super.keyPressed(event);
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

@ApiStatus.Internal
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
        return this.field().longValue();
    }

    private void updateDraftFromSlider(double progress) {
        this.field().setDraft(Long.valueOf(longFromProgress(progress, this.min, this.max)));
    }

    private void stepValue(int direction) {
        long previousValue = this.currentValue();
        long nextValue = stepLong(previousValue, direction, this.min, this.max);
        if (nextValue == previousValue) {
            return;
        }
        this.field().setDraft(Long.valueOf(nextValue));
        this.slider.syncToProgress(progressFor(nextValue, this.min, this.max));
        this.commitOrRevert(Long.valueOf(previousValue));
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

        @Override
        protected boolean resetToDefault() {
            LongSliderRow.this.resetToDefault();
            return true;
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
            if (this.canChangeValue && (event.isLeft() || event.isRight())) {
                LongSliderRow.this.stepValue(event.isLeft() ? -1 : 1);
                return true;
            }
            return super.keyPressed(event);
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

@ApiStatus.Internal
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
        return this.field().doubleValue();
    }

    private void updateDraftFromSlider(double progress) {
        this.field().setDraft(Double.valueOf(doubleFromProgress(progress, this.min, this.max)));
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

        @Override
        protected boolean resetToDefault() {
            DoubleSliderRow.this.resetToDefault();
            return true;
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
