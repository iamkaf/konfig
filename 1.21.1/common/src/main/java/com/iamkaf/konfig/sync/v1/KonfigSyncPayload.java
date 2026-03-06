package com.iamkaf.konfig.sync.v1;

import com.iamkaf.konfig.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KonfigSyncPayload(String configId, String jsonPayload) implements CustomPacketPayload {
    public static final ResourceLocation ID = Constants.resource("sync_snapshot");
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
