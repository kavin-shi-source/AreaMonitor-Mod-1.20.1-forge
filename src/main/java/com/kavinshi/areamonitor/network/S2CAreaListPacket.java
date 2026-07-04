package com.kavinshi.areamonitor.network;

import com.kavinshi.areamonitor.MonitorArea;
import com.kavinshi.areamonitor.TriggerConfig;
import com.google.gson.Gson;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

/**
 * S2C: Server sends area list data to client for GUI display.
 */
public class S2CAreaListPacket {

    private final List<AreaEntry> areas;

    public S2CAreaListPacket(List<AreaEntry> areas) {
        this.areas = areas;
    }

    public S2CAreaListPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.areas = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            areas.add(AreaEntry.decode(buf));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(areas.size());
        for (AreaEntry entry : areas) {
            entry.encode(buf);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraft.client.gui.screens.Screen screen =
                net.minecraft.client.Minecraft.getInstance().screen;
            if (screen instanceof com.kavinshi.areamonitor.client.gui.AreaManagementScreen gui) {
                gui.updateAreaList(areas);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static S2CAreaListPacket fromAreas(Collection<MonitorArea> areas) {
        List<AreaEntry> entries = new ArrayList<>();
        for (MonitorArea area : areas) {
            entries.add(new AreaEntry(area));
        }
        return new S2CAreaListPacket(entries);
    }

    public List<AreaEntry> getAreas() {
        return areas;
    }

    public record AreaEntry(
        String name,
        boolean enabled,
        String dimension,
        String enterMode,
        String leaveMode,
        String boundsType,
        String displayName,
        boolean protBlockBreak,
        boolean protBlockPlace,
        boolean protBlockInteract,
        boolean protPvp,
        boolean protExplosion,
        boolean protEntityDamage,
        boolean protContainerInteract,
        boolean protFluidPlace,
        boolean protItemDrop,
        String enterTriggerJson,
        String leaveTriggerJson,
        String whitelistJson,
        String restrictionsJson,
        String protWhitelistJson,
        String scheduleJson,
        String conditionJson,
        String chainJson,
        String boundsCoordsJson
    ) {
        private static final Gson TGSON = new Gson();

        public AreaEntry(MonitorArea area) {
            this(area.getName(),
                 area.isEnabled(),
                 area.getDimension(),
                 area.getEnterMode().getName(),
                 area.getLeaveMode().getName(),
                 area.getBounds().getType().name(),
                 area.getDisplayName(),
                 area.getProtection().isBlockBreak(),
                 area.getProtection().isBlockPlace(),
                 area.getProtection().isBlockInteract(),
                 area.getProtection().isPvp(),
                 area.getProtection().isExplosion(),
                 area.getProtection().isEntityDamage(),
                 area.getProtection().isContainerInteract(),
                 area.getProtection().isFluidPlace(),
                 area.getProtection().isItemDrop(),
                 area.hasEnterTrigger() ? TGSON.toJson(area.getEnterTrigger()) : null,
                 area.hasLeaveTrigger() ? TGSON.toJson(area.getLeaveTrigger()) : null,
                 area.getWhitelist().isEmpty() ? null : TGSON.toJson(area.getWhitelist()),
                 TGSON.toJson(area.getRestrictions()),
                 area.getProtectionWhitelist().isEmpty() ? null : TGSON.toJson(area.getProtectionWhitelist()),
                 scheduleToJson(area),
                 conditionToJson(area),
                 chainToJson(area),
                 boundsCoordsToJson(area));
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(name);
            buf.writeBoolean(enabled);
            buf.writeUtf(dimension);
            buf.writeUtf(enterMode);
            buf.writeUtf(leaveMode);
            buf.writeUtf(boundsType);
            buf.writeUtf(displayName);
            int protBits = (protBlockBreak ? 1 : 0) | (protBlockPlace ? 2 : 0)
                | (protBlockInteract ? 4 : 0) | (protPvp ? 8 : 0)
                | (protExplosion ? 16 : 0) | (protEntityDamage ? 32 : 0)
                | (protContainerInteract ? 64 : 0) | (protFluidPlace ? 128 : 0)
                | (protItemDrop ? 256 : 0);
            buf.writeShort(protBits);
            writeNullableJson(buf, enterTriggerJson);
            writeNullableJson(buf, leaveTriggerJson);
            writeNullableJson(buf, whitelistJson);
            writeNullableJson(buf, restrictionsJson);
            writeNullableJson(buf, protWhitelistJson);
            writeNullableJson(buf, scheduleJson);
            writeNullableJson(buf, conditionJson);
            writeNullableJson(buf, chainJson);
            writeNullableJson(buf, boundsCoordsJson);
        }

        public static AreaEntry decode(FriendlyByteBuf buf) {
            String name = buf.readUtf();
            boolean enabled = buf.readBoolean();
            String dim = buf.readUtf();
            String enter = buf.readUtf();
            String leave = buf.readUtf();
            String bounds = buf.readUtf();
            String disp = buf.readUtf();
            int bits = buf.readShort();
            return new AreaEntry(name, enabled, dim, enter, leave, bounds, disp,
                (bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0,
                (bits & 8) != 0, (bits & 16) != 0, (bits & 32) != 0,
                (bits & 64) != 0, (bits & 128) != 0, (bits & 256) != 0,
                readNullableJson(buf), readNullableJson(buf), readNullableJson(buf),
                readNullableJson(buf), readNullableJson(buf), readNullableJson(buf),
                readNullableJson(buf), readNullableJson(buf), readNullableJson(buf));
        }

        private static void writeNullableJson(FriendlyByteBuf buf, String json) {
            boolean has = json != null && !json.isEmpty();
            buf.writeBoolean(has);
            if (has) buf.writeUtf(json);
        }

        private static String readNullableJson(FriendlyByteBuf buf) {
            return buf.readBoolean() ? buf.readUtf() : null;
        }

        private static String scheduleToJson(MonitorArea area) {
            if (!area.isScheduleEnabled()) return null;
            var obj = new com.google.gson.JsonObject();
            obj.addProperty("enabled", true);
            if (area.getScheduleTimeMin() != null) obj.addProperty("timeMin", area.getScheduleTimeMin());
            if (area.getScheduleTimeMax() != null) obj.addProperty("timeMax", area.getScheduleTimeMax());
            return obj.toString();
        }

        private static String conditionToJson(MonitorArea area) {
            if (!area.isConditionEnabled()) return null;
            var obj = new com.google.gson.JsonObject();
            obj.addProperty("enabled", true);
            if (area.getConditionMinPlayers() != null) obj.addProperty("minPlayers", area.getConditionMinPlayers());
            if (area.getConditionRequirePlayer() != null) obj.addProperty("requirePlayer", area.getConditionRequirePlayer());
            return obj.toString();
        }

        private static String chainToJson(MonitorArea area) {
            if (!area.hasChainTarget()) return null;
            var obj = new com.google.gson.JsonObject();
            obj.addProperty("chainNext", area.getChainNext());
            obj.addProperty("chainDelayTicks", area.getChainDelayTicks());
            return obj.toString();
        }

        private static String boundsCoordsToJson(MonitorArea area) {
            var obj = new com.google.gson.JsonObject();
            if (area.getBounds() instanceof MonitorArea.RectangleBounds rect) {
                obj.addProperty("minX", rect.getMinX());
                obj.addProperty("minZ", rect.getMinZ());
                obj.addProperty("maxX", rect.getMaxX());
                obj.addProperty("maxZ", rect.getMaxZ());
            } else if (area.getBounds() instanceof MonitorArea.CircleBounds circle) {
                obj.addProperty("centerX", circle.getCenterX());
                obj.addProperty("centerZ", circle.getCenterZ());
                obj.addProperty("radius", circle.getRadius());
            }
            return obj.toString();
        }
    }
}
