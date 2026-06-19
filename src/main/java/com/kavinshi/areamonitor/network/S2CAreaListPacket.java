package com.kavinshi.areamonitor.network;

import com.kavinshi.areamonitor.MonitorArea;
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
        boolean protEntityDamage
    ) {
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
                 area.getProtection().isEntityDamage());
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(name);
            buf.writeBoolean(enabled);
            buf.writeUtf(dimension);
            buf.writeUtf(enterMode);
            buf.writeUtf(leaveMode);
            buf.writeUtf(boundsType);
            buf.writeUtf(displayName);
            // Encode 6 protection bools as bitmap byte
            int protBits = (protBlockBreak ? 1 : 0) | (protBlockPlace ? 2 : 0)
                | (protBlockInteract ? 4 : 0) | (protPvp ? 8 : 0)
                | (protExplosion ? 16 : 0) | (protEntityDamage ? 32 : 0);
            buf.writeByte(protBits);
        }

        public static AreaEntry decode(FriendlyByteBuf buf) {
            String name = buf.readUtf();
            boolean enabled = buf.readBoolean();
            String dim = buf.readUtf();
            String enter = buf.readUtf();
            String leave = buf.readUtf();
            String bounds = buf.readUtf();
            String disp = buf.readUtf();
            int bits = buf.readByte();
            return new AreaEntry(name, enabled, dim, enter, leave, bounds, disp,
                (bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0,
                (bits & 8) != 0, (bits & 16) != 0, (bits & 32) != 0);
        }
    }
}
