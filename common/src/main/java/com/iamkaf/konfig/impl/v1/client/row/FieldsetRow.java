//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.row;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter.button;

import com.iamkaf.konfig.api.v1.fieldset.FieldsetValue;
import com.iamkaf.konfig.impl.v1.client.fieldset.KonfigFieldsetRowSummary;
import com.iamkaf.konfig.impl.v1.client.fieldset.KonfigFieldsetScreens;
import com.iamkaf.konfig.impl.v1.client.screen.EntryRef;
import com.iamkaf.konfig.impl.v1.client.screen.KonfigRowHost;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

@ApiStatus.Internal
final class FieldsetRow extends KonfigConfigRow {
    private final Button button;

    FieldsetRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.button = button(
                0,
                0,
                host.controlMinWidth(),
                host.controlHeight(),
                this.summaryText(),
                ignored -> this.host.openFieldsetEditor(entry)
        );
    }

    @Override
    protected AbstractWidget control() {
        return this.button;
    }

    @Override
    protected void syncFromDraft() {
        this.button.setMessage(this.summaryText());
    }

    private Component summaryText() {
        Object draft = this.field().draft();
        if (!(draft instanceof FieldsetValue value)) {
            return Component.literal("Invalid fieldset");
        }
        KonfigFieldsetRowSummary summary = KonfigFieldsetScreens.summary(this.entry.label, value, this.field().editable());
        StringBuilder text = new StringBuilder(summary.countText().getString());
        if (summary.errorCount() > 0) {
            text.append("  [").append(summary.errorCount()).append(" invalid]");
        }
        if (summary.readOnly()) {
            text.append("  [read-only]");
        }
        return Component.literal(text.toString());
    }
}
//?}
