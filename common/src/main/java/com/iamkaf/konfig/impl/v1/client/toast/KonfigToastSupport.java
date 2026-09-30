package com.iamkaf.konfig.impl.v1.client.toast;

import org.jetbrains.annotations.ApiStatus;

//? if >=26.1 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
//?} elif >=1.21.11 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;*/
//?} elif >=1.21.8 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;*/
//?} elif >=1.21.6 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;*/
//?} elif >=1.21.2 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;*/
//?} elif >=1.20 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;*/
//?} elif >=1.19 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;*/
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;*/
//?}


@ApiStatus.Internal
public final class KonfigToastSupport {
    private static final Object FAILURE_TOKEN = new Object();
    private static final long DISPLAY_TIME_MS = 5000L;
    private static final int WIDTH = 160;
    private static final int TITLE_COLOR = 0xFFFFFF00;
    private static final int MESSAGE_COLOR = 0xFFFFFFFF;

    private KonfigToastSupport() {
    }

    public static void saveFailed(String detail) {
        showFailure("konfig.toast.save_failed", detail);
    }

    public static void resetFailed(String detail) {
        showFailure("konfig.toast.reset_failed", detail);
    }

    public static void openFailed(String target) {
        showFailure("konfig.toast.open_failed", target);
    }

    public static void missingUrl() {
        showFailure("konfig.toast.missing_url", null);
    }

//? if >=1.21.2 {
    private static void showFailure(String titleKey, String detail) {
        showToast(titleKey, isBlank(detail) ? null : literal(detail));
    }

    private static void showToast(String titleKey, Component message) {
//? if >=26.2 {
        ToastManager toastManager = Minecraft.getInstance().gui.toastManager();
//?} else {
/*        ToastManager toastManager = Minecraft.getInstance().getToastManager();*/
//?}
        Component title = translatable(titleKey);
        KonfigFailureToast toast = toastManager.getToast(KonfigFailureToast.class, FAILURE_TOKEN);
        if (toast == null) {
            toastManager.addToast(new KonfigFailureToast(title, message));
        } else {
            toast.reset(title, message);
        }
    }

    private static final class KonfigFailureToast implements Toast {
        private final KonfigToastContent content = new KonfigToastContent();
        private final KonfigToastTimer timer = new KonfigToastTimer(DISPLAY_TIME_MS);
        private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

        private KonfigFailureToast(Component title, Component message) {
            this.reset(title, message);
        }

        private void reset(Component title, Component message) {
            this.content.reset(title, message);
            this.timer.markChanged();
        }

        @Override
        public int width() {
            return this.content.width(WIDTH);
        }

        @Override
        public int height() {
            return this.content.dynamicHeight();
        }

        @Override
        public Toast.Visibility getWantedVisibility() {
            return this.wantedVisibility;
        }

        @Override
        public void update(ToastManager manager, long fullyVisibleForMs) {
            this.wantedVisibility = this.timer.isVisible(fullyVisibleForMs, manager.getNotificationDisplayTimeMultiplier()) ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
        }

//? if >=26.1 {
        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
            KonfigToastRenderer.render(this.content, this.width(), this.height(), graphics, font);
        }
//?} elif >=1.21.6 {
/*        @Override
        public void render(GuiGraphics graphics, Font font, long fullyVisibleForMs) {
            KonfigToastRenderer.render(this.content, this.width(), this.height(), graphics, font);
        }*/
//?} else {
/*        @Override
        public void render(GuiGraphics graphics, Font font, long fullyVisibleForMs) {
            KonfigToastRenderer.render(this.content, this.width(), this.height(), graphics, font);
        }*/
//?}

        @Override
        public Object getToken() {
            return FAILURE_TOKEN;
        }
    }
//?} else {
/*    private static void showFailure(String titleKey, String detail) {
        showToast(titleKey, isBlank(detail) ? null : literal(detail));
    }

    private static void showToast(String titleKey, Component message) {
        ToastComponent toastComponent = Minecraft.getInstance().getToasts();
        Component title = translatable(titleKey);
        KonfigFailureToast toast = toastComponent.getToast(KonfigFailureToast.class, FAILURE_TOKEN);
        if (toast == null) {
            toastComponent.addToast(new KonfigFailureToast(title, message));
        } else {
            toast.reset(title, message);
        }
    }

    private static final class KonfigFailureToast implements Toast {
        private final KonfigToastContent content = new KonfigToastContent();
        private final KonfigToastTimer timer = new KonfigToastTimer(DISPLAY_TIME_MS);

        private KonfigFailureToast(Component title, Component message) {
            this.reset(title, message);
        }

        private void reset(Component title, Component message) {
            this.content.reset(title, message);
            this.timer.markChanged();
        }

        @Override
        public int width() {
            return WIDTH;
        }

//? if >=1.20 {
        @Override
        public Toast.Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long visibleForMs) {
            KonfigToastRenderer.render(this.content, WIDTH, this.height(), graphics, toastComponent);
            return this.timer.isVisible(visibleForMs, toastComponent.getNotificationDisplayTimeMultiplier()) ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
        }
//?} else {
        @Override
        public Toast.Visibility render(PoseStack graphics, ToastComponent toastComponent, long visibleForMs) {
            KonfigToastRenderer.render(this.content, WIDTH, this.height(), graphics, toastComponent);
            return this.timer.isVisible(visibleForMs, 1.0D) ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
        }
//?}

        @Override
        public Object getToken() {
            return FAILURE_TOKEN;
        }
    }*/
//?}

//? if >=1.19 {
    private static Component translatable(String key) {
        return Component.translatable(key);
    }

    private static Component literal(String value) {
        return Component.literal(value);
    }
//?} else {
/*    private static Component translatable(String key) {
        return new TranslatableComponent(key);
    }

    private static Component literal(String value) {
        return new TextComponent(value);
    }*/
//?}

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
