package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.MonitorArea;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AreaStatsCommands {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AreaStatsCommands() {}

    public static int showStats(CommandContext<CommandSourceStack> context) {
        var areas = AreaManager.getInstance().getAllAreas();
        context.getSource().sendSystemMessage(
            Component.translatable("area.stats.header"));
        for (MonitorArea area : areas) {
            String lastTime = area.getLastVisitTime() > 0
                ? Instant.ofEpochMilli(area.getLastVisitTime()).atZone(ZoneId.systemDefault()).toLocalDateTime().format(TIME_FORMAT)
                : "-";
            context.getSource().sendSystemMessage(
                Component.translatable("area.stats.line",
                    area.getName(), area.getEntryCount(), area.getLastVisitor(), lastTime));
        }
        context.getSource().sendSystemMessage(
            Component.translatable("area.stats.total", areas.size()));
        return 1;
    }
}
