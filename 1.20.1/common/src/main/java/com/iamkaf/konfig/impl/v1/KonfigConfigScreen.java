package com.iamkaf.konfig.impl.v1;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigDebugConfig;
import com.iamkaf.konfig.api.v1.ConfigValue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
    private final Map<ConfigValueImpl<?>, Object> drafts = new LinkedHashMap<ConfigValueImpl<?>, Object>();
    private final Map<ConfigValueImpl<?>, EditBox> visibleTextInputs = new LinkedHashMap<ConfigValueImpl<?>, EditBox>();

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
            this.drafts.put(entry.value, entry.value.get());
        }
    }

    @Override
    protected void init() {
        this.entriesPerPage = Math.max(1, (this.height - 72) / 24);
        this.rebuildEntryWidgets();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xC0101010);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, translate("konfig.screen.page", Integer.valueOf(this.page + 1), Integer.valueOf(totalPages()), Integer.valueOf(this.entries.size())), 12, 12, 0xFFC0C0C0);

        int row = 0;
        for (int index = this.visibleStart; index < this.visibleEnd; index++) {
            EntryRef entry = this.entries.get(index);
            int y = 34 + row * 24;
            int color = entry.editable ? 0xFFFFFFFF : 0xFFA0A0A0;
            guiGraphics.drawString(this.font, entry.displayLabel(), 12, y + 6, color);
            if (isHoveringRow(mouseX, mouseY, y) && !isBlank(entry.tooltip)) {
                guiGraphics.renderComponentTooltip(this.font, tooltipLines(entry.tooltip), mouseX, mouseY);
            }
            row++;
        }

        if (!this.statusMessage.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, text(this.statusMessage), this.width / 2, this.height - 38, this.statusColor);
        }

        if (this.entries.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, translate("konfig.screen.empty"), this.width / 2, this.height / 2 - 10, 0xFFC0C0C0);
        }
    }

    private void rebuildEntryWidgets() {
        this.clearWidgets();
        this.visibleTextInputs.clear();

        int pages = totalPages();
        if (this.page >= pages) {
            this.page = pages - 1;
        }
        if (this.page < 0) {
            this.page = 0;
        }

        Button prev = this.addRenderableWidget(Button.builder(translate("konfig.screen.previous"), button -> this.changePage(-1))
                .bounds(this.width - 56, 8, 20, 20)
                .build());
        prev.active = this.page > 0;

        Button next = this.addRenderableWidget(Button.builder(translate("konfig.screen.next"), button -> this.changePage(1))
                .bounds(this.width - 32, 8, 20, 20)
                .build());
        next.active = this.page + 1 < pages;

        int footerY = this.height - 26;
        this.addRenderableWidget(Button.builder(translate("konfig.screen.save"), button -> this.saveDrafts())
                .bounds(this.width / 2 - 122, footerY, 80, 20)
                .build());
        this.addRenderableWidget(Button.builder(translate("konfig.screen.reload"), button -> this.reloadFromDisk())
                .bounds(this.width / 2 - 40, footerY, 80, 20)
                .build());
        this.addRenderableWidget(Button.builder(translate("konfig.screen.done"), button -> this.onClose())
                .bounds(this.width / 2 + 42, footerY, 80, 20)
                .build());

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
            Button unsupported = this.addRenderableWidget(Button.builder(translate("konfig.screen.unsupported"), button -> {
            }).bounds(x, y, width, 20).build());
            unsupported.active = false;
            return;
        }

        if (entry.value.kind() == EntryKind.BOOLEAN) {
            this.addRenderableWidget(Button.builder(text(booleanString(entry.value)), button -> {
                boolean next = !readBoolean(entry.value);
                this.drafts.put(entry.value, Boolean.valueOf(next));
                button.setMessage(text(Boolean.toString(next)));
            }).bounds(x, y, width, 20).build());
            return;
        }

        if (entry.value.kind() == EntryKind.ENUM) {
            this.addRenderableWidget(Button.builder(text(enumString(entry.value)), button -> {
                Enum<?> next = cycleEnum(entry.value);
                this.drafts.put(entry.value, next);
                button.setMessage(text(next.name()));
            }).bounds(x, y, width, 20).build());
            return;
        }

        EditBox input = this.addRenderableWidget(new EditBox(this.font, x, y, width, 20, entry.label));
        input.setMaxLength(256);
        input.setValue(stringValue(this.drafts.get(entry.value)));
        input.setResponder(value -> this.drafts.put(entry.value, value));
        this.visibleTextInputs.put(entry.value, input);
    }

    private void saveDrafts() {
        try {
            Map<ConfigValueImpl<?>, Object> parsed = new LinkedHashMap<ConfigValueImpl<?>, Object>();

            for (EntryRef entry : this.entries) {
                if (!entry.editable) {
                    continue;
                }
                Object value = parseDraft(entry.value, this.drafts.get(entry.value));
                parsed.put(entry.value, value);
            }

            Set<ConfigHandleImpl> touchedHandles = new LinkedHashSet<ConfigHandleImpl>();
            for (EntryRef entry : this.entries) {
                if (!parsed.containsKey(entry.value)) {
                    continue;
                }
                setRawValue(entry.value, parsed.get(entry.value));
                touchedHandles.add(entry.handle);
            }

            for (ConfigHandleImpl handle : touchedHandles) {
                handle.save();
            }

            this.statusMessage = translate("konfig.screen.status.saved").getString();
            this.statusColor = 0xFF80FF80;
        } catch (Exception exception) {
            this.statusMessage = exception.getMessage() == null ? translate("konfig.screen.status.save_failed").getString() : exception.getMessage();
            this.statusColor = 0xFFFF8080;
        }
    }

    private void reloadFromDisk() {
        Set<ConfigHandleImpl> handles = new LinkedHashSet<ConfigHandleImpl>();
        for (EntryRef entry : this.entries) {
            handles.add(entry.handle);
        }

        for (ConfigHandleImpl handle : handles) {
            handle.load();
        }

        this.drafts.clear();
        for (EntryRef entry : this.entries) {
            this.drafts.put(entry.value, entry.value.get());
        }

        this.statusMessage = translate("konfig.screen.status.reloaded").getString();
        this.statusColor = 0xFF80FF80;
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
                case ENUM:
                    return parseEnum(value, draft);
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setRawValue(ConfigValueImpl<?> value, Object parsed) {
        ((ConfigValue) value).set(parsed);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean readBoolean(ConfigValueImpl<?> value) {
        Object current = this.drafts.get(value);
        if (current instanceof Boolean) {
            return ((Boolean) current).booleanValue();
        }
        return ((Boolean) value.get()).booleanValue();
    }

    private String booleanString(ConfigValueImpl<?> value) {
        return Boolean.toString(readBoolean(value));
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

    private String enumString(ConfigValueImpl<?> value) {
        return currentEnum(value).name();
    }

    private static Component translate(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private static Component text(String value) {
        return Component.nullToEmpty(value);
    }

    private static Component translatedLabel(ConfigHandleImpl handle, ConfigValueImpl<?> value) {
        String key = "konfig.config." + handle.modId() + "." + handle.name() + "." + value.path();
        Component translated = translate(key);
        return key.equals(translated.getString()) ? text(fallbackLabel(handle, value)) : translated;
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
                builder.append(character);
            }
        }
        return builder.toString().trim();
    }

    private static List<Component> tooltipLines(String tooltip) {
        List<Component> lines = new ArrayList<Component>();
        String normalized = tooltip.replace('\r', '\n');
        for (String line : normalized.split("\\n")) {
            lines.add(text(line));
        }
        return lines;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class EntryRef {
        private final ConfigHandleImpl handle;
        private final ConfigValueImpl<?> value;
        private final Component label;
        private final String tooltip;
        private final boolean editable;

        private EntryRef(ConfigHandleImpl handle, ConfigValueImpl<?> value, boolean editable) {
            this.handle = handle;
            this.value = value;
            this.label = translatedLabel(handle, value);
            this.tooltip = handle.tooltip(value.path());
            this.editable = editable;
        }

        private Component displayLabel() {
            if (this.editable) {
                return this.label;
            }
            return Component.empty().append(this.label).append(translate("konfig.screen.read_only"));
        }
    }
}
