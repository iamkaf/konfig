package com.iamkaf.konfig.impl.v1;

//? if >=26.1 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
//?} elif >=1.21.11 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;*/
//?} elif >=1.21.8 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;*/
//?} elif >=1.21.6 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;*/
//?} elif >=1.21.2 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;*/
//?} elif >=1.20 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;*/
//?} elif >=1.19 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;*/
//?} elif >=1.17 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;*/
//?} elif >=1.16 {
/*// Pre-1.17 toast APIs use different names on Fabric and Forge; reflection imports live below.*/
//?} else {
/*// Pre-1.17 toast APIs use different names on Fabric and Forge; reflection imports live below.*/
//?}

//? if >=1.16 {
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
//?}
//? if <=1.16.5 {
/*import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;*/
//?}

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
//? if >=26.1 {
        private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/system");
//?} elif >=1.21.11 {
/*        private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/system");*/
//?} else {
/*        private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/system");*/
//?}
        private List<FormattedCharSequence> titleLines;
        private List<FormattedCharSequence> messageLines;
        private long lastChanged;
        private boolean changed;
        private int width;
        private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

        private KonfigFailureToast(Component title, Component message) {
            this.reset(title, message);
        }

        private static List<FormattedCharSequence> split(Component text) {
            if (text == null) {
                return Collections.emptyList();
            }
            return Minecraft.getInstance().font.split(text, 200);
        }

        private void reset(Component title, Component message) {
            this.titleLines = split(title);
            this.messageLines = split(message);
            this.recalculateWidth();
            this.changed = true;
        }

        private void recalculateWidth() {
            int contentWidth = Stream.concat(this.titleLines.stream(), this.messageLines.stream())
                    .mapToInt(Minecraft.getInstance().font::width)
                    .max()
                    .orElse(130);
            this.width = Math.max(WIDTH, contentWidth + 30);
        }

        @Override
        public int width() {
            return this.width;
        }

        @Override
        public int height() {
            int titleHeight = Math.max(1, this.titleLines.size()) * 12;
            int messageHeight = this.messageLines.isEmpty() ? 0 : this.messageLines.size() * 12;
            return 16 + titleHeight + messageHeight;
        }

        @Override
        public Toast.Visibility getWantedVisibility() {
            return this.wantedVisibility;
        }

        @Override
        public void update(ToastManager manager, long fullyVisibleForMs) {
            if (this.changed) {
                this.lastChanged = fullyVisibleForMs;
                this.changed = false;
            }

            double displayTime = DISPLAY_TIME_MS * manager.getNotificationDisplayTimeMultiplier();
            this.wantedVisibility = fullyVisibleForMs - this.lastChanged < displayTime ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
        }

//? if >=26.1 {
        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
            this.renderText(graphics, font);
        }

        private void renderText(GuiGraphicsExtractor graphics, Font font) {
            int y = this.messageLines.isEmpty() ? 12 : 7;
            for (FormattedCharSequence line : this.titleLines) {
                graphics.text(font, line, 18, y, TITLE_COLOR, false);
                y += 12;
            }
            for (FormattedCharSequence line : this.messageLines) {
                graphics.text(font, line, 18, y, MESSAGE_COLOR, false);
                y += 12;
            }
        }
//?} elif >=1.21.6 {
/*        @Override
        public void render(GuiGraphics graphics, Font font, long fullyVisibleForMs) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
            this.renderText(graphics, font);
        }

        private void renderText(GuiGraphics graphics, Font font) {
            int y = this.messageLines.isEmpty() ? 12 : 7;
            for (FormattedCharSequence line : this.titleLines) {
                graphics.drawString(font, line, 18, y, TITLE_COLOR, false);
                y += 12;
            }
            for (FormattedCharSequence line : this.messageLines) {
                graphics.drawString(font, line, 18, y, MESSAGE_COLOR, false);
                y += 12;
            }
        }*/
//?} else {
/*        @Override
        public void render(GuiGraphics graphics, Font font, long fullyVisibleForMs) {
            graphics.blitSprite(RenderType::guiTextured, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
            this.renderText(graphics, font);
        }

        private void renderText(GuiGraphics graphics, Font font) {
            int y = this.messageLines.isEmpty() ? 12 : 7;
            for (FormattedCharSequence line : this.titleLines) {
                graphics.drawString(font, line, 18, y, TITLE_COLOR, false);
                y += 12;
            }
            for (FormattedCharSequence line : this.messageLines) {
                graphics.drawString(font, line, 18, y, MESSAGE_COLOR, false);
                y += 12;
            }
        }*/
//?}

        @Override
        public Object getToken() {
            return FAILURE_TOKEN;
        }
    }
//?} elif >=1.17 {
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
//? if >=1.21 {
        private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/system");
//?} elif >=1.20.2 {
        private static final ResourceLocation BACKGROUND_SPRITE = new ResourceLocation("toast/system");
//?}
        private Component title;
        private Component message;
        private long lastChanged;
        private boolean changed;

        private KonfigFailureToast(Component title, Component message) {
            this.reset(title, message);
        }

        private void reset(Component title, Component message) {
            this.title = title;
            this.message = message;
            this.changed = true;
        }

//? if >=1.16 {
        @Override
        public int width() {
            return WIDTH;
        }
//?}

//? if >=1.20 {
        @Override
        public Toast.Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long visibleForMs) {
            if (this.changed) {
                this.lastChanged = visibleForMs;
                this.changed = false;
            }
//? if >=1.20.2 {
            graphics.blitSprite(BACKGROUND_SPRITE, 0, 0, WIDTH, this.height());
//?} else {
            graphics.blit(Toast.TEXTURE, 0, 0, 0, 64, WIDTH, this.height());
//?}
            this.renderText(graphics, toastComponent);
            return visibleForMs - this.lastChanged < DISPLAY_TIME_MS * toastComponent.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
        }

        private void renderText(GuiGraphics graphics, ToastComponent toastComponent) {
            List<FormattedCharSequence> detailLines = this.message == null
                    ? Collections.emptyList()
                    : toastComponent.getMinecraft().font.split(this.message, 124);
            if (detailLines.isEmpty()) {
                graphics.drawString(toastComponent.getMinecraft().font, this.title, 18, 12, TITLE_COLOR, false);
            } else {
                graphics.drawString(toastComponent.getMinecraft().font, this.title, 18, 7, TITLE_COLOR, false);
                graphics.drawString(toastComponent.getMinecraft().font, detailLines.get(0), 18, 18, MESSAGE_COLOR, false);
            }
        }
//?} elif >=1.17 {
        @Override
        public Toast.Visibility render(PoseStack graphics, ToastComponent toastComponent, long visibleForMs) {
            if (this.changed) {
                this.lastChanged = visibleForMs;
                this.changed = false;
            }
            RenderSystem.setShaderTexture(0, Toast.TEXTURE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            toastComponent.blit(graphics, 0, 0, 0, 64, WIDTH, this.height());
            this.renderText(graphics, toastComponent);
            return visibleForMs - this.lastChanged < DISPLAY_TIME_MS ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
        }

        private void renderText(PoseStack graphics, ToastComponent toastComponent) {
            List<FormattedCharSequence> detailLines = this.message == null
                    ? Collections.emptyList()
                    : toastComponent.getMinecraft().font.split(this.message, 124);
            if (detailLines.isEmpty()) {
                toastComponent.getMinecraft().font.draw(graphics, this.title, 18.0F, 12.0F, TITLE_COLOR);
            } else {
                toastComponent.getMinecraft().font.draw(graphics, this.title, 18.0F, 7.0F, TITLE_COLOR);
                toastComponent.getMinecraft().font.draw(graphics, detailLines.get(0), 18.0F, 18.0F, MESSAGE_COLOR);
            }
        }
//?} elif >=1.16 {
        @Override
        public Toast.Visibility render(PoseStack graphics, ToastComponent toastComponent, long visibleForMs) {
            if (this.changed) {
                this.lastChanged = visibleForMs;
                this.changed = false;
            }
            toastComponent.getMinecraft().getTextureManager().bind(Toast.TEXTURE);
            RenderSystem.color3f(1.0F, 1.0F, 1.0F);
            toastComponent.blit(graphics, 0, 0, 0, 64, WIDTH, this.height());
            this.renderText(graphics, toastComponent);
            return visibleForMs - this.lastChanged < DISPLAY_TIME_MS ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
        }

        private void renderText(PoseStack graphics, ToastComponent toastComponent) {
            List<FormattedCharSequence> detailLines = this.message == null
                    ? Collections.emptyList()
                    : toastComponent.getMinecraft().font.split(this.message, 124);
            if (detailLines.isEmpty()) {
                toastComponent.getMinecraft().font.draw(graphics, this.title, 18.0F, 12.0F, TITLE_COLOR);
            } else {
                toastComponent.getMinecraft().font.draw(graphics, this.title, 18.0F, 7.0F, TITLE_COLOR);
                toastComponent.getMinecraft().font.draw(graphics, detailLines.get(0), 18.0F, 18.0F, MESSAGE_COLOR);
            }
        }
//?} else {
        @Override
        public Toast.Visibility render(ToastComponent toastComponent, long visibleForMs) {
            if (this.changed) {
                this.lastChanged = visibleForMs;
                this.changed = false;
            }
            toastComponent.getMinecraft().getTextureManager().bind(Toast.TEXTURE);
            GlStateManager.color3f(1.0F, 1.0F, 1.0F);
            toastComponent.blit(0, 0, 0, 64, WIDTH, 32);
            if (this.message == null) {
                toastComponent.getMinecraft().font.draw(this.title.getString(), 18.0F, 12.0F, TITLE_COLOR);
            } else {
                toastComponent.getMinecraft().font.draw(this.title.getString(), 18.0F, 7.0F, TITLE_COLOR);
                toastComponent.getMinecraft().font.draw(this.message.getString(), 18.0F, 18.0F, MESSAGE_COLOR);
            }
            return visibleForMs - this.lastChanged < DISPLAY_TIME_MS ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
        }
//?}

        @Override
        public Object getToken() {
            return FAILURE_TOKEN;
        }
    }*/
//?} else {
/*    private static void showFailure(String titleKey, String detail) {
        showToast(translateString(titleKey), isBlank(detail) ? null : detail);
    }

    private static void showToast(String title, String message) {
        try {
            Object toastComponent = minecraft().getClass().getMethod("getToasts").invoke(minecraft());
            Class<?> toastInterface = toastInterface();
            Object existing = toastComponent.getClass()
                    .getMethod("getToast", Class.class, Object.class)
                    .invoke(toastComponent, toastInterface, FAILURE_TOKEN);
            if (existing != null && Proxy.isProxyClass(existing.getClass())) {
                InvocationHandler handler = Proxy.getInvocationHandler(existing);
                if (handler instanceof LegacyToastInvocationHandler) {
                    ((LegacyToastInvocationHandler) handler).reset(title, message);
                    return;
                }
            }

            LegacyToastInvocationHandler handler = new LegacyToastInvocationHandler(title, message, toastInterface);
            Object toast = Proxy.newProxyInstance(
                    toastInterface.getClassLoader(),
                    new Class<?>[] { toastInterface },
                    handler
            );
            toastComponent.getClass().getMethod("addToast", toastInterface).invoke(toastComponent, toast);
        } catch (Exception exception) {
            com.iamkaf.konfig.Constants.LOG.warn("Failed to show Konfig toast", exception);
        }
    }

    private static Object minecraft() throws Exception {
        return Class.forName("net.minecraft.client.Minecraft").getMethod("getInstance").invoke(null);
    }

    private static Class<?> toastInterface() throws ClassNotFoundException {
        try {
            return Class.forName("net.minecraft.client.gui.components.toasts.Toast");
        } catch (ClassNotFoundException exception) {
            return Class.forName("net.minecraft.client.gui.toasts.IToast");
        }
    }

    private static final class LegacyToastInvocationHandler implements InvocationHandler {
        private final Class<?> toastInterface;
        private String title;
        private String message;
        private long lastChanged;
        private boolean changed;

        private LegacyToastInvocationHandler(String title, String message, Class<?> toastInterface) {
            this.toastInterface = toastInterface;
            this.reset(title, message);
        }

        private void reset(String title, String message) {
            this.title = title;
            this.message = message;
            this.changed = true;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("getToken".equals(name)) {
                return FAILURE_TOKEN;
            }
            if ("width".equals(name)) {
                return Integer.valueOf(WIDTH);
            }
            if ("height".equals(name)) {
                return Integer.valueOf(32);
            }
            if ("render".equals(name)) {
                return this.render(method, args == null ? new Object[0] : args);
            }
            if ("toString".equals(name)) {
                return "KonfigToast";
            }
            return defaultValue(method.getReturnType());
        }

        private Object render(Method method, Object[] args) throws Exception {
            Object graphics = args.length == 3 ? args[0] : null;
            Object toastComponent = args.length == 3 ? args[1] : args[0];
            long visibleForMs = ((Long) args[args.length - 1]).longValue();
            if (this.changed) {
                this.lastChanged = visibleForMs;
                this.changed = false;
            }

            this.bindToastTexture(toastComponent);
            this.blitBackground(graphics, toastComponent);
            this.renderText(graphics, toastComponent);
            boolean visible = visibleForMs - this.lastChanged < DISPLAY_TIME_MS;
            return visibility(method.getReturnType(), visible);
        }

        private void bindToastTexture(Object toastComponent) {
            try {
                Object minecraft = toastComponent.getClass().getMethod("getMinecraft").invoke(toastComponent);
                Object textureManager = minecraft.getClass().getMethod("getTextureManager").invoke(minecraft);
                Object texture = this.toastInterface.getField("TEXTURE").get(null);
                for (Method method : textureManager.getClass().getMethods()) {
                    if ("bind".equals(method.getName()) && method.getParameterCount() == 1) {
                        method.invoke(textureManager, texture);
                        break;
                    }
                }
                Class.forName("com.mojang.blaze3d.systems.RenderSystem")
                        .getMethod("color3f", float.class, float.class, float.class)
                        .invoke(null, Float.valueOf(1.0F), Float.valueOf(1.0F), Float.valueOf(1.0F));
            } catch (Exception ignored) {
            }
        }

        private void blitBackground(Object graphics, Object toastComponent) {
            try {
                for (Method method : toastComponent.getClass().getMethods()) {
                    if (!"blit".equals(method.getName())) {
                        continue;
                    }
                    Class<?>[] parameters = method.getParameterTypes();
                    if (graphics != null && parameters.length == 7 && parameters[0].isAssignableFrom(graphics.getClass())) {
                        method.invoke(toastComponent, graphics, 0, 0, 0, 64, WIDTH, 32);
                        return;
                    }
                    if (graphics == null && parameters.length == 6) {
                        method.invoke(toastComponent, 0, 0, 0, 64, WIDTH, 32);
                        return;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        private void renderText(Object graphics, Object toastComponent) {
            try {
                Object minecraft = toastComponent.getClass().getMethod("getMinecraft").invoke(toastComponent);
                Object font = minecraft.getClass().getField("font").get(minecraft);
                boolean hasMessage = !isBlank(this.message);
                drawText(font, graphics, this.title, 18.0F, hasMessage ? 7.0F : 12.0F, TITLE_COLOR);
                if (hasMessage) {
                    drawText(font, graphics, this.message, 18.0F, 18.0F, MESSAGE_COLOR);
                }
            } catch (Exception ignored) {
            }
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
//?} elif >=1.17 {
/*    private static Component translatable(String key) {
        return new TranslatableComponent(key);
    }

    private static Component literal(String value) {
        return new TextComponent(value);
    }*/
//?} else {
/*    private static String translateString(String key) {
        try {
            return invokeI18n("net.minecraft.client.resources.language.I18n", key);
        } catch (Exception exception) {
            try {
                return invokeI18n("net.minecraft.client.resources.I18n", key);
            } catch (Exception ignored) {
                return key;
            }
        }
    }

    private static String invokeI18n(String className, String key) throws Exception {
        Method method = Class.forName(className).getMethod("get", String.class, Object[].class);
        return (String) method.invoke(null, new Object[] { key, new Object[0] });
    }

    private static void drawText(Object font, Object graphics, String text, float x, float y, int color) throws Exception {
        for (Method method : font.getClass().getMethods()) {
            if (!"draw".equals(method.getName())) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            if (graphics != null
                    && parameters.length == 5
                    && parameters[0].isAssignableFrom(graphics.getClass())
                    && parameters[1] == String.class) {
                method.invoke(font, graphics, text, Float.valueOf(x), Float.valueOf(y), Integer.valueOf(color));
                return;
            }
            if (graphics == null && parameters.length == 4 && parameters[0] == String.class) {
                method.invoke(font, text, Float.valueOf(x), Float.valueOf(y), Integer.valueOf(color));
                return;
            }
        }
    }

    private static Object visibility(Class<?> visibilityType, boolean visible) {
        if (visibilityType.isEnum()) {
            Object[] constants = visibilityType.getEnumConstants();
            String wanted = visible ? "SHOW" : "HIDE";
            for (Object constant : constants) {
                if (((Enum<?>) constant).name().equals(wanted)) {
                    return constant;
                }
            }
            if (constants.length > 0) {
                return constants[0];
            }
        }
        return null;
    }

    private static Object defaultValue(Class<?> type) {
        if (type == Void.TYPE) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (type == Integer.TYPE) {
            return Integer.valueOf(0);
        }
        if (type == Long.TYPE) {
            return Long.valueOf(0L);
        }
        if (type == Float.TYPE) {
            return Float.valueOf(0.0F);
        }
        if (type == Double.TYPE) {
            return Double.valueOf(0.0D);
        }
        return null;
    }*/
//?}

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
