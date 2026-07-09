package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.AreaMonitorMod;
import com.kavinshi.areamonitor.ConfigManager;
import com.kavinshi.areamonitor.MonitorArea;
import com.kavinshi.areamonitor.ProtectionSettings;
import com.kavinshi.areamonitor.TriggerConfig;
import com.kavinshi.areamonitor.model.RestrictionSettings;
import com.kavinshi.areamonitor.util.AuditLogger;
import com.kavinshi.areamonitor.util.DimensionUtils;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

public class AreaExportCommands {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AreaExportCommands() {}

    public static int exportArea(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName);
        if (area == null) return 0;
        var json = areaToJson(area);
        String jsonStr = GSON.toJson(json);
        context.getSource().sendSystemMessage(
            Component.translatable("area.export.header", areaName));
        try {
            Path exportDir = FMLPaths.CONFIGDIR.get().resolve("areamonitor").resolve("exports");
            Files.createDirectories(exportDir);
            Path exportFile = exportDir.resolve(areaName + ".json");
            Files.writeString(exportFile, jsonStr);
            context.getSource().sendSystemMessage(
                Component.translatable("area.export.file_saved", exportFile.toString()));
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.warn("Failed to write export file, falling back to chat", e);
            context.getSource().sendSystemMessage(
                Component.literal(jsonStr));
        }
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
            if (!ConfigManager.safeSaveConfig()) {
                AreaManager.getInstance().removeArea(areaName);
                context.getSource().sendSystemMessage(
                    Component.translatable("area.import.persist_failed", areaName));
                return 0;
            }
            context.getSource().sendSystemMessage(
                Component.translatable("area.import.success", areaName));
            AuditLogger.log(context.getSource(), "AREA_IMPORT", areaName);
        } catch (Exception e) {
            AreaManager.getInstance().removeArea(areaName);
            context.getSource().sendSystemMessage(
                Component.translatable("area.import.failed", e.getMessage()));
            return 0;
        }
        return 1;
    }

    private static JsonObject areaToJson(MonitorArea area) {
        var areaJson = new JsonObject();
        areaJson.addProperty("displayName", area.getDisplayName());
        areaJson.addProperty("dimension", area.getDimension());
        areaJson.addProperty("enterMode", area.getEnterMode().getName());
        areaJson.addProperty("leaveMode", area.getLeaveMode().getName());
        areaJson.addProperty("enabled", area.isEnabled());
        if (area.getBounds() instanceof MonitorArea.RectangleBounds r) {
            areaJson.addProperty("boundsType", "RECTANGLE");
            areaJson.addProperty("minX", r.getMinX()); areaJson.addProperty("minZ", r.getMinZ());
            areaJson.addProperty("maxX", r.getMaxX()); areaJson.addProperty("maxZ", r.getMaxZ());
        } else if (area.getBounds() instanceof MonitorArea.CircleBounds c) {
            areaJson.addProperty("boundsType", "CIRCLE");
            areaJson.addProperty("centerX", c.getCenterX()); areaJson.addProperty("centerZ", c.getCenterZ());
            areaJson.addProperty("radius", c.getRadius());
        } else if (area.getBounds() instanceof MonitorArea.PolygonBounds polygon) {
            areaJson.addProperty("boundsType", "POLYGON");
            var arr = new com.google.gson.JsonArray();
            for (MonitorArea.Vec2i v : polygon.getVertices()) {
                var pt = new com.google.gson.JsonArray();
                pt.add(v.x()); pt.add(v.z());
                arr.add(pt);
            }
            areaJson.add("vertices", arr);
        }
        areaJson.add("protection", GSON.toJsonTree(area.getProtection()));
        if (area.hasEnterTrigger()) areaJson.add("enterTrigger", GSON.toJsonTree(area.getEnterTrigger()));
        if (area.hasLeaveTrigger()) areaJson.add("leaveTrigger", GSON.toJsonTree(area.getLeaveTrigger()));
        areaJson.add("whitelist", GSON.toJsonTree(area.getWhitelist()));
        areaJson.add("restrictions", GSON.toJsonTree(area.getRestrictions()));
        areaJson.add("protectionWhitelist", GSON.toJsonTree(area.getProtectionWhitelist()));
        var scheduleJson = new JsonObject();
        scheduleJson.addProperty("enabled", area.isScheduleEnabled());
        if (area.getScheduleTimeMin() != null) scheduleJson.addProperty("timeMin", area.getScheduleTimeMin());
        if (area.getScheduleTimeMax() != null) scheduleJson.addProperty("timeMax", area.getScheduleTimeMax());
        areaJson.add("schedule", scheduleJson);
        var conditionJson = new JsonObject();
        conditionJson.addProperty("enabled", area.isConditionEnabled());
        if (area.getConditionMinPlayers() != null) conditionJson.addProperty("minPlayers", area.getConditionMinPlayers());
        if (area.getConditionRequirePlayer() != null) conditionJson.addProperty("requirePlayer", area.getConditionRequirePlayer());
        areaJson.add("condition", conditionJson);
        if (area.getChainNext() != null) {
            areaJson.addProperty("chainNext", area.getChainNext());
        }
        return areaJson;
    }

    private static void applyJsonToArea(MonitorArea area, JsonObject obj) {
        if (obj.has("displayName")) area.setDisplayName(obj.get("displayName").getAsString());
        if (obj.has("dimension") && !obj.get("dimension").isJsonNull()) {
            String dim = obj.get("dimension").getAsString();
            if (DimensionUtils.isValidDimension(dim)) {
                area.setDimension(dim);
            } else {
                AreaMonitorMod.LOGGER.warn("Skipping invalid dimension '{}' in import/apply", dim);
            }
        }
        if (obj.has("enterMode")) {
            GameType mode = GameType.byName(obj.get("enterMode").getAsString());
            if (mode != null) area.setEnterMode(mode);
            else AreaMonitorMod.LOGGER.warn("Skipping invalid enterMode '{}' in import", obj.get("enterMode").getAsString());
        }
        if (obj.has("leaveMode")) {
            GameType mode = GameType.byName(obj.get("leaveMode").getAsString());
            if (mode != null) area.setLeaveMode(mode);
            else AreaMonitorMod.LOGGER.warn("Skipping invalid leaveMode '{}' in import", obj.get("leaveMode").getAsString());
        }
        if (obj.has("enabled")) area.setEnabled(obj.get("enabled").getAsBoolean());
        if (obj.has("boundsType")) {
            String t = obj.get("boundsType").getAsString();
            if ("RECTANGLE".equals(t) && obj.has("minX"))
                area.setBounds(new MonitorArea.RectangleBounds(obj.get("minX").getAsInt(), obj.get("minZ").getAsInt(), obj.get("maxX").getAsInt(), obj.get("maxZ").getAsInt()));
            else if ("CIRCLE".equals(t) && obj.has("centerX"))
                area.setBounds(new MonitorArea.CircleBounds(obj.get("centerX").getAsInt(), obj.get("centerZ").getAsInt(), obj.get("radius").getAsInt()));
            else if ("POLYGON".equals(t) && obj.has("vertices") && obj.get("vertices").isJsonArray()) {
                java.util.List<MonitorArea.Vec2i> verts = new java.util.ArrayList<>();
                for (var e : obj.getAsJsonArray("vertices")) {
                    if (e.isJsonArray()) {
                        var pt = e.getAsJsonArray();
                        if (pt.size() >= 2) verts.add(new MonitorArea.Vec2i(pt.get(0).getAsInt(), pt.get(1).getAsInt()));
                    }
                }
                if (verts.size() >= 3) area.setBounds(new MonitorArea.PolygonBounds(verts));
            }
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
            area.getProtectionWhitelist().clear();
            for (var e : obj.getAsJsonArray("protectionWhitelist")) area.getProtectionWhitelist().add(e.getAsString().toLowerCase());
        }
        if (obj.has("schedule")) {
            JsonObject schedObj = obj.getAsJsonObject("schedule");
            area.setScheduleEnabled(schedObj.has("enabled") && schedObj.get("enabled").getAsBoolean());
            if (schedObj.has("timeMin")) area.setScheduleTimeMin(schedObj.get("timeMin").getAsInt());
            if (schedObj.has("timeMax")) area.setScheduleTimeMax(schedObj.get("timeMax").getAsInt());
        }
        if (obj.has("condition")) {
            JsonObject condObj = obj.getAsJsonObject("condition");
            area.setConditionEnabled(condObj.has("enabled") && condObj.get("enabled").getAsBoolean());
            if (condObj.has("minPlayers")) area.setConditionMinPlayers(condObj.get("minPlayers").getAsInt());
            if (condObj.has("requirePlayer")) area.setConditionRequirePlayer(condObj.get("requirePlayer").getAsString().toLowerCase());
        }
        if (obj.has("chainNext")) {
            area.setChainNext(obj.get("chainNext").isJsonNull() ? null : obj.get("chainNext").getAsString());
        }
    }
}
