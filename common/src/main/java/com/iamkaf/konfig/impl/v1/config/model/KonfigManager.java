package com.iamkaf.konfig.impl.v1.config.model;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.impl.v1.model.ConfigGraph;
import com.iamkaf.konfig.impl.v1.model.ConfigGraphAdapters;
import com.iamkaf.konfig.impl.v1.presentation.ConfigPresentationGraph;
//? if >=1.21.11 {
import com.iamkaf.konfig.impl.v1.sync.ConfigEditTarget;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
//?}

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
public final class KonfigManager {
    private static final KonfigManager INSTANCE = new KonfigManager();

    private final Map<String, ConfigHandleImpl> handles = new LinkedHashMap<>();
    private final Map<Path, String> pathOwners = new LinkedHashMap<>();
//? if >=1.21.11
    private final Map<ConfigHandleImpl, ConfigEditTarget> remoteTargets = new LinkedHashMap<>();

    private KonfigManager() {
    }

    public static KonfigManager get() {
        return INSTANCE;
    }

    public synchronized void register(ConfigHandleImpl handle) {
        String id = handle.id();
        if (this.handles.containsKey(id)) {
            throw new IllegalStateException("Config already registered: " + id);
        }

        Path path = handle.path().toAbsolutePath().normalize();
        String pathOwner = this.pathOwners.get(path);
        if (pathOwner != null) {
            throw new IllegalStateException(
                    "Config file already owned by " + pathOwner + ": " + path
            );
        }

        this.handles.put(id, handle);
        this.pathOwners.put(path, id);
    }

    public void registerAndLoad(ConfigHandleImpl handle) {
        register(handle);
        try {
            handle.load();
//? if >=1.21.11 {
            if (handle.scope() != com.iamkaf.konfig.api.v1.ConfigScope.CLIENT
                    && handle.syncMode() != com.iamkaf.konfig.api.v1.SyncMode.NONE) {
                ConfigEditTarget target = handle.remoteEditTarget();
                KonfigSync.authority().register(target);
                synchronized (this) {
                    this.remoteTargets.put(handle, target);
                }
            }
//?}
        } catch (RuntimeException exception) {
            unregister(handle);
            throw exception;
        }
    }

    private synchronized void unregister(ConfigHandleImpl handle) {
//? if >=1.21.11 {
        ConfigEditTarget remoteTarget = this.remoteTargets.remove(handle);
        if (remoteTarget != null) {
            KonfigSync.authority().unregister(remoteTarget);
        }
//?}
        if (this.handles.get(handle.id()) != handle) {
            return;
        }
        this.handles.remove(handle.id());
        this.pathOwners.remove(handle.path().toAbsolutePath().normalize());
    }

    public synchronized Collection<ConfigHandleImpl> all() {
        return Collections.unmodifiableList(new ArrayList<ConfigHandleImpl>(this.handles.values()));
    }

    public synchronized ConfigHandleImpl find(String configId) {
        return this.handles.get(configId);
    }

    public ConfigGraph graph() {
        return ConfigGraphAdapters.graph(all());
    }

    public ConfigPresentationGraph presentationGraph() {
        Collection<ConfigHandleImpl> snapshot = all();
        ConfigGraph graph = ConfigGraphAdapters.graph(snapshot);
        return ConfigGraphAdapters.presentationGraph(graph, snapshot);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Collection<ConfigScreenHandle> screenHandles() {
        return (Collection) all();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Collection<ConfigHandle> allPublicHandles() {
        return (Collection) all();
    }

    public void reloadAll() {
        all().forEach(ConfigHandleImpl::reload);
    }

    public void applySnapshot(String configId, String jsonPayload) {
        ConfigHandleImpl handle = find(configId);
        if (handle != null) {
            handle.applySyncSnapshot(jsonPayload);
        }
    }

    public void clearAllSynced() {
        all().forEach(ConfigHandleImpl::clearSyncedValues);
    }
}
