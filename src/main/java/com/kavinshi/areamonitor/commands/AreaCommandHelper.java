package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.MonitorArea;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public final class AreaCommandHelper {

    private AreaCommandHelper() {}

    public static MonitorArea requireArea(CommandContext<CommandSourceStack> context, String areaName) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
        }
        return area;
    }
}
