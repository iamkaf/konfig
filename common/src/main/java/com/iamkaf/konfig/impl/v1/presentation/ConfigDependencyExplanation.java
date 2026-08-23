package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigFieldIdentity;
import com.iamkaf.konfig.impl.v1.model.ConfigText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public final class ConfigDependencyExplanation {
    public enum Effect {
        HIDDEN,
        DISABLED,
        RESTART_REQUIRED
    }

    private final Effect effect;
    private final ConfigText message;
    private final List<ConfigFieldIdentity> relatedFields;

    public ConfigDependencyExplanation(Effect effect, ConfigText message, List<ConfigFieldIdentity> relatedFields) {
        this.effect = Objects.requireNonNull(effect, "effect");
        this.message = message == null ? ConfigText.empty() : message;
        this.relatedFields = relatedFields == null || relatedFields.isEmpty()
                ? Collections.<ConfigFieldIdentity>emptyList()
                : Collections.unmodifiableList(new ArrayList<ConfigFieldIdentity>(relatedFields));
    }

    public Effect effect() {
        return this.effect;
    }

    public ConfigText message() {
        return this.message;
    }

    public List<ConfigFieldIdentity> relatedFields() {
        return this.relatedFields;
    }
}
