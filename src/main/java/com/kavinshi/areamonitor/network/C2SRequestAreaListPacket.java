package com.kavinshi.areamonitor.network;

import com.kavinshi.areamonitor.AreaManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * C2S: Client requests area list data from server.
 */
public class C2SRequestAreaListPacket {

    public C2SRequestAreaListPacket() {}

    public C2SRequestAreaListPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(2)) {
                S2CAreaListPacket response = S2CAreaListPacket.fromAreas(
                    AreaManager.getInstance().getAllAreas());
                ModNetwork.sendToPlayer(response, player);
            } else if (player != null) {
                // Permission denied: send empty list so client GUI doesn't hang forever
                ModNetwork.sendToPlayer(new S2CAreaListPacket(List.of()), player);
            }
        });
        context.setPacketHandled(true);
    }
}
