package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.AreaMonitorMod;
import com.kavinshi.areamonitor.ConfigManager;
import com.kavinshi.areamonitor.ItemBlacklistManager;
import com.kavinshi.areamonitor.SelectionTool;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Selection tool and config management commands.
 */
public class SelectionCommands {

    private SelectionCommands() {}

    // ---- Create area from selection ----

    public static int createAreaFromSelection(String areaName, CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        SelectionTool.createAreaFromSelection(player, areaName);
        return 1;
    }

    // ---- Cancel selection ----

    public static int cancelSelection(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        SelectionTool.cancelSelection(player);
        return 1;
    }

    // ---- Show selection info ----

    public static int showSelectionInfo(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        SelectionTool.showCurrentSelection(player);
        return 1;
    }

    // ---- Show tutorial ----

    public static int showTutorial(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        showTutorialMessage(player);
        return 1;
    }

    public static void showTutorialMessage(ServerPlayer player) {
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.tutorial.header").withStyle(ChatFormatting.BOLD),
            false
        );

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step1"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step1.command"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step1.description"), false);

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step2"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step2.action"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step2.feedback"), false);

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step3"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step3.action"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step3.feedback"), false);

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step4"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step4.command"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step4.example"), false);

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step5"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.set_enter_mode"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.set_leave_mode"), false);

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.other_commands"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.view_area"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.show_boundary"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.cancel_selection"), false);

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.end"), false);
    }

    // ---- Polygon selection commands ----

    public static int startPolygonSelection(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }
        SelectionTool.startPolygonMode(player);
        return 1;
    }

    public static int finishPolygonSelection(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }
        SelectionTool.finishPolygon(player);
        return 1;
    }

    // ---- Reload configs ----

    public static int reloadConfigs(CommandContext<CommandSourceStack> context) {
        try {
            ConfigManager.loadAreasConfig();
            ItemBlacklistManager.loadBlacklistConfig();
            ConfigManager.validateConfig();

            MessageUtils.sendSuccess(context.getSource(), "config.reloaded", true);
        } catch (Exception e) {
            MessageUtils.sendFailure(context.getSource(), "config.reload_failed", e.getMessage());
        }
        return 1;
    }

    // ---- Generate configs ----

    public static int generateConfigs(CommandContext<CommandSourceStack> context) {
        try {
            ConfigManager.ensureConfigFiles();

            MessageUtils.sendSuccess(context.getSource(), "config.generated", true);
            MessageUtils.sendSuccess(context.getSource(), "config.path_info", false);
            MessageUtils.sendSuccess(context.getSource(), "config.areas_path", false);
            MessageUtils.sendSuccess(context.getSource(), "config.blacklist_path", false);

        } catch (Exception e) {
            MessageUtils.sendFailure(context.getSource(), "config.regenerate_failed", e.getMessage());
        }
        return 1;
    }
}
