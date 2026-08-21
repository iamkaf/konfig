package com.iamkaf.konfig.impl.v1.client.legacy;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.config.model.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.config.model.ConfigValueImpl;
import com.iamkaf.konfig.impl.v1.config.model.EntryKind;

@ApiStatus.Internal
public final class LegacyConfigEntry {
    private final ConfigHandleImpl handle;
    private final ConfigValueImpl<?> value;
    private final boolean editable;
    private final String categoryPath;

    LegacyConfigEntry(ConfigHandleImpl handle, ConfigValueImpl<?> value) {
        this.handle = handle;
        this.value = value;
        this.editable = !value.isDecoration() && value.kind() != EntryKind.CUSTOM;
        this.categoryPath = categoryPath(value.path());
    }

    public ConfigHandleImpl handle() {
        return this.handle;
    }

    public ConfigValueImpl<?> value() {
        return this.value;
    }

    public boolean editable() {
        return this.editable;
    }

    public String categoryPath() {
        return this.categoryPath;
    }

    private static String categoryPath(String path) {
        int lastSeparator = path.lastIndexOf('.');
        if (lastSeparator < 0) {
            return "";
        }
        return path.substring(0, lastSeparator);
    }
}
