package com.kavinshi.areamonitor;

import com.kavinshi.areamonitor.util.MessageUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Selection tool event handler.
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID)
public class SelectionEventHandler {

    /**
     * Handle player right-click block event.
     */
    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (SelectionTool.isHoldingSelectionTool(player)) {
            SelectionTool.handlePlayerInteract(player, event.getPos(), InteractionHand.MAIN_HAND);
            event.setCanceled(true);
        }
    }

    /**
     * Handle player right-click air event (optional, for showing help).
     */
    @SubscribeEvent
    public static void onPlayerRightClickAir(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (SelectionTool.isHoldingSelectionTool(player)) {
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.tool.instructions"),
                false
            );
        }
    }
}
