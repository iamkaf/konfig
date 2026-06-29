package com.iamkaf.konfig.impl.v1.client.legacy;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.config.model.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.config.model.ConfigValueImpl;
import com.iamkaf.konfig.impl.v1.config.model.KonfigManager;
import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@ApiStatus.Internal
public final class LegacyConfigEntries {
    private LegacyConfigEntries() {
    }

    public static List<LegacyConfigEntry> collect(String modIdFilter) {
        List<LegacyConfigEntry> result = new ArrayList<LegacyConfigEntry>();

        for (ConfigHandleImpl handle : KonfigManager.get().all()) {
            if (modIdFilter != null && !modIdFilter.equals(handle.modId())) {
                continue;
            }
            for (ConfigValueImpl<?> value : handle.screenValues()) {
                if (!isVisibleOnThisSide(value)) {
                    continue;
                }
                result.add(new LegacyConfigEntry(handle, value));
            }
        }

        Collections.sort(result, Comparator.comparing(entry -> entry.handle().id()));
        return result;
    }

    private static boolean isVisibleOnThisSide(ConfigValueImpl<?> value) {
        if (value.clientOnly() && !KonfigRuntime.isClient()) {
            return false;
        }
        if (value.serverOnly() && KonfigRuntime.isClient()) {
            return false;
        }
        return true;
    }
}
