//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.row;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.render.KonfigRegistryAdapter.supportsRegistryIcon;
import static com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter.button;

import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
import com.iamkaf.konfig.impl.v1.client.screen.EntryRef;
import com.iamkaf.konfig.impl.v1.client.screen.KonfigRowHost;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;

import java.util.List;

@ApiStatus.Internal
final class BooleanRow extends KonfigConfigRow {
    private final Button button;

    BooleanRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.button = button(0, 0, host.controlMinWidth(), host.controlHeight(), this.field().booleanText(), button -> {
            Object previousDraft = this.field().draft();
            this.field().setBoolean(!this.field().booleanValue());
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
        this.button.setMessage(this.field().booleanText());
    }
}

@ApiStatus.Internal
final class EnumRow extends KonfigConfigRow {
    private final Button button;

    EnumRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.button = button(0, 0, host.controlMinWidth(), host.controlHeight(), this.field().enumText(), button -> {
            Object previousDraft = this.field().draft();
            this.field().setDraft(this.field().nextEnumValue());
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
        this.button.setMessage(this.field().enumText());
    }
}

@ApiStatus.Internal
final class ColorRow extends KonfigConfigRow {
    private static final int PREVIEW_SIZE = 16;
    private static final int PREVIEW_GAP = 6;

    private final Button button;

    ColorRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.button = button(0, 0, host.controlMinWidth(), host.controlHeight(), this.field().colorText(), ignored -> this.host.openColorEditor(entry));
    }

    @Override
    protected AbstractWidget control() {
        return this.button;
    }

    @Override
    protected void syncFromDraft() {
        this.button.setMessage(this.field().colorText());
    }

    @Override
    protected void renderRowContent(KonfigRenderContext context, KonfigRowLayout layout, int mouseX, int mouseY, boolean hovered, float partialTick, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom) {
        int previewX = layout.controlX - PREVIEW_GAP - PREVIEW_SIZE;
        int previewY = layout.y + (layout.height - PREVIEW_SIZE) / 2;
        this.renderColorRow(context, layout, mouseX, mouseY, hovered, partialTick, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom, previewX, previewY, PREVIEW_SIZE);
    }
}

@ApiStatus.Internal
final class StringListRow extends KonfigConfigRow {
    private static final int PREVIEW_SIZE = 16;
    private static final int PREVIEW_GAP = 6;

    private final Button button;

    StringListRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.button = button(0, 0, host.controlMinWidth(), host.controlHeight(), this.field().stringListText(), ignored -> this.host.openStringListEditor(entry));
    }

    @Override
    protected AbstractWidget control() {
        return this.button;
    }

    @Override
    protected void syncFromDraft() {
        this.button.setMessage(this.field().stringListText());
    }

    @Override
    protected void renderRowContent(KonfigRenderContext context, KonfigRowLayout layout, int mouseX, int mouseY, boolean hovered, float partialTick, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom) {
        super.renderRowContent(context, layout, mouseX, mouseY, hovered, partialTick, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom);
        this.renderPreviewIcon(context);
    }

    private void renderPreviewIcon(KonfigRenderContext context) {
        if (!this.entry.value.hasBoundRegistry() || !supportsRegistryIcon(this.entry.value.boundRegistryKey())) {
            return;
        }

        List<String> values = this.field().stringListValue();
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

@ApiStatus.Internal
final class TextInputRow extends KonfigConfigRow {
    private final EditBox input;
    private String validationMessage = "";

    TextInputRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.input = new EditBox(host.font(), 0, 0, host.controlMinWidth(), host.controlHeight(), entry.label);
        this.input.setMaxLength(256);
        this.input.setValue(this.field().stringValue());
        this.input.setResponder(value -> {
            this.field().setDraft(value);
            try {
                this.field().validateDraft(value);
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
    public int preferredHeight(int rowWidth) {
        return this.host.rowHeight() + 12;
    }

    @Override
    protected String validationMessage() {
        return this.validationMessage;
    }

    @Override
    protected void syncFromDraft() {
        this.input.setValue(this.field().stringValue());
    }
}
//?}
