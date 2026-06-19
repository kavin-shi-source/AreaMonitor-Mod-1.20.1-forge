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
        String displayName
    ) {
        public AreaEntry(MonitorArea area) {
            this(area.getName(),
                 area.isEnabled(),
                 area.getDimension(),
                 area.getEnterMode().getName(),
                 area.getLeaveMode().getName(),
                 area.getBounds().getType().name(),
                 area.getDisplayName());
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(name);
            buf.writeBoolean(enabled);
            buf.writeUtf(dimension);
            buf.writeUtf(enterMode);
            buf.writeUtf(leaveMode);
            buf.writeUtf(boundsType);
            buf.writeUtf(displayName);
        }

        public static AreaEntry decode(FriendlyByteBuf buf) {
            return new AreaEntry(
                buf.readUtf(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf()
            );
        }
    }
}
