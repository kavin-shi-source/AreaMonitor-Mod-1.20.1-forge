package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.AreaMonitorMod;
import com.kavinshi.areamonitor.ConfigManager;
import com.kavinshi.areamonitor.WhitelistManager;
import com.kavinshi.areamonitor.util.AuditLogger;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Whitelist, toggle, and help commands.
 */
public class WhitelistCommands {

    private WhitelistCommands() {}

    // ---- Toggle monitoring ----

    public static int toggleMonitor(CommandContext<CommandSourceStack> context) {
        boolean current = ConfigManager.CONFIG.isEnabled.get();
        boolean newState = !current;

        try {
            ConfigManager.CONFIG.isEnabled.set(newState);
            ConfigManager.CONFIG.isEnabled.save();
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("Failed to save config toggle for isEnabled", e);
            MessageUtils.sendFailure(context.getSource(), "config.save_failed", e.getMessage());
            return 0;
        }

        String messageKey = newState ?
                "command.areamonitor.toggle.enabled" :
                "command.areamonitor.toggle.disabled";

        MessageUtils.sendSuccess(context.getSource(), messageKey, true);

        return 1;
    }

    // ---- Whitelist add ----

    public static int addToWhitelist(String player, CommandContext<CommandSourceStack> context) {
        if (!isValidPlayerName(player)) {
            context.getSource().sendSystemMessage(Component.translatable("gui.error.invalid_player_name"));
            return 0;
        }
        if (WhitelistManager.addToWhitelist(player)) {
            AuditLogger.log(context.getSource(), "WHITELIST_ADD", player);
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.add.success", true, player);
        } else {
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.add.exists", true, player);
        }
        return 1;
    }

    // ---- Whitelist remove ----

    public static int removeFromWhitelist(String player, CommandContext<CommandSourceStack> context) {
        if (!isValidPlayerName(player)) {
            context.getSource().sendSystemMessage(Component.translatable("gui.error.invalid_player_name"));
            return 0;
        }
        if (WhitelistManager.removeFromWhitelist(player)) {
            AuditLogger.log(context.getSource(), "WHITELIST_REMOVE", player);
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.remove.success", true, player);
        } else {
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.remove.not_found", true, player);
        }
        return 1;
    }

    // ---- Whitelist list ----

    public static int listWhitelist(CommandContext<CommandSourceStack> context) {
        Set<String> whitelist = WhitelistManager.getWhitelist();

        if (whitelist.isEmpty()) {
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.list.empty", false);
        } else {
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.list.header", false);

            List<String> whitelistArray = new ArrayList<>(whitelist);

            for (int i = 0; i < whitelistArray.size(); i++) {
                final int index = i + 1;
                final String player = whitelistArray.get(i);

                context.getSource().sendSuccess(
                        () -> Component.translatable("whitelist.list.entry", index, player),
                        false
                );
            }

            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.list.count", false, whitelist.size());
        }
        return 1;
    }

    // ---- Whitelist clear ----

    public static int clearWhitelist(CommandContext<CommandSourceStack> context) {
        WhitelistManager.clearWhitelist();
        AuditLogger.log(context.getSource(), "WHITELIST_CLEAR");
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.clear.success", true);
        return 1;
    }

    // ---- Help ----

    public static int showHelp(CommandContext<CommandSourceStack> context) {
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.header", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.basic", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.toggle", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.list", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.area", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.create", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.delete", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.list", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.toggle", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.info", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.set_enter_mode", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.set_leave_mode", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.visual", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.visual.tool", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.visual.show", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.visual.hide", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.selection", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.selection.create", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.selection.cancel", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.selection.info", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.selection.tutorial", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.whitelist", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.whitelist.add", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.whitelist.remove", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.whitelist.list", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.whitelist.clear", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.other", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.performance", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.blacklist.info", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.blacklist.add", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.blacklist.remove", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.blacklist.list", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.blacklist.toggle", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.blacklist.reload", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.config", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.config.reload", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.config.generate", false);

        return 1;
    }

    /**
     * : Validate Minecraft player name format (3-16 chars, alphanumeric + underscore).
     */
    private static boolean isValidPlayerName(String name) {
        if (name == null || name.length() < 3 || name.length() > 16) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) return false;
        }
        return true;
    }
}
