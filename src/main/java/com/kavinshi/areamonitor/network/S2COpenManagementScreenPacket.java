package com.kavinshi.areamonitor.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: Sent from server to client to open the Area Management GUI.
 */
public class S2COpenManagementScreenPacket {

    public S2COpenManagementScreenPacket() {}

    public S2COpenManagementScreenPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft.getInstance().setScreen(
                new com.kavinshi.areamonitor.client.gui.AreaManagementScreen());
        });
        ctx.get().setPacketHandled(true);
    }
}
