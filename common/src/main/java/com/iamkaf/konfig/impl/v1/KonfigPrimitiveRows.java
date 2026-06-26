//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

import static com.iamkaf.konfig.impl.v1.KonfigRegistryAdapter.supportsRegistryIcon;
import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.*;
import static com.iamkaf.konfig.impl.v1.KonfigUiAdapter.button;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;

import java.util.List;

final class BooleanRow extends KonfigConfigRow {
    private final Button button;

    BooleanRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.button = button(0, 0, host.controlMinWidth(), host.controlHeight(), host.booleanText(entry.value), button -> {
            Object previousDraft = this.host.draft(entry.value);
            this.host.setDraft(entry.value, Boolean.valueOf(!this.host.readBoolean(entry.value)));
            this.commitOrRevert(previousDraft);
            this.syncFromDraft();
        });
    }

    @Override
    protected AbstractWidget control() {
        return this.button;
    }

    @Override
    protected void syncFromDraft() {
        this.button.setMessage(this.host.booleanText(this.entry.value));
    }
}

final class EnumRow extends KonfigConfigRow {
    private final Button button;

    EnumRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.button = button(0, 0, host.controlMinWidth(), host.controlHeight(), host.enumText(entry, host.currentEnum(entry.value)), button -> {
            Object previousDraft = this.host.draft(entry.value);
            this.host.setDraft(entry.value, this.host.cycleEnum(entry.value));
            this.commitOrRevert(previousDraft);
            this.syncFromDraft();
        });
    }

    @Override
    protected AbstractWidget control() {
        return this.button;
    }

    @Override
    protected void syncFromDraft() {
        this.button.setMessage(this.host.enumText(this.entry, this.host.currentEnum(this.entry.value)));
    }
}

final class ColorRow extends KonfigConfigRow {
    private static final int PREVIEW_SIZE = 16;
    private static final int PREVIEW_GAP = 6;

    private final Button button;

    ColorRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.button = button(0, 0, host.controlMinWidth(), host.controlHeight(), host.colorText(entry.value), ignored -> this.host.openColorEditor(entry));
    }

    @Override
    protected AbstractWidget control() {
        return this.button;
    }

    @Override
    protected void syncFromDraft() {
        this.button.setMessage(this.host.colorText(this.entry.value));
    }

//? if >=26.1 {
    @Override
    public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        KonfigRowLayout layout = this.rowLayout(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight());
        int previewX = layout.controlX - PREVIEW_GAP - PREVIEW_SIZE;
        int previewY = layout.y + (layout.height - PREVIEW_SIZE) / 2;
        this.renderColorRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), previewX, previewY, PREVIEW_SIZE);
    }
//?} elif >=1.21.9 {
    @Override
    public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        KonfigRowLayout layout = this.rowLayout(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight());
        int previewX = layout.controlX - PREVIEW_GAP - PREVIEW_SIZE;
        int previewY = layout.y + (layout.height - PREVIEW_SIZE) / 2;
        this.renderColorRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), previewX, previewY, PREVIEW_SIZE);
    }
//?} elif >=1.20 {
    @Override
    protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        KonfigRowLayout layout = this.rowLayout(x, y, width, height);
        int previewX = layout.controlX - PREVIEW_GAP - PREVIEW_SIZE;
        int previewY = layout.y + (layout.height - PREVIEW_SIZE) / 2;
        this.renderColorRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, previewX, previewY, PREVIEW_SIZE);
    }
//?} else {
    @Override
    protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        KonfigRowLayout layout = this.rowLayout(x, y, width, height);
        int previewX = layout.controlX - PREVIEW_GAP - PREVIEW_SIZE;
        int previewY = layout.y + (layout.height - PREVIEW_SIZE) / 2;
        this.renderColorRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, previewX, previewY, PREVIEW_SIZE);
    }
//?}
}

final class StringListRow extends KonfigConfigRow {
    private static final int PREVIEW_SIZE = 16;
    private static final int PREVIEW_GAP = 6;

    private final Button button;

    StringListRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.button = button(0, 0, host.controlMinWidth(), host.controlHeight(), host.stringListText(entry.value), ignored -> this.host.openStringListEditor(entry));
    }

    @Override
    protected AbstractWidget control() {
        return this.button;
    }

    @Override
    protected void syncFromDraft() {
        this.button.setMessage(this.host.stringListText(this.entry.value));
    }

//? if >=26.1 {
    @Override
    public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        super.extractContent(guiGraphics, mouseX, mouseY, hovered, partialTick);
        this.renderPreviewIcon(KonfigRenderContext.of(guiGraphics));
    }
//?} elif >=1.21.9 {
    @Override
    public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        super.renderContent(guiGraphics, mouseX, mouseY, hovered, partialTick);
        this.renderPreviewIcon(KonfigRenderContext.of(guiGraphics));
    }
//?} elif >=1.20 {
    @Override
    protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        super.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
        this.renderPreviewIcon(KonfigRenderContext.of(guiGraphics));
    }
//?} else {
    @Override
    protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        super.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
        this.renderPreviewIcon(KonfigRenderContext.of(guiGraphics));
    }
//?}

    private void renderPreviewIcon(KonfigRenderContext context) {
        if (!this.entry.value.hasBoundRegistry() || !supportsRegistryIcon(this.entry.value.boundRegistryKey())) {
            return;
        }

        List<String> values = this.host.currentStringList(this.entry.value);
        if (values.isEmpty()) {
            return;
        }

//? if >=1.19.3 {
        int previewX = this.button.getX() - PREVIEW_GAP - PREVIEW_SIZE;
        int previewY = this.button.getY() + (this.host.controlHeight() - PREVIEW_SIZE) / 2;
//?} else {
        int previewX = this.button.x - PREVIEW_GAP - PREVIEW_SIZE;
        int previewY = this.button.y + (this.host.controlHeight() - PREVIEW_SIZE) / 2;
//?}
        context.renderRegistryIcon(this.entry.value.boundRegistryKey(), values.get(0), previewX, previewY);
    }
}

final class TextInputRow extends KonfigConfigRow {
    private final EditBox input;
    private String validationMessage = "";

    TextInputRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.input = new EditBox(host.font(), 0, 0, host.controlMinWidth(), host.controlHeight(), entry.label);
        this.input.setMaxLength(256);
        this.input.setValue(host.currentStringValue(entry.value));
        this.input.setResponder(value -> {
            this.host.setDraft(entry.value, value);
            try {
                parseDraft(entry.value, value);
                this.validationMessage = "";
                this.host.persistEntry(entry);
            } catch (Exception exception) {
                this.validationMessage = exception.getMessage() == null ? "" : exception.getMessage();
            }
        });
    }

    @Override
    protected AbstractWidget control() {
        return this.input;
    }

    @Override
    protected int preferredHeight(int rowWidth) {
        return this.host.rowHeight() + 12;
    }

    @Override
    protected String validationMessage() {
        return this.validationMessage;
    }

    @Override
    protected void syncFromDraft() {
        this.input.setValue(this.host.currentStringValue(this.entry.value));
    }
}
//?}
