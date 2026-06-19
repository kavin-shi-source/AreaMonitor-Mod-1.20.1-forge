package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.AreaMonitorMod;
import com.kavinshi.areamonitor.AreaVisualizer;
import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.MonitorArea;
import com.kavinshi.areamonitor.PerformanceMonitor;
import com.kavinshi.areamonitor.SelectionTool;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Visual tool, area visualization, and performance monitoring commands.
 */
public class VisualCommands {

    private VisualCommands() {}

    // ---- Give visual tool ----

    public static int giveVisualTool(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        SelectionTool.giveSelectionTool(player);
        return 1;
    }

    // ---- Show area visual ----

    public static int showAreaVisual(String areaName, CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        AreaVisualizer.startPersistentVisualization(player, area);
        MessageUtils.sendSuccess(context.getSource(), "area.start_showing_boundary", true, area.getDisplayName());
        return 1;
    }

    // ---- Hide area visual ----

    public static int hideAreaVisual(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        AreaVisualizer.stopPersistentVisualization(player);
        MessageUtils.sendSuccess(context.getSource(), "area.stop_showing_boundary", true);
        return 1;
    }

    // ---- Performance stats ----

    public static int showPerformance(CommandContext<CommandSourceStack> context) {
        Map<String, String> stats = PerformanceMonitor.getPerformanceStats();

        MessageUtils.sendSuccess(context.getSource(), "performance.header", false);

        for (Map.Entry<String, String> entry : stats.entrySet()) {
            String key = "performance." + entry.getKey();
            String label = LocalizationManager.translate(key);
            context.getSource().sendSuccess(
                () -> Component.literal(label + ": §f" + entry.getValue()),
                false
            );
        }

        return 1;
    }
}
