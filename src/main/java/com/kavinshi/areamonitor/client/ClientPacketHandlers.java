package com.kavinshi.areamonitor.client;

import com.kavinshi.areamonitor.client.gui.AreaManagementScreen;
import com.kavinshi.areamonitor.network.S2CAreaListPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Client-side packet handlers. All methods in this class may reference
 * client-only classes (Minecraft, AreaManagementScreen, etc.) safely because
 * the class itself is annotated with {@link OnlyIn(Dist.CLIENT)} and is only
 * loaded via {@link net.minecraftforge.fml.DistExecutor#unsafeRunWhenOn} on
 * the client. Dedicated servers will never touch this class.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {}

    /**
     * Open the area management screen. Called by S2COpenManagementScreenPacket.
     */
    public static void handleOpenManagementScreen() {
        Minecraft.getInstance().setScreen(new AreaManagementScreen());
    }

    /**
     * Update the area list on the currently-open AreaManagementScreen.
     * Called by S2CAreaListPacket.
     * P2 #31: when a sub-panel (edit/trigger/whitelist/restriction) is open
     * instead of the management screen, buffer the update so it gets applied
     * when the user returns to the management screen.
     */
    public static void handleAreaList(List<S2CAreaListPacket.AreaEntry> areas) {
        if (Minecraft.getInstance().screen instanceof AreaManagementScreen gui) {
            gui.updateAreaList(areas);
        } else {
            AreaManagementScreen.bufferAreaList(areas);
        }
    }
}
