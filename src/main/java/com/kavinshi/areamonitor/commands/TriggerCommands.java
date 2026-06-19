package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.*;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public class TriggerCommands {

    private TriggerCommands() {}

    private static TriggerConfig getOrCreateTrigger(MonitorArea area, boolean enter) {
        TriggerConfig tc = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        if (tc == null) {
            tc = new TriggerConfig();
            if (enter) area.setEnterTrigger(tc);
            else area.setLeaveTrigger(tc);
        }
        return tc;
    }

    private static MonitorArea getArea(CommandContext<CommandSourceStack> context, String name) {
        MonitorArea area = AreaManager.getInstance().getArea(name);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "error.area_not_found", name);
        }
        return area;
    }

    public static int addCommand(String areaName, boolean enter, String command, CommandContext<CommandSourceStack> context) {
        MonitorArea area = getArea(context, areaName); if (area == null) return 0;
        TriggerConfig tc = getOrCreateTrigger(area, enter);
        tc.getCommands().add(command);
        ConfigManager.saveAreasConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.command_added", false, command);
        return 1;
    }

    public static int removeCommand(String areaName, boolean enter, int index, CommandContext<CommandSourceStack> context) {
        MonitorArea area = getArea(context, areaName); if (area == null) return 0;
        TriggerConfig tc = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        if (tc == null || index < 0 || index >= tc.getCommands().size()) {
            MessageUtils.sendFailure(context.getSource(), "trigger.invalid_index");
            return 0;
        }
        tc.getCommands().remove(index);
        ConfigManager.saveAreasConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.command_removed", false, index);
        return 1;
    }

    public static int listCommands(String areaName, boolean enter, CommandContext<CommandSourceStack> context) {
        MonitorArea area = getArea(context, areaName); if (area == null) return 0;
        TriggerConfig tc = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        String label = enter ? "enter" : "leave";
        context.getSource().sendSystemMessage(
            net.minecraft.network.chat.Component.literal("§6=== Trigger Commands (" + label + ") for " + areaName + " ==="));
        if (tc == null || tc.getCommands().isEmpty()) {
            MessageUtils.sendFailure(context.getSource(), "trigger.no_commands");
        } else {
            for (int i = 0; i < tc.getCommands().size(); i++) {
                context.getSource().sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§e[" + i + "] §f" + tc.getCommands().get(i)));
            }
        }
        return 1;
    }

    public static int clearCommands(String areaName, boolean enter, CommandContext<CommandSourceStack> context) {
        MonitorArea area = getArea(context, areaName); if (area == null) return 0;
        TriggerConfig tc = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        if (tc != null) tc.getCommands().clear();
        ConfigManager.saveAreasConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.commands_cleared", false);
        return 1;
    }

    public static int setSound(String areaName, boolean enter, String sound, float vol, float pitch, CommandContext<CommandSourceStack> context) {
        MonitorArea area = getArea(context, areaName); if (area == null) return 0;
        TriggerConfig tc = getOrCreateTrigger(area, enter);
        tc.setSoundEvent(sound);
        tc.setSoundVolume(vol);
        tc.setSoundPitch(pitch);
        ConfigManager.saveAreasConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.sound_set", false, sound);
        return 1;
    }

    public static int clearSound(String areaName, boolean enter, CommandContext<CommandSourceStack> context) {
        MonitorArea area = getArea(context, areaName); if (area == null) return 0;
        TriggerConfig tc = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        if (tc != null) tc.setSoundEvent(null);
        ConfigManager.saveAreasConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.sound_cleared", false);
        return 1;
    }

    public static int setTitle(String areaName, boolean enter, String main, String sub, CommandContext<CommandSourceStack> context) {
        MonitorArea area = getArea(context, areaName); if (area == null) return 0;
        TriggerConfig tc = getOrCreateTrigger(area, enter);
        tc.setTitleMain(main);
        if (sub != null && !sub.isEmpty()) tc.setTitleSub(sub);
        ConfigManager.saveAreasConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.title_set", false);
        return 1;
    }

    public static int clearTitle(String areaName, boolean enter, CommandContext<CommandSourceStack> context) {
        MonitorArea area = getArea(context, areaName); if (area == null) return 0;
        TriggerConfig tc = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        if (tc != null) { tc.setTitleMain(null); tc.setTitleSub(null); }
        ConfigManager.saveAreasConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.title_cleared", false);
        return 1;
    }

    public static int setTp(String areaName, boolean enter, String dim, double x, double y, double z, CommandContext<CommandSourceStack> context) {
        MonitorArea area = getArea(context, areaName); if (area == null) return 0;
        TriggerConfig tc = getOrCreateTrigger(area, enter);
        tc.setTeleportTarget(dim + "," + x + "," + y + "," + z);
        ConfigManager.saveAreasConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.tp_set", false, dim, x, y, z);
        return 1;
    }

    public static int clearTp(String areaName, boolean enter, CommandContext<CommandSourceStack> context) {
        MonitorArea area = getArea(context, areaName); if (area == null) return 0;
        TriggerConfig tc = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        if (tc != null) tc.setTeleportTarget(null);
        ConfigManager.saveAreasConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.tp_cleared", false);
        return 1;
    }

    public static int showInfo(String areaName, boolean enter, CommandContext<CommandSourceStack> context) {
        MonitorArea area = getArea(context, areaName); if (area == null) return 0;
        TriggerConfig tc = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        String label = enter ? "enter" : "leave";
        context.getSource().sendSystemMessage(
            net.minecraft.network.chat.Component.literal("§6=== Trigger Config (" + label + ") for " + areaName + " ==="));
        if (tc == null) {
            context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.literal("§eNo trigger configured"));
        } else {
            context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.literal("§eCommands: §f" + tc.getCommands().size()));
            context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.literal("§eSound: §f" + (tc.getSoundEvent() != null ? tc.getSoundEvent() : "none")));
            context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.literal("§eTitle: §f" + (tc.getTitleMain() != null ? tc.getTitleMain() : "none")));
            context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.literal("§eTeleport: §f" + (tc.getTeleportTarget() != null ? tc.getTeleportTarget() : "none")));
        }
        return 1;
    }
}
