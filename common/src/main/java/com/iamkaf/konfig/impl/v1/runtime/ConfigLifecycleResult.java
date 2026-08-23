//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public record ConfigLifecycleResult(Status status, List<String> completedConfigIds, Map<String, Throwable> failures) {
    public ConfigLifecycleResult {
        completedConfigIds = List.copyOf(completedConfigIds);
        failures = Collections.unmodifiableMap(new LinkedHashMap<>(failures));
        if (status == Status.COMPLETED && !failures.isEmpty()) {
            throw new IllegalArgumentException("A completed lifecycle result cannot contain failures");
        }
    }

    public static ConfigLifecycleResult completed(List<String> configIds) {
        return new ConfigLifecycleResult(Status.COMPLETED, configIds, Map.of());
    }

    public static ConfigLifecycleResult failed(List<String> configIds, Map<String, Throwable> failures) {
        return new ConfigLifecycleResult(Status.FAILED, configIds, failures);
    }

    public enum Status {
        COMPLETED,
        FAILED
    }
}
//?}
