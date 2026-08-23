//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.runtime;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ReloadCause;
import com.iamkaf.konfig.impl.v1.state.ConfigSession;

@ApiStatus.Internal
public interface ConfigRuntimeBinding {
    String id();

    ConfigScope scope();

    void load();

    void reload(ReloadCause cause);

    void unload();

    ConfigSession openSession(ConfigRuntimeContext context);
}
//?}
