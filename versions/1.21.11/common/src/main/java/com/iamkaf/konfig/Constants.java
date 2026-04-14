package com.iamkaf.konfig;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Constants {
    public static final String MOD_ID = "konfig";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private Constants() {
    }

    public static Identifier resource(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
