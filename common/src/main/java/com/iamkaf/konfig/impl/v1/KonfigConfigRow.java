//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.text;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

import java.util.Collections;
import java.util.List;

abstract class KonfigConfigRow extends ContainerObjectSelectionList.Entry<KonfigConfigRow> {
    protected final KonfigRowHost host;
    protected final EntryRef entry;

    KonfigConfigRow(KonfigRowHost host, EntryRef entry) {
        this.host = host;
        this.entry = entry;
    }

    protected abstract AbstractWidget control();

    protected void tick() {
    }

    protected int preferredHeight(int rowWidth) {
        return this.host.rowHeight();
    }

    protected String validationMessage() {
        return "";
    }

    protected String rowTooltip() {
        return this.entry.tooltip;
    }

    protected final KonfigRowLayout rowLayout(int x, int y, int width, int height) {
        int controlWidth = Math.min(this.host.controlMaxWidth(), Math.max(this.host.controlMinWidth(), width / 2));
        return new KonfigRowLayout(x, y, width, height, controlWidth, x + width - controlWidth, y + (height - this.host.controlHeight()) / 2);
    }

    protected final void renderStandardRow(KonfigRenderContext context, KonfigRowLayout layout, int mouseX, int mouseY, boolean hovered, float partialTick, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom, int labelColor) {
        this.host.updateHoveredEntry(this.entry, hovered);
        if (hovered) {
            context.fill(layout.x, layout.y, layout.x + layout.width, layout.y + layout.height, 0x22000000);
        }

        context.showTooltip(this.host.screen(), this.host.font(), this.rowTooltip(), mouseX, mouseY, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom);
        layoutControl(this.control(), layout.controlX, layout.controlY, layout.controlWidth);
        context.drawText(this.host.font(), this.entry.contextLabel, layout.x + 4, layout.y + 1, 0xFFA0A0A0);
        context.drawText(this.host.font(), this.entry.displayLabel(), layout.x + 4, layout.y + 12, labelColor);
        context.renderWidget(this.control(), mouseX, mouseY, partialTick);
        if (!this.validationMessage().isEmpty()) {
            context.drawText(this.host.font(), text(this.validationMessage()), layout.controlX, layout.controlY + this.host.controlHeight() + 2, this.host.validationColor());
        }
    }

    protected final void renderColorRow(KonfigRenderContext context, KonfigRowLayout layout, int mouseX, int mouseY, boolean hovered, float partialTick, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom, int previewX, int previewY, int previewSize) {
        this.renderStandardRow(context, layout, mouseX, mouseY, hovered, partialTick, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom, 0xFFFFFFFF);
        context.drawColorSwatch(previewX, previewY, previewSize, this.host.currentColor(this.entry.value), this.entry.value.kind());
    }

    protected void renderRowContent(KonfigRenderContext context, KonfigRowLayout layout, int mouseX, int mouseY, boolean hovered, float partialTick, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom) {
        this.renderStandardRow(context, layout, mouseX, mouseY, hovered, partialTick, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom, this.entry.editable ? 0xFFFFFFFF : 0xFFA0A0A0);
    }

    private void renderRowContent(KonfigRenderContext context, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom) {
        KonfigRowLayout layout = this.rowLayout(x, y, width, height);
        this.renderRowContent(context, layout, mouseX, mouseY, hovered, partialTick, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom);
    }

//? if >=26.1 {
    @Override
    public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderRowContent(KonfigRenderContext.of(guiGraphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
    }
//?} elif >=1.21.9 {
    @Override
    public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderRowContent(KonfigRenderContext.of(guiGraphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
    }
//?} elif >=1.20 {
    @Override
    public void render(GuiGraphics guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderRowContent(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height);
    }
//?} else {
    @Override
    public void render(PoseStack guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderRowContent(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height);
    }
//?}

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.singletonList(this.control());
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return Collections.singletonList(this.control());
    }

    protected final void layoutControl(AbstractWidget control, int x, int y, int width) {
//? if >=1.19.3 {
        control.setX(x);
        control.setY(y);
//?} else {
        control.x = x;
        control.y = y;
//?}
        control.setWidth(width);
    }

    protected void revertDraft(Object previousValue) {
        this.host.setDraft(this.entry.value, previousValue);
    }

    protected void commitOrRevert(Object previousValue) {
        if (!this.host.persistEntry(this.entry)) {
            this.revertDraft(previousValue);
            this.syncFromDraft();
        }
    }

    protected void syncFromDraft() {
    }
}
//?}
