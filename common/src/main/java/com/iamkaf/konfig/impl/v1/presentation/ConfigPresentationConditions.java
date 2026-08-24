package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApiStatus.Internal
public final class ConfigPresentationConditions {
    private static final ConfigPresentationConditions ALWAYS = new ConfigPresentationConditions(
            Collections.<ConfigAccessInput>emptyList(),
            Collections.<ConfigAccessInput>emptyList()
    );

    private final List<ConfigAccessInput> visibilityInputs;
    private final List<ConfigAccessInput> editabilityInputs;

    public ConfigPresentationConditions(
            List<ConfigAccessInput> visibilityInputs,
            List<ConfigAccessInput> editabilityInputs
    ) {
        this.visibilityInputs = immutable(visibilityInputs);
        this.editabilityInputs = immutable(editabilityInputs);
    }

    public static ConfigPresentationConditions always() {
        return ALWAYS;
    }

    public List<ConfigAccessInput> visibilityInputs() {
        return this.visibilityInputs;
    }

    public List<ConfigAccessInput> editabilityInputs() {
        return this.editabilityInputs;
    }

    private static List<ConfigAccessInput> immutable(List<ConfigAccessInput> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<ConfigAccessInput>(values));
    }
}
