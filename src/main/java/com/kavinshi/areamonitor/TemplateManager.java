package com.kavinshi.areamonitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kavinshi.areamonitor.util.GameModeUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Loads and manages area templates from bundled resources and user config.
 * Templates define preset configurations (game modes, protection, triggers) for quick area creation.
 */
public class TemplateManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BUILTIN_TEMPLATES_DIR = "/assets/areamonitor/templates/";

    private TemplateManager() {}

    /**
     * Load all available templates (built-in + user custom).
     */
    public static List<TemplateData> loadAllTemplates() {
        List<TemplateData> templates = new ArrayList<>();

        // Load built-in templates from resources
        String[] builtinNames = {"pvp_arena", "creative_zone", "adventure_zone"};
        for (String name : builtinNames) {
            TemplateData td = loadBuiltinTemplate(name);
            if (td != null) templates.add(td);
        }

        return templates;
    }

    /**
     * Load a built-in template by name from the jar resources.
     */
    private static TemplateData loadBuiltinTemplate(String name) {
        String resourcePath = BUILTIN_TEMPLATES_DIR + name + ".json";
        try (InputStream is = TemplateManager.class.getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return GSON.fromJson(content, TemplateData.class);
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("Failed to load built-in template: {}", name, e);
            return null;
        }
    }

    /**
     * Load a specific template by name.
     */
    public static TemplateData loadTemplate(String name) {
        List<TemplateData> all = loadAllTemplates();
        for (TemplateData td : all) {
            if (td.name.equalsIgnoreCase(name)) return td;
        }
        return null;
    }

    /**
     * Create an area from a template using the player's current selection.
     */
    public static void createFromTemplate(String templateName, String areaName,
                                           net.minecraft.server.level.ServerPlayer player) {
        TemplateData template = loadTemplate(templateName);
        if (template == null) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§cTemplate not found: " + templateName), false);
            return;
        }

        // Get current selection
        SelectionPoints selection = SelectionTool.getPlayerSelection(player.getUUID());
        if (selection == null) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§cNo active selection. Use the selection tool first."), false);
            return;
        }

        if (!selection.isComplete() && !selection.hasEnoughVerticesForPolygon()) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§cSelection not complete. Please select an area first."), false);
            return;
        }

        String currentDimension = player.level().dimension().location().toString();

        // Create area
        MonitorArea area = new MonitorArea(areaName);
        area.setDisplayName(template.displayName != null ? template.displayName : areaName);
        area.setDimension(currentDimension);

        // Set bounds from selection
        if (selection.isMultiPointMode()) {
            List<MonitorArea.Vec2i> vecList = new ArrayList<>();
            for (net.minecraft.core.BlockPos v : selection.getVertexPoints()) {
                vecList.add(new MonitorArea.Vec2i(v.getX(), v.getZ()));
            }
            area.setBounds(new MonitorArea.PolygonBounds(vecList));
        } else {
            net.minecraft.core.BlockPos pos1 = selection.getFirstPoint();
            net.minecraft.core.BlockPos pos2 = selection.getSecondPoint();
            area.setBounds(new MonitorArea.RectangleBounds(
                pos1.getX(), pos1.getZ(), pos2.getX(), pos2.getZ()));
        }

        // Apply template config
        area.setEnterMode(GameModeUtils.fromName(template.enterMode));
        area.setLeaveMode(GameModeUtils.fromName(template.leaveMode));

        if (template.protection != null) {
            area.setProtection(template.protection);
        }
        if (template.trigger != null && template.trigger.enter != null) {
            area.setEnterTrigger(template.trigger.enter);
        }
        if (template.trigger != null && template.trigger.leave != null) {
            area.setLeaveTrigger(template.trigger.leave);
        }

        AreaManager.getInstance().addArea(area);
        ConfigManager.saveAreasConfig();

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§a✓ Area '" + areaName + "' created from template '" + templateName + "'"),
            false);
    }

    /**
     * Template data DTO matching the JSON structure.
     */
    public static class TemplateData {
        public String name;
        public String displayName;
        public String enterMode = "adventure";
        public String leaveMode = "survival";
        public ProtectionSettings protection;
        public TemplateTriggerData trigger;

        public static class TemplateTriggerData {
            public TriggerConfig enter;
            public TriggerConfig leave;
        }
    }
}
