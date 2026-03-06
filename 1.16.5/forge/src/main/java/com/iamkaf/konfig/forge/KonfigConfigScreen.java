package com.iamkaf.konfig.forge;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigDebugConfig;
import com.iamkaf.konfig.impl.v1.ColorValueHelper;
import com.iamkaf.konfig.impl.v1.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.ConfigValueImpl;
import com.iamkaf.konfig.impl.v1.EntryKind;
import com.iamkaf.konfig.impl.v1.KonfigManager;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;
import com.iamkaf.konfig.impl.v1.StringListValueHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class KonfigConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 34;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_MIN_WIDTH = 132;
    private static final int CONTROL_MAX_WIDTH = 200;
    private static final int LIST_ROW_HEIGHT = 24;
    private static final int LIST_EDITOR_TOP = 56;

    private final Screen parent;
    private final List<EntryRef> entries;
    private final Map<ConfigValueImpl<?>, Object> drafts = new LinkedHashMap<ConfigValueImpl<?>, Object>();
    private final Map<ConfigValueImpl<?>, Object> sessionStartValues = new LinkedHashMap<ConfigValueImpl<?>, Object>();
    private final Map<ConfigValueImpl<?>, TextFieldWidget> visibleTextInputs = new LinkedHashMap<ConfigValueImpl<?>, TextFieldWidget>();

    private int page;
    private int entriesPerPage = 6;
    private int visibleStart;
    private int visibleEnd;

    private String statusMessage = "";
    private int statusColor = 0xFFFF8080;

    public KonfigConfigScreen(Screen parent) {
        super(translate("konfig.screen.title"));
        this.parent = parent;
        this.entries = collectEntries();
        if (KonfigDebugConfig.enabled()) {
            Constants.LOG.info("[Konfig/Debug] creating screen parent={} entries={}", parent == null ? "null" : parent.getClass().getName(), this.entries.size());
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
        this.entriesPerPage = Math.max(1, (this.height - 84) / ROW_HEIGHT);
        this.rebuildEntryWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        for (TextFieldWidget input : this.visibleTextInputs.values()) {
            input.tick();
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
    public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
        fill(poseStack, 0, 0, this.width, this.height, 0xC0101010);
        super.render(poseStack, mouseX, mouseY, partialTick);

        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        this.font.draw(poseStack, translate("konfig.screen.page", Integer.valueOf(this.page + 1), Integer.valueOf(totalPages()), Integer.valueOf(this.entries.size())), 12.0F, 12.0F, 0xFFC0C0C0);

        int row = 0;
        for (int index = this.visibleStart; index < this.visibleEnd; index++) {
            EntryRef entry = this.entries.get(index);
            int y = 32 + row * ROW_HEIGHT;
            int color = entry.editable ? 0xFFFFFFFF : 0xFFA0A0A0;
            this.font.draw(poseStack, entry.contextLabel, 12.0F, y + 1.0F, 0xFFA0A0A0);
            this.font.draw(poseStack, entry.displayLabel(), 12.0F, y + 12.0F, color);
            if (isHoveringRow(mouseX, mouseY, y) && !isBlank(entry.tooltip)) {
                this.renderTooltip(poseStack, this.font.split(text(entry.tooltip), Math.max(this.width / 2, 200)), mouseX, mouseY);
            }
            row++;
        }

        if (!this.statusMessage.isEmpty()) {
            drawCenteredString(poseStack, this.font, text(this.statusMessage), this.width / 2, this.height - 38, this.statusColor);
        }

        if (this.entries.isEmpty()) {
            drawCenteredString(poseStack, this.font, translate("konfig.screen.empty"), this.width / 2, this.height / 2 - 10, 0xFFC0C0C0);
        }
    }

    private void rebuildEntryWidgets() {
        this.buttons.clear();
        this.children.clear();
        this.visibleTextInputs.clear();

        int pages = totalPages();
        if (this.page >= pages) {
            this.page = pages - 1;
        }
        if (this.page < 0) {
            this.page = 0;
        }

        Button prev = this.addButton(new Button(this.width - 56, 8, 20, 20, translate("konfig.screen.previous"), button -> this.changePage(-1)));
        prev.active = this.page > 0;

        Button next = this.addButton(new Button(this.width - 32, 8, 20, 20, translate("konfig.screen.next"), button -> this.changePage(1)));
        next.active = this.page + 1 < pages;

        int footerY = this.height - 26;
        this.addButton(new Button(this.width / 2 - 82, footerY, 80, 20, translate("konfig.screen.reset"), button -> this.resetEntries()));
        this.addButton(new Button(this.width / 2 + 2, footerY, 80, 20, translate("konfig.screen.done"), button -> this.onClose()));

        this.visibleStart = this.page * this.entriesPerPage;
        this.visibleEnd = Math.min(this.entries.size(), this.visibleStart + this.entriesPerPage);

        int row = 0;
        for (int index = this.visibleStart; index < this.visibleEnd; index++) {
            EntryRef entry = this.entries.get(index);
            int y = 32 + row * ROW_HEIGHT;
            int controlWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, this.width / 2 - 24));
            int controlX = this.width - 12 - controlWidth;
            int controlY = y + 7;
            this.addControl(entry, controlX, controlY, controlWidth);
            row++;
        }
    }

    private void addControl(EntryRef entry, int x, int y, int width) {
        if (!entry.editable) {
            Button unsupported = this.addButton(new Button(x, y, width, CONTROL_HEIGHT, translate("konfig.screen.unsupported"), button -> {
            }));
            unsupported.active = false;
            return;
        }

        if (entry.value.kind() == EntryKind.BOOLEAN) {
            this.addButton(new Button(x, y, width, CONTROL_HEIGHT, booleanText(entry.value), button -> {
                Object previousDraft = snapshotValue(entry.value, entry.value.get());
                this.drafts.put(entry.value, Boolean.valueOf(!this.readBoolean(entry.value)));
                if (!this.persistEntry(entry)) {
                    this.drafts.put(entry.value, copyDraftValue(entry.value, previousDraft));
                }
                button.setMessage(booleanText(entry.value));
            }));
            return;
        }

        if (entry.value.kind() == EntryKind.ENUM) {
            this.addButton(new Button(x, y, width, CONTROL_HEIGHT, enumText(entry, this.currentEnum(entry.value)), button -> {
                Object previousDraft = snapshotValue(entry.value, entry.value.get());
                this.drafts.put(entry.value, this.cycleEnum(entry.value));
                if (!this.persistEntry(entry)) {
                    this.drafts.put(entry.value, copyDraftValue(entry.value, previousDraft));
                }
                button.setMessage(enumText(entry, this.currentEnum(entry.value)));
            }));
            return;
        }

        if (entry.value.kind() == EntryKind.COLOR_RGB || entry.value.kind() == EntryKind.COLOR_ARGB) {
            this.addButton(new Button(x, y, width, CONTROL_HEIGHT, colorText(entry.value), button -> this.minecraft.setScreen(new ColorEditorScreen(entry))));
            return;
        }

        if (entry.value.kind() == EntryKind.STRING_LIST) {
            this.addButton(new Button(x, y, width, CONTROL_HEIGHT, stringListText(entry.value), button -> this.minecraft.setScreen(new StringListEditorScreen(entry))));
            return;
        }

        if (entry.value.kind() == EntryKind.INTEGER && entry.value.hasNumericRange()) {
            this.addButton(new IntegerSliderWidget(entry, x, y, width));
            return;
        }

        if (entry.value.kind() == EntryKind.LONG && entry.value.hasNumericRange()) {
            this.addButton(new LongSliderWidget(entry, x, y, width));
            return;
        }

        if (entry.value.kind() == EntryKind.DOUBLE && entry.value.hasNumericRange()) {
            this.addButton(new DoubleSliderWidget(entry, x, y, width));
            return;
        }

        TextFieldWidget input = this.addButton(new TextFieldWidget(this.font, x, y, width, CONTROL_HEIGHT, entry.label));
        input.setMaxLength(256);
        input.setValue(stringValue(this.drafts.get(entry.value)));
        input.setResponder(value -> {
            this.drafts.put(entry.value, value);
            this.persistEntry(entry);
        });
        this.visibleTextInputs.put(entry.value, input);
    }

    private boolean persistEntry(EntryRef entry) {
        Object previousValue = snapshotValue(entry.value, entry.value.get());
        try {
            Object parsed = parseDraft(entry.value, this.drafts.get(entry.value));
            if (sameValue(previousValue, parsed)) {
                this.drafts.put(entry.value, copyDraftValue(entry.value, parsed));
                this.statusMessage = translate("konfig.screen.status.saved").getString();
                this.statusColor = 0xFF80FF80;
                return true;
            }

            setRawValue(entry.value, parsed);
            entry.handle.save();
            this.drafts.put(entry.value, copyDraftValue(entry.value, parsed));
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
                Object resetValue = this.sessionStartValues.get(entry.value);
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
        this.rebuildEntryWidgets();
    }

    private void changePage(int delta) {
        int next = this.page + delta;
        if (next < 0 || next >= totalPages()) {
            return;
        }
        this.page = next;
        this.rebuildEntryWidgets();
    }

    private int totalPages() {
        if (this.entries.isEmpty()) {
            return 1;
        }
        return (this.entries.size() + this.entriesPerPage - 1) / this.entriesPerPage;
    }

    private boolean isHoveringRow(int mouseX, int mouseY, int y) {
        return mouseX >= 12 && mouseX <= this.width - 12 && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
    }

    private static List<EntryRef> collectEntries() {
        List<EntryRef> result = new ArrayList<EntryRef>();

        for (ConfigHandleImpl handle : KonfigManager.get().all()) {
            for (com.iamkaf.konfig.api.v1.ConfigValue<?> value : handle.values()) {
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
        ((com.iamkaf.konfig.api.v1.ConfigValue) value).set(parsed);
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
        Enum<?> current = this.currentEnum(value);
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

    private List<String> currentStringList(ConfigValueImpl<?> value) {
        return stringListValue(this.drafts.get(value), value.path());
    }

    private int currentColor(ConfigValueImpl<?> value) {
        Object current = this.drafts.get(value);
        if (current instanceof Number) {
            return ((Number) current).intValue();
        }
        return ((Number) value.get()).intValue();
    }

    private static ITextComponent booleanText(ConfigValueImpl<?> value, boolean enabled) {
        return translate(enabled ? "options.on" : "options.off");
    }

    private ITextComponent booleanText(ConfigValueImpl<?> value) {
        return booleanText(value, this.readBoolean(value));
    }

    private static ITextComponent enumText(EntryRef entry, Enum<?> value) {
        String enumKey = "konfig.config." + entry.handle.modId() + "." + entry.handle.name() + "." + entry.value.path() + "." + value.name().toLowerCase(Locale.ROOT);
        ITextComponent translated = translate(enumKey);
        if (!enumKey.equals(translated.getString())) {
            return translated;
        }
        return text(prettySegment(value.name()));
    }

    private ITextComponent colorText(ConfigValueImpl<?> value) {
        int color = this.currentColor(value);
        if (value.kind() == EntryKind.COLOR_ARGB) {
            return text(ColorValueHelper.formatArgb(color));
        }
        return text(ColorValueHelper.formatRgb(color));
    }

    private ITextComponent stringListText(ConfigValueImpl<?> value) {
        List<String> values = this.currentStringList(value);
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

    private static double progressFor(double current, double min, double max) {
        double span = max - min;
        if (span <= 0.0D) {
            return 0.0D;
        }
        return clampProgress((current - min) / span);
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

    private static double clampProgress(double progress) {
        if (progress < 0.0D) {
            return 0.0D;
        }
        if (progress > 1.0D) {
            return 1.0D;
        }
        return progress;
    }

    private static String formatDouble(double value) {
        String formatted = String.format(Locale.ROOT, "%.3f", Double.valueOf(value));
        while (formatted.contains(".") && (formatted.endsWith("0") || formatted.endsWith("."))) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    private static void drawColorSwatch(MatrixStack poseStack, int x, int y, int size, int color, EntryKind kind) {
        fill(poseStack, x - 1, y - 1, x + size + 1, y + size + 1, 0xFF202020);
        if (kind == EntryKind.COLOR_ARGB && ColorValueHelper.alpha(color) < 255) {
            int cell = Math.max(2, size / 4);
            for (int row = 0; row < size; row += cell) {
                for (int column = 0; column < size; column += cell) {
                    boolean dark = ((row / cell) + (column / cell)) % 2 == 0;
                    fill(
                            poseStack,
                            x + column,
                            y + row,
                            x + Math.min(size, column + cell),
                            y + Math.min(size, row + cell),
                            dark ? 0xFF707070 : 0xFFC0C0C0
                    );
                }
            }
        } else {
            fill(poseStack, x, y, x + size, y + size, 0xFFFFFFFF);
        }
        fill(poseStack, x, y, x + size, y + size, ColorValueHelper.toRenderColor(kind, color));
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

        protected final void clearEditorWidgets() {
            this.buttons.clear();
            this.children.clear();
        }

        protected final void returnToParent() {
            KonfigConfigScreen.this.rebuildEntryWidgets();
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

        protected final void renderEditorChrome(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
            fill(poseStack, 0, 0, this.width, this.height, 0xC0101010);
            super.render(poseStack, mouseX, mouseY, partialTick);
            drawCenteredString(poseStack, this.font, this.title, this.width / 2, EDITOR_TITLE_Y, 0xFFFFFFFF);
            this.font.draw(poseStack, this.entry.contextLabel, 12.0F, EDITOR_CONTEXT_Y, 0xFFA0A0A0);
            if (!isBlank(this.entry.tooltip) && mouseX >= 12 && mouseX <= this.width - 12 && mouseY >= 8 && mouseY <= EDITOR_CONTENT_TOP - 4) {
                this.renderTooltip(poseStack, this.font.split(text(this.entry.tooltip), Math.max(this.width / 2, 200)), mouseX, mouseY);
            }
            if (!this.statusMessage.isEmpty()) {
                drawCenteredString(poseStack, this.font, text(this.statusMessage), this.width / 2, this.height - 38, this.statusColor);
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
        private static final int PREVIEW_SIZE = 28;
        private static final int PREVIEW_Y = EDITOR_CONTENT_TOP;
        private static final int HEX_WIDTH = 108;
        private static final int HEX_Y = PREVIEW_Y + PREVIEW_SIZE + 10;
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
            this.clearEditorWidgets();

            this.hexInput = this.addButton(new TextFieldWidget(this.font, this.width / 2 - HEX_WIDTH / 2, HEX_Y, HEX_WIDTH, CONTROL_HEIGHT, this.entry.label));
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
        public void tick() {
            super.tick();
            if (this.hexInput != null) {
                this.hexInput.tick();
            }
        }

        @Override
        public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
            this.renderEditorChrome(poseStack, mouseX, mouseY, partialTick);
            int previewX = this.width / 2 - PREVIEW_SIZE / 2;
            drawColorSwatch(poseStack, previewX, PREVIEW_Y, PREVIEW_SIZE, KonfigConfigScreen.this.currentColor(this.entry.value), this.entry.value.kind());
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
                this.persistEditedValue(previousValue);
                this.syncWidgetsFromDraft();
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
                super(x, y, SLIDER_WIDTH, ColorEditorScreen.this.currentChannel(channel) / 255.0D);
                this.channel = channel;
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
                int before = ColorEditorScreen.this.currentChannel(this.channel);
                boolean arrow = keyCode == 263 || keyCode == 262;
                super.keyPressed(keyCode, scanCode, modifiers);
                int after = ColorEditorScreen.this.currentChannel(this.channel);
                if (arrow && before != after) {
                    Object previousValue = snapshotValue(ColorEditorScreen.this.entry.value, ColorEditorScreen.this.entry.value.get());
                    KonfigConfigScreen.this.drafts.put(ColorEditorScreen.this.entry.value, Integer.valueOf(ColorEditorScreen.this.withChannel(this.channel, before)));
                    KonfigConfigScreen.this.drafts.put(ColorEditorScreen.this.entry.value, Integer.valueOf(ColorEditorScreen.this.withChannel(this.channel, after)));
                    if (ColorEditorScreen.this.persistEditedValue(Integer.valueOf(ColorEditorScreen.this.withChannel(this.channel, before)))) {
                        ColorEditorScreen.this.syncWidgetsFromDraft();
                    }
                    return true;
                }
                return arrow;
            }
        }
    }

    private final class StringListEditorScreen extends EntryEditorScreen {
        private final Map<Integer, TextFieldWidget> visibleInputs = new LinkedHashMap<Integer, TextFieldWidget>();
        private int page;
        private int itemsPerPage = 5;
        private int visibleStart;
        private int visibleEnd;

        private StringListEditorScreen(EntryRef entry) {
            super(entry);
        }

        @Override
        protected void init() {
            this.itemsPerPage = Math.max(1, (this.height - 96) / LIST_ROW_HEIGHT);
            this.rebuildEditorWidgets();
        }

        @Override
        public void tick() {
            super.tick();
            for (TextFieldWidget input : this.visibleInputs.values()) {
                input.tick();
            }
        }

        @Override
        public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
            this.renderEditorChrome(poseStack, mouseX, mouseY, partialTick);
            String count = translate("konfig.screen.list.count", Integer.valueOf(KonfigConfigScreen.this.currentStringList(this.entry.value).size())).getString();
            this.font.draw(poseStack, text(count), this.width - 12 - this.font.width(count), EDITOR_CONTEXT_Y, 0xFFC0C0C0);
            this.font.draw(poseStack, translate("konfig.screen.page", Integer.valueOf(this.page + 1), Integer.valueOf(this.totalPages()), Integer.valueOf(KonfigConfigScreen.this.currentStringList(this.entry.value).size())), 12.0F, 36.0F, 0xFFC0C0C0);

            if (KonfigConfigScreen.this.currentStringList(this.entry.value).isEmpty()) {
                drawCenteredString(poseStack, this.font, translate("konfig.screen.list.empty"), this.width / 2, this.height / 2 - 12, 0xFFC0C0C0);
            }

            for (int index = this.visibleStart; index < this.visibleEnd; index++) {
                int row = index - this.visibleStart;
                int y = LIST_EDITOR_TOP + row * LIST_ROW_HEIGHT;
                this.font.draw(poseStack, text(Integer.toString(index + 1)), 18.0F, y + 6.0F, 0xFFA0A0A0);
            }
        }

        private void rebuildEditorWidgets() {
            this.clearEditorWidgets();
            this.visibleInputs.clear();

            int pages = this.totalPages();
            if (this.page >= pages) {
                this.page = pages - 1;
            }
            if (this.page < 0) {
                this.page = 0;
            }

            Button prev = this.addButton(new Button(this.width - 56, 8, 20, 20, translate("konfig.screen.previous"), ignored -> this.changePage(-1)));
            prev.active = this.page > 0;

            Button next = this.addButton(new Button(this.width - 32, 8, 20, 20, translate("konfig.screen.next"), ignored -> this.changePage(1)));
            next.active = this.page + 1 < pages;

            this.visibleStart = this.page * this.itemsPerPage;
            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            this.visibleEnd = Math.min(values.size(), this.visibleStart + this.itemsPerPage);

            int inputX = 40;
            int inputWidth = Math.max(120, this.width - 124);
            for (int index = this.visibleStart; index < this.visibleEnd; index++) {
                int row = index - this.visibleStart;
                int y = LIST_EDITOR_TOP + row * LIST_ROW_HEIGHT;
                this.addListRow(index, inputX, y, inputWidth);
            }

            int footerY = this.height - 26;
            this.addButton(new Button(this.width / 2 - 122, footerY, 80, 20, translate("konfig.screen.list.add"), ignored -> this.addValue()));
            this.addButton(new Button(this.width / 2 - 40, footerY, 80, 20, translate("konfig.screen.reset"), ignored -> {
                if (this.resetToSessionStart()) {
                    this.page = 0;
                    this.rebuildEditorWidgets();
                }
            }));
            this.addButton(new Button(this.width / 2 + 42, footerY, 80, 20, translate("konfig.screen.done"), ignored -> this.onClose()));
        }

        private void addListRow(int index, int inputX, int y, int inputWidth) {
            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            TextFieldWidget input = this.addButton(new TextFieldWidget(this.font, inputX, y, inputWidth, CONTROL_HEIGHT, this.entry.label));
            input.setMaxLength(256);
            input.setValue(values.get(index));
            input.setResponder(value -> this.onValueChanged(index, input, value));
            this.visibleInputs.put(Integer.valueOf(index), input);

            int buttonX = inputX + inputWidth + 4;
            Button up = this.addButton(new Button(buttonX, y, 20, 20, text("^"), ignored -> this.move(index, -1)));
            up.active = index > 0;
            Button down = this.addButton(new Button(buttonX + 22, y, 20, 20, text("v"), ignored -> this.move(index, 1)));
            down.active = index + 1 < values.size();
            this.addButton(new Button(buttonX + 44, y, 20, 20, text("-"), ignored -> this.remove(index)));
        }

        private void onValueChanged(int index, TextFieldWidget input, String value) {
            Object previousValue = snapshotValue(this.entry.value, this.entry.value.get());
            List<String> values = StringListValueHelper.mutableCopy(KonfigConfigScreen.this.currentStringList(this.entry.value));
            values.set(index, value);
            KonfigConfigScreen.this.drafts.put(this.entry.value, values);
            if (!this.persistEditedValue(previousValue)) {
                input.setValue(KonfigConfigScreen.this.currentStringList(this.entry.value).get(index));
            }
        }

        private void addValue() {
            Object previousValue = snapshotValue(this.entry.value, this.entry.value.get());
            List<String> values = StringListValueHelper.mutableCopy(KonfigConfigScreen.this.currentStringList(this.entry.value));
            values.add(translate("konfig.screen.list.new_item").getString());
            KonfigConfigScreen.this.drafts.put(this.entry.value, values);
            if (this.persistEditedValue(previousValue)) {
                this.page = this.totalPages() - 1;
                this.rebuildEditorWidgets();
            }
        }

        private void move(int index, int delta) {
            int target = index + delta;
            List<String> currentValues = KonfigConfigScreen.this.currentStringList(this.entry.value);
            if (target < 0 || target >= currentValues.size()) {
                return;
            }
            Object previousValue = snapshotValue(this.entry.value, this.entry.value.get());
            List<String> values = StringListValueHelper.mutableCopy(currentValues);
            String moved = values.remove(index);
            values.add(target, moved);
            KonfigConfigScreen.this.drafts.put(this.entry.value, values);
            if (this.persistEditedValue(previousValue)) {
                this.rebuildEditorWidgets();
            }
        }

        private void remove(int index) {
            Object previousValue = snapshotValue(this.entry.value, this.entry.value.get());
            List<String> values = StringListValueHelper.mutableCopy(KonfigConfigScreen.this.currentStringList(this.entry.value));
            values.remove(index);
            KonfigConfigScreen.this.drafts.put(this.entry.value, values);
            if (this.persistEditedValue(previousValue)) {
                this.rebuildEditorWidgets();
            }
        }

        private void changePage(int delta) {
            int next = this.page + delta;
            if (next < 0 || next >= this.totalPages()) {
                return;
            }
            this.page = next;
            this.rebuildEditorWidgets();
        }

        private int totalPages() {
            int size = KonfigConfigScreen.this.currentStringList(this.entry.value).size();
            if (size == 0) {
                return 1;
            }
            return (size + this.itemsPerPage - 1) / this.itemsPerPage;
        }
    }

    private abstract class BaseSliderWidget extends AbstractSlider {
        private BaseSliderWidget(int x, int y, int width, double initialProgress) {
            super(x, y, width, CONTROL_HEIGHT, text(""), clampProgress(initialProgress));
        }

        protected final void syncToProgress(double progress) {
            this.value = clampProgress(progress);
            this.updateMessage();
        }
    }

    private final class IntegerSliderWidget extends BaseSliderWidget {
        private final EntryRef entry;
        private final int min;
        private final int max;

        private IntegerSliderWidget(EntryRef entry, int x, int y, int width) {
            super(x, y, width, progressFor(currentInteger(entry.value), entry.value.rangeMin().intValue(), entry.value.rangeMax().intValue()));
            this.entry = entry;
            this.min = entry.value.rangeMin().intValue();
            this.max = entry.value.rangeMax().intValue();
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(text(Integer.toString(currentInteger(this.entry.value))));
        }

        @Override
        protected void applyValue() {
            KonfigConfigScreen.this.drafts.put(this.entry.value, Integer.valueOf(intFromProgress(this.value, this.min, this.max)));
            this.updateMessage();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            Object previousValue = snapshotValue(this.entry.value, this.entry.value.get());
            super.onRelease(mouseX, mouseY);
            if (!KonfigConfigScreen.this.persistEntry(this.entry)) {
                KonfigConfigScreen.this.drafts.put(this.entry.value, copyDraftValue(this.entry.value, previousValue));
                this.syncToProgress(progressFor(currentInteger(this.entry.value), this.min, this.max));
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            int before = currentInteger(this.entry.value);
            boolean arrow = keyCode == 263 || keyCode == 262;
            super.keyPressed(keyCode, scanCode, modifiers);
            int after = currentInteger(this.entry.value);
            if (arrow && before != after) {
                if (!KonfigConfigScreen.this.persistEntry(this.entry)) {
                    KonfigConfigScreen.this.drafts.put(this.entry.value, Integer.valueOf(before));
                    this.syncToProgress(progressFor(before, this.min, this.max));
                }
                return true;
            }
            return arrow;
        }
    }

    private final class LongSliderWidget extends BaseSliderWidget {
        private final EntryRef entry;
        private final long min;
        private final long max;

        private LongSliderWidget(EntryRef entry, int x, int y, int width) {
            super(x, y, width, progressFor(currentLong(entry.value), entry.value.rangeMin().longValue(), entry.value.rangeMax().longValue()));
            this.entry = entry;
            this.min = entry.value.rangeMin().longValue();
            this.max = entry.value.rangeMax().longValue();
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(text(Long.toString(currentLong(this.entry.value))));
        }

        @Override
        protected void applyValue() {
            KonfigConfigScreen.this.drafts.put(this.entry.value, Long.valueOf(longFromProgress(this.value, this.min, this.max)));
            this.updateMessage();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            Object previousValue = snapshotValue(this.entry.value, this.entry.value.get());
            super.onRelease(mouseX, mouseY);
            if (!KonfigConfigScreen.this.persistEntry(this.entry)) {
                KonfigConfigScreen.this.drafts.put(this.entry.value, copyDraftValue(this.entry.value, previousValue));
                this.syncToProgress(progressFor(currentLong(this.entry.value), this.min, this.max));
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            long before = currentLong(this.entry.value);
            boolean arrow = keyCode == 263 || keyCode == 262;
            super.keyPressed(keyCode, scanCode, modifiers);
            long after = currentLong(this.entry.value);
            if (arrow && before != after) {
                if (!KonfigConfigScreen.this.persistEntry(this.entry)) {
                    KonfigConfigScreen.this.drafts.put(this.entry.value, Long.valueOf(before));
                    this.syncToProgress(progressFor(before, this.min, this.max));
                }
                return true;
            }
            return arrow;
        }
    }

    private final class DoubleSliderWidget extends BaseSliderWidget {
        private final EntryRef entry;
        private final double min;
        private final double max;

        private DoubleSliderWidget(EntryRef entry, int x, int y, int width) {
            super(x, y, width, progressFor(currentDouble(entry.value), entry.value.rangeMin().doubleValue(), entry.value.rangeMax().doubleValue()));
            this.entry = entry;
            this.min = entry.value.rangeMin().doubleValue();
            this.max = entry.value.rangeMax().doubleValue();
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(text(formatDouble(currentDouble(this.entry.value))));
        }

        @Override
        protected void applyValue() {
            KonfigConfigScreen.this.drafts.put(this.entry.value, Double.valueOf(doubleFromProgress(this.value, this.min, this.max)));
            this.updateMessage();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            Object previousValue = snapshotValue(this.entry.value, this.entry.value.get());
            super.onRelease(mouseX, mouseY);
            if (!KonfigConfigScreen.this.persistEntry(this.entry)) {
                KonfigConfigScreen.this.drafts.put(this.entry.value, copyDraftValue(this.entry.value, previousValue));
                this.syncToProgress(progressFor(currentDouble(this.entry.value), this.min, this.max));
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            double before = currentDouble(this.entry.value);
            boolean arrow = keyCode == 263 || keyCode == 262;
            super.keyPressed(keyCode, scanCode, modifiers);
            double after = currentDouble(this.entry.value);
            if (arrow && !sameValue(Double.valueOf(before), Double.valueOf(after))) {
                if (!KonfigConfigScreen.this.persistEntry(this.entry)) {
                    KonfigConfigScreen.this.drafts.put(this.entry.value, Double.valueOf(before));
                    this.syncToProgress(progressFor(before, this.min, this.max));
                }
                return true;
            }
            return arrow;
        }
    }

    private int currentInteger(ConfigValueImpl<?> value) {
        Object draft = this.drafts.get(value);
        if (draft instanceof Number) {
            return ((Number) draft).intValue();
        }
        return ((Number) value.get()).intValue();
    }

    private long currentLong(ConfigValueImpl<?> value) {
        Object draft = this.drafts.get(value);
        if (draft instanceof Number) {
            return ((Number) draft).longValue();
        }
        return ((Number) value.get()).longValue();
    }

    private double currentDouble(ConfigValueImpl<?> value) {
        Object draft = this.drafts.get(value);
        if (draft instanceof Number) {
            return ((Number) draft).doubleValue();
        }
        return ((Number) value.get()).doubleValue();
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
            return new StringTextComponent("").append(this.label).append(translate("konfig.screen.read_only"));
        }
    }
}
