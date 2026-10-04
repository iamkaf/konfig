package com.iamkaf.konfig.impl.v1.bootstrap;

import org.jetbrains.annotations.ApiStatus;

//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApiStatus.Internal
public final class Constants {
    public static final String MOD_ID = "konfig";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private Constants() {
    }

//? if >=1.21.11 {
    public static Identifier resource(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
//?} elif >=1.21 {
    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
//?} else {
    public static ResourceLocation resource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
//?}
}
