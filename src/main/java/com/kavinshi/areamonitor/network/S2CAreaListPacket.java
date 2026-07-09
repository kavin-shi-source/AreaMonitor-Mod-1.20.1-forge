package com.kavinshi.areamonitor.network;

import com.kavinshi.areamonitor.AreaMonitorMod;
import com.kavinshi.areamonitor.MonitorArea;
import com.kavinshi.areamonitor.TriggerConfig;
import com.google.gson.Gson;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

/**
 * S2C: Server sends area list data to client for GUI display.
 */
public class S2CAreaListPacket {

    private static final int MAX_TOTAL_BYTES = 262144; // 256 KiB hard cap per packet
    private final List<AreaEntry> areas;

    public S2CAreaListPacket(List<AreaEntry> areas) {
        this.areas = areas;
    }

    public S2CAreaListPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > 200) {
            throw new IllegalArgumentException("Invalid area list size: " + size);
        }
        int startReaderIndex = buf.readerIndex();
        this.areas = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int bytesConsumed = buf.readerIndex() - startReaderIndex;
            if (bytesConsumed > MAX_TOTAL_BYTES) {
                AreaMonitorMod.LOGGER.warn("Area list packet exceeded {} bytes after {} entries, truncating", MAX_TOTAL_BYTES, i);
                break;
            }
            try {
                areas.add(AreaEntry.decode(buf));
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.warn("Failed to decode area entry #{} (corrupted or truncated data), skipping", i, e);
                break;
            }
        }
    }

    public void encode(FriendlyByteBuf buf) {
        // Pre-encode entries into a temp buffer to know exact count within budget
        io.netty.buffer.ByteBuf temp = io.netty.buffer.Unpooled.buffer();
        FriendlyByteBuf tempBuf = new FriendlyByteBuf(temp);
        int startWriterIndex = buf.writerIndex();
        int written = 0;
        for (AreaEntry entry : areas) {
            int bytesSoFar = buf.writerIndex() - startWriterIndex + tempBuf.writerIndex();
            if (bytesSoFar > MAX_TOTAL_BYTES) {
                AreaMonitorMod.LOGGER.warn("Area list encode exceeded {} bytes after {} of {} entries, truncating",
                    MAX_TOTAL_BYTES, written, areas.size());
                break;
            }
            entry.encode(tempBuf);
            written++;
        }
        buf.writeVarInt(written);
        buf.writeBytes(temp);
        temp.release();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.kavinshi.areamonitor.client.ClientPacketHandlers.handleAreaList(areas))
        );
        ctx.get().setPacketHandled(true);
    }

    public static S2CAreaListPacket fromAreas(Collection<MonitorArea> areas) {
        List<AreaEntry> entries = new ArrayList<>();
        int count = 0;
        for (MonitorArea area : areas) {
            if (count >= 200) {
                AreaMonitorMod.LOGGER.warn("Area list truncated to 200 entries (total {})", areas.size());
                break;
            }
            entries.add(new AreaEntry(area));
            count++;
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
            buf.writeBoolean(displayName != null);
            if (displayName != null) buf.writeUtf(displayName);
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
            String disp = buf.readBoolean() ? buf.readUtf() : null;
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
            // If JSON exceeds packet limit, skip the field entirely rather than truncating
            // mid-token (which would produce invalid JSON that the client fails to parse).
            boolean has = json != null && !json.isEmpty() && json.length() <= 32760;
            buf.writeBoolean(has);
            if (has) {
                buf.writeUtf(json);
            }
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
            } else if (area.getBounds() instanceof MonitorArea.PolygonBounds poly) {
                var arr = new com.google.gson.JsonArray();
                for (MonitorArea.Vec2i v : poly.getVertices()) {
                    var p = new com.google.gson.JsonArray();
                    p.add(v.x());
                    p.add(v.z());
                    arr.add(p);
                }
                obj.add("vertices", arr);
            } else {
                AreaMonitorMod.LOGGER.warn("Unknown bounds type for area '{}': {}", area.getName(),
                    area.getBounds() != null ? area.getBounds().getClass().getSimpleName() : "null");
            }
            return obj.toString();
        }
    }
}
