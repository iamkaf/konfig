//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.runtime;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ReloadCause;
import com.iamkaf.konfig.impl.v1.state.ConfigChangeResult;
import com.iamkaf.konfig.impl.v1.state.ConfigMutation;
import com.iamkaf.konfig.impl.v1.state.ConfigSession;
import com.iamkaf.konfig.impl.v1.state.ConfigValidation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ApiStatus.Internal
public final class DefaultConfigRuntimeService implements ConfigRuntimeService {
    private final Map<String, ConfigRuntimeBinding> configs = new LinkedHashMap<>();
    private final Set<ConfigSession> sessions = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean registrationComplete;

    @Override
    public synchronized void register(ConfigRuntimeBinding binding) {
        Objects.requireNonNull(binding, "binding");
        if (this.registrationComplete) {
            throw new IllegalStateException("Config registration is complete");
        }
        ConfigRuntimeBinding previous = this.configs.putIfAbsent(binding.id(), binding);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate config id '" + binding.id() + "'");
        }
    }

    @Override
    public synchronized void registrationComplete() {
        this.registrationComplete = true;
    }

    @Override
    public synchronized ConfigLifecycleResult loadAll(ConfigRuntimeContext context) {
        requireRegistrationComplete();
        Objects.requireNonNull(context, "context");
        var selected = this.configs.values().stream()
                .filter(config -> new ConfigScopeRules().loadsOn(config.scope(), context))
                .toList();
        return runLifecycle(selected, ConfigRuntimeBinding::load);
    }

    @Override
    public synchronized ConfigLifecycleResult reload(String configId, ReloadCause cause) {
        ConfigRuntimeBinding config = requireConfig(configId);
        return runLifecycle(List.of(config), binding -> binding.reload(cause));
    }

    @Override
    public synchronized ConfigLifecycleResult reloadAll(ReloadCause cause) {
        return runLifecycle(this.configs.values(), binding -> binding.reload(cause));
    }

    @Override
    public synchronized ConfigSessionOpenResult openSession(String configId, ConfigRuntimeContext context) {
        ConfigRuntimeBinding config = this.configs.get(configId);
        if (config == null) {
            return new ConfigSessionOpenResult.UnknownConfig(configId);
        }
        if (!new ConfigScopeRules().visible(config.scope(), true, context)) {
            return new ConfigSessionOpenResult.Unavailable(configId, "Config is not visible in this runtime context");
        }
        try {
            ConfigSession session = Objects.requireNonNull(config.openSession(context), "opened session");
            this.sessions.add(session);
            return new ConfigSessionOpenResult.Opened(session);
        } catch (RuntimeException exception) {
            return new ConfigSessionOpenResult.Failed(configId, "Failed to open config session", exception);
        }
    }

    @Override
    public synchronized ConfigChangeResult applySession(ConfigSession session, long expectedRevision) {
        requireOpenSession(session);
        return session.apply(expectedRevision);
    }

    @Override
    public synchronized ConfigChangeResult resetSession(ConfigSession session, long expectedRevision) {
        requireOpenSession(session);
        ConfigChangeResult reset = session.mutate(new ConfigMutation.ResetAll());
        if (!reset.accepted()) {
            return reset;
        }
        return session.apply(expectedRevision);
    }

    @Override
    public synchronized void closeSession(ConfigSession session) {
        Objects.requireNonNull(session, "session");
        if (this.sessions.remove(session)) {
            session.close();
        }
    }

    @Override
    public synchronized ConfigLifecycleResult unloadAll() {
        var openSessions = List.copyOf(this.sessions);
        openSessions.forEach(this::closeSession);
        return runLifecycle(this.configs.values(), ConfigRuntimeBinding::unload);
    }

    private void requireRegistrationComplete() {
        if (!this.registrationComplete) {
            throw new IllegalStateException("Config registration is not complete");
        }
    }

    private ConfigRuntimeBinding requireConfig(String configId) {
        ConfigRuntimeBinding config = this.configs.get(configId);
        if (config == null) {
            throw new IllegalArgumentException("Unknown config '" + configId + "'");
        }
        return config;
    }

    private void requireOpenSession(ConfigSession session) {
        if (!this.sessions.contains(Objects.requireNonNull(session, "session"))) {
            throw new IllegalArgumentException("Session is not owned by this runtime service");
        }
    }

    private static ConfigLifecycleResult runLifecycle(
            Iterable<ConfigRuntimeBinding> configs,
            LifecycleAction action
    ) {
        var completed = new ArrayList<String>();
        var failures = new LinkedHashMap<String, Throwable>();
        for (ConfigRuntimeBinding config : configs) {
            try {
                action.run(config);
                completed.add(config.id());
            } catch (RuntimeException exception) {
                failures.put(config.id(), exception);
            }
        }
        return failures.isEmpty()
                ? ConfigLifecycleResult.completed(completed)
                : ConfigLifecycleResult.failed(completed, failures);
    }

    @FunctionalInterface
    private interface LifecycleAction {
        void run(ConfigRuntimeBinding binding);
    }
}
//?}
