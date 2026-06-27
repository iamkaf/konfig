package com.iamkaf.konfig.impl.v1.sync;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
//? if >=1.20.5 {
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
import net.minecraft.resources.ResourceLocation;
//?}
//?} elif >=1.17 {
import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
import net.minecraft.resources.ResourceLocation;
//?}
//?}

@ApiStatus.Internal
public final class KonfigNetwork {
    public static final int FORGE_PROTOCOL_VERSION = 1;
    public static final String FORGE_PROTOCOL = "1";

    private KonfigNetwork() {
    }

//? if >=1.17 {
//? if >=1.21.11 {
    public static Identifier mainChannel() {
//?} else {
    public static ResourceLocation mainChannel() {
//?}
        return Constants.resource("main");
    }

//? if >=1.21.11 {
    public static Identifier syncSnapshotChannel() {
//?} else {
    public static ResourceLocation syncSnapshotChannel() {
//?}
        return Constants.resource("sync_snapshot");
    }
//?}

    public static SyncSnapshot snapshot(String configId, String jsonPayload) {
        return new SyncSnapshot(configId, jsonPayload);
    }

    public static KonfigSyncPayload snapshotPayload(String configId, String jsonPayload) {
        return new KonfigSyncPayload(configId, jsonPayload);
    }

    public static void receiveClientSnapshot(SyncSnapshot snapshot) {
        KonfigRuntime.clientReceivedSnapshot(snapshot.configId(), snapshot.jsonPayload());
    }

    public static void receiveClientSnapshot(KonfigSyncPayload payload) {
        KonfigRuntime.clientReceivedSnapshot(payload.configId(), payload.jsonPayload());
    }

//? if >=1.20.5 {
    public static CustomPacketPayload.Type<KonfigSyncPayload> snapshotPayloadType() {
        return KonfigSyncPayload.TYPE;
    }

    public static StreamCodec<FriendlyByteBuf, KonfigSyncPayload> snapshotPayloadCodec() {
        return KonfigSyncPayload.STREAM_CODEC;
    }
//?}

//? if >=1.17 {
// FriendlyByteBuf is the shared modern network buffer; legacy Forge 1.16 keeps
// PacketBuffer handling in its loader root because the mapped type differs.
    public static void encodeSnapshot(SyncSnapshot snapshot, FriendlyByteBuf buffer) {
        buffer.writeUtf(snapshot.configId(), 256);
        buffer.writeUtf(snapshot.jsonPayload());
    }

    public static SyncSnapshot decodeSnapshot(FriendlyByteBuf buffer) {
        return snapshot(buffer.readUtf(256), buffer.readUtf());
    }
//?}
}
