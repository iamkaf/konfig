//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.runtime;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ReloadCause;
import com.iamkaf.konfig.impl.v1.state.ConfigChangeResult;
import com.iamkaf.konfig.impl.v1.state.ConfigSession;

@ApiStatus.Internal
public interface ConfigRuntimeService {
    void register(ConfigRuntimeBinding binding);

    void registrationComplete();

    ConfigLifecycleResult loadAll(ConfigRuntimeContext context);

    ConfigLifecycleResult reload(String configId, ReloadCause cause);

    ConfigLifecycleResult reloadAll(ReloadCause cause);

    ConfigSessionOpenResult openSession(String configId, ConfigRuntimeContext context);

    ConfigChangeResult applySession(ConfigSession session, long expectedRevision);

    ConfigChangeResult resetSession(ConfigSession session, long expectedRevision);

    void closeSession(ConfigSession session);

    ConfigLifecycleResult unloadAll();
}
//?}
