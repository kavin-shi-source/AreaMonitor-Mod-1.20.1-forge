package com.kavinshi.areamonitor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 选择工具事件处理器
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SelectionEventHandler {

    /**
     * 处理玩家右键点击方块事件
     */
    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 检查是否是选择工具的右键点击
        if (SelectionTool.isHoldingSelectionTool(player) && event.getHand() == InteractionHand.MAIN_HAND) {
            // 处理选择
            SelectionTool.handlePlayerInteract(player, event.getPos(), event.getHand());

            // 取消事件，防止其他操作
            event.setCanceled(true);
        }
    }

    /**
     * 处理玩家右键点击空气事件（可选，用于显示帮助）
     */
    @SubscribeEvent
    public static void onPlayerRightClickEmpty(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 如果手持选择工具但没有点击方块，显示帮助信息
        if (SelectionTool.isHoldingSelectionTool(player) && event.getHand() == InteractionHand.MAIN_HAND) {
            // 每秒只显示一次帮助信息，避免刷屏
            long currentTime = System.currentTimeMillis();
            Long lastHelpTime = SelectionTool.getLastHelpTime(player.getUUID());

            if (lastHelpTime == null || currentTime - lastHelpTime > SelectionTool.HELP_MESSAGE_COOLDOWN_MS) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("command.areamonitor.selection.help"),
                    true
                );
                SelectionTool.setLastHelpTime(player.getUUID(), currentTime);
            }
        }
    }
}