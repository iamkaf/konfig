package com.iamkaf.konfig.impl.v1.client.row;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
//? if >=1.21.9 {
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
//?}
//? if <1.19.4 {
import com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
//?}

/** A list row drawn through KonfigRenderContext inside its content box (the row inset by 2px), on every line. */
@ApiStatus.Internal
public abstract class KonfigListRow<E extends KonfigListRow<E>> extends ContainerObjectSelectionList.Entry<E> {
    protected abstract void renderRow(KonfigRenderContext context, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick);

//? if >=26.1 {
    @Override
    public final void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderedContentY = this.getContentY();
        this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, hovered, partialTick);
    }
//?} elif >=1.21.9 {
    @Override
    public final void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderedContentY = this.getContentY();
        this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, hovered, partialTick);
    }
//?} elif >=1.20 {
    // Never called by KonfigSelectionList, which draws rows itself; required because the method is abstract.
    @Override
    public final void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, hovered, partialTick);
    }
//?} else {
    @Override
    public final void render(PoseStack graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, hovered, partialTick);
    }
//?}

    private KonfigSelectionList<E> owner;

//? if >=1.21.9 {
    // Content top at the last render, where renderRow placed the child widgets. The list may have scrolled since.
    private int renderedContentY = Integer.MIN_VALUE;

    final void attach(KonfigSelectionList<E> owner) {
        this.owner = owner;
    }

    /** Where {@code child} is now, or null before this row was first rendered and its children were placed. */
    final ScreenRectangle currentBounds(GuiEventListener child) {
        if (this.renderedContentY == Integer.MIN_VALUE) {
            return null;
        }
        ScreenRectangle rendered = child.getRectangle();
        return new ScreenRectangle(rendered.left(), rendered.top() - this.renderedContentY + this.getContentY(), rendered.width(), rendered.height());
    }

    // The list selects this row, and may scroll it, before the focused child is set.
    @Override
    public void setFocused(GuiEventListener listener) {
        super.setFocused(listener);
        if (this.owner != null && listener != null) {
            this.owner.revealFocusedChild(this, listener);
        }
    }
//?} else {
    private int slotHeight;

    final void attach(KonfigSelectionList<E> owner, int slotHeight) {
        this.owner = owner;
        this.slotHeight = slotHeight;
    }

    final int slotHeight() {
        return this.slotHeight;
    }

    // Same names and values as AbstractSelectionList.Entry from 1.21.9, so callers and TeaKit see one geometry on every line.
    public int getContentX() {
        return this.owner == null ? 0 : this.owner.getRowLeft();
    }

    public int getContentY() {
        return this.owner == null ? 0 : this.owner.getRowTop(this.owner.children().indexOf(this));
    }

    public int getContentWidth() {
        return this.owner == null ? 0 : this.owner.getRowWidth() - 4;
    }

    public int getContentHeight() {
        return this.slotHeight - 4;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.owner != null && this.owner.rowAt(mouseX, mouseY) == this;
    }
//?}

//? if <1.19.4 {
    // From 1.19.4 ContainerObjectSelectionList.Entry unfocuses the previous child itself; before that a card with
    // several EditBoxes would keep more than one focused.
    @Override
    public void setFocused(GuiEventListener listener) {
        GuiEventListener previous = this.getFocused();
        if (previous != listener && previous instanceof EditBox box) {
            KonfigUiAdapter.focus(box, false);
        }
        super.setFocused(listener);
    }
//?}
}
