package com.iamkaf.konfig.forge;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigDebugConfig;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.impl.v1.ColorValueHelper;
import com.iamkaf.konfig.impl.v1.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.ConfigValueImpl;
import com.iamkaf.konfig.impl.v1.EntryKind;
import com.iamkaf.konfig.impl.v1.KonfigManager;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;
import com.iamkaf.konfig.impl.v1.StringListValueHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.INestedGuiEventHandler;
import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Block;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class KonfigConfigScreen extends Screen {
    private static final int LIST_TOP = 28;
    private static final int LIST_BOTTOM_MARGIN = 52;
    private static final int ROW_HEIGHT = 34;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_MIN_WIDTH = 132;
    private static final int CONTROL_MAX_WIDTH = 200;
    private static final int SUGGESTION_LIMIT = 7;
    private static final int SUGGESTION_ROW_HEIGHT = 14;

    private final Screen parent;
    private final String modIdFilter;
    private final List<EntryRef> entries;
    private final Map<ConfigValueImpl<?>, Object> drafts = new LinkedHashMap<ConfigValueImpl<?>, Object>();
    private final Map<ConfigValueImpl<?>, Object> sessionStartValues = new LinkedHashMap<ConfigValueImpl<?>, Object>();
    private final Map<String, List<String>> registrySuggestionCache = new LinkedHashMap<String, List<String>>();

    private EntryList list;
    private RegistryTextInputRow activeRegistryRow;
    private RegistryTextInputRow renderedRegistryRow;
    private String statusMessage = "";
    private int statusColor = 0xFFFF8080;

    public KonfigConfigScreen(Screen parent) {
        this(parent, null);
    }

    public KonfigConfigScreen(Screen parent, String modIdFilter) {
        super(translate("konfig.screen.title"));
        this.parent = parent;
        this.modIdFilter = modIdFilter;
        this.entries = collectEntries(modIdFilter);
        if (KonfigDebugConfig.enabled()) {
            Constants.LOG.info(
                    "[Konfig/Debug] creating screen parent={} modFilter={} entries={}",
                    parent == null ? "null" : parent.getClass().getName(),
                    modIdFilter == null ? "<all>" : modIdFilter,
                    this.entries.size()
            );
        }
        for (EntryRef entry : this.entries) {
            Object value = entry.value.get();
            this.drafts.put(entry.value, copyDraftValue(entry.value, value));
            if (entry.editable) {
                this.sessionStartValues.put(entry.value, snapshotValue(entry.value, value));
            }
        }
    }

    @Override
    protected void init() {
        this.rebuildScreenWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.list == null) {
            return;
        }
        for (ConfigRow row : this.list.children()) {
            row.tick();
        }
    }

    @Override
    public void onClose() {
        this.closeScreen();
    }

    private void closeScreen() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.activeRegistryRow != null && this.activeRegistryRow.handleSuggestionClick(mouseX, mouseY)) {
            return true;
        }

        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        RegistryTextInputRow focusedRow = this.findFocusedRegistryRow();
        if (focusedRow != null) {
            this.setActiveRegistryRow(focusedRow);
            focusedRow.activateSuggestions();
        } else if (this.activeRegistryRow != null && !this.activeRegistryRow.isPointInsideInput(mouseX, mouseY)) {
            this.activeRegistryRow.closeSuggestions();
        }

        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.activeRegistryRow != null && this.activeRegistryRow.handleSuggestionKey(keyCode)) {
            return true;
        }
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        if (this.activeRegistryRow != null && this.activeRegistryRow.isFocused()) {
            this.activeRegistryRow.refreshSuggestions();
        }
        return handled;
    }

    @Override
    public void render(MatrixStack guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderedRegistryRow = null;
        AbstractGui.fill(guiGraphics, 0, 0, this.width, this.height, 0xC0101010);
        if (this.list != null) {
            this.list.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawCenteredString(guiGraphics, this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        this.font.draw(guiGraphics, text(entryCountText()), 12, 12, 0xFFC0C0C0);

        if (!this.statusMessage.isEmpty()) {
            drawCenteredString(guiGraphics, this.font, text(this.statusMessage), this.width / 2, this.height - 38, this.statusColor);
        }

        if (this.entries.isEmpty()) {
            drawCenteredString(guiGraphics, this.font, translate("konfig.screen.empty"), this.width / 2, this.height / 2 - 10, 0xFFC0C0C0);
        }

        if (this.renderedRegistryRow != null) {
            this.renderedRegistryRow.renderSuggestions(guiGraphics, mouseX, mouseY);
        }
    }

    private void rebuildScreenWidgets() {
        this.buttons.clear();
        this.children.clear();

        int listHeight = Math.max(48, this.height - LIST_TOP - LIST_BOTTOM_MARGIN);
        this.list = new EntryList(this.minecraft, this.width, listHeight, LIST_TOP);
        this.children.add(this.list);
        for (EntryRef entry : this.entries) {
            this.list.addKonfigEntry(createRow(entry));
        }

        int footerY = this.height - 26;
        this.addButton(new Button(this.width / 2 - 82, footerY, 80, 20, translate("konfig.screen.reset"), button -> this.resetEntries()));
        this.addButton(new Button(this.width / 2 + 2, footerY, 80, 20, translate("konfig.screen.done"), button -> this.onClose()));
    }

    private ConfigRow createRow(EntryRef entry) {
        if (!entry.editable) {
            return new UnsupportedRow(entry);
        }
        if (entry.value.kind() == EntryKind.BOOLEAN) {
            return new BooleanRow(entry);
        }
        if (entry.value.kind() == EntryKind.ENUM) {
            return new EnumRow(entry);
        }
        if (entry.value.kind() == EntryKind.COLOR_RGB || entry.value.kind() == EntryKind.COLOR_ARGB) {
            return new ColorRow(entry);
        }
        if (entry.value.kind() == EntryKind.STRING_LIST) {
            return new StringListRow(entry);
        }
        if (entry.value.kind() == EntryKind.INTEGER && entry.value.hasNumericRange()) {
            return new IntegerSliderRow(entry);
        }
        if (entry.value.kind() == EntryKind.LONG && entry.value.hasNumericRange()) {
            return new LongSliderRow(entry);
        }
        if (entry.value.kind() == EntryKind.DOUBLE && entry.value.hasNumericRange()) {
            return new DoubleSliderRow(entry);
        }
        if (entry.value.kind() == EntryKind.STRING && entry.value.hasBoundRegistry()) {
            return new RegistryTextInputRow(entry);
        }
        return new TextInputRow(entry);
    }

    private boolean persistEntry(EntryRef entry) {
        Object previousValue = entry.value.get();
        try {
            Object parsed = parseDraft(entry.value, this.drafts.get(entry.value));
            if (sameValue(previousValue, parsed)) {
                this.statusMessage = translate("konfig.screen.status.saved").getString();
                this.statusColor = 0xFF80FF80;
                return true;
            }

            setRawValue(entry.value, parsed);
            entry.handle.save();
            this.statusMessage = translate("konfig.screen.status.saved").getString();
            this.statusColor = 0xFF80FF80;
            return true;
        } catch (Exception exception) {
            setRawValue(entry.value, previousValue);
            this.statusMessage = exception.getMessage() == null ? translate("konfig.screen.status.save_failed").getString() : exception.getMessage();
            this.statusColor = 0xFFFF8080;
            return false;
        }
    }

    private void resetEntries() {
        Map<ConfigValueImpl<?>, Object> previousValues = new LinkedHashMap<ConfigValueImpl<?>, Object>();
        Set<ConfigHandleImpl> handles = new LinkedHashSet<ConfigHandleImpl>();
        try {
            for (EntryRef entry : this.entries) {
                if (!entry.editable) {
                    continue;
                }
                Object resetValue = snapshotValue(entry.value, this.sessionStartValues.get(entry.value));
                previousValues.put(entry.value, snapshotValue(entry.value, entry.value.get()));
                this.drafts.put(entry.value, copyDraftValue(entry.value, resetValue));
                setRawValue(entry.value, resetValue);
                handles.add(entry.handle);
            }

            for (ConfigHandleImpl handle : handles) {
                handle.save();
            }

            this.statusMessage = translate("konfig.screen.status.reset").getString();
            this.statusColor = 0xFF80FF80;
        } catch (Exception exception) {
            for (Map.Entry<ConfigValueImpl<?>, Object> previousValue : previousValues.entrySet()) {
                setRawValue(previousValue.getKey(), previousValue.getValue());
                this.drafts.put(previousValue.getKey(), copyDraftValue(previousValue.getKey(), previousValue.getValue()));
            }
            this.statusMessage = exception.getMessage() == null ? translate("konfig.screen.status.save_failed").getString() : exception.getMessage();
            this.statusColor = 0xFFFF8080;
        }
        this.rebuildScreenWidgets();
    }

    private String entryCountText() {
        return this.entries.size() + (this.entries.size() == 1 ? " entry" : " entries");
    }

    private static List<EntryRef> collectEntries(String modIdFilter) {
        List<EntryRef> result = new ArrayList<EntryRef>();

        for (ConfigHandleImpl handle : KonfigManager.get().all()) {
            if (modIdFilter != null && !modIdFilter.equals(handle.modId())) {
                continue;
            }
            for (ConfigValue<?> value : handle.values()) {
                if (!(value instanceof ConfigValueImpl)) {
                    continue;
                }

                ConfigValueImpl<?> impl = (ConfigValueImpl<?>) value;
                if (!isVisibleOnThisSide(impl)) {
                    continue;
                }

                boolean editable = impl.kind() != EntryKind.CUSTOM;
                result.add(new EntryRef(handle, impl, editable));
            }
        }

        Collections.sort(result, Comparator.comparing(entry -> entry.handle.id() + ":" + entry.value.path()));
        return result;
    }

    private static boolean isVisibleOnThisSide(ConfigValueImpl<?> value) {
        if (value.clientOnly() && !RuntimeEnvironment.isClient()) {
            return false;
        }
        if (value.serverOnly() && RuntimeEnvironment.isClient()) {
            return false;
        }
        return true;
    }

    private static Object parseDraft(ConfigValueImpl<?> value, Object draft) {
        try {
            switch (value.kind()) {
                case BOOLEAN:
                    return parseBoolean(draft, value.path());
                case INTEGER:
                    return Integer.valueOf(Integer.parseInt(stringValue(draft).trim()));
                case LONG:
                    return Long.valueOf(Long.parseLong(stringValue(draft).trim()));
                case DOUBLE:
                    return Double.valueOf(Double.parseDouble(stringValue(draft).trim()));
                case STRING:
                    return stringValue(draft);
                case STRING_LIST:
                    return parseStringList(draft, value.path());
                case ENUM:
                    return parseEnum(value, draft);
                case COLOR_RGB:
                    return Integer.valueOf(parseColor(value, draft));
                case COLOR_ARGB:
                    return Integer.valueOf(parseColor(value, draft));
                case CUSTOM:
                default:
                    return value.get();
            }
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("Invalid number for '" + value.path() + "'.");
        }
    }

    private static Boolean parseBoolean(Object draft, String path) {
        if (draft instanceof Boolean) {
            return (Boolean) draft;
        }

        String value = stringValue(draft).trim();
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Invalid boolean for '" + path + "' (expected true/false).");
    }

    private static Object parseEnum(ConfigValueImpl<?> value, Object draft) {
        Object defaultValue = value.defaultValue();
        if (!(defaultValue instanceof Enum<?>)) {
            return defaultValue;
        }

        Class<?> enumClass = defaultValue.getClass();
        if (enumClass.isInstance(draft)) {
            return draft;
        }

        String target = stringValue(draft);
        Object[] constants = enumClass.getEnumConstants();
        for (Object constant : constants) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(target)) {
                return constant;
            }
        }

        throw new IllegalArgumentException("Invalid value for '" + value.path() + "'.");
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseStringList(Object draft, String path) {
        if (draft instanceof List<?>) {
            return StringListValueHelper.immutableCopy((List<String>) draft, path);
        }
        throw new IllegalArgumentException("Invalid list for '" + path + "'.");
    }

    private static int parseColor(ConfigValueImpl<?> value, Object draft) {
        if (draft instanceof Number) {
            int encoded = ((Number) draft).intValue();
            if (value.kind() == EntryKind.COLOR_RGB) {
                return ColorValueHelper.requireRgb(encoded, value.path());
            }
            return encoded;
        }

        String raw = stringValue(draft);
        if (value.kind() == EntryKind.COLOR_ARGB) {
            return ColorValueHelper.parseArgb(raw, value.path());
        }
        return ColorValueHelper.parseRgb(raw, value.path());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setRawValue(ConfigValueImpl<?> value, Object parsed) {
        ((ConfigValue) value).set(parsed);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean sameValue(Object left, Object right) {
        return left == right || (left != null && left.equals(right));
    }

    private static Object snapshotValue(ConfigValueImpl<?> value, Object currentValue) {
        if (value.kind() == EntryKind.STRING_LIST) {
            return StringListValueHelper.immutableCopy(stringListValue(currentValue, value.path()), value.path());
        }
        return currentValue;
    }

    private static Object copyDraftValue(ConfigValueImpl<?> value, Object currentValue) {
        if (value.kind() == EntryKind.STRING_LIST) {
            return StringListValueHelper.mutableCopy(stringListValue(currentValue, value.path()));
        }
        return currentValue;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringListValue(Object currentValue, String path) {
        if (currentValue == null) {
            return Collections.emptyList();
        }
        if (!(currentValue instanceof List<?>)) {
            throw new IllegalArgumentException("Expected list value for '" + path + "'.");
        }
        return (List<String>) currentValue;
    }

    private boolean readBoolean(ConfigValueImpl<?> value) {
        Object current = this.drafts.get(value);
        if (current instanceof Boolean) {
            return ((Boolean) current).booleanValue();
        }
        return ((Boolean) value.get()).booleanValue();
    }

    private Enum<?> currentEnum(ConfigValueImpl<?> value) {
        Object defaultValue = value.defaultValue();
        if (!(defaultValue instanceof Enum<?>)) {
            throw new IllegalStateException("Expected enum value for '" + value.path() + "'.");
        }

        Object current = this.drafts.get(value);
        if (current != null && defaultValue.getClass().isInstance(current)) {
            return (Enum<?>) current;
        }

        return (Enum<?>) defaultValue;
    }

    private Enum<?> cycleEnum(ConfigValueImpl<?> value) {
        Enum<?> current = currentEnum(value);
        Object[] constants = current.getDeclaringClass().getEnumConstants();

        int index = 0;
        for (int i = 0; i < constants.length; i++) {
            if (constants[i] == current) {
                index = i;
                break;
            }
        }

        return (Enum<?>) constants[(index + 1) % constants.length];
    }

    private int currentColor(ConfigValueImpl<?> value) {
        Object current = this.drafts.get(value);
        if (current instanceof Number) {
            return ((Number) current).intValue();
        }
        return ((Number) value.get()).intValue();
    }

    private List<String> currentStringList(ConfigValueImpl<?> value) {
        Object current = this.drafts.get(value);
        if (current instanceof List<?>) {
            return StringListValueHelper.mutableCopy(stringListValue(current, value.path()));
        }
        return StringListValueHelper.mutableCopy(stringListValue(value.get(), value.path()));
    }

    private ITextComponent booleanText(ConfigValueImpl<?> value) {
        return translate(readBoolean(value) ? "options.on" : "options.off");
    }

    private ITextComponent enumText(EntryRef entry, Enum<?> value) {
        String key = "konfig.value." + entry.handle.modId() + "." + entry.handle.name() + "." + entry.value.path() + "." + value.name().toLowerCase(Locale.ROOT);
        ITextComponent translated = translate(key);
        return key.equals(translated.getString()) ? text(prettySegment(value.name())) : translated;
    }

    private ITextComponent colorText(ConfigValueImpl<?> value) {
        int color = currentColor(value);
        if (value.kind() == EntryKind.COLOR_ARGB) {
            return text(ColorValueHelper.formatArgb(color));
        }
        return text(ColorValueHelper.formatRgb(color));
    }

    private ITextComponent stringListText(ConfigValueImpl<?> value) {
        List<String> values = currentStringList(value);
        if (values.isEmpty()) {
            return translate("konfig.screen.list.empty");
        }
        if (values.size() == 1) {
            return text(values.get(0));
        }
        if (values.size() == 2) {
            return text(values.get(0) + ", " + values.get(1));
        }
        return translate("konfig.screen.list.summary", values.get(0), Integer.valueOf(values.size() - 1));
    }

    private String currentStringValue(ConfigValueImpl<?> value) {
        Object current = this.drafts.get(value);
        if (current instanceof String) {
            return (String) current;
        }
        return stringValue(value.get());
    }

    private static double progressFor(double current, double min, double max) {
        double span = max - min;
        if (span <= 0.0D) {
            return 0.0D;
        }
        return MathHelper.clamp((current - min) / span, 0.0D, 1.0D);
    }

    private static int intFromProgress(double progress, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + (int) Math.round((max - min) * progress);
    }

    private static long longFromProgress(double progress, long min, long max) {
        if (max <= min) {
            return min;
        }
        return min + Math.round((max - min) * progress);
    }

    private static double doubleFromProgress(double progress, double min, double max) {
        if (max <= min) {
            return min;
        }
        return min + (max - min) * progress;
    }

    private static String formatDouble(double value) {
        String formatted = String.format(Locale.ROOT, "%.3f", value);
        while (formatted.contains(".") && (formatted.endsWith("0") || formatted.endsWith("."))) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    private static void drawColorSwatch(MatrixStack guiGraphics, int x, int y, int size, int color, EntryKind kind) {
        AbstractGui.fill(guiGraphics, x - 1, y - 1, x + size + 1, y + size + 1, 0xFF202020);
        if (kind == EntryKind.COLOR_ARGB && ColorValueHelper.alpha(color) < 255) {
            int cell = Math.max(2, size / 4);
            for (int row = 0; row < size; row += cell) {
                for (int column = 0; column < size; column += cell) {
                    boolean dark = ((row / cell) + (column / cell)) % 2 == 0;
                    AbstractGui.fill(guiGraphics, 
                            x + column,
                            y + row,
                            x + Math.min(size, column + cell),
                            y + Math.min(size, row + cell),
                            dark ? 0xFF707070 : 0xFFC0C0C0
                    );
                }
            }
        } else {
            AbstractGui.fill(guiGraphics, x, y, x + size, y + size, 0xFFFFFFFF);
        }
        AbstractGui.fill(guiGraphics, x, y, x + size, y + size, ColorValueHelper.toRenderColor(kind, color));
    }

    private static ResourceLocation parseIdentifier(String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return ResourceLocation.tryParse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean supportsRegistryIcon(String registryId) {
        return "minecraft:item".equals(registryId) || "minecraft:block".equals(registryId);
    }

    private static ItemStack registryIconStack(String registryId, String value) {
        if (!supportsRegistryIcon(registryId)) {
            return ItemStack.EMPTY;
        }

        ResourceLocation identifier = parseIdentifier(value);
        if (identifier == null) {
            return ItemStack.EMPTY;
        }

        if ("minecraft:item".equals(registryId)) {
            Item item = Registry.ITEM.get(identifier);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
            return ItemStack.EMPTY;
        }

        Block block = Registry.BLOCK.get(identifier);
        if (block == null) {
            return ItemStack.EMPTY;
        }

        Item item = block.asItem();
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private static void renderRegistryIcon(MatrixStack guiGraphics, String registryId, String value, int x, int y) {
        ItemStack stack = registryIconStack(registryId, value);
        if (!stack.isEmpty()) {
            Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x, y);
        }
    }

    private static String normalizeHexInput(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        } else if (normalized.regionMatches(true, 0, "0x", 0, 2)) {
            normalized = normalized.substring(2);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean isHexPrefix(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static ITextComponent translate(String key, Object... args) {
        return new TranslationTextComponent(key, args);
    }

    private static ITextComponent text(String value) {
        return new StringTextComponent(value == null ? "" : value);
    }

    private static ITextComponent translatedLabel(ConfigHandleImpl handle, ConfigValueImpl<?> value) {
        String key = "konfig.config." + handle.modId() + "." + handle.name() + "." + value.path();
        ITextComponent translated = translate(key);
        return key.equals(translated.getString()) ? text(fallbackLabel(handle, value)) : translated;
    }

    private static ITextComponent contextLabel(ConfigHandleImpl handle, ConfigValueImpl<?> value) {
        List<String> parts = new ArrayList<String>();
        parts.add(prettySegment(handle.name()));
        String[] pathParts = value.path().split("\\.");
        for (int i = 0; i < pathParts.length - 1; i++) {
            parts.add(prettySegment(pathParts[i]));
        }
        return text(String.join(" / ", parts));
    }

    private static String fallbackLabel(ConfigHandleImpl handle, ConfigValueImpl<?> value) {
        List<String> parts = new ArrayList<String>();
        parts.add(prettySegment(handle.name()));
        String[] pathParts = value.path().split("\\.");
        for (String pathPart : pathParts) {
            parts.add(prettySegment(pathPart));
        }
        return String.join(" > ", parts);
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

    private static List<ITextComponent> tooltipLines(String tooltip) {
        List<ITextComponent> lines = new ArrayList<ITextComponent>();
        String normalized = tooltip.replace('\r', '\n');
        for (String line : normalized.split("\\n")) {
            lines.add(text(line));
        }
        return lines;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private RegistryTextInputRow findFocusedRegistryRow() {
        if (this.list == null) {
            return null;
        }
        for (ConfigRow row : this.list.children()) {
            if (row instanceof RegistryTextInputRow) {
                RegistryTextInputRow registryRow = (RegistryTextInputRow) row;
                if (registryRow.isFocused()) {
                    return registryRow;
                }
            }
        }
        return null;
    }

    private void setActiveRegistryRow(RegistryTextInputRow row) {
        if (this.activeRegistryRow == row) {
            return;
        }
        if (this.activeRegistryRow != null) {
            this.activeRegistryRow.closeSuggestions();
        }
        this.activeRegistryRow = row;
    }

    private List<String> registrySuggestions(String registryId) {
        if (isBlank(registryId)) {
            return Collections.emptyList();
        }
        List<String> cached = this.registrySuggestionCache.get(registryId);
        if (cached != null) {
            return cached;
        }

        List<String> values = new ArrayList<String>();
        Registry<?> registry = builtInRegistry(registryId);
        if (registry != null) {
            for (Object key : registry.keySet()) {
                values.add(String.valueOf(key));
            }
            Collections.sort(values);
        }

        List<String> immutable = Collections.unmodifiableList(values);
        this.registrySuggestionCache.put(registryId, immutable);
        return immutable;
    }

    @SuppressWarnings("unchecked")
    private static Registry<?> builtInRegistry(String registryId) {
        ResourceLocation identifier = parseIdentifier(registryId);
        if (identifier == null || !Registry.REGISTRY.containsKey(identifier)) {
            return null;
        }
        return (Registry<?>) Registry.REGISTRY.get(identifier);
    }

    private static List<String> filterRegistrySuggestions(List<String> allSuggestions, String query) {
        if (allSuggestions.isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<String> exact = new ArrayList<String>();
        List<String> prefix = new ArrayList<String>();
        List<String> contains = new ArrayList<String>();

        for (String candidate : allSuggestions) {
            String lowerCandidate = candidate.toLowerCase(Locale.ROOT);
            String pathCandidate = registryPath(lowerCandidate);
            if (normalized.isEmpty()) {
                prefix.add(candidate);
                continue;
            }
            if (lowerCandidate.equals(normalized) || pathCandidate.equals(normalized)) {
                exact.add(candidate);
            } else if (lowerCandidate.startsWith(normalized) || pathCandidate.startsWith(normalized)) {
                prefix.add(candidate);
            } else if (lowerCandidate.contains(normalized) || pathCandidate.contains(normalized)) {
                contains.add(candidate);
            }
        }

        List<String> result = new ArrayList<String>(SUGGESTION_LIMIT);
        appendSuggestions(result, exact);
        appendSuggestions(result, prefix);
        appendSuggestions(result, contains);
        return result;
    }

    private static void appendSuggestions(List<String> target, List<String> source) {
        for (String value : source) {
            if (target.size() >= SUGGESTION_LIMIT) {
                return;
            }
            target.add(value);
        }
    }

    private static String registryPath(String registryId) {
        int separator = registryId.indexOf(':');
        return separator >= 0 ? registryId.substring(separator + 1) : registryId;
    }

    private static String suggestionSuffix(String currentValue, String suggestion) {
        if (isBlank(suggestion)) {
            return "";
        }
        String current = currentValue == null ? "" : currentValue;
        if (current.isEmpty()) {
            return suggestion;
        }
        if (suggestion.regionMatches(true, 0, current, 0, current.length())) {
            return suggestion.substring(current.length());
        }
        return "";
    }

    private final class EntryList extends ExtendedList<ConfigRow> {
        private EntryList(net.minecraft.client.Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, y + height, ROW_HEIGHT);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
        }

        private void addKonfigEntry(ConfigRow row) {
            super.addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return KonfigConfigScreen.this.width - 28;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.x1 - 6;
        }

        @Override
        protected void renderBackground(MatrixStack guiGraphics) {
            AbstractGui.fill(guiGraphics, this.x0, this.y0, this.x1, this.y1, 0x66000000);
        }
    }

    private abstract class ConfigRow extends ExtendedList.AbstractListEntry<ConfigRow> implements INestedGuiEventHandler {
        protected final EntryRef entry;
        private IGuiEventListener focused;
        private boolean dragging;

        private ConfigRow(EntryRef entry) {
            this.entry = entry;
        }

        protected abstract Widget control();

        protected void tick() {
        }

        @Override
        public void render(MatrixStack guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
        }

        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (hovered) {
                AbstractGui.fill(guiGraphics, x, y, x + width, y + height, 0x22000000);
            }

            if (!isBlank(this.entry.tooltip)) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    KonfigConfigScreen.this.renderTooltip(
                            guiGraphics,
                            KonfigConfigScreen.this.font.split(text(this.entry.tooltip), Math.max(KonfigConfigScreen.this.width / 2, 200)),
                            mouseX,
                            mouseY
                    );
                }
            }

            int controlWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, width / 2));
            int controlX = x + width - controlWidth;
            int controlY = y + (height - CONTROL_HEIGHT) / 2;
            layoutControl(this.control(), controlX, controlY, controlWidth);

            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.contextLabel, x + 4.0F, y + 1.0F, 0xFFA0A0A0);
            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.displayLabel(), x + 4.0F, y + 12.0F, this.entry.editable ? 0xFFFFFFFF : 0xFFA0A0A0);
            this.control().render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends IGuiEventListener> children() {
            return Collections.singletonList(this.control());
        }

        @Override
        public IGuiEventListener getFocused() {
            return this.focused;
        }

        @Override
        public void setFocused(IGuiEventListener listener) {
            this.focused = listener;
        }

        @Override
        public boolean isDragging() {
            return this.dragging;
        }

        @Override
        public void setDragging(boolean dragging) {
            this.dragging = dragging;
        }

        protected final void layoutControl(Widget control, int x, int y, int width) {
            control.x = x;
            control.y = y;
            control.setWidth(width);
        }

        protected void revertDraft(Object previousValue) {
            KonfigConfigScreen.this.drafts.put(this.entry.value, copyDraftValue(this.entry.value, previousValue));
        }

        protected void commitOrRevert(Object previousValue) {
            if (!KonfigConfigScreen.this.persistEntry(this.entry)) {
                this.revertDraft(previousValue);
                this.syncFromDraft();
            }
        }

        protected void syncFromDraft() {
        }
    }

    private final class UnsupportedRow extends ConfigRow {
        private final Button button;

        private UnsupportedRow(EntryRef entry) {
            super(entry);
            this.button = new Button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, translate("konfig.screen.unsupported"), ignored -> {
            });
            this.button.active = false;
        }

        @Override
        protected Widget control() {
            return this.button;
        }
    }

    private final class BooleanRow extends ConfigRow {
        private final Button button;

        private BooleanRow(EntryRef entry) {
            super(entry);
            this.button = new Button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, booleanText(entry.value), button -> {
                Object previousDraft = KonfigConfigScreen.this.drafts.get(entry.value);
                KonfigConfigScreen.this.drafts.put(entry.value, Boolean.valueOf(!KonfigConfigScreen.this.readBoolean(entry.value)));
                this.commitOrRevert(previousDraft);
                this.syncFromDraft();
            });
        }

        @Override
        protected Widget control() {
            return this.button;
        }

        @Override
        protected void syncFromDraft() {
            this.button.setMessage(booleanText(this.entry.value));
        }
    }

    private final class EnumRow extends ConfigRow {
        private final Button button;

        private EnumRow(EntryRef entry) {
            super(entry);
            this.button = new Button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, enumText(entry, KonfigConfigScreen.this.currentEnum(entry.value)), button -> {
                Object previousDraft = KonfigConfigScreen.this.drafts.get(entry.value);
                KonfigConfigScreen.this.drafts.put(entry.value, KonfigConfigScreen.this.cycleEnum(entry.value));
                this.commitOrRevert(previousDraft);
                this.syncFromDraft();
            });
        }

        @Override
        protected Widget control() {
            return this.button;
        }

        @Override
        protected void syncFromDraft() {
            this.button.setMessage(enumText(this.entry, KonfigConfigScreen.this.currentEnum(this.entry.value)));
        }
    }

    private final class ColorRow extends ConfigRow {
        private static final int PREVIEW_SIZE = 16;
        private static final int PREVIEW_GAP = 6;

        private final Button button;

        private ColorRow(EntryRef entry) {
            super(entry);
            this.button = new Button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, colorText(entry.value), ignored -> {
                KonfigConfigScreen.this.minecraft.setScreen(new ColorEditorScreen(entry));
            });
        }

        @Override
        protected Widget control() {
            return this.button;
        }

        @Override
        protected void syncFromDraft() {
            this.button.setMessage(colorText(this.entry.value));
        }

        @Override
        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (hovered) {
                AbstractGui.fill(guiGraphics, x, y, x + width, y + height, 0x22000000);
            }

            if (!isBlank(this.entry.tooltip)) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    KonfigConfigScreen.this.renderTooltip(
                            guiGraphics,
                            KonfigConfigScreen.this.font.split(text(this.entry.tooltip), Math.max(KonfigConfigScreen.this.width / 2, 200)),
                            mouseX,
                            mouseY
                    );
                }
            }

            int controlWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, width / 2));
            int previewX = x + width - controlWidth - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = y + (height - PREVIEW_SIZE) / 2;
            int buttonWidth = controlWidth;
            layoutControl(this.control(), x + width - buttonWidth, y + (height - CONTROL_HEIGHT) / 2, buttonWidth);

            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.contextLabel, x + 4.0F, y + 1.0F, 0xFFA0A0A0);
            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.displayLabel(), x + 4.0F, y + 12.0F, 0xFFFFFFFF);
            drawColorSwatch(guiGraphics, previewX, previewY, PREVIEW_SIZE, KonfigConfigScreen.this.currentColor(this.entry.value), this.entry.value.kind());
            this.control().render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private final class StringListRow extends ConfigRow {
        private static final int PREVIEW_SIZE = 16;
        private static final int PREVIEW_GAP = 6;

        private final Button button;

        private StringListRow(EntryRef entry) {
            super(entry);
            this.button = new Button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, stringListText(entry.value), ignored -> {
                KonfigConfigScreen.this.minecraft.setScreen(new StringListEditorScreen(entry));
            });
        }

        @Override
        protected Widget control() {
            return this.button;
        }

        @Override
        protected void syncFromDraft() {
            this.button.setMessage(stringListText(this.entry.value));
        }

        @Override
        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            super.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
            if (!this.entry.value.hasBoundRegistry() || !supportsRegistryIcon(this.entry.value.boundRegistryId())) {
                return;
            }

            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            if (values.isEmpty()) {
                return;
            }

            int previewX = this.button.x - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = this.button.y + (CONTROL_HEIGHT - PREVIEW_SIZE) / 2;
            renderRegistryIcon(guiGraphics, this.entry.value.boundRegistryId(), values.get(0), previewX, previewY);
        }
    }

    private abstract class BaseSliderWidget extends AbstractSlider {
        private BaseSliderWidget(double initialProgress) {
            super(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, StringTextComponent.EMPTY, initialProgress);
        }

        protected final void syncToProgress(double progress) {
            this.value = MathHelper.clamp(progress, 0.0D, 1.0D);
            this.updateMessage();
        }
    }

    private final class IntegerSliderRow extends ConfigRow {
        private final int min;
        private final int max;
        private final SliderWidget slider;

        private IntegerSliderRow(EntryRef entry) {
            super(entry);
            this.min = entry.value.rangeMin().intValue();
            this.max = entry.value.rangeMax().intValue();
            this.slider = new SliderWidget();
        }

        @Override
        protected Widget control() {
            return this.slider;
        }

        @Override
        protected void syncFromDraft() {
            this.slider.syncToProgress(progressFor(this.currentValue(), this.min, this.max));
        }

        private int currentValue() {
            Object draft = KonfigConfigScreen.this.drafts.get(this.entry.value);
            if (draft instanceof Number) {
                return ((Number) draft).intValue();
            }
            return ((Number) this.entry.value.get()).intValue();
        }

        private void updateDraftFromSlider(double progress) {
            KonfigConfigScreen.this.drafts.put(this.entry.value, Integer.valueOf(intFromProgress(progress, this.min, this.max)));
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
        }
    }

    private final class LongSliderRow extends ConfigRow {
        private final long min;
        private final long max;
        private final SliderWidget slider;

        private LongSliderRow(EntryRef entry) {
            super(entry);
            this.min = entry.value.rangeMin().longValue();
            this.max = entry.value.rangeMax().longValue();
            this.slider = new SliderWidget();
        }

        @Override
        protected Widget control() {
            return this.slider;
        }

        @Override
        protected void syncFromDraft() {
            this.slider.syncToProgress(progressFor(this.currentValue(), this.min, this.max));
        }

        private long currentValue() {
            Object draft = KonfigConfigScreen.this.drafts.get(this.entry.value);
            if (draft instanceof Number) {
                return ((Number) draft).longValue();
            }
            return ((Number) this.entry.value.get()).longValue();
        }

        private void updateDraftFromSlider(double progress) {
            KonfigConfigScreen.this.drafts.put(this.entry.value, Long.valueOf(longFromProgress(progress, this.min, this.max)));
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
        }
    }

    private final class DoubleSliderRow extends ConfigRow {
        private final double min;
        private final double max;
        private final SliderWidget slider;

        private DoubleSliderRow(EntryRef entry) {
            super(entry);
            this.min = entry.value.rangeMin().doubleValue();
            this.max = entry.value.rangeMax().doubleValue();
            this.slider = new SliderWidget();
        }

        @Override
        protected Widget control() {
            return this.slider;
        }

        @Override
        protected void syncFromDraft() {
            this.slider.syncToProgress(progressFor(this.currentValue(), this.min, this.max));
        }

        private double currentValue() {
            Object draft = KonfigConfigScreen.this.drafts.get(this.entry.value);
            if (draft instanceof Number) {
                return ((Number) draft).doubleValue();
            }
            return ((Number) this.entry.value.get()).doubleValue();
        }

        private void updateDraftFromSlider(double progress) {
            KonfigConfigScreen.this.drafts.put(this.entry.value, Double.valueOf(doubleFromProgress(progress, this.min, this.max)));
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
        }
    }

    private final class RegistryTextInputRow extends ConfigRow {
        private static final int ICON_SIZE = 16;
        private static final int ICON_GAP = 6;

        private final TextFieldWidget input;
        private final List<String> visibleSuggestions = new ArrayList<String>();
        private boolean suppressResponder;
        private boolean suggestionsDismissed;
        private String dismissedValue = "";
        private int selectedSuggestionIndex;
        private int lastInputX;
        private int lastInputY;
        private int lastInputWidth;
        private int lastDropdownX;
        private int lastDropdownY;
        private int lastDropdownWidth;
        private int lastDropdownHeight;

        private RegistryTextInputRow(EntryRef entry) {
            super(entry);
            this.input = new TextFieldWidget(KonfigConfigScreen.this.font, 0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, entry.label);
            this.input.setMaxLength(256);
            this.input.setValue(stringValue(KonfigConfigScreen.this.drafts.get(entry.value)));
            this.input.setResponder(value -> {
                if (this.suppressResponder) {
                    return;
                }
                this.suggestionsDismissed = false;
                this.dismissedValue = "";
                KonfigConfigScreen.this.drafts.put(entry.value, value);
                KonfigConfigScreen.this.persistEntry(entry);
                this.refreshSuggestions();
            });
        }

        @Override
        protected Widget control() {
            return this.input;
        }

        @Override
        protected void tick() {
            if (this.input.isFocused()) {
                KonfigConfigScreen.this.setActiveRegistryRow(this);
                this.refreshSuggestions();
            }
        }

        @Override
        protected void syncFromDraft() {
            this.suppressResponder = true;
            this.input.setValue(stringValue(KonfigConfigScreen.this.drafts.get(this.entry.value)));
            this.suppressResponder = false;
            this.suggestionsDismissed = false;
            this.dismissedValue = "";
            this.refreshSuggestions();
        }

        @Override
        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (hovered) {
                AbstractGui.fill(guiGraphics, x, y, x + width, y + height, 0x22000000);
            }

            if (!isBlank(this.entry.tooltip)) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    KonfigConfigScreen.this.renderTooltip(
                            guiGraphics,
                            KonfigConfigScreen.this.font.split(text(this.entry.tooltip), Math.max(KonfigConfigScreen.this.width / 2, 200)),
                            mouseX,
                            mouseY
                    );
                }
            }

            int controlWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, width / 2));
            int controlX = x + width - controlWidth;
            int controlY = y + (height - CONTROL_HEIGHT) / 2;
            layoutControl(this.control(), controlX, controlY, controlWidth);
            this.lastInputX = controlX;
            this.lastInputY = controlY;
            this.lastInputWidth = controlWidth;

            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.contextLabel, x + 4.0F, y + 1.0F, 0xFFA0A0A0);
            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.displayLabel(), x + 4.0F, y + 12.0F, 0xFFFFFFFF);
            if (this.entry.value.hasBoundRegistry() && supportsRegistryIcon(this.entry.value.boundRegistryId())) {
                renderRegistryIcon(
                        guiGraphics,
                        this.entry.value.boundRegistryId(),
                        KonfigConfigScreen.this.currentStringValue(this.entry.value),
                        controlX - ICON_GAP - ICON_SIZE,
                        y + (height - ICON_SIZE) / 2
                );
            }
            this.input.render(guiGraphics, mouseX, mouseY, partialTick);

            if (this.input.isFocused()) {
                KonfigConfigScreen.this.setActiveRegistryRow(this);
                this.refreshSuggestions();
            }
            if (KonfigConfigScreen.this.activeRegistryRow == this && !this.visibleSuggestions.isEmpty()) {
                KonfigConfigScreen.this.renderedRegistryRow = this;
            }
        }

        public boolean isFocused() {
            return this.input.isFocused();
        }

        private boolean isPointInsideInput(double mouseX, double mouseY) {
            return mouseX >= this.lastInputX
                    && mouseX <= this.lastInputX + this.lastInputWidth
                    && mouseY >= this.lastInputY
                    && mouseY <= this.lastInputY + CONTROL_HEIGHT;
        }

        private void refreshSuggestions() {
            if (!this.entry.value.hasBoundRegistry()) {
                this.closeSuggestions();
                return;
            }

            if (this.suggestionsDismissed) {
                if (sameValue(this.input.getValue(), this.dismissedValue)) {
                    this.visibleSuggestions.clear();
                    this.selectedSuggestionIndex = 0;
                    this.input.setSuggestion("");
                    return;
                }
                this.suggestionsDismissed = false;
                this.dismissedValue = "";
            }

            this.visibleSuggestions.clear();
            this.visibleSuggestions.addAll(filterRegistrySuggestions(
                    KonfigConfigScreen.this.registrySuggestions(this.entry.value.boundRegistryId()),
                    this.input.getValue()
            ));

            if (this.visibleSuggestions.isEmpty()) {
                this.selectedSuggestionIndex = 0;
                this.input.setSuggestion("");
                return;
            }

            this.selectedSuggestionIndex = MathHelper.clamp(this.selectedSuggestionIndex, 0, this.visibleSuggestions.size() - 1);
            this.updateInlineSuggestion();
        }

        private void activateSuggestions() {
            this.suggestionsDismissed = false;
            this.dismissedValue = "";
            this.refreshSuggestions();
        }

        private void dismissSuggestions() {
            this.suggestionsDismissed = true;
            this.dismissedValue = this.input.getValue();
            this.visibleSuggestions.clear();
            this.selectedSuggestionIndex = 0;
            this.input.setSuggestion("");
        }

        private void closeSuggestions() {
            this.suggestionsDismissed = false;
            this.dismissedValue = "";
            this.visibleSuggestions.clear();
            this.selectedSuggestionIndex = 0;
            this.input.setSuggestion("");
            if (KonfigConfigScreen.this.activeRegistryRow == this) {
                KonfigConfigScreen.this.activeRegistryRow = null;
            }
        }

        private void renderSuggestions(MatrixStack guiGraphics, int mouseX, int mouseY) {
            if (KonfigConfigScreen.this.activeRegistryRow != this || this.visibleSuggestions.isEmpty()) {
                return;
            }

            this.layoutSuggestionBox();
            AbstractGui.fill(guiGraphics, this.lastDropdownX - 1, this.lastDropdownY - 1, this.lastDropdownX + this.lastDropdownWidth + 1, this.lastDropdownY + this.lastDropdownHeight + 1, 0xFF202020);
            AbstractGui.fill(guiGraphics, this.lastDropdownX, this.lastDropdownY, this.lastDropdownX + this.lastDropdownWidth, this.lastDropdownY + this.lastDropdownHeight, 0xF0101010);

            for (int index = 0; index < this.visibleSuggestions.size(); index++) {
                int rowY = this.lastDropdownY + 2 + (index * SUGGESTION_ROW_HEIGHT);
                int rowBottom = rowY + SUGGESTION_ROW_HEIGHT;
                boolean hovered = index == this.hoveredSuggestionIndex(mouseX, mouseY);
                if (hovered || index == this.selectedSuggestionIndex) {
                    AbstractGui.fill(guiGraphics, this.lastDropdownX + 1, rowY, this.lastDropdownX + this.lastDropdownWidth - 1, rowBottom, hovered ? 0x80406080 : 0x50303030);
                }
                int textX = this.lastDropdownX + 4;
                if (this.entry.value.hasBoundRegistry() && supportsRegistryIcon(this.entry.value.boundRegistryId())) {
                    renderRegistryIcon(guiGraphics, this.entry.value.boundRegistryId(), this.visibleSuggestions.get(index), this.lastDropdownX + 2, rowY - 1);
                    textX += 18;
                }
                KonfigConfigScreen.this.font.draw(
                        guiGraphics,
                        text(this.visibleSuggestions.get(index)),
                        (float) textX,
                        (float) (rowY + 3),
                        0xFFFFFFFF
                );
            }
        }

        private boolean handleSuggestionClick(double mouseX, double mouseY) {
            if (KonfigConfigScreen.this.activeRegistryRow != this || this.visibleSuggestions.isEmpty()) {
                return false;
            }

            int hovered = this.hoveredSuggestionIndex((int) mouseX, (int) mouseY);
            if (hovered < 0) {
                return false;
            }

            this.acceptSuggestion(this.visibleSuggestions.get(hovered));
            return true;
        }

        private boolean handleSuggestionKey(int keyCode) {
            if (KonfigConfigScreen.this.activeRegistryRow != this) {
                return false;
            }

            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.dismissSuggestions();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                this.dismissSuggestions();
                return true;
            }
            if (this.visibleSuggestions.isEmpty()) {
                return false;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                this.selectedSuggestionIndex = (this.selectedSuggestionIndex + 1) % this.visibleSuggestions.size();
                this.updateInlineSuggestion();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                this.selectedSuggestionIndex = (this.selectedSuggestionIndex + this.visibleSuggestions.size() - 1) % this.visibleSuggestions.size();
                this.updateInlineSuggestion();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                this.acceptSuggestion(this.visibleSuggestions.get(this.selectedSuggestionIndex));
                return true;
            }
            return false;
        }

        private void acceptSuggestion(String suggestion) {
            this.suppressResponder = true;
            this.input.setValue(suggestion);
            this.suppressResponder = false;
            KonfigConfigScreen.this.drafts.put(this.entry.value, suggestion);
            KonfigConfigScreen.this.persistEntry(this.entry);
            this.dismissSuggestions();
            this.input.setFocus(true);
        }

        private void updateInlineSuggestion() {
            if (this.visibleSuggestions.isEmpty()) {
                this.input.setSuggestion("");
                return;
            }
            this.input.setSuggestion(suggestionSuffix(this.input.getValue(), this.visibleSuggestions.get(this.selectedSuggestionIndex)));
        }

        private void layoutSuggestionBox() {
            this.lastDropdownX = this.lastInputX;
            this.lastDropdownWidth = this.lastInputWidth;
            this.lastDropdownHeight = (this.visibleSuggestions.size() * SUGGESTION_ROW_HEIGHT) + 4;

            int belowY = this.lastInputY + CONTROL_HEIGHT + 2;
            int aboveY = this.lastInputY - this.lastDropdownHeight - 2;
            boolean openAbove = belowY + this.lastDropdownHeight > KonfigConfigScreen.this.height - 32 && aboveY >= LIST_TOP;
            this.lastDropdownY = openAbove ? aboveY : belowY;
        }

        private int hoveredSuggestionIndex(int mouseX, int mouseY) {
            if (mouseX < this.lastDropdownX
                    || mouseX > this.lastDropdownX + this.lastDropdownWidth
                    || mouseY < this.lastDropdownY + 2
                    || mouseY > this.lastDropdownY + this.lastDropdownHeight - 2) {
                return -1;
            }

            int index = (mouseY - this.lastDropdownY - 2) / SUGGESTION_ROW_HEIGHT;
            return index >= 0 && index < this.visibleSuggestions.size() ? index : -1;
        }
    }

    private final class TextInputRow extends ConfigRow {
        private final TextFieldWidget input;

        private TextInputRow(EntryRef entry) {
            super(entry);
            this.input = new TextFieldWidget(KonfigConfigScreen.this.font, 0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, entry.label);
            this.input.setMaxLength(256);
            this.input.setValue(stringValue(KonfigConfigScreen.this.drafts.get(entry.value)));
            this.input.setResponder(value -> {
                KonfigConfigScreen.this.drafts.put(entry.value, value);
                KonfigConfigScreen.this.persistEntry(entry);
            });
        }

        @Override
        protected Widget control() {
            return this.input;
        }

        @Override
        protected void tick() {
        }

        @Override
        protected void syncFromDraft() {
            this.input.setValue(stringValue(KonfigConfigScreen.this.drafts.get(this.entry.value)));
        }
    }

    private abstract class EntryEditorScreen extends Screen {
        protected static final int EDITOR_TITLE_Y = 8;
        protected static final int EDITOR_CONTEXT_Y = 24;
        protected static final int EDITOR_CONTENT_TOP = 42;

        protected final EntryRef entry;
        protected String statusMessage = "";
        protected int statusColor = 0xFFFF8080;

        private EntryEditorScreen(EntryRef entry) {
            super(entry.label);
            this.entry = entry;
        }

        @Override
        public void onClose() {
            this.returnToParent();
        }

        protected final void returnToParent() {
            KonfigConfigScreen.this.rebuildScreenWidgets();
            this.minecraft.setScreen(KonfigConfigScreen.this);
        }

        protected final boolean persistEditedValue(Object previousValue) {
            if (!KonfigConfigScreen.this.persistEntry(this.entry)) {
                KonfigConfigScreen.this.drafts.put(this.entry.value, copyDraftValue(this.entry.value, previousValue));
                this.copyStatusFromParent();
                return false;
            }
            this.copyStatusFromParent();
            return true;
        }

        protected final boolean resetToSessionStart() {
            Object previousValue = snapshotValue(this.entry.value, this.entry.value.get());
            try {
                Object resetValue = snapshotValue(this.entry.value, KonfigConfigScreen.this.sessionStartValues.get(this.entry.value));
                KonfigConfigScreen.this.drafts.put(this.entry.value, copyDraftValue(this.entry.value, resetValue));
                setRawValue(this.entry.value, resetValue);
                this.entry.handle.save();
                KonfigConfigScreen.this.statusMessage = translate("konfig.screen.status.reset").getString();
                KonfigConfigScreen.this.statusColor = 0xFF80FF80;
                this.copyStatusFromParent();
                return true;
            } catch (Exception exception) {
                setRawValue(this.entry.value, previousValue);
                KonfigConfigScreen.this.drafts.put(this.entry.value, copyDraftValue(this.entry.value, previousValue));
                KonfigConfigScreen.this.statusMessage = exception.getMessage() == null
                        ? translate("konfig.screen.status.save_failed").getString()
                        : exception.getMessage();
                KonfigConfigScreen.this.statusColor = 0xFFFF8080;
                this.copyStatusFromParent();
                return false;
            }
        }

        protected final void copyStatusFromParent() {
            this.statusMessage = KonfigConfigScreen.this.statusMessage;
            this.statusColor = KonfigConfigScreen.this.statusColor;
        }

        protected final void renderEditorChrome(MatrixStack guiGraphics, int mouseX, int mouseY, float partialTick) {
            AbstractGui.fill(guiGraphics, 0, 0, this.width, this.height, 0xC0101010);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            drawCenteredString(guiGraphics, this.font, this.title, this.width / 2, EDITOR_TITLE_Y, 0xFFFFFFFF);
            this.font.draw(guiGraphics, this.entry.contextLabel, 12, EDITOR_CONTEXT_Y, 0xFFA0A0A0);
            if (!this.statusMessage.isEmpty()) {
                drawCenteredString(guiGraphics, this.font, text(this.statusMessage), this.width / 2, this.height - 38, this.statusColor);
            }
        }
    }

    private enum ColorChannel {
        RED("konfig.screen.color.red"),
        GREEN("konfig.screen.color.green"),
        BLUE("konfig.screen.color.blue"),
        ALPHA("konfig.screen.color.alpha");

        private final String translationKey;

        ColorChannel(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private final class ColorEditorScreen extends EntryEditorScreen {
        private static final int PREVIEW_SIZE = 32;
        private static final int PREVIEW_Y = EDITOR_CONTENT_TOP;
        private static final int HEX_WIDTH = 108;
        private static final int HEX_Y = PREVIEW_Y + PREVIEW_SIZE + 8;
        private static final int SLIDER_WIDTH = 220;
        private static final int SLIDER_Y = HEX_Y + 28;
        private static final int SLIDER_STEP = 26;

        private TextFieldWidget hexInput;
        private ChannelSlider redSlider;
        private ChannelSlider greenSlider;
        private ChannelSlider blueSlider;
        private ChannelSlider alphaSlider;
        private boolean suppressHexResponder;

        private ColorEditorScreen(EntryRef entry) {
            super(entry);
        }

        @Override
        protected void init() {
            this.buttons.clear();
            this.children.clear();

            this.hexInput = this.addButton(new TextFieldWidget(this.font, this.width / 2 - HEX_WIDTH / 2, HEX_Y, HEX_WIDTH, 20, this.entry.label));
            this.hexInput.setMaxLength(this.entry.value.kind() == EntryKind.COLOR_ARGB ? 9 : 7);
            this.hexInput.setValue(this.currentHex());
            this.hexInput.setResponder(this::onHexChanged);

            int sliderX = this.width / 2 - SLIDER_WIDTH / 2;
            this.redSlider = this.addButton(new ChannelSlider(ColorChannel.RED, sliderX, SLIDER_Y));
            this.greenSlider = this.addButton(new ChannelSlider(ColorChannel.GREEN, sliderX, SLIDER_Y + SLIDER_STEP));
            this.blueSlider = this.addButton(new ChannelSlider(ColorChannel.BLUE, sliderX, SLIDER_Y + (SLIDER_STEP * 2)));
            if (this.entry.value.kind() == EntryKind.COLOR_ARGB) {
                this.alphaSlider = this.addButton(new ChannelSlider(ColorChannel.ALPHA, sliderX, SLIDER_Y + (SLIDER_STEP * 3)));
            }

            int footerY = this.height - 26;
            this.addButton(new Button(this.width / 2 - 122, footerY, 80, 20, translate("konfig.screen.reset"), ignored -> {
                if (this.resetToSessionStart()) {
                    this.syncWidgetsFromDraft();
                }
            }));
            this.addButton(new Button(this.width / 2 + 42, footerY, 80, 20, translate("konfig.screen.done"), ignored -> this.onClose()));

            this.syncWidgetsFromDraft();
        }

        @Override
        public void render(MatrixStack guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick);
            int previewX = this.width / 2 - PREVIEW_SIZE / 2;
            drawColorSwatch(guiGraphics, previewX, PREVIEW_Y, PREVIEW_SIZE, KonfigConfigScreen.this.currentColor(this.entry.value), this.entry.value.kind());
        }

        @Override
        public void tick() {
            super.tick();
        }

        private void onHexChanged(String value) {
            if (this.suppressHexResponder) {
                return;
            }

            String normalized = normalizeHexInput(value);
            int expectedDigits = ColorValueHelper.expectedDigits(this.entry.value.kind());
            if (normalized.isEmpty()) {
                this.statusMessage = "";
                return;
            }
            if (!isHexPrefix(normalized) || normalized.length() > expectedDigits) {
                this.statusMessage = translate("konfig.screen.color.invalid", Integer.valueOf(expectedDigits)).getString();
                this.statusColor = 0xFFFF8080;
                return;
            }
            if (normalized.length() < expectedDigits) {
                this.statusMessage = "";
                return;
            }

            Object previousValue = snapshotValue(this.entry.value, this.entry.value.get());
            try {
                int parsed = parseColor(this.entry.value, value);
                KonfigConfigScreen.this.drafts.put(this.entry.value, Integer.valueOf(parsed));
                if (this.persistEditedValue(previousValue)) {
                    this.syncWidgetsFromDraft();
                } else {
                    this.syncWidgetsFromDraft();
                }
            } catch (Exception exception) {
                KonfigConfigScreen.this.drafts.put(this.entry.value, copyDraftValue(this.entry.value, previousValue));
                this.statusMessage = exception.getMessage() == null
                        ? translate("konfig.screen.color.invalid", Integer.valueOf(expectedDigits)).getString()
                        : exception.getMessage();
                this.statusColor = 0xFFFF8080;
                this.syncWidgetsFromDraft();
            }
        }

        private String currentHex() {
            int color = KonfigConfigScreen.this.currentColor(this.entry.value);
            if (this.entry.value.kind() == EntryKind.COLOR_ARGB) {
                return ColorValueHelper.formatArgb(color);
            }
            return ColorValueHelper.formatRgb(color);
        }

        private void syncWidgetsFromDraft() {
            this.suppressHexResponder = true;
            this.hexInput.setValue(this.currentHex());
            this.suppressHexResponder = false;
            this.redSlider.syncToDraft();
            this.greenSlider.syncToDraft();
            this.blueSlider.syncToDraft();
            if (this.alphaSlider != null) {
                this.alphaSlider.syncToDraft();
            }
        }

        private int currentChannel(ColorChannel channel) {
            int color = KonfigConfigScreen.this.currentColor(this.entry.value);
            switch (channel) {
                case RED:
                    return ColorValueHelper.red(color);
                case GREEN:
                    return ColorValueHelper.green(color);
                case BLUE:
                    return ColorValueHelper.blue(color);
                case ALPHA:
                    return this.entry.value.kind() == EntryKind.COLOR_ARGB ? ColorValueHelper.alpha(color) : 255;
                default:
                    return 0;
            }
        }

        private int withChannel(ColorChannel channel, int value) {
            int current = KonfigConfigScreen.this.currentColor(this.entry.value);
            int alpha = this.entry.value.kind() == EntryKind.COLOR_ARGB ? ColorValueHelper.alpha(current) : 255;
            int red = ColorValueHelper.red(current);
            int green = ColorValueHelper.green(current);
            int blue = ColorValueHelper.blue(current);

            switch (channel) {
                case RED:
                    red = value;
                    break;
                case GREEN:
                    green = value;
                    break;
                case BLUE:
                    blue = value;
                    break;
                case ALPHA:
                    alpha = value;
                    break;
                default:
                    break;
            }

            if (this.entry.value.kind() == EntryKind.COLOR_ARGB) {
                return ColorValueHelper.argb(alpha, red, green, blue);
            }
            return ColorValueHelper.rgb(red, green, blue);
        }

        private final class ChannelSlider extends BaseSliderWidget {
            private final ColorChannel channel;

            private ChannelSlider(ColorChannel channel, int x, int y) {
                super(ColorEditorScreen.this.currentChannel(channel) / 255.0D);
                this.channel = channel;
                this.x = x;
                this.y = y;
                this.setWidth(SLIDER_WIDTH);
                this.updateMessage();
            }

            private void syncToDraft() {
                this.syncToProgress(ColorEditorScreen.this.currentChannel(this.channel) / 255.0D);
            }

            @Override
            protected void updateMessage() {
                this.setMessage(translate(this.channel.translationKey, Integer.valueOf(ColorEditorScreen.this.currentChannel(this.channel))));
            }

            @Override
            protected void applyValue() {
                KonfigConfigScreen.this.drafts.put(ColorEditorScreen.this.entry.value, Integer.valueOf(ColorEditorScreen.this.withChannel(this.channel, intFromProgress(this.value, 0, 255))));
            }

            @Override
            public void onRelease(double mouseX, double mouseY) {
                Object previousValue = snapshotValue(ColorEditorScreen.this.entry.value, ColorEditorScreen.this.entry.value.get());
                super.onRelease(mouseX, mouseY);
                if (ColorEditorScreen.this.persistEditedValue(previousValue)) {
                    ColorEditorScreen.this.syncWidgetsFromDraft();
                }
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                Object previousValue = snapshotValue(ColorEditorScreen.this.entry.value, ColorEditorScreen.this.entry.value.get());
                int before = ColorEditorScreen.this.currentChannel(this.channel);
                boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
                if (handled && before != ColorEditorScreen.this.currentChannel(this.channel)) {
                    if (ColorEditorScreen.this.persistEditedValue(previousValue)) {
                        ColorEditorScreen.this.syncWidgetsFromDraft();
                    }
                }
                return handled;
            }
        }
    }

    private final class StringListEditorScreen extends EntryEditorScreen {
        private static final int ITEM_ROW_HEIGHT = 28;

        private ListEntryList list;
        private ListEntryRow activeRegistryRow;
        private ListEntryRow renderedRegistryRow;

        private StringListEditorScreen(EntryRef entry) {
            super(entry);
        }

        @Override
        protected void init() {
            this.rebuildEditorWidgets();
        }

        @Override
        public void render(MatrixStack guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderedRegistryRow = null;
            this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick);
            if (this.list != null) {
                this.list.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            String count = translate("konfig.screen.list.count", Integer.valueOf(KonfigConfigScreen.this.currentStringList(this.entry.value).size())).getString();
            this.font.draw(guiGraphics, text(count), this.width - 12 - this.font.width(count), EDITOR_CONTEXT_Y, 0xFFC0C0C0);
            if (KonfigConfigScreen.this.currentStringList(this.entry.value).isEmpty()) {
                drawCenteredString(guiGraphics, this.font, translate("konfig.screen.list.empty"), this.width / 2, this.height / 2 - 12, 0xFFC0C0C0);
            }
            if (this.renderedRegistryRow != null) {
                this.renderedRegistryRow.renderSuggestions(guiGraphics, mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.activeRegistryRow != null && this.activeRegistryRow.handleSuggestionClick(mouseX, mouseY)) {
                return true;
            }

            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            ListEntryRow focusedRow = this.findFocusedRegistryRow();
            if (focusedRow != null) {
                this.setActiveRegistryRow(focusedRow);
                focusedRow.activateSuggestions();
            } else if (this.activeRegistryRow != null && !this.activeRegistryRow.isPointInsideInput(mouseX, mouseY)) {
                this.activeRegistryRow.closeSuggestions();
            }
            return handled;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.activeRegistryRow != null && this.activeRegistryRow.handleSuggestionKey(keyCode)) {
                return true;
            }
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (this.activeRegistryRow != null && this.activeRegistryRow.isFocused() && this.activeRegistryRow.hasRegistryBinding()) {
                this.activeRegistryRow.refreshSuggestions();
            }
            return handled;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.list == null) {
                return;
            }
            for (ListEntryRow row : this.list.children()) {
                row.tick();
            }
        }

        private void rebuildEditorWidgets() {
            this.buttons.clear();
            this.children.clear();
            this.activeRegistryRow = null;
            this.renderedRegistryRow = null;
            int listTop = EDITOR_CONTENT_TOP;
            int listHeight = Math.max(48, this.height - listTop - LIST_BOTTOM_MARGIN);
            this.list = new ListEntryList(this.minecraft, this.width, listHeight, listTop);
            this.children.add(this.list);
            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            for (int i = 0; i < values.size(); i++) {
                this.list.addListEntry(new ListEntryRow(i, values.get(i)));
            }

            int footerY = this.height - 26;
            this.addButton(new Button(this.width / 2 - 122, footerY, 80, 20, translate("konfig.screen.list.add"), ignored -> this.addValue()));
            this.addButton(new Button(this.width / 2 - 40, footerY, 80, 20, translate("konfig.screen.reset"), ignored -> {
                if (this.resetToSessionStart()) {
                    this.rebuildEditorWidgets();
                }
            }));
            this.addButton(new Button(this.width / 2 + 42, footerY, 80, 20, translate("konfig.screen.done"), ignored -> this.onClose()));
        }

        private void addValue() {
            Object previousValue = snapshotValue(this.entry.value, this.entry.value.get());
            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            values.add(this.entry.value.hasBoundRegistry() ? "" : translate("konfig.screen.list.new_item").getString());
            KonfigConfigScreen.this.drafts.put(this.entry.value, values);
            if (this.persistEditedValue(previousValue)) {
                this.rebuildEditorWidgets();
            }
        }

        private ListEntryRow findFocusedRegistryRow() {
            if (this.list == null) {
                return null;
            }
            for (ListEntryRow row : this.list.children()) {
                if (row.hasRegistryBinding() && row.isFocused()) {
                    return row;
                }
            }
            return null;
        }

        private void setActiveRegistryRow(ListEntryRow row) {
            if (this.activeRegistryRow == row) {
                return;
            }
            if (this.activeRegistryRow != null) {
                this.activeRegistryRow.closeSuggestions();
            }
            this.activeRegistryRow = row;
        }

        private final class ListEntryList extends ExtendedList<ListEntryRow> {
            private ListEntryList(net.minecraft.client.Minecraft minecraft, int width, int height, int y) {
                super(minecraft, width, height, y, y + height, ITEM_ROW_HEIGHT);
                this.setRenderBackground(false);
                this.setRenderTopAndBottom(false);
            }

            private void addListEntry(ListEntryRow row) {
                super.addEntry(row);
            }

            @Override
            public int getRowWidth() {
                return StringListEditorScreen.this.width - 28;
            }

            @Override
            protected int getScrollbarPosition() {
                return this.x1 - 6;
            }

            @Override
            protected void renderBackground(MatrixStack guiGraphics) {
                AbstractGui.fill(guiGraphics, this.x0, this.y0, this.x1, this.y1, 0x66000000);
            }
        }

        private final class ListEntryRow extends ExtendedList.AbstractListEntry<ListEntryRow> implements INestedGuiEventHandler {
            private static final int ICON_SIZE = 16;
            private static final int ICON_GAP = 4;

            private final int index;
            private final TextFieldWidget input;
            private final Button moveUpButton;
            private final Button moveDownButton;
            private final Button removeButton;
            private final List<String> visibleSuggestions = new ArrayList<String>();
            private boolean suppressResponder;
            private boolean suggestionsDismissed;
            private String dismissedValue = "";
            private int selectedSuggestionIndex;
            private int lastInputX;
            private int lastInputY;
            private int lastInputWidth;
            private int lastDropdownX;
            private int lastDropdownY;
            private int lastDropdownWidth;
            private int lastDropdownHeight;
            private IGuiEventListener focused;
            private boolean dragging;

            private ListEntryRow(int index, String value) {
                this.index = index;
                this.input = new TextFieldWidget(StringListEditorScreen.this.font, 0, 0, 140, CONTROL_HEIGHT, StringListEditorScreen.this.entry.label);
                this.input.setMaxLength(256);
                this.input.setValue(value);
                this.input.setResponder(this::onValueChanged);

                this.moveUpButton = new Button(0, 0, 20, 20, text("^"), ignored -> this.move(-1));
                this.moveDownButton = new Button(0, 0, 20, 20, text("v"), ignored -> this.move(1));
                this.removeButton = new Button(0, 0, 20, 20, text("-"), ignored -> this.remove());
            }

            private void tick() {
                if (this.input.isFocused() && this.hasRegistryBinding()) {
                    StringListEditorScreen.this.setActiveRegistryRow(this);
                    this.refreshSuggestions();
                }
            }

            private void onValueChanged(String value) {
                if (this.suppressResponder) {
                    return;
                }

                this.suggestionsDismissed = false;
                this.dismissedValue = "";
                this.persistListValue(value);
            }

            private boolean hasRegistryBinding() {
                return StringListEditorScreen.this.entry.value.hasBoundRegistry();
            }

            private String registryKey() {
                return StringListEditorScreen.this.entry.value.boundRegistryId();
            }

            public boolean isFocused() {
                return this.input.isFocused();
            }

            private boolean isPointInsideInput(double mouseX, double mouseY) {
                return mouseX >= this.lastInputX
                        && mouseX <= this.lastInputX + this.lastInputWidth
                        && mouseY >= this.lastInputY
                        && mouseY <= this.lastInputY + CONTROL_HEIGHT;
            }

            private boolean persistListValue(String value) {
                Object previousValue = snapshotValue(StringListEditorScreen.this.entry.value, StringListEditorScreen.this.entry.value.get());
                List<String> values = KonfigConfigScreen.this.currentStringList(StringListEditorScreen.this.entry.value);
                values.set(this.index, value);
                KonfigConfigScreen.this.drafts.put(StringListEditorScreen.this.entry.value, values);
                if (!StringListEditorScreen.this.persistEditedValue(previousValue)) {
                    this.suppressResponder = true;
                    this.input.setValue(currentStringList(StringListEditorScreen.this.entry.value).get(this.index));
                    this.suppressResponder = false;
                    this.refreshSuggestions();
                    return false;
                }
                this.refreshSuggestions();
                return true;
            }

            private void move(int delta) {
                int targetIndex = this.index + delta;
                List<String> current = KonfigConfigScreen.this.currentStringList(StringListEditorScreen.this.entry.value);
                if (targetIndex < 0 || targetIndex >= current.size()) {
                    return;
                }

                Object previousValue = snapshotValue(StringListEditorScreen.this.entry.value, StringListEditorScreen.this.entry.value.get());
                Collections.swap(current, this.index, targetIndex);
                KonfigConfigScreen.this.drafts.put(StringListEditorScreen.this.entry.value, current);
                if (StringListEditorScreen.this.persistEditedValue(previousValue)) {
                    StringListEditorScreen.this.rebuildEditorWidgets();
                }
            }

            private void remove() {
                List<String> current = KonfigConfigScreen.this.currentStringList(StringListEditorScreen.this.entry.value);
                if (this.index < 0 || this.index >= current.size()) {
                    return;
                }

                Object previousValue = snapshotValue(StringListEditorScreen.this.entry.value, StringListEditorScreen.this.entry.value.get());
                current.remove(this.index);
                KonfigConfigScreen.this.drafts.put(StringListEditorScreen.this.entry.value, current);
                if (StringListEditorScreen.this.persistEditedValue(previousValue)) {
                    StringListEditorScreen.this.rebuildEditorWidgets();
                }
            }

            @Override
            public void render(MatrixStack guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                if (hovered) {
                    AbstractGui.fill(guiGraphics, x, y, x + width, y + ITEM_ROW_HEIGHT, 0x22000000);
                }

                int buttonY = y + 4;
                int removeX = x + width - 20;
                int downX = removeX - 24;
                int upX = downX - 24;
                int iconOffset = 0;
                if (this.hasRegistryBinding() && supportsRegistryIcon(this.registryKey())) {
                    renderRegistryIcon(guiGraphics, this.registryKey(), this.input.getValue(), x, y + (ITEM_ROW_HEIGHT - ICON_SIZE) / 2);
                    iconOffset = ICON_SIZE + ICON_GAP;
                }
                int inputX = x + iconOffset;
                int inputWidth = Math.max(60, upX - inputX - 8);

                this.input.setX(inputX);
                this.input.y = buttonY;
                this.input.setWidth(inputWidth);
                this.lastInputX = inputX;
                this.lastInputY = buttonY;
                this.lastInputWidth = inputWidth;

                this.moveUpButton.x = upX;
                this.moveUpButton.y = buttonY;
                this.moveUpButton.active = this.index > 0;

                this.moveDownButton.x = downX;
                this.moveDownButton.y = buttonY;
                this.moveDownButton.active = this.index + 1 < KonfigConfigScreen.this.currentStringList(StringListEditorScreen.this.entry.value).size();

                this.removeButton.x = removeX;
                this.removeButton.y = buttonY;

                this.input.render(guiGraphics, mouseX, mouseY, partialTick);
                this.moveUpButton.render(guiGraphics, mouseX, mouseY, partialTick);
                this.moveDownButton.render(guiGraphics, mouseX, mouseY, partialTick);
                this.removeButton.render(guiGraphics, mouseX, mouseY, partialTick);

                if (this.hasRegistryBinding() && this.input.isFocused()) {
                    StringListEditorScreen.this.setActiveRegistryRow(this);
                    this.refreshSuggestions();
                }
                if (StringListEditorScreen.this.activeRegistryRow == this && !this.visibleSuggestions.isEmpty()) {
                    StringListEditorScreen.this.renderedRegistryRow = this;
                }
            }

            private void refreshSuggestions() {
                if (!this.hasRegistryBinding()) {
                    this.closeSuggestions();
                    return;
                }

                if (this.suggestionsDismissed) {
                    if (sameValue(this.input.getValue(), this.dismissedValue)) {
                        this.visibleSuggestions.clear();
                        this.selectedSuggestionIndex = 0;
                        this.input.setSuggestion("");
                        return;
                    }
                    this.suggestionsDismissed = false;
                    this.dismissedValue = "";
                }

                this.visibleSuggestions.clear();
                this.visibleSuggestions.addAll(filterRegistrySuggestions(
                        KonfigConfigScreen.this.registrySuggestions(this.registryKey()),
                        this.input.getValue()
                ));
                if (this.visibleSuggestions.isEmpty()) {
                    this.selectedSuggestionIndex = 0;
                    this.input.setSuggestion("");
                    return;
                }

                this.selectedSuggestionIndex = MathHelper.clamp(this.selectedSuggestionIndex, 0, this.visibleSuggestions.size() - 1);
                this.updateInlineSuggestion();
            }

            private void activateSuggestions() {
                this.suggestionsDismissed = false;
                this.dismissedValue = "";
                this.refreshSuggestions();
            }

            private void dismissSuggestions() {
                this.suggestionsDismissed = true;
                this.dismissedValue = this.input.getValue();
                this.visibleSuggestions.clear();
                this.selectedSuggestionIndex = 0;
                this.input.setSuggestion("");
            }

            private void closeSuggestions() {
                this.suggestionsDismissed = false;
                this.dismissedValue = "";
                this.visibleSuggestions.clear();
                this.selectedSuggestionIndex = 0;
                this.input.setSuggestion("");
                if (StringListEditorScreen.this.activeRegistryRow == this) {
                    StringListEditorScreen.this.activeRegistryRow = null;
                }
            }

            private void renderSuggestions(MatrixStack guiGraphics, int mouseX, int mouseY) {
                if (StringListEditorScreen.this.activeRegistryRow != this || this.visibleSuggestions.isEmpty()) {
                    return;
                }

                this.layoutSuggestionBox();
                AbstractGui.fill(guiGraphics, this.lastDropdownX - 1, this.lastDropdownY - 1, this.lastDropdownX + this.lastDropdownWidth + 1, this.lastDropdownY + this.lastDropdownHeight + 1, 0xFF202020);
                AbstractGui.fill(guiGraphics, this.lastDropdownX, this.lastDropdownY, this.lastDropdownX + this.lastDropdownWidth, this.lastDropdownY + this.lastDropdownHeight, 0xF0101010);

                for (int suggestionIndex = 0; suggestionIndex < this.visibleSuggestions.size(); suggestionIndex++) {
                    int rowY = this.lastDropdownY + 2 + (suggestionIndex * SUGGESTION_ROW_HEIGHT);
                    int rowBottom = rowY + SUGGESTION_ROW_HEIGHT;
                    boolean suggestionHovered = suggestionIndex == this.hoveredSuggestionIndex(mouseX, mouseY);
                    if (suggestionHovered || suggestionIndex == this.selectedSuggestionIndex) {
                        AbstractGui.fill(guiGraphics, this.lastDropdownX + 1, rowY, this.lastDropdownX + this.lastDropdownWidth - 1, rowBottom, suggestionHovered ? 0x80406080 : 0x50303030);
                    }
                    int textX = this.lastDropdownX + 4;
                    if (supportsRegistryIcon(this.registryKey())) {
                        renderRegistryIcon(guiGraphics, this.registryKey(), this.visibleSuggestions.get(suggestionIndex), this.lastDropdownX + 2, rowY - 1);
                        textX += 18;
                    }
                    StringListEditorScreen.this.font.draw(guiGraphics, text(this.visibleSuggestions.get(suggestionIndex)), (float) textX, (float) (rowY + 3), 0xFFFFFFFF);
                }
            }

            private boolean handleSuggestionClick(double mouseX, double mouseY) {
                if (StringListEditorScreen.this.activeRegistryRow != this || this.visibleSuggestions.isEmpty()) {
                    return false;
                }

                int hovered = this.hoveredSuggestionIndex((int) mouseX, (int) mouseY);
                if (hovered < 0) {
                    return false;
                }

                this.acceptSuggestion(this.visibleSuggestions.get(hovered));
                return true;
            }

            private boolean handleSuggestionKey(int keyCode) {
                if (StringListEditorScreen.this.activeRegistryRow != this) {
                    return false;
                }
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    this.dismissSuggestions();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    this.dismissSuggestions();
                    return true;
                }
                if (this.visibleSuggestions.isEmpty()) {
                    return false;
                }
                if (keyCode == GLFW.GLFW_KEY_DOWN) {
                    this.selectedSuggestionIndex = (this.selectedSuggestionIndex + 1) % this.visibleSuggestions.size();
                    this.updateInlineSuggestion();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_UP) {
                    this.selectedSuggestionIndex = (this.selectedSuggestionIndex + this.visibleSuggestions.size() - 1) % this.visibleSuggestions.size();
                    this.updateInlineSuggestion();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_TAB) {
                    this.acceptSuggestion(this.visibleSuggestions.get(this.selectedSuggestionIndex));
                    return true;
                }
                return false;
            }

            private void acceptSuggestion(String suggestion) {
                this.suppressResponder = true;
                this.input.setValue(suggestion);
                this.suppressResponder = false;
                if (this.persistListValue(suggestion)) {
                    this.dismissSuggestions();
                    this.input.setFocus(true);
                }
            }

            private void updateInlineSuggestion() {
                if (this.visibleSuggestions.isEmpty()) {
                    this.input.setSuggestion("");
                    return;
                }
                this.input.setSuggestion(suggestionSuffix(this.input.getValue(), this.visibleSuggestions.get(this.selectedSuggestionIndex)));
            }

            private void layoutSuggestionBox() {
                this.lastDropdownX = this.lastInputX;
                this.lastDropdownWidth = this.lastInputWidth;
                this.lastDropdownHeight = (this.visibleSuggestions.size() * SUGGESTION_ROW_HEIGHT) + 4;
                int belowY = this.lastInputY + CONTROL_HEIGHT + 2;
                int aboveY = this.lastInputY - this.lastDropdownHeight - 2;
                boolean openAbove = belowY + this.lastDropdownHeight > StringListEditorScreen.this.height - 32 && aboveY >= LIST_TOP;
                this.lastDropdownY = openAbove ? aboveY : belowY;
            }

            private int hoveredSuggestionIndex(int mouseX, int mouseY) {
                if (mouseX < this.lastDropdownX
                        || mouseX > this.lastDropdownX + this.lastDropdownWidth
                        || mouseY < this.lastDropdownY + 2
                        || mouseY > this.lastDropdownY + this.lastDropdownHeight - 2) {
                    return -1;
                }
                int suggestionIndex = (mouseY - this.lastDropdownY - 2) / SUGGESTION_ROW_HEIGHT;
                return suggestionIndex >= 0 && suggestionIndex < this.visibleSuggestions.size() ? suggestionIndex : -1;
            }

            @Override
            public List<? extends IGuiEventListener> children() {
                return Arrays.asList(this.input, this.moveUpButton, this.moveDownButton, this.removeButton);
            }

            @Override
            public IGuiEventListener getFocused() {
                return this.focused;
            }

            @Override
            public void setFocused(IGuiEventListener listener) {
                this.focused = listener;
            }

            @Override
            public boolean isDragging() {
                return this.dragging;
            }

            @Override
            public void setDragging(boolean dragging) {
                this.dragging = dragging;
            }
        }
    }

    private static final class EntryRef {
        private final ConfigHandleImpl handle;
        private final ConfigValueImpl<?> value;
        private final ITextComponent label;
        private final ITextComponent contextLabel;
        private final String tooltip;
        private final boolean editable;

        private EntryRef(ConfigHandleImpl handle, ConfigValueImpl<?> value, boolean editable) {
            this.handle = handle;
            this.value = value;
            this.label = translatedLabel(handle, value);
            this.contextLabel = contextLabel(handle, value);
            this.tooltip = handle.tooltip(value.path());
            this.editable = editable;
        }

        private ITextComponent displayLabel() {
            if (this.editable) {
                return this.label;
            }
            return StringTextComponent.EMPTY.copy().append(this.label).append(translate("konfig.screen.read_only"));
        }
    }
}
