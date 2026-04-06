package com.iamkaf.konfig.impl.v1;

import com.iamkaf.konfig.api.v1.ConfigHandle;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KonfigManager {
    private static final KonfigManager INSTANCE = new KonfigManager();

    private final Map<String, ConfigHandleImpl> handles = new ConcurrentHashMap<>();

    private KonfigManager() {
    }

    public static KonfigManager get() {
        return INSTANCE;
    }

    public void register(ConfigHandleImpl handle) {
        String id = handle.id();
        ConfigHandleImpl old = this.handles.putIfAbsent(id, handle);
        if (old != null) {
            throw new IllegalStateException("Config already registered: " + id);
        }
    }

    public Collection<ConfigHandleImpl> all() {
        return Collections.unmodifiableCollection(this.handles.values());
    }

    public Collection<ConfigHandle> allPublicHandles() {
        return (Collection) all();
    }

    public void reloadAll() {
        this.handles.values().forEach(ConfigHandleImpl::reload);
    }

    public void applySnapshot(String configId, String jsonPayload) {
        ConfigHandleImpl handle = this.handles.get(configId);
        if (handle != null) {
            handle.applySyncSnapshot(jsonPayload);
        }
    }

    public void clearAllSynced() {
        this.handles.values().forEach(ConfigHandleImpl::clearSyncedValues);
    }
}
