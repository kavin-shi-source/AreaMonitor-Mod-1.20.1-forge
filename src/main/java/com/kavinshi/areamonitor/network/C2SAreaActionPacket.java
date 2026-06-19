package com.kavinshi.areamonitor.network;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.ConfigManager;
import com.kavinshi.areamonitor.MonitorArea;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: Client sends an area management action to server.
 * Supported actions: TOGGLE, DELETE, CREATE
 */
public class C2SAreaActionPacket {

    public enum Action { TOGGLE, DELETE, CREATE }

    private final Action action;
    private final String areaName;

    public C2SAreaActionPacket(Action action, String areaName) {
        this.action = action;
        this.areaName = areaName;
    }

    public C2SAreaActionPacket(FriendlyByteBuf buf) {
        this.action = Action.valueOf(buf.readUtf());
        this.areaName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(action.name());
        buf.writeUtf(areaName);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) return;

            MonitorArea area = action != Action.CREATE ? AreaManager.getInstance().getArea(areaName) : null;

            switch (action) {
                case TOGGLE:
                    if (area == null) return;
                    area.setEnabled(!area.isEnabled());
                    ConfigManager.saveAreasConfig();
                    break;
                case DELETE:
                    if (area == null) return;
                    AreaManager.getInstance().removeArea(areaName);
                    ConfigManager.saveAreasConfig();
                    break;
                case CREATE:
                    if (AreaManager.getInstance().getArea(areaName) != null) return;
                    MonitorArea newArea = new MonitorArea(areaName);
                    AreaManager.getInstance().addArea(newArea);
                    ConfigManager.saveAreasConfig();
                    break;
            }

            // Send updated list back to the player
            S2CAreaListPacket response = S2CAreaListPacket.fromAreas(
                AreaManager.getInstance().getAllAreas());
            ModNetwork.sendToPlayer(response, player);
        });
        context.setPacketHandled(true);
    }
}
