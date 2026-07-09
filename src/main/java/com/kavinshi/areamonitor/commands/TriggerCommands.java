package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.*;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Set;

public class TriggerCommands {

    // P2 #27: increased from 256 to 1024 to allow complex /execute chains
    private static final int MAX_COMMAND_LENGTH = 1024;
    private static final Set<String> DANGEROUS_COMMANDS = Set.of(
        "stop", "restart", "op", "deop", "ban", "ban-ip", "pardon", "pardon-ip",
        "whitelist", "reload", "reload-confirm", "save-off", "save-all"
    );

    private TriggerCommands() {}

    private static TriggerConfig getOrCreateTrigger(MonitorArea area, boolean enter) {
        TriggerConfig triggerConfig = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        if (triggerConfig == null) {
            triggerConfig = new TriggerConfig();
            if (enter) area.setEnterTrigger(triggerConfig);
            else area.setLeaveTrigger(triggerConfig);
        }
        return triggerConfig;
    }

    public static int addCommand(String areaName, boolean enter, String command, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName); if (area == null) return 0;

        if (command == null || command.trim().isEmpty()) {
            MessageUtils.sendFailure(context.getSource(), "trigger.command_empty");
            return 0;
        }
        if (command.length() > MAX_COMMAND_LENGTH) {
            MessageUtils.sendFailure(context.getSource(), "trigger.command_too_long", MAX_COMMAND_LENGTH);
            return 0;
        }
        String trimmed = command.trim();
        String baseCmd = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        baseCmd = baseCmd.split(" ")[0].toLowerCase();
        int colonIdx = baseCmd.indexOf(':');
        if (colonIdx > 0) {
            baseCmd = baseCmd.substring(colonIdx + 1);
        }
        if (DANGEROUS_COMMANDS.contains(baseCmd)) {
            MessageUtils.sendFailure(context.getSource(), "trigger.command_blocked", baseCmd);
            return 0;
        }
        String lowerCmd = trimmed.toLowerCase();
        for (String dangerous : DANGEROUS_COMMANDS) {
            // Only flag dangerous commands that appear as sub-commands (preceded by / or "run "), not in chat text
            if (lowerCmd.matches(".*(?:/|\\brun\\s+)" + java.util.regex.Pattern.quote(dangerous) + "(?:\\s|$).*")) {
                MessageUtils.sendFailure(context.getSource(), "trigger.command_blocked", dangerous);
                return 0;
            }
        }

        TriggerConfig triggerConfig = getOrCreateTrigger(area, enter);
        triggerConfig.getCommands().add(command);
        ConfigManager.safeSaveConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.command_added", false, command);
        return 1;
    }

    public static int removeCommand(String areaName, boolean enter, int index, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName); if (area == null) return 0;
        TriggerConfig triggerConfig = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        if (triggerConfig == null || index < 0 || index >= triggerConfig.getCommands().size()) {
            MessageUtils.sendFailure(context.getSource(), "trigger.invalid_index");
            return 0;
        }
        triggerConfig.getCommands().remove(index);
        ConfigManager.safeSaveConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.command_removed", false, index);
        return 1;
    }

    public static int listCommands(String areaName, boolean enter, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName); if (area == null) return 0;
        TriggerConfig triggerConfig = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        String label = enter ? "enter" : "leave";
        context.getSource().sendSystemMessage(
            Component.translatable("trigger.commands.header", label, areaName));
        if (triggerConfig == null || triggerConfig.getCommands().isEmpty()) {
            MessageUtils.sendFailure(context.getSource(), "trigger.no_commands");
        } else {
            for (int i = 0; i < triggerConfig.getCommands().size(); i++) {
                context.getSource().sendSystemMessage(
                    Component.translatable("trigger.commands.entry", i, triggerConfig.getCommands().get(i)));
            }
        }
        return 1;
    }

    public static int clearCommands(String areaName, boolean enter, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName); if (area == null) return 0;
        TriggerConfig triggerConfig = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        if (triggerConfig != null) triggerConfig.getCommands().clear();
        ConfigManager.safeSaveConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.commands_cleared", false);
        return 1;
    }

    public static int setSound(String areaName, boolean enter, String sound, float vol, float pitch, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName); if (area == null) return 0;
        TriggerConfig triggerConfig = getOrCreateTrigger(area, enter);
        triggerConfig.setSoundEvent(sound);
        triggerConfig.setSoundVolume(vol);
        triggerConfig.setSoundPitch(pitch);
        ConfigManager.safeSaveConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.sound_set", false, sound);
        return 1;
    }

    public static int clearSound(String areaName, boolean enter, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName); if (area == null) return 0;
        TriggerConfig triggerConfig = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        if (triggerConfig != null) triggerConfig.setSoundEvent(null);
        ConfigManager.safeSaveConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.sound_cleared", false);
        return 1;
    }

    public static int setTitle(String areaName, boolean enter, String main, String sub, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName); if (area == null) return 0;
        TriggerConfig triggerConfig = getOrCreateTrigger(area, enter);
        triggerConfig.setTitleMain(main);
        if (sub != null && !sub.isEmpty()) triggerConfig.setTitleSub(sub);
        ConfigManager.safeSaveConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.title_set", false);
        return 1;
    }

    public static int clearTitle(String areaName, boolean enter, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName); if (area == null) return 0;
        TriggerConfig triggerConfig = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        if (triggerConfig != null) { triggerConfig.setTitleMain(null); triggerConfig.setTitleSub(null); }
        ConfigManager.safeSaveConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.title_cleared", false);
        return 1;
    }

    public static int setTp(String areaName, boolean enter, String dim, double x, double y, double z, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName); if (area == null) return 0;
        TriggerConfig triggerConfig = getOrCreateTrigger(area, enter);
        // Use space-separated format (consistent with TriggerEditPanel). AreaTriggerManager
        // parser handles both space and comma for backwards compatibility with old configs.
        triggerConfig.setTeleportTarget(dim + " " + x + " " + y + " " + z);
        ConfigManager.safeSaveConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.tp_set", false, dim, x, y, z);
        return 1;
    }

    public static int clearTp(String areaName, boolean enter, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName); if (area == null) return 0;
        TriggerConfig triggerConfig = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        if (triggerConfig != null) triggerConfig.setTeleportTarget(null);
        ConfigManager.safeSaveConfig();
        MessageUtils.sendSuccess(context.getSource(), "trigger.tp_cleared", false);
        return 1;
    }

    public static int showInfo(String areaName, boolean enter, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName); if (area == null) return 0;
        TriggerConfig triggerConfig = enter ? area.getEnterTrigger() : area.getLeaveTrigger();
        String label = enter ? "enter" : "leave";
        context.getSource().sendSystemMessage(
            Component.translatable("trigger.config.header", label, areaName));
        if (triggerConfig == null) {
            context.getSource().sendSystemMessage(
                Component.translatable("trigger.config.none"));
        } else {
            String noneStr = LocalizationManager.translate("common.none");
            context.getSource().sendSystemMessage(
                Component.translatable("trigger.config.commands", triggerConfig.getCommands().size()));
            context.getSource().sendSystemMessage(
                Component.translatable("trigger.config.sound", triggerConfig.getSoundEvent() != null ? triggerConfig.getSoundEvent() : noneStr));
            context.getSource().sendSystemMessage(
                Component.translatable("trigger.config.title", triggerConfig.getTitleMain() != null ? triggerConfig.getTitleMain() : noneStr));
            context.getSource().sendSystemMessage(
                Component.translatable("trigger.config.teleport", triggerConfig.getTeleportTarget() != null ? triggerConfig.getTeleportTarget() : noneStr));
        }
        return 1;
    }
}
