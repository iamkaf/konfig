//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.row;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
import com.iamkaf.konfig.impl.v1.client.screen.EntryRef;
import com.iamkaf.konfig.impl.v1.client.screen.KonfigRowHost;
import com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.vertex.PoseStack;
//?}

@ApiStatus.Internal
public final class KonfigEntryList extends ContainerObjectSelectionList<KonfigConfigRow> {
    private final KonfigRowFactory rowFactory;
    private final int rowWidth;

    public KonfigEntryList(Minecraft minecraft, KonfigRowHost host, int width, int screenHeight, int height, int y, int rowWidth) {
//? if >=1.20.3 {
        super(minecraft, width, height, y, KonfigScreenMetrics.ROW_HEIGHT);
//?} else {
        super(minecraft, width, screenHeight, y, y + height, KonfigScreenMetrics.ROW_HEIGHT);
//?}
        this.rowFactory = new KonfigRowFactory(host);
        this.rowWidth = rowWidth;
//? if <=1.16.3 {
        this.setRenderHeader(false, 0);
//?} elif <=1.20.4 {
        this.setRenderBackground(false);
//?}
    }

    public void addKonfigEntry(EntryRef entry) {
        KonfigConfigRow row = this.rowFactory.create(entry);
//? if >=26.1 {
        super.addEntry(row, row.preferredHeight(this.getRowWidth()));
//?} else {
        super.addEntry(row);
//?}
    }

    public void tickRows() {
        for (KonfigConfigRow row : this.children()) {
            row.tick();
        }
    }

    public RegistryTextInputRowHandle focusedRegistryRow() {
        for (KonfigConfigRow row : this.children()) {
            if (row instanceof RegistryTextInputRowHandle registryRow && registryRow.isFocused()) {
                return registryRow;
            }
        }
        return null;
    }

    public DropdownRowHandle focusedDropdownRow() {
        for (KonfigConfigRow row : this.children()) {
            if (row instanceof DropdownRowHandle dropdownRow && dropdownRow.isButtonFocused()) {
                return dropdownRow;
            }
        }
        return null;
    }

    @Override
    public int getRowWidth() {
        return this.rowWidth;
    }

//? if <=1.20.6 {
    @Override
    protected int getScrollbarPosition() {
//? if >=1.20.3 {
        return this.getRight() - 6;
//?} else {
        return this.x1 - 6;
//?}
    }
//?}

//? if >=26.1 {
    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderListBackground(KonfigRenderContext.of(guiGraphics), this.getX(), this.getY(), this.getRight(), this.getBottom());
        super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }
//?} elif >=1.20.3 {
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderListBackground(KonfigRenderContext.of(guiGraphics), this.getX(), this.getY(), this.getRight(), this.getBottom());
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }
//?} elif >=1.20 {
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderListBackground(KonfigRenderContext.of(guiGraphics), this.x0, this.y0, this.x1, this.y1);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
//?} else {
    @Override
    protected void renderBackground(PoseStack guiGraphics) {
        this.renderListBackground(KonfigRenderContext.of(guiGraphics), this.x0, this.y0, this.x1, this.y1);
    }
//?}

    private void renderListBackground(KonfigRenderContext context, int left, int top, int right, int bottom) {
        context.fill(left, top, right, bottom, 0x66000000);
    }
}
//?}
