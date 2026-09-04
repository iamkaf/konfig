package com.iamkaf.konfig.forge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.impl.v1.sync.KonfigNetwork;
import com.iamkaf.konfig.impl.v1.sync.SyncSnapshot;
//? if >=1.21.11 {
import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditCapabilities;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditRequest;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditResult;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditSnapshot;
import com.iamkaf.konfig.impl.v1.sync.ConfigSyncAuthority;
import com.iamkaf.konfig.impl.v1.sync.KonfigRemotePayloads;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
import net.minecraft.network.Connection;
import net.minecraft.server.permissions.Permissions;
import net.minecraftforge.network.NetworkDirection;
//?}
//? if >=1.17 {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
//?}
//? if >=1.20.2 {
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
//?} elif >=1.18 {
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
//?} elif >=1.17 {
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
//?} else {
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
//?}

//? if <=1.20.1 {
import java.util.function.Supplier;
//?}

@Mod(KonfigRuntime.MOD_ID)
@ApiStatus.Internal
public final class KonfigForge {
//? if >=1.20.2 {
    private static final int PROTOCOL = KonfigNetwork.FORGE_PROTOCOL_VERSION;
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(KonfigNetwork.mainChannel())
            .networkProtocolVersion(PROTOCOL)
            .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .simpleChannel();
//? if >=1.21.11 {
    private static final SimpleChannel REMOTE_CHANNEL = ChannelBuilder
            .named(Constants.resource("remote_edit_v1"))
            .networkProtocolVersion(ConfigSyncAuthority.PROTOCOL_VERSION)
            .optional()
            .simpleChannel();
//?}
//?} else {
    private static final String PROTOCOL = KonfigNetwork.FORGE_PROTOCOL;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
//? if >=1.17 {
            KonfigNetwork.mainChannel(),
//?} else {
            new ResourceLocation(KonfigRuntime.MOD_ID, "main"),
//?}
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );
//?}

    public KonfigForge() {
        KonfigRuntime.initialize(FMLPaths.CONFIGDIR.get(), FMLLoader.getDist().isClient());

        if (FMLLoader.getDist().isClient()) {
            KonfigForgeClient.init();
        }

//? if >=1.20.2 {
        CHANNEL.messageBuilder(SyncMessage.class)
                .encoder(SyncMessage::encode)
                .decoder(SyncMessage::decode)
                .consumerMainThread((message, context) -> {
                    if (context.getSender() == null) {
                        KonfigNetwork.receiveClientSnapshot(message.snapshot);
                    }
                })
                .add();
//? if >=1.21.11 {
        REMOTE_CHANNEL.messageBuilder(KonfigRemotePayloads.Hello.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> KonfigRemotePayloads.Hello.STREAM_CODEC.encode(buffer, message))
                .decoder(KonfigRemotePayloads.Hello.STREAM_CODEC::decode)
                .consumerMainThread((message, context) -> {
                    net.minecraft.server.level.ServerPlayer player = context.getSender();
                    if (player != null) {
                        KonfigSync.onClientHello(player, message.protocolVersion(), canEdit(player));
                    }
                })
                .add();
        REMOTE_CHANNEL.messageBuilder(KonfigRemotePayloads.Capabilities.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> KonfigRemotePayloads.Capabilities.STREAM_CODEC.encode(buffer, message))
                .decoder(KonfigRemotePayloads.Capabilities.STREAM_CODEC::decode)
                .consumerMainThread((message, context) -> KonfigNetwork.receiveClientCapabilities(message))
                .add();
        REMOTE_CHANNEL.messageBuilder(KonfigRemotePayloads.Snapshot.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> KonfigRemotePayloads.Snapshot.STREAM_CODEC.encode(buffer, message))
                .decoder(KonfigRemotePayloads.Snapshot.STREAM_CODEC::decode)
                .consumerMainThread((message, context) -> KonfigNetwork.receiveClientAuthoritySnapshot(message))
                .add();
        REMOTE_CHANNEL.messageBuilder(KonfigRemotePayloads.EditRequest.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> KonfigRemotePayloads.EditRequest.STREAM_CODEC.encode(buffer, message))
                .decoder(KonfigRemotePayloads.EditRequest.STREAM_CODEC::decode)
                .consumerMainThread((message, context) -> {
                    net.minecraft.server.level.ServerPlayer player = context.getSender();
                    if (player != null) {
                        KonfigSync.onRemoteEdit(player, canEdit(player), KonfigNetwork.editRequest(message));
                    }
                })
                .add();
        REMOTE_CHANNEL.messageBuilder(KonfigRemotePayloads.EditResult.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> KonfigRemotePayloads.EditResult.STREAM_CODEC.encode(buffer, message))
                .decoder(KonfigRemotePayloads.EditResult.STREAM_CODEC::decode)
                .consumerMainThread((message, context) -> KonfigNetwork.receiveClientEditResult(message))
                .add();
//?}
//?} else {
        CHANNEL.registerMessage(0, SyncMessage.class, SyncMessage::encode, SyncMessage::decode,
                (message, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    context.enqueueWork(() -> {
                        if (context.getSender() == null) {
                            KonfigNetwork.receiveClientSnapshot(message.snapshot);
                        }
                    });
                    context.setPacketHandled(true);
                });
//?}

//? if >=1.20.2 {
        KonfigRuntime.setSyncSender((player, configId, jsonPayload) ->
                CHANNEL.send(SyncMessage.of(configId, jsonPayload), PacketDistributor.PLAYER.with(player))
        );
//?} elif >=1.17 {
        KonfigRuntime.setSyncSender((player, configId, jsonPayload) ->
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), SyncMessage.of(configId, jsonPayload))
        );
//?} else {
        KonfigRuntime.setSyncSender((player, configId, jsonPayload) ->
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), SyncMessage.of(configId, jsonPayload))
        );
//?}

//? if >=1.21.11 {
        KonfigSync.setRemoteSender(new KonfigSync.RemoteSender() {
            @Override
            public void sendCapabilities(net.minecraft.server.level.ServerPlayer player, ConfigEditCapabilities capabilities) {
                if (supportsRemoteEditing(player.connection.getConnection())) {
                    REMOTE_CHANNEL.send(KonfigNetwork.remoteCapabilitiesPayload(capabilities), PacketDistributor.PLAYER.with(player));
                }
            }

            @Override
            public void sendSnapshot(net.minecraft.server.level.ServerPlayer player, ConfigEditSnapshot snapshot) {
                if (supportsRemoteEditing(player.connection.getConnection())) {
                    REMOTE_CHANNEL.send(KonfigNetwork.remoteSnapshotPayload(snapshot), PacketDistributor.PLAYER.with(player));
                }
            }

            @Override
            public void sendResult(net.minecraft.server.level.ServerPlayer player, ConfigEditResult result) {
                if (supportsRemoteEditing(player.connection.getConnection())) {
                    REMOTE_CHANNEL.send(KonfigNetwork.remoteResultPayload(result), PacketDistributor.PLAYER.with(player));
                }
            }
        });
//?}

//? if >=1.21.6 {
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(this::onPlayerJoin);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(this::onPlayerLeave);
//?} else {
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLeave);
//?}
    }

//? if >=1.21.11 {
    static boolean supportsRemoteEditing(Connection connection) {
        return REMOTE_CHANNEL.isRemotePresent(connection);
    }

    static void sendRemoteHello(int protocolVersion) {
        REMOTE_CHANNEL.send(KonfigNetwork.remoteHelloPayload(protocolVersion), PacketDistributor.SERVER.noArg());
    }

    static void sendRemoteEdit(ConfigEditRequest request) {
        REMOTE_CHANNEL.send(KonfigNetwork.remoteEditPayload(request), PacketDistributor.SERVER.noArg());
    }

    private static boolean canEdit(net.minecraft.server.level.ServerPlayer player) {
        return ConfigSyncAuthority.canEdit(
                player.level().getServer().isSingleplayerOwner(player.nameAndId()),
                player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
        );
    }
//?}

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
//? if >=1.17 {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            KonfigRuntime.playerJoined(player);
        }
//?} else {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            KonfigRuntime.playerJoined(player);
        }
//?}
    }

    private void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
//? if >=1.17 {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            KonfigRuntime.playerLeft(player);
        }
//?} else {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            KonfigRuntime.playerLeft(player);
        }
//?}
//? if >=1.20.2 {
        if (event.getEntity().level().isClientSide()) {
            KonfigRuntime.clientDisconnected();
        }
//?} else {
        if (FMLLoader.getDist().isClient()) {
            KonfigRuntime.clientDisconnected();
        }
//?}
    }

    private static final class SyncMessage {
        private final SyncSnapshot snapshot;

        private SyncMessage(SyncSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        private static SyncMessage of(String configId, String jsonPayload) {
            return new SyncMessage(KonfigNetwork.snapshot(configId, jsonPayload));
        }

//? if >=1.20.2 {
        private static void encode(SyncMessage message, FriendlyByteBuf buffer) {
            KonfigNetwork.encodeSnapshot(message.snapshot, buffer);
        }

        private static SyncMessage decode(FriendlyByteBuf buffer) {
            return new SyncMessage(KonfigNetwork.decodeSnapshot(buffer));
        }
//?} elif >=1.17 {
        private static void encode(SyncMessage message, FriendlyByteBuf buffer) {
            KonfigNetwork.encodeSnapshot(message.snapshot, buffer);
        }

        private static SyncMessage decode(FriendlyByteBuf buffer) {
            return new SyncMessage(KonfigNetwork.decodeSnapshot(buffer));
        }
//?} else {
        private static void encode(SyncMessage message, PacketBuffer buffer) {
            buffer.writeUtf(message.snapshot.configId());
            buffer.writeUtf(message.snapshot.jsonPayload());
        }

        private static SyncMessage decode(PacketBuffer buffer) {
            return SyncMessage.of(buffer.readUtf(256), buffer.readUtf());
        }
//?}
    }
}
