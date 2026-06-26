package com.iamkaf.konfig.impl.v1.client.screen;

import org.jetbrains.annotations.ApiStatus;

//? if >=1.17 {
import com.iamkaf.konfig.impl.v1.config.model.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.config.model.ConfigValueImpl;
import com.iamkaf.konfig.impl.v1.config.model.EntryKind;
import net.minecraft.network.chat.Component;

@ApiStatus.Internal
public final class EntryRef {
    public final ConfigHandleImpl handle;
    public final ConfigValueImpl<?> value;
    public final Component label;
    public final Component contextLabel;
    public final String tooltip;
    public final String categoryPath;
    public final boolean editable;

    public EntryRef(ConfigHandleImpl handle, ConfigValueImpl<?> value, boolean editable) {
        this.handle = handle;
        this.value = value;
        if (value.isDecoration()) {
            this.label = KonfigScreenSupport.decorationLabel(value);
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

    public Component displayLabel() {
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
