package com.iamkaf.konfig.fabric;

import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;
import com.iamkaf.konfig.sync.v1.KonfigSync;
import com.iamkaf.konfig.sync.v1.KonfigSyncPayload;
//? if <=1.20.4 {
import com.iamkaf.konfig.Constants;
//? if <=1.16.5 {
import net.minecraft.resources.ResourceLocation;
//?}
//?}
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public final class KonfigFabricClient implements ClientModInitializer {
//? if <=1.20.4 {
//? if <=1.16.5 {
    private static final ResourceLocation SYNC_CHANNEL = new ResourceLocation(Constants.MOD_ID, "sync_snapshot");
//?} else {
    private static final net.minecraft.resources.ResourceLocation SYNC_CHANNEL = Constants.resource("sync_snapshot");
//?}
//?}

    @Override
    public void onInitializeClient() {
        RuntimeEnvironment.initialize(FabricLoader.getInstance().getConfigDir(), true);

//? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(KonfigSyncPayload.TYPE, (payload, context) ->
                KonfigSync.onClientSnapshot(payload.configId(), payload.jsonPayload())
        );
//?} else {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_CHANNEL, (client, handler, buffer, responseSender) -> {
//? if <=1.16.5 {
            KonfigSyncPayload payload = new KonfigSyncPayload(buffer.readUtf(256), buffer.readUtf());
//?} else {
            KonfigSyncPayload payload = KonfigSyncPayload.decode(buffer);
//?}
            KonfigSync.onClientSnapshot(payload.configId(), payload.jsonPayload());
        });
//?}

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> KonfigSync.onClientDisconnect());
    }
}
