package com.iamkaf.konfig.sync.v1;

//? if >=1.20.5 {
import com.iamkaf.konfig.Constants;
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
//?}

//? if >=1.20.5 {
public record KonfigSyncPayload(String configId, String jsonPayload) implements CustomPacketPayload {
//? if >=1.21.11 {
    public static final Identifier ID = Constants.resource("sync_snapshot");
//?} else {
    public static final ResourceLocation ID = Constants.resource("sync_snapshot");
//?}
    public static final Type<KonfigSyncPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, KonfigSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.configId(), 256);
                buffer.writeUtf(payload.jsonPayload());
            },
            buffer -> new KonfigSyncPayload(buffer.readUtf(256), buffer.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//?} else {
public final class KonfigSyncPayload {
    private final String configId;
    private final String jsonPayload;

    public KonfigSyncPayload(String configId, String jsonPayload) {
        this.configId = configId;
        this.jsonPayload = jsonPayload;
    }

    public String configId() {
        return this.configId;
    }

    public String jsonPayload() {
        return this.jsonPayload;
    }

//? if >=1.17 {
    public static void encode(KonfigSyncPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUtf(payload.configId(), 256);
        buffer.writeUtf(payload.jsonPayload());
    }

    public static KonfigSyncPayload decode(FriendlyByteBuf buffer) {
        return new KonfigSyncPayload(buffer.readUtf(256), buffer.readUtf());
    }
//?}
}
//?}
