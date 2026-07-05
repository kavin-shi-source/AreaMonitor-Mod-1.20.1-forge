package com.kavinshi.areamonitor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: Sent from server to client to open the Area Management GUI.
 *
 * <p>Client-only classes ({@link com.kavinshi.areamonitor.client.gui.AreaManagementScreen},
 * {@code Minecraft}) are referenced only inside a {@link DistExecutor#unsafeRunWhenOn}
 * supplier, so this packet class is safe to load on a dedicated server.</p>
 */
public class S2COpenManagementScreenPacket {

    public S2COpenManagementScreenPacket() {}

    public S2COpenManagementScreenPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.kavinshi.areamonitor.client.ClientPacketHandlers.handleOpenManagementScreen())
        );
        ctx.get().setPacketHandled(true);
    }
}
