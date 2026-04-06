package com.iamkaf.konfig.api.v1;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.function.Predicate;

public interface StringValueBuilder extends ValueBuilder<String> {
    @Override
    StringValueBuilder comment(String comment);

    @Override
    StringValueBuilder restart(RestartRequirement requirement);

    @Override
    StringValueBuilder sync(boolean sync);

    @Override
    StringValueBuilder clientOnly();

    @Override
    StringValueBuilder serverOnly();

    @Override
    StringValueBuilder validate(Predicate<String> validator, String errorMessage);

    StringValueBuilder registry(ResourceKey<? extends Registry<?>> registryKey);
}
