package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.AreaMonitorMod;
import com.kavinshi.areamonitor.ConfigManager;
import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.MonitorArea;
import com.kavinshi.areamonitor.ProtectionSettings;
import com.kavinshi.areamonitor.TriggerConfig;
import com.kavinshi.areamonitor.model.RestrictionSettings;
import com.kavinshi.areamonitor.util.GameModeUtils;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
            } else if (area.getBounds() instanceof MonitorArea.PolygonBounds poly) {
                coordinates = String.format(LocalizationManager.translate("bounds.polygon"), poly.getVertices().size());
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
        } else if (area.getBounds() instanceof MonitorArea.PolygonBounds poly) {
            String coords = String.format(LocalizationManager.translate("bounds.polygon"), poly.getVertices().size());
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

    // ==== Export / Import / Clone ====

    public static int exportArea(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }
        var json = areaToJson(area);
        context.getSource().sendSystemMessage(
            Component.literal("§6=== Export: " + areaName + " ==="));
        context.getSource().sendSystemMessage(
            Component.literal(GSON.toJson(json)));
        return 1;
    }

    public static int importArea(String areaName, String jsonStr, CommandContext<CommandSourceStack> context) {
        if (AreaManager.getInstance().getArea(areaName) != null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.exists", areaName);
            return 0;
        }
        try {
            JsonObject obj = GSON.fromJson(jsonStr, JsonObject.class);
            MonitorArea area = new MonitorArea(areaName);
            applyJsonToArea(area, obj);
            AreaManager.getInstance().addArea(area);
            ConfigManager.saveAreasConfig();
            context.getSource().sendSystemMessage(
                Component.literal("§a✓ Area '" + areaName + "' imported successfully"));
        } catch (Exception e) {
            context.getSource().sendSystemMessage(
                Component.literal("§cFailed to import area: " + e.getMessage()));
            return 0;
        }
        return 1;
    }

    public static int cloneArea(String srcName, String targetName, CommandContext<CommandSourceStack> context) {
        MonitorArea src = AreaManager.getInstance().getArea(srcName);
        if (src == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", srcName);
            return 0;
        }
        if (AreaManager.getInstance().getArea(targetName) != null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.exists", targetName);
            return 0;
        }
        MonitorArea clone = new MonitorArea(targetName);
        clone.setDisplayName(targetName);
        clone.setDimension(src.getDimension());
        clone.setBounds(src.getBounds());
        clone.setEnterMode(src.getEnterMode());
        clone.setLeaveMode(src.getLeaveMode());
        clone.setEnabled(src.isEnabled());
        clone.setProtection(copyProtection(src.getProtection()));
        clone.setEnterTrigger(copyTrigger(src.getEnterTrigger()));
        clone.setLeaveTrigger(copyTrigger(src.getLeaveTrigger()));
        clone.setRestrictions(copyRestrictions(src.getRestrictions()));
        clone.setScheduleEnabled(src.isScheduleEnabled());
        clone.setScheduleTimeMin(src.getScheduleTimeMin());
        clone.setScheduleTimeMax(src.getScheduleTimeMax());
        clone.setScheduleWasDisabledBySchedule(src.isScheduleWasDisabledBySchedule());
        AreaManager.getInstance().addArea(clone);
        ConfigManager.saveAreasConfig();
        context.getSource().sendSystemMessage(
            Component.literal("§a✓ Cloned '" + srcName + "' → '" + targetName + "'"));
        return 1;
    }

    private static JsonObject areaToJson(MonitorArea area) {
        var j = new JsonObject();
        j.addProperty("displayName", area.getDisplayName());
        j.addProperty("dimension", area.getDimension());
        j.addProperty("enterMode", area.getEnterMode().getName());
        j.addProperty("leaveMode", area.getLeaveMode().getName());
        j.addProperty("enabled", area.isEnabled());
        // bounds
        if (area.getBounds() instanceof MonitorArea.RectangleBounds r) {
            j.addProperty("boundsType", "RECTANGLE");
            j.addProperty("minX", r.getMinX()); j.addProperty("minZ", r.getMinZ());
            j.addProperty("maxX", r.getMaxX()); j.addProperty("maxZ", r.getMaxZ());
        } else if (area.getBounds() instanceof MonitorArea.CircleBounds c) {
            j.addProperty("boundsType", "CIRCLE");
            j.addProperty("centerX", c.getCenterX()); j.addProperty("centerZ", c.getCenterZ());
            j.addProperty("radius", c.getRadius());
        }
        j.add("protection", GSON.toJsonTree(area.getProtection()));
        if (area.hasEnterTrigger()) j.add("enterTrigger", GSON.toJsonTree(area.getEnterTrigger()));
        if (area.hasLeaveTrigger()) j.add("leaveTrigger", GSON.toJsonTree(area.getLeaveTrigger()));
        j.add("whitelist", GSON.toJsonTree(area.getWhitelist()));
        j.add("restrictions", GSON.toJsonTree(area.getRestrictions()));
        j.add("protectionWhitelist", GSON.toJsonTree(area.getProtectionWhitelist()));
        if (area.isScheduleEnabled()) {
            var sched = new JsonObject();
            sched.addProperty("enabled", true);
            if (area.getScheduleTimeMin() != null) sched.addProperty("timeMin", area.getScheduleTimeMin());
            if (area.getScheduleTimeMax() != null) sched.addProperty("timeMax", area.getScheduleTimeMax());
            j.add("schedule", sched);
        }
        return j;
    }

    private static void applyJsonToArea(MonitorArea area, JsonObject obj) {
        if (obj.has("displayName")) area.setDisplayName(obj.get("displayName").getAsString());
        if (obj.has("dimension")) area.setDimension(obj.get("dimension").getAsString());
        if (obj.has("enterMode")) area.setEnterMode(GameType.byName(obj.get("enterMode").getAsString()));
        if (obj.has("leaveMode")) area.setLeaveMode(GameType.byName(obj.get("leaveMode").getAsString()));
        if (obj.has("enabled")) area.setEnabled(obj.get("enabled").getAsBoolean());
        if (obj.has("boundsType")) {
            String t = obj.get("boundsType").getAsString();
            if ("RECTANGLE".equals(t) && obj.has("minX"))
                area.setBounds(new MonitorArea.RectangleBounds(obj.get("minX").getAsInt(), obj.get("minZ").getAsInt(), obj.get("maxX").getAsInt(), obj.get("maxZ").getAsInt()));
            else if ("CIRCLE".equals(t) && obj.has("centerX"))
                area.setBounds(new MonitorArea.CircleBounds(obj.get("centerX").getAsInt(), obj.get("centerZ").getAsInt(), obj.get("radius").getAsInt()));
        }
        if (obj.has("protection")) area.setProtection(GSON.fromJson(obj.get("protection"), ProtectionSettings.class));
        if (obj.has("enterTrigger")) area.setEnterTrigger(GSON.fromJson(obj.get("enterTrigger"), TriggerConfig.class));
        if (obj.has("leaveTrigger")) area.setLeaveTrigger(GSON.fromJson(obj.get("leaveTrigger"), TriggerConfig.class));
        if (obj.has("whitelist") && obj.get("whitelist").isJsonArray()) {
            area.getWhitelist().clear();
            for (var e : obj.getAsJsonArray("whitelist")) area.getWhitelist().add(e.getAsString().toLowerCase());
        }
        if (obj.has("restrictions")) area.setRestrictions(GSON.fromJson(obj.get("restrictions"), RestrictionSettings.class));
        if (obj.has("protectionWhitelist") && obj.get("protectionWhitelist").isJsonArray()) {
            for (var e : obj.getAsJsonArray("protectionWhitelist")) area.getProtectionWhitelist().add(e.getAsString().toLowerCase());
        }
        if (obj.has("schedule")) {
            JsonObject schedObj = obj.getAsJsonObject("schedule");
            area.setScheduleEnabled(schedObj.has("enabled") && schedObj.get("enabled").getAsBoolean());
            if (schedObj.has("timeMin")) area.setScheduleTimeMin(schedObj.get("timeMin").getAsInt());
            if (schedObj.has("timeMax")) area.setScheduleTimeMax(schedObj.get("timeMax").getAsInt());
        }
    }

    private static ProtectionSettings copyProtection(ProtectionSettings src) {
        var p = new ProtectionSettings();
        p.setBlockBreak(src.isBlockBreak()); p.setBlockPlace(src.isBlockPlace());
        p.setBlockInteract(src.isBlockInteract()); p.setPvp(src.isPvp());
        p.setExplosion(src.isExplosion()); p.setEntityDamage(src.isEntityDamage());
        p.setContainerInteract(src.isContainerInteract()); p.setFluidPlace(src.isFluidPlace());
        p.setItemDrop(src.isItemDrop());
        return p;
    }

    private static TriggerConfig copyTrigger(TriggerConfig src) {
        if (src == null || !src.hasAnyAction()) return null;
        return GSON.fromJson(GSON.toJsonTree(src), TriggerConfig.class);
    }

    private static RestrictionSettings copyRestrictions(RestrictionSettings src) {
        return GSON.fromJson(GSON.toJsonTree(src), RestrictionSettings.class);
    }
}
