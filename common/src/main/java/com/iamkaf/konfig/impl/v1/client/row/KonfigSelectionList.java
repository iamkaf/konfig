package com.iamkaf.konfig.impl.v1.client.row;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
//? if <1.21.9 {
import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
//? if >=1.21.4 {
import net.minecraft.client.gui.components.events.GuiEventListener;
import java.util.Optional;
//?}
import java.util.List;
//?}

/**
 * Selection list whose rows keep their own heights on every line. From 1.21.9 vanilla stores row heights; below it
 * AbstractSelectionList lays rows out at one fixed itemHeight, so this class owns layout, hit testing and row
 * rendering there, using the 1.21.9 geometry.
 */
@ApiStatus.Internal
public abstract class KonfigSelectionList<E extends KonfigListRow<E>> extends ContainerObjectSelectionList<E> {
    protected KonfigSelectionList(Minecraft minecraft, int width, int height, int y, int defaultRowHeight) {
//? if >=1.20.3 {
        super(minecraft, width, height, y, defaultRowHeight);
//?} else {
        super(minecraft, width, height, y, y + height, defaultRowHeight);
//?}
//? if <=1.20.4
        this.setRenderBackground(false);
//? if <=1.20.1
        this.setRenderTopAndBottom(false);
    }

    /** Adds a row that occupies {@code height} pixels, including 2px padding on each side. */
    public final void addRow(E row, int height) {
//? if >=1.21.9 {
        this.addEntry(row, height);
//?} else {
        row.attach(this, height);
        this.addEntry(row);
//?}
    }

    public final double scrollOffset() {
//? if >=1.21.4 {
        return this.scrollAmount();
//?} else {
        return this.getScrollAmount();
//?}
    }

    public final void setLeft(int x) {
//? if >=1.20.3 {
        this.setX(x);
//?} else {
        this.setLeftPos(x);
//?}
    }

    /** Scrolls a row into view. A row taller than the list is aligned to the list top. */
    public final void revealRow(E row) {
//? if >=1.21.9 {
        if (row.getHeight() > this.getHeight() - 4) {
            int topOffset = row.getY() - this.getY() - 2;
            this.setScrollAmount(this.scrollAmount() + topOffset);
        } else {
            this.scrollToEntry(row);
        }
//?} else {
        int rowY = this.getRowTop(this.children().indexOf(row)) - 2;
        if (row.slotHeight() > this.listBottom() - this.listTop() - 4) {
            this.setScrollAmount(this.scrollOffset() + rowY - this.listTop() - 2);
        } else {
            this.ensureVisible(row);
        }
//?}
    }

//? if >=26.1 {
    @Override
    protected int scrollBarX() {
        return this.getRight() - this.scrollbarWidth();
    }
//?} elif >=1.21.4 {
    @Override
    protected int scrollBarX() {
        return this.getRight() - 6;
    }
//?} elif >=1.20.3 {
    @Override
    protected int getScrollbarPosition() {
        return this.getRight() - 6;
    }
//?} else {
    @Override
    protected int getScrollbarPosition() {
        return this.x1 - 6;
    }
//?}

//? if <1.21.9 {
    // Layout: rows start 2px below the list top, content is inset 2px, and the scroll range is the sum of row heights
    // plus 4. This matches AbstractSelectionList from 1.21.9.
    @Override
    public int getRowTop(int index) {
        int y = this.listTop() + 4 - (int) this.scrollOffset();
        List<E> rows = this.children();
        for (int i = 0; i < index; i++) {
            y += rows.get(i).slotHeight();
        }
        return y;
    }

//? if >=1.21.4 {
    @Override
    protected int contentHeight() {
        return this.rowsHeight() + 4;
    }
//?} else {
    @Override
    protected int getMaxPosition() {
        return this.rowsHeight();
    }
//?}

    @Override
    protected void ensureVisible(E row) {
        int index = row == null ? -1 : this.children().indexOf(row);
        if (index < 0) {
            return;
        }
        int above = this.getRowTop(index) - 2 - this.listTop() - 2;
        if (above < 0) {
            this.setScrollAmount(this.scrollOffset() + above);
        }
        int below = this.listBottom() - (this.getRowTop(index) - 2) - row.slotHeight() - 2;
        if (below < 0) {
            this.setScrollAmount(this.scrollOffset() - below);
        }
    }

    // Hit test: getEntryAtPosition is final and assumes itemHeight, so every caller that affects behaviour goes through rowAt.
    final E rowAt(double mouseX, double mouseY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return null;
        }
        int x = (int) mouseX;
        int y = (int) mouseY;
        int left = this.getRowLeft() - 2;
        if (x < left || x >= left + this.getRowWidth()) {
            return null;
        }
        int top = this.listTop() + 2 - (int) this.scrollOffset();
        for (E row : this.children()) {
            int height = row.slotHeight();
            if (y >= top && y < top + height) {
                return row;
            }
            top += height;
        }
        return null;
    }

//? if >=1.21.4 {
    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        return Optional.ofNullable(this.rowAt(mouseX, mouseY));
    }
//?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
//? if >=1.20.2
        if (!this.isValidMouseClick(button)) { return false; }
        this.updateScrollingState(mouseX, mouseY, button);
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        E row = this.rowAt(mouseX, mouseY);
        if (row != null && row.mouseClicked(mouseX, mouseY, button)) {
            E previous = this.getFocused();
            if (previous != null && previous != row) {
                previous.setFocused(null);
            }
            this.setFocused(row);
            this.setDragging(true);
            return true;
        }
        int scrollbarX = this.getScrollbarPosition();
        return button == 0 && mouseX >= scrollbarX && mouseX < scrollbarX + 6;
    }
//?}

    // Rendering: vanilla's loop passes itemHeight - 4 as every row's height.
//? if >=1.20.5 {
    @Override
    protected void renderListItems(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderRows(KonfigRenderContext.of(graphics), mouseX, mouseY, partialTick);
    }
//?} elif >=1.20 {
    @Override
    protected void renderList(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderRows(KonfigRenderContext.of(graphics), mouseX, mouseY, partialTick);
    }
//?} elif >=1.19.1 {
    @Override
    protected void renderList(PoseStack graphics, int mouseX, int mouseY, float partialTick) {
        this.renderRows(KonfigRenderContext.of(graphics), mouseX, mouseY, partialTick);
    }
//?} else {
    @Override
    protected void renderList(PoseStack graphics, int rowLeft, int listTop, int mouseX, int mouseY, float partialTick) {
        this.renderRows(KonfigRenderContext.of(graphics), mouseX, mouseY, partialTick);
    }
//?}

    private void renderRows(KonfigRenderContext context, int mouseX, int mouseY, float partialTick) {
//? if <=1.19.3 {
        // Lists do not scissor their rows before 1.19.4.
        context.renderScissored(this.listLeft(), this.listTop(), this.listRight(), this.listBottom(), layer -> this.drawRows(layer, mouseX, mouseY, partialTick));
//?} else {
        this.drawRows(context, mouseX, mouseY, partialTick);
//?}
    }

    private void drawRows(KonfigRenderContext context, int mouseX, int mouseY, float partialTick) {
        E hovered = this.rowAt(mouseX, mouseY);
        int x = this.getRowLeft();
        int width = this.getRowWidth() - 4;
        int top = this.listTop() + 2 - (int) this.scrollOffset();
        for (E row : this.children()) {
            int height = row.slotHeight();
            if (top + height >= this.listTop() && top <= this.listBottom()) {
                row.renderRow(context, x, top + 2, width, height - 4, mouseX, mouseY, row == hovered, partialTick);
            }
            top += height;
        }
    }

    private int rowsHeight() {
        int total = 0;
        for (E row : this.children()) {
            total += row.slotHeight();
        }
        return total;
    }

    private int listLeft() {
//? if >=1.20.3 {
        return this.getX();
//?} else {
        return this.x0;
//?}
    }

    private int listTop() {
//? if >=1.20.3 {
        return this.getY();
//?} else {
        return this.y0;
//?}
    }

    private int listRight() {
//? if >=1.20.3 {
        return this.getRight();
//?} else {
        return this.x1;
//?}
    }

    private int listBottom() {
//? if >=1.20.3 {
        return this.getBottom();
//?} else {
        return this.y1;
//?}
    }
//?}
}
