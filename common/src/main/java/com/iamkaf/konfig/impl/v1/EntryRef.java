package com.iamkaf.konfig.impl.v1;

//? if >=1.17 {
import net.minecraft.network.chat.Component;
//? if <=1.18.2 {
import net.minecraft.network.chat.TextComponent;
//?}

final class EntryRef {
    final ConfigHandleImpl handle;
    final ConfigValueImpl<?> value;
    final Component label;
    final Component contextLabel;
    final String tooltip;
    final boolean editable;

    EntryRef(ConfigHandleImpl handle, ConfigValueImpl<?> value, boolean editable) {
        this.handle = handle;
        this.value = value;
        this.label = KonfigScreenSupport.translatedLabel(handle, value);
        this.contextLabel = KonfigScreenSupport.contextLabel(handle, value);
        this.tooltip = handle.tooltip(value.path());
        this.editable = editable;
    }

    Component displayLabel() {
        if (this.editable) {
            return this.label;
        }
//? if >=1.19 {
        return Component.empty().copy().append(this.label).append(KonfigScreenSupport.translate("konfig.screen.read_only"));
//?} else {
        return TextComponent.EMPTY.copy().append(this.label).append(KonfigScreenSupport.translate("konfig.screen.read_only"));
//?}
    }
}
//?}
