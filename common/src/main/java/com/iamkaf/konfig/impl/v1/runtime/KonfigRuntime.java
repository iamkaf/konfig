package com.iamkaf.konfig.impl.v1.runtime;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ReloadCause;
import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import com.iamkaf.konfig.impl.v1.bootstrap.KonfigCommon;
import com.iamkaf.konfig.impl.v1.bootstrap.RuntimeEnvironment;
import com.iamkaf.konfig.impl.v1.config.model.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
//? if >=1.17 {
import net.minecraft.server.level.ServerPlayer;
//?}
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} elif >=1.17 {
import net.minecraft.resources.ResourceLocation;
//?}

import java.nio.file.Path;

@ApiStatus.Internal
public final class KonfigRuntime {
    public static final String MOD_ID = Constants.MOD_ID;

    private KonfigRuntime() {
    }

    public static void initialize(Path configDir, boolean client) {
        RuntimeEnvironment.initialize(configDir, client);
        KonfigCommon.init();
    }

    public static void initializeClient(Path configDir) {
        initialize(configDir, true);
    }

    public static Path configDirectory() {
        return RuntimeEnvironment.configDirectory();
    }

    public static boolean isClient() {
        return RuntimeEnvironment.isClient();
    }

//? if <=1.16.5 {
    public static String resource(String path) {
//?} elif >=1.21.11 {
    public static Identifier resource(String path) {
//?} elif >=1.17 {
    public static ResourceLocation resource(String path) {
//?}
        return Constants.resource(path);
    }

    public static void setSyncSender(SyncSender sender) {
        if (sender == null) {
            KonfigSync.setSender(null);
            return;
        }

        KonfigSync.setSender((player, snapshot) ->
                sender.send(player, snapshot.configId(), snapshot.jsonPayload())
        );
    }

//? if <=1.16.5 {
    public static void playerJoined(Object player) {
//?} else {
    public static void playerJoined(ServerPlayer player) {
//?}
        KonfigSync.onPlayerJoin(player);
    }

//? if <=1.16.5 {
    public static void playerLeft(Object player) {
//?} else {
    public static void playerLeft(ServerPlayer player) {
//?}
        KonfigSync.onPlayerLeave(player);
    }

    public static void clientReceivedSnapshot(String configId, String jsonPayload) {
        KonfigSync.onClientSnapshot(configId, jsonPayload);
    }

    public static void clientDisconnected() {
        KonfigSync.onClientDisconnect();
    }

    public static void configReloaded(ConfigHandleImpl handle, ReloadCause cause) {
        KonfigSync.onReload(handle, cause);
    }

    @FunctionalInterface
    public interface SyncSender {
//? if <=1.16.5 {
        void send(Object player, String configId, String jsonPayload);
//?} else {
        void send(ServerPlayer player, String configId, String jsonPayload);
//?}
    }
}
