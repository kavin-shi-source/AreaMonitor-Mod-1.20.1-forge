package com.kavinshi.areamonitor;

import com.kavinshi.areamonitor.util.MessageUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Selection tool event handler.
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SelectionEventHandler {

    /**
     * Handle player right-click block event.
     * Uses HIGHEST priority and receiveCanceled=true so that OPs holding the
     * selection tool can still select points inside protected areas where
     * AreaProtectionManager (HIGH priority) cancels the event.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (SelectionTool.isHoldingSelectionTool(player)) {
            SelectionTool.handlePlayerInteract(player, event.getPos(), event.getHand());
            event.setCanceled(true);
        }
    }

    /**
     * Handle player right-click air event (optional, for showing help).
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onPlayerRightClickAir(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (SelectionTool.isHoldingSelectionTool(player)) {
            // Auto-finish polygon if in polygon mode with enough vertices
            SelectionPoints sel = SelectionTool.getPlayerSelection(player.getUUID());
            if (sel != null && sel.isMultiPointMode() && sel.hasEnoughVerticesForPolygon()) {
                SelectionTool.finishPolygon(player);
                event.setCanceled(true);
                return;
            }

            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.tool.instructions"),
                false
            );
            event.setCanceled(true);
        }
    }
}
