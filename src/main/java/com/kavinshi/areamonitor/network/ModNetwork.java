package com.kavinshi.areamonitor.network;

import com.kavinshi.areamonitor.AreaMonitorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.tryParse(AreaMonitorMod.MOD_ID + ":main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++,
            S2COpenManagementScreenPacket.class,
            S2COpenManagementScreenPacket::encode,
            S2COpenManagementScreenPacket::new,
            S2COpenManagementScreenPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++,
            C2SRequestAreaListPacket.class,
            C2SRequestAreaListPacket::encode,
            C2SRequestAreaListPacket::new,
            C2SRequestAreaListPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++,
            S2CAreaListPacket.class,
            S2CAreaListPacket::encode,
            S2CAreaListPacket::new,
            S2CAreaListPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++,
            C2SAreaActionPacket.class,
            C2SAreaActionPacket::encode,
            C2SAreaActionPacket::new,
            C2SAreaActionPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
