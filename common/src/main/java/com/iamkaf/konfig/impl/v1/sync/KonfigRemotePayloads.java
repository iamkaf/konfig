package com.iamkaf.konfig.impl.v1.sync;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;
//? if >=1.20.5 {
import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

@ApiStatus.Internal
public final class KonfigRemotePayloads {
    private KonfigRemotePayloads() {
    }

    public record Hello(int protocolVersion)
//? if >=1.20.5
            implements CustomPacketPayload {
//? if <1.20.5
            {
        public static final String PATH = "remote_hello";
//? if >=1.20.5 {
        public static final Type<Hello> TYPE = new Type<>(Constants.resource(PATH));
        public static final StreamCodec<FriendlyByteBuf, Hello> STREAM_CODEC = StreamCodec.of(Hello::write, Hello::read);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
//?}

        public static void write(FriendlyByteBuf buffer, Hello payload) {
            buffer.writeVarInt(payload.protocolVersion());
        }

        public static Hello read(FriendlyByteBuf buffer) {
            return new Hello(buffer.readVarInt());
        }
    }

    public record Capabilities(int protocolVersion, boolean canEdit)
//? if >=1.20.5
            implements CustomPacketPayload {
//? if <1.20.5
            {
        public static final String PATH = "remote_capabilities";
//? if >=1.20.5 {
        public static final Type<Capabilities> TYPE = new Type<>(Constants.resource(PATH));
        public static final StreamCodec<FriendlyByteBuf, Capabilities> STREAM_CODEC = StreamCodec.of(Capabilities::write, Capabilities::read);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
//?}

        public static void write(FriendlyByteBuf buffer, Capabilities payload) {
            buffer.writeVarInt(payload.protocolVersion());
            buffer.writeBoolean(payload.canEdit());
        }

        public static Capabilities read(FriendlyByteBuf buffer) {
            return new Capabilities(buffer.readVarInt(), buffer.readBoolean());
        }
    }

    public record Snapshot(String configId, long revision, String jsonPayload)
//? if >=1.20.5
            implements CustomPacketPayload {
//? if <1.20.5
            {
        public static final String PATH = "remote_snapshot";
//? if >=1.20.5 {
        public static final Type<Snapshot> TYPE = new Type<>(Constants.resource(PATH));
        public static final StreamCodec<FriendlyByteBuf, Snapshot> STREAM_CODEC = StreamCodec.of(Snapshot::write, Snapshot::read);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
//?}

        public static void write(FriendlyByteBuf buffer, Snapshot payload) {
            buffer.writeUtf(payload.configId(), ConfigSyncAuthority.MAX_CONFIG_ID_LENGTH);
            buffer.writeVarLong(payload.revision());
            buffer.writeUtf(payload.jsonPayload(), ConfigSyncAuthority.MAX_JSON_LENGTH);
        }

        public static Snapshot read(FriendlyByteBuf buffer) {
            return new Snapshot(
                    buffer.readUtf(ConfigSyncAuthority.MAX_CONFIG_ID_LENGTH),
                    buffer.readVarLong(),
                    buffer.readUtf(ConfigSyncAuthority.MAX_JSON_LENGTH)
            );
        }
    }

    public record EditRequest(long requestId, String configId, long baseRevision, String draftJson)
//? if >=1.20.5
            implements CustomPacketPayload {
//? if <1.20.5
            {
        public static final String PATH = "remote_edit";
//? if >=1.20.5 {
        public static final Type<EditRequest> TYPE = new Type<>(Constants.resource(PATH));
        public static final StreamCodec<FriendlyByteBuf, EditRequest> STREAM_CODEC = StreamCodec.of(EditRequest::write, EditRequest::read);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
//?}

        public static void write(FriendlyByteBuf buffer, EditRequest payload) {
            buffer.writeVarLong(payload.requestId());
            buffer.writeUtf(payload.configId(), ConfigSyncAuthority.MAX_CONFIG_ID_LENGTH);
            buffer.writeVarLong(payload.baseRevision());
            buffer.writeUtf(payload.draftJson(), ConfigSyncAuthority.MAX_JSON_LENGTH);
        }

        public static EditRequest read(FriendlyByteBuf buffer) {
            return new EditRequest(
                    buffer.readVarLong(),
                    buffer.readUtf(ConfigSyncAuthority.MAX_CONFIG_ID_LENGTH),
                    buffer.readVarLong(),
                    buffer.readUtf(ConfigSyncAuthority.MAX_JSON_LENGTH)
            );
        }
    }

    public record EditResult(
            long requestId,
            String configId,
            ConfigEditStatus status,
            long revision,
            String snapshotJson,
            String detail
    )
//? if >=1.20.5
            implements CustomPacketPayload {
//? if <1.20.5
            {
        public static final String PATH = "remote_result";
//? if >=1.20.5 {
        public static final Type<EditResult> TYPE = new Type<>(Constants.resource(PATH));
        public static final StreamCodec<FriendlyByteBuf, EditResult> STREAM_CODEC = StreamCodec.of(EditResult::write, EditResult::read);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
//?}

        public static void write(FriendlyByteBuf buffer, EditResult payload) {
            buffer.writeVarLong(payload.requestId());
            buffer.writeUtf(payload.configId(), ConfigSyncAuthority.MAX_CONFIG_ID_LENGTH);
            buffer.writeUtf(payload.status().name(), 32);
            buffer.writeVarLong(payload.revision());
            buffer.writeUtf(payload.snapshotJson(), ConfigSyncAuthority.MAX_JSON_LENGTH);
            buffer.writeUtf(payload.detail(), ConfigSyncAuthority.MAX_DETAIL_LENGTH);
        }

        public static EditResult read(FriendlyByteBuf buffer) {
            return new EditResult(
                    buffer.readVarLong(),
                    buffer.readUtf(ConfigSyncAuthority.MAX_CONFIG_ID_LENGTH),
                    decodeStatus(buffer.readUtf(32)),
                    buffer.readVarLong(),
                    buffer.readUtf(ConfigSyncAuthority.MAX_JSON_LENGTH),
                    buffer.readUtf(ConfigSyncAuthority.MAX_DETAIL_LENGTH)
            );
        }
    }

    private static ConfigEditStatus decodeStatus(String value) {
        try {
            return ConfigEditStatus.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return ConfigEditStatus.INVALID;
        }
    }
}
