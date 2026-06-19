package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.AreaMonitorMod;
import com.kavinshi.areamonitor.ConfigManager;
import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.MonitorArea;
import com.kavinshi.areamonitor.util.GameModeUtils;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Area management commands: create, delete, list, toggle, info, set modes.
 */
public class AreaCommands {
    public static final List<String> GAME_MODES = List.of("survival", "creative", "adventure", "spectator");

    private AreaCommands() {}

    // ---- Area name suggestions ----

    public static CompletableFuture<Suggestions> suggestAreaNames(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (String areaName : AreaManager.getInstance().getAreaNames()) {
            if (areaName.startsWith(builder.getRemaining().toLowerCase())) {
                builder.suggest(areaName);
            }
        }
        return builder.buildFuture();
    }

    // ---- Area create ----

    public static int createArea(String areaName, CommandContext<CommandSourceStack> context) {
        if (AreaManager.getInstance().getArea(areaName) != null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.exists", areaName);
            return 0;
        }

        MonitorArea area = new MonitorArea(areaName);
        AreaManager.getInstance().addArea(area);
        ConfigManager.saveAreasConfig();

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.area.created", true, areaName);
        return 1;
    }

    // ---- Area delete ----

    public static int deleteArea(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        AreaManager.getInstance().removeArea(areaName);
        ConfigManager.saveAreasConfig();

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.area.deleted", true, areaName);
        return 1;
    }

    // ---- Area list ----

    public static int listAreas(CommandContext<CommandSourceStack> context) {
        Set<String> areaNames = AreaManager.getInstance().getAreaNames();

        if (areaNames.isEmpty()) {
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.area.list.empty", false);
            return 1;
        }

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.area.list.header", false);

        int index = 1;
        for (String areaName : areaNames) {
            MonitorArea area = AreaManager.getInstance().getArea(areaName);
            if (area == null) continue;

            String status = area.isEnabled() ? "area.enabled" : "area.disabled";
            String coordinates = "Not set";

            if (area.getBounds() instanceof MonitorArea.RectangleBounds rect) {
                coordinates = String.format("X[%d ~ %d], Z[%d ~ %d]",
                    rect.getMinX(), rect.getMaxX(), rect.getMinZ(), rect.getMaxZ());
            } else if (area.getBounds() instanceof MonitorArea.CircleBounds circle) {
                coordinates = String.format("Center(%d, %d), Radius %d",
                    circle.getCenterX(), circle.getCenterZ(), circle.getRadius());
            }

            final int currentIndex = index++;
            final String finalCoordinates = coordinates;
            final String finalStatus = status;

            context.getSource().sendSuccess(
                () -> MessageUtils.smartComponent(context.getSource(), "area.status",
                    String.format("§e%d. §f%s", currentIndex, area.getDisplayName()) + " " + finalStatus),
                false
            );

            MessageUtils.sendSuccess(context.getSource(), "area.coordinates_format", false, finalCoordinates);
            MessageUtils.sendSuccess(context.getSource(), "area.dimension", false, area.getDimension());
            MessageUtils.sendSuccess(context.getSource(), "area.enter_mode", false, area.getEnterMode().getName());
            MessageUtils.sendSuccess(context.getSource(), "area.leave_mode", false, area.getLeaveMode().getName());

            context.getSource().sendSuccess(
                () -> Component.literal("   §7────────────────"),
                false
            );
        }

        MessageUtils.sendSuccess(context.getSource(), "area.count", false, areaNames.size());

        return 1;
    }

    // ---- Area toggle ----

    public static int toggleArea(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        boolean currentState = area.isEnabled();
        area.setEnabled(!currentState);
        ConfigManager.saveAreasConfig();

        String newState = !currentState ? "area.enabled" : "area.disabled";
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.area.toggled", true, area.getDisplayName(), newState);

        return 1;
    }

    // ---- Area info ----

    public static int showAreaInfo(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.area.info.header", false, area.getDisplayName());

        String statusKey = area.isEnabled() ? "area.enabled" : "area.disabled";
        String status = LocalizationManager.translate(statusKey);
        MessageUtils.sendSuccess(context.getSource(), "area.status", false, status);
        MessageUtils.sendSuccess(context.getSource(), "area.dimension", false, area.getDimension());

        if (area.getBounds() instanceof MonitorArea.RectangleBounds rect) {
            String coords = String.format("X[%d ~ %d], Z[%d ~ %d]",
                rect.getMinX(), rect.getMaxX(), rect.getMinZ(), rect.getMaxZ());
            MessageUtils.sendSuccess(context.getSource(), "area.coordinates_format", false, coords);
        } else if (area.getBounds() instanceof MonitorArea.CircleBounds circle) {
            String coords = String.format("Center(%d, %d), Radius %d",
                circle.getCenterX(), circle.getCenterZ(), circle.getRadius());
            MessageUtils.sendSuccess(context.getSource(), "area.coordinates_format", false, coords);
        }

        String enterMode = LocalizationManager.getGameModeDisplayName(area.getEnterMode());
        String leaveMode = LocalizationManager.getGameModeDisplayName(area.getLeaveMode());
        MessageUtils.sendSuccess(context.getSource(), "area.enter_mode", false, enterMode);
        MessageUtils.sendSuccess(context.getSource(), "area.leave_mode", false, leaveMode);

        return 1;
    }

    // ---- Area setEnterMode ----

    public static int setAreaEnterMode(String areaName, String mode, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        String modeLower = mode.toLowerCase();
        if (!GAME_MODES.contains(modeLower)) {
            MessageUtils.sendFailure(context.getSource(), "area.invalid_gamemode", mode, String.join(", ", GAME_MODES));
            return 0;
        }

        GameType gameMode = GameModeUtils.fromName(modeLower);

        area.setEnterMode(gameMode);
        ConfigManager.saveAreasConfig();

        MessageUtils.sendSuccess(context.getSource(), "area.enter_mode_set", true, areaName, modeLower);

        return 1;
    }

    // ---- Area setLeaveMode ----

    public static int setAreaLeaveMode(String areaName, String mode, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        String modeLower = mode.toLowerCase();
        if (!GAME_MODES.contains(modeLower)) {
            MessageUtils.sendFailure(context.getSource(), "area.invalid_gamemode", mode, String.join(", ", GAME_MODES));
            return 0;
        }

        GameType gameMode = GameModeUtils.fromName(modeLower);

        area.setLeaveMode(gameMode);
        ConfigManager.saveAreasConfig();

        MessageUtils.sendSuccess(context.getSource(), "area.leave_mode_set", true, areaName, modeLower);

        return 1;
    }
}
