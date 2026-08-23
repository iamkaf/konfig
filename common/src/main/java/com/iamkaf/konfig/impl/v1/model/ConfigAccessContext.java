package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigAccessContext {
    public enum RuntimeSide {
        CLIENT,
        SERVER
    }

    public enum ValueSource {
        LOCAL,
        SYNCHRONIZED_SERVER
    }

    private final RuntimeSide runtimeSide;
    private final ValueSource valueSource;
    private final boolean mayEditServerValues;

    public ConfigAccessContext(RuntimeSide runtimeSide, ValueSource valueSource, boolean mayEditServerValues) {
        this.runtimeSide = Objects.requireNonNull(runtimeSide, "runtimeSide");
        this.valueSource = Objects.requireNonNull(valueSource, "valueSource");
        if (valueSource == ValueSource.SYNCHRONIZED_SERVER && runtimeSide != RuntimeSide.CLIENT) {
            throw new IllegalArgumentException("A synchronized server view only exists on the client side");
        }
        this.mayEditServerValues = mayEditServerValues;
    }

    public static ConfigAccessContext local(RuntimeSide runtimeSide) {
        return new ConfigAccessContext(runtimeSide, ValueSource.LOCAL, false);
    }

    public static ConfigAccessContext synchronizedServer(boolean mayEditServerValues) {
        return new ConfigAccessContext(RuntimeSide.CLIENT, ValueSource.SYNCHRONIZED_SERVER, mayEditServerValues);
    }

    public RuntimeSide runtimeSide() {
        return this.runtimeSide;
    }

    public ValueSource valueSource() {
        return this.valueSource;
    }

    public boolean mayEditServerValues() {
        return this.mayEditServerValues;
    }
}
