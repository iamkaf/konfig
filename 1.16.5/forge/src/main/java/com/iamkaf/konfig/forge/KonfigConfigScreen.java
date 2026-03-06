package com.iamkaf.konfig.forge;

import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigDebugConfig;
import com.iamkaf.konfig.impl.v1.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.KonfigManager;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class KonfigConfigScreen extends Screen {
    private final Screen parent;
    private final List<EntryRef> entries;
    private final Map<ConfigValue<?>, Object> drafts = new LinkedHashMap<ConfigValue<?>, Object>();
    private final Map<ConfigValue<?>, Object> sessionStartValues = new LinkedHashMap<ConfigValue<?>, Object>();
    private final Map<ConfigValue<?>, TextFieldWidget> visibleTextInputs = new LinkedHashMap<ConfigValue<?>, TextFieldWidget>();

    private int page;
    private int entriesPerPage = 8;
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
            this.drafts.put(entry.value, value);
            if (entry.editable) {
                this.sessionStartValues.put(entry.value, value);
            }
        }
    }

    @Override
    protected void init() {
        this.entriesPerPage = Math.max(1, (this.height - 72) / 24);
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
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);

        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        this.font.draw(poseStack, translate("konfig.screen.page", Integer.valueOf(this.page + 1), Integer.valueOf(totalPages()), Integer.valueOf(this.entries.size())), 12.0F, 12.0F, 0xFFC0C0C0);

        int row = 0;
        for (int index = this.visibleStart; index < this.visibleEnd; index++) {
            EntryRef entry = this.entries.get(index);
            int y = 34 + row * 24;
            int color = entry.editable ? 0xFFFFFFFF : 0xFFA0A0A0;
            this.font.draw(poseStack, entry.displayLabel(), 12.0F, y + 6.0F, color);
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
        int controlX = this.width / 2;
        int controlWidth = this.width / 2 - 14;

        for (int index = this.visibleStart; index < this.visibleEnd; index++) {
            EntryRef entry = this.entries.get(index);
            int y = 34 + row * 24;
            this.addControl(entry, controlX, y, controlWidth);
            row++;
        }
    }

    private void addControl(EntryRef entry, int x, int y, int width) {
        if (!entry.editable) {
            Button unsupported = this.addButton(new Button(x, y, width, 20, translate("konfig.screen.unsupported"), button -> {
            }));
            unsupported.active = false;
            return;
        }

        Object defaultValue = entry.value.defaultValue();
        if (defaultValue instanceof Boolean) {
            this.addButton(new Button(x, y, width, 20, text(booleanString(entry.value)), button -> {
                Object previousDraft = this.drafts.get(entry.value);
                boolean next = !readBoolean(entry.value);
                this.drafts.put(entry.value, Boolean.valueOf(next));
                if (!this.persistEntry(entry)) {
                    this.drafts.put(entry.value, previousDraft);
                }
                button.setMessage(text(booleanString(entry.value)));
            }));
            return;
        }

        if (defaultValue instanceof Enum<?>) {
            this.addButton(new Button(x, y, width, 20, text(enumString(entry.value)), button -> {
                Object previousDraft = this.drafts.get(entry.value);
                Enum<?> next = cycleEnum(entry.value);
                this.drafts.put(entry.value, next);
                if (!this.persistEntry(entry)) {
                    this.drafts.put(entry.value, previousDraft);
                }
                button.setMessage(text(enumString(entry.value)));
            }));
            return;
        }

        TextFieldWidget input = this.addButton(new TextFieldWidget(this.font, x, y, width, 20, entry.label));
        input.setMaxLength(256);
        input.setValue(stringValue(this.drafts.get(entry.value)));
        input.setResponder(value -> {
            this.drafts.put(entry.value, value);
            this.persistEntry(entry);
        });
        this.visibleTextInputs.put(entry.value, input);
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
        Map<ConfigValue<?>, Object> previousValues = new LinkedHashMap<ConfigValue<?>, Object>();
        Set<ConfigHandleImpl> handles = new LinkedHashSet<ConfigHandleImpl>();
        try {
            for (EntryRef entry : this.entries) {
                if (!entry.editable) {
                    continue;
                }
                Object resetValue = this.sessionStartValues.get(entry.value);
                previousValues.put(entry.value, entry.value.get());
                this.drafts.put(entry.value, resetValue);
                setRawValue(entry.value, resetValue);
                handles.add(entry.handle);
            }

            for (ConfigHandleImpl handle : handles) {
                handle.save();
            }

            this.statusMessage = translate("konfig.screen.status.reset").getString();
            this.statusColor = 0xFF80FF80;
        } catch (Exception exception) {
            for (Map.Entry<ConfigValue<?>, Object> previousValue : previousValues.entrySet()) {
                setRawValue(previousValue.getKey(), previousValue.getValue());
                this.drafts.put(previousValue.getKey(), previousValue.getValue());
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
        return mouseX >= 12 && mouseX <= this.width - 12 && mouseY >= y && mouseY < y + 20;
    }

    private static List<EntryRef> collectEntries() {
        List<EntryRef> result = new ArrayList<EntryRef>();

        for (ConfigHandleImpl handle : KonfigManager.get().all()) {
            for (ConfigValue<?> value : handle.values()) {
                result.add(new EntryRef(handle, value, isEditable(value)));
            }
        }

        Collections.sort(result, Comparator.comparing(entry -> entry.handle.id() + ":" + entry.value.path()));
        return result;
    }

    private static boolean isEditable(ConfigValue<?> value) {
        Object defaultValue = value.defaultValue();
        return defaultValue instanceof Boolean
                || defaultValue instanceof Integer
                || defaultValue instanceof Long
                || defaultValue instanceof Double
                || defaultValue instanceof String
                || defaultValue instanceof Enum<?>;
    }

    private static Object parseDraft(ConfigValue<?> value, Object draft) {
        Object defaultValue = value.defaultValue();

        try {
            if (defaultValue instanceof Boolean) {
                return parseBoolean(draft, value.path());
            }
            if (defaultValue instanceof Integer) {
                return Integer.valueOf(Integer.parseInt(stringValue(draft).trim()));
            }
            if (defaultValue instanceof Long) {
                return Long.valueOf(Long.parseLong(stringValue(draft).trim()));
            }
            if (defaultValue instanceof Double) {
                return Double.valueOf(Double.parseDouble(stringValue(draft).trim()));
            }
            if (defaultValue instanceof String) {
                return stringValue(draft);
            }
            if (defaultValue instanceof Enum<?>) {
                return parseEnum(value, draft);
            }
            return value.get();
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

    private static Object parseEnum(ConfigValue<?> value, Object draft) {
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setRawValue(ConfigValue<?> value, Object parsed) {
        ((ConfigValue) value).set(parsed);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean sameValue(Object left, Object right) {
        return left == right || (left != null && left.equals(right));
    }

    private boolean readBoolean(ConfigValue<?> value) {
        Object current = this.drafts.get(value);
        if (current instanceof Boolean) {
            return ((Boolean) current).booleanValue();
        }
        return ((Boolean) value.get()).booleanValue();
    }

    private String booleanString(ConfigValue<?> value) {
        return Boolean.toString(readBoolean(value));
    }

    private Enum<?> cycleEnum(ConfigValue<?> value) {
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

    private Enum<?> currentEnum(ConfigValue<?> value) {
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

    private String enumString(ConfigValue<?> value) {
        return currentEnum(value).name();
    }

    private static ITextComponent translate(String key, Object... args) {
        return new TranslationTextComponent(key, args);
    }

    private static ITextComponent text(String value) {
        return new StringTextComponent(value == null ? "" : value);
    }

    private static ITextComponent translatedLabel(ConfigHandleImpl handle, ConfigValue<?> value) {
        String key = "konfig.config." + handle.modId() + "." + handle.name() + "." + value.path();
        ITextComponent translated = translate(key);
        return key.equals(translated.getString()) ? text(fallbackLabel(handle, value)) : translated;
    }

    private static String fallbackLabel(ConfigHandleImpl handle, ConfigValue<?> value) {
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
                builder.append(character);
            }
        }
        return builder.toString().trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class EntryRef {
        private final ConfigHandleImpl handle;
        private final ConfigValue<?> value;
        private final ITextComponent label;
        private final String tooltip;
        private final boolean editable;

        private EntryRef(ConfigHandleImpl handle, ConfigValue<?> value, boolean editable) {
            this.handle = handle;
            this.value = value;
            this.label = translatedLabel(handle, value);
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
