package com.iamkaf.konfig.fabric;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.impl.v1.sync.KonfigNetwork;
//? if <=1.20.4 {
//? if <=1.16.5 {
import net.minecraft.resources.ResourceLocation;
//?}
//?}
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

@ApiStatus.Internal
public final class KonfigFabricClient implements ClientModInitializer {
//? if <=1.20.4 {
//? if <=1.16.5 {
    private static final ResourceLocation SYNC_CHANNEL = new ResourceLocation(KonfigRuntime.MOD_ID, "sync_snapshot");
//?} else {
    private static final net.minecraft.resources.ResourceLocation SYNC_CHANNEL = KonfigNetwork.syncSnapshotChannel();
//?}
//?}

    @Override
    public void onInitializeClient() {
        KonfigRuntime.initializeClient(FabricLoader.getInstance().getConfigDir());

//? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(KonfigNetwork.snapshotPayloadType(), (payload, context) ->
                KonfigNetwork.receiveClientSnapshot(payload)
        );
//?} else {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_CHANNEL, (client, handler, buffer, responseSender) -> {
//? if <=1.16.5 {
            KonfigNetwork.receiveClientSnapshot(KonfigNetwork.snapshot(buffer.readUtf(256), buffer.readUtf()));
//?} else {
            KonfigNetwork.receiveClientSnapshot(KonfigNetwork.decodeSnapshot(buffer));
//?}
        });
//?}

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> KonfigRuntime.clientDisconnected());
    }
}
