package com.iamkaf.konfig.impl.v1.client.legacy;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.config.model.ConfigValueImpl;
import com.iamkaf.konfig.impl.v1.config.model.StringListValueHelper;

import java.util.Collections;
import java.util.List;

@ApiStatus.Internal
public final class LegacyStringListState {
    private LegacyStringListState() {
    }

    public static List<String> current(LegacyDraftSession session, ConfigValueImpl<?> value) {
        return session.currentStringList(value);
    }

    public static List<String> withAdded(List<String> values, String value) {
        List<String> current = mutable(values);
        current.add(value);
        return current;
    }

    public static List<String> withReplaced(List<String> values, int index, String value) {
        List<String> current = mutable(values);
        if (index >= 0 && index < current.size()) {
            current.set(index, value);
        }
        return current;
    }

    public static List<String> withRemoved(List<String> values, int index) {
        List<String> current = mutable(values);
        if (index >= 0 && index < current.size()) {
            current.remove(index);
        }
        return current;
    }

    public static List<String> withMoved(List<String> values, int index, int offset) {
        List<String> current = mutable(values);
        int target = index + offset;
        if (index >= 0 && index < current.size() && target >= 0 && target < current.size()) {
            Collections.swap(current, index, target);
        }
        return current;
    }

    private static List<String> mutable(List<String> values) {
        return StringListValueHelper.mutableCopy(values);
    }
}
