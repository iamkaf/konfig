package com.iamkaf.konfig;

//? if <=1.16.5 {
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//?} elif >=1.21.11 {
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?} else {
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?}

public final class Constants {
    public static final String MOD_ID = "konfig";
//? if <=1.16.5 {
    public static final Logger LOG = LogManager.getLogger(MOD_ID);
//?} else {
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
//?}

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
//?} elif >=1.17 {
    public static ResourceLocation resource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
//?}
}
