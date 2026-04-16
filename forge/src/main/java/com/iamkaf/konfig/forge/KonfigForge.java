package com.iamkaf.konfig.forge;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigCommon;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;
import com.iamkaf.konfig.sync.v1.KonfigSync;
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
import com.iamkaf.konfig.sync.v1.KonfigSyncPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
//?} elif >=1.17 {
import com.iamkaf.konfig.sync.v1.KonfigSyncPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
//?} else {
import com.iamkaf.konfig.sync.v1.KonfigSyncPayload;
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

@Mod(Constants.MOD_ID)
public final class KonfigForge {
//? if >=1.20.2 {
    private static final int PROTOCOL = 1;
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Constants.resource("main"))
            .networkProtocolVersion(PROTOCOL)
            .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .simpleChannel();
//?} else {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
//? if >=1.17 {
            Constants.resource("main"),
//?} else {
            new ResourceLocation(Constants.MOD_ID, "main"),
//?}
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );
//?}

    public KonfigForge() {
        RuntimeEnvironment.initialize(FMLPaths.CONFIGDIR.get(), FMLLoader.getDist().isClient());
        KonfigCommon.init();

        if (FMLLoader.getDist().isClient()) {
            KonfigForgeClient.init();
        }

//? if >=1.20.2 {
        CHANNEL.messageBuilder(SyncMessage.class)
                .encoder(SyncMessage::encode)
                .decoder(SyncMessage::decode)
                .consumerMainThread((message, context) -> {
                    if (context.getSender() == null) {
                        KonfigSync.onClientSnapshot(message.configId, message.jsonPayload);
                    }
                })
                .add();
//?} else {
        CHANNEL.registerMessage(0, SyncMessage.class, SyncMessage::encode, SyncMessage::decode,
                (message, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    context.enqueueWork(() -> {
                        if (context.getSender() == null) {
                            KonfigSync.onClientSnapshot(message.configId, message.jsonPayload);
                        }
                    });
                    context.setPacketHandled(true);
                });
//?}

//? if >=1.20.2 {
        KonfigSync.setSender((player, snapshot) ->
                CHANNEL.send(new SyncMessage(snapshot.configId(), snapshot.jsonPayload()), PacketDistributor.PLAYER.with(player))
        );
//?} elif >=1.17 {
        KonfigSync.setSender((player, snapshot) ->
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMessage(snapshot.configId(), snapshot.jsonPayload()))
        );
//?} else {
        KonfigSync.setSender((player, snapshot) ->
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new SyncMessage(snapshot.configId(), snapshot.jsonPayload()))
        );
//?}

//? if >=1.21.6 {
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(this::onPlayerJoin);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(this::onPlayerLeave);
//?} else {
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLeave);
//?}
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
//? if >=1.17 {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            KonfigSync.onPlayerJoin(player);
        }
//?} else {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            KonfigSync.onPlayerJoin(player);
        }
//?}
    }

    private void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
//? if >=1.20.2 {
        if (event.getEntity().level().isClientSide()) {
            KonfigSync.onClientDisconnect();
        }
//?} else {
        if (FMLLoader.getDist().isClient()) {
            KonfigSync.onClientDisconnect();
        }
//?}
    }

    private static final class SyncMessage {
        private final String configId;
        private final String jsonPayload;

        private SyncMessage(String configId, String jsonPayload) {
            this.configId = configId;
            this.jsonPayload = jsonPayload;
        }

//? if >=1.20.2 {
        private static void encode(SyncMessage message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.configId, 256);
            buffer.writeUtf(message.jsonPayload);
        }

        private static SyncMessage decode(FriendlyByteBuf buffer) {
            return new SyncMessage(buffer.readUtf(256), buffer.readUtf());
        }
//?} elif >=1.17 {
        private static void encode(SyncMessage message, FriendlyByteBuf buffer) {
            KonfigSyncPayload.encode(new KonfigSyncPayload(message.configId, message.jsonPayload), buffer);
        }

        private static SyncMessage decode(FriendlyByteBuf buffer) {
            KonfigSyncPayload payload = KonfigSyncPayload.decode(buffer);
            return new SyncMessage(payload.configId(), payload.jsonPayload());
        }
//?} else {
        private static void encode(SyncMessage message, PacketBuffer buffer) {
            buffer.writeUtf(message.configId);
            buffer.writeUtf(message.jsonPayload);
        }

        private static SyncMessage decode(PacketBuffer buffer) {
            KonfigSyncPayload payload = new KonfigSyncPayload(buffer.readUtf(256), buffer.readUtf());
            return new SyncMessage(payload.configId(), payload.jsonPayload());
        }
//?}
    }
}
