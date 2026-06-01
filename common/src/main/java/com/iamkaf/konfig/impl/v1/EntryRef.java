package com.iamkaf.konfig.impl.v1;

//? if >=1.17 {
import net.minecraft.network.chat.Component;

final class EntryRef {
    final ConfigHandleImpl handle;
    final ConfigValueImpl<?> value;
    final Component label;
    final Component contextLabel;
    final String tooltip;
    final String categoryPath;
    final boolean editable;

    EntryRef(ConfigHandleImpl handle, ConfigValueImpl<?> value, boolean editable) {
        this.handle = handle;
        this.value = value;
        if (value.isDecoration()) {
            this.label = KonfigScreenSupport.text(value.inlineLabel());
            this.contextLabel = KonfigScreenSupport.text("");
            this.tooltip = value.kind() == EntryKind.URL && !KonfigScreenSupport.isBlank(value.inlineTarget())
                    ? value.inlineTarget()
                    : handle.tooltip(value.path());
            this.categoryPath = categoryPath(value.path());
            this.editable = false;
        } else {
            this.label = KonfigScreenSupport.translatedLabel(handle, value);
            this.contextLabel = KonfigScreenSupport.contextLabel(handle, value);
            this.tooltip = handle.tooltip(value.path());
            this.categoryPath = categoryPath(value.path());
            this.editable = editable;
        }
    }

    Component displayLabel() {
        return this.label;
    }

    private static String categoryPath(String path) {
        int lastSeparator = path.lastIndexOf('.');
        if (lastSeparator < 0) {
            return "";
        }
        return path.substring(0, lastSeparator);
    }
}
//?}
