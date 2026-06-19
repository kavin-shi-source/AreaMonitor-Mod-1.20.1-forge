package com.kavinshi.areamonitor.network;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.ConfigManager;
import com.kavinshi.areamonitor.MonitorArea;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: Client sends an area management action to server.
 * Supported actions: TOGGLE, DELETE, CREATE, UPDATE
 */
public class C2SAreaActionPacket {

    private static final Gson GSON = new Gson();

    public enum Action { TOGGLE, DELETE, CREATE, UPDATE }

    private final Action action;
    private final String areaName;
    private final String payload; // JSON for UPDATE action

    public C2SAreaActionPacket(Action action, String areaName) {
        this(action, areaName, null);
    }

    public C2SAreaActionPacket(Action action, String areaName, String payload) {
        this.action = action;
        this.areaName = areaName;
        this.payload = payload;
    }

    public C2SAreaActionPacket(FriendlyByteBuf buf) {
        this.action = Action.valueOf(buf.readUtf());
        this.areaName = buf.readUtf();
        this.payload = buf.readBoolean() ? buf.readUtf() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(action.name());
        buf.writeUtf(areaName);
        buf.writeBoolean(payload != null);
        if (payload != null) buf.writeUtf(payload);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) return;

            switch (action) {
                case TOGGLE: {
                    MonitorArea area = AreaManager.getInstance().getArea(areaName);
                    if (area == null) return;
                    area.setEnabled(!area.isEnabled());
                    ConfigManager.saveAreasConfig();
                    break;
                }
                case DELETE: {
                    MonitorArea area = AreaManager.getInstance().getArea(areaName);
                    if (area == null) return;
                    AreaManager.getInstance().removeArea(areaName);
                    ConfigManager.saveAreasConfig();
                    break;
                }
                case CREATE: {
                    if (AreaManager.getInstance().getArea(areaName) != null) return;
                    MonitorArea newArea = new MonitorArea(areaName);
                    AreaManager.getInstance().addArea(newArea);
                    ConfigManager.saveAreasConfig();
                    break;
                }
                case UPDATE: {
                    MonitorArea area = AreaManager.getInstance().getArea(areaName);
                    if (area == null || payload == null) return;
                    applyUpdate(area, payload);
                    ConfigManager.saveAreasConfig();
                    break;
                }
            }

            // Send updated list back to the player
            S2CAreaListPacket response = S2CAreaListPacket.fromAreas(
                AreaManager.getInstance().getAllAreas());
            ModNetwork.sendToPlayer(response, player);
        });
        context.setPacketHandled(true);
    }

    private static void applyUpdate(MonitorArea area, String json) {
        try {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj.has("displayName") && !obj.get("displayName").isJsonNull()) {
                area.setDisplayName(obj.get("displayName").getAsString());
            }
            if (obj.has("dimension") && !obj.get("dimension").isJsonNull()) {
                area.setDimension(obj.get("dimension").getAsString());
            }
            if (obj.has("enterMode") && !obj.get("enterMode").isJsonNull()) {
                area.setEnterMode(GameType.byName(obj.get("enterMode").getAsString()));
            }
            if (obj.has("leaveMode") && !obj.get("leaveMode").isJsonNull()) {
                area.setLeaveMode(GameType.byName(obj.get("leaveMode").getAsString()));
            }
            if (obj.has("enabled") && !obj.get("enabled").isJsonNull()) {
                area.setEnabled(obj.get("enabled").getAsBoolean());
            }
        } catch (Exception e) {
            com.kavinshi.areamonitor.AreaMonitorMod.LOGGER.error("Failed to apply area update", e);
        }
    }
}
