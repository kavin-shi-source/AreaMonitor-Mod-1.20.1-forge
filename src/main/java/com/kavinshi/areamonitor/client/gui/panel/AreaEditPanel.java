package com.kavinshi.areamonitor.client.gui.panel;

import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.client.gui.AreaManagementScreen;
import com.kavinshi.areamonitor.network.C2SAreaActionPacket;
import com.kavinshi.areamonitor.network.S2CAreaListPacket;
import com.kavinshi.areamonitor.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Full area edit panel.
 * Covers: display name, dimension, enter/leave mode, bounds (rectangle/circle), enabled toggle,
 * and quick links to Protection / Trigger / Whitelist / Restriction sub-panels (CLI fallback).
 */
public class AreaEditPanel extends Screen {

    private final AreaManagementScreen parentScreen;
    private final S2CAreaListPacket.AreaEntry entry;
    private EditBox displayNameInput;
    private EditBox bx1, bz1, bx2, bz2; // rectangle: minX,minZ,maxX,maxZ; circle: cx,cz (bx1,bz1), radius(bx2)
    private boolean enabled;
    private boolean rectangleMode = true; // true=rectangle, false=circle

    private static final List<String> DIMENSIONS = List.of(
        "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    private static final List<String> GAME_MODES = List.of(
        "survival", "creative", "adventure", "spectator");
    private static final String[] MODE_KEYS = {
        "gameMode.survival", "gameMode.creative", "gameMode.adventure", "gameMode.spectator"
    };

    private int dimIdx = 0;
    private int enterIdx = 0;
    private int leaveIdx = 0;

    public AreaEditPanel(AreaManagementScreen parent, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.edit_area") + ": " + entry.name()));
        this.parentScreen = parent;
        this.entry = entry;
        this.enabled = entry.enabled();
        this.rectangleMode = !"CIRCLE".equals(entry.boundsType());

        dimIdx = Math.max(0, DIMENSIONS.indexOf(entry.dimension()));
        enterIdx = Math.max(0, GAME_MODES.indexOf(entry.enterMode()));
        leaveIdx = Math.max(0, GAME_MODES.indexOf(entry.leaveMode()));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int y = 32;
        int lx = cx - 110; // label x
        int vx = cx - 45;  // value x
        int vw = 155;      // value width

        // === Section: Basic Info ===
        drawSection(guiGraphics(), y - 8, "Basic");

        // Display Name
        addStaticLabel(lx, y, "gui.display");
        displayNameInput = new EditBox(this.font, vx, y, vw, 20,
            Component.literal(LocalizationManager.translate("gui.area_name_hint")));
        displayNameInput.setMaxLength(48);
        displayNameInput.setValue(entry.displayName() != null ? entry.displayName() : entry.name());
        addRenderableWidget(displayNameInput);
        y += 26;

        // Dimension
        addStaticLabel(lx, y, "gui.dimension");
        addCycleButtons(vx, vw, y, dimIdx, DIMENSIONS, null,
            v -> { dimIdx = v; rebuild(); });
        y += 26;

        // Enter Mode
        addStaticLabel(lx, y, "gui.enter_mode");
        addCycleButtons(vx, vw, y, enterIdx,
            java.util.Arrays.stream(MODE_KEYS).map(LocalizationManager::translate).toList(),
            GAME_MODES,
            v -> { enterIdx = v; rebuild(); });
        y += 26;

        // Leave Mode
        addStaticLabel(lx, y, "gui.leave_mode");
        addCycleButtons(vx, vw, y, leaveIdx,
            java.util.Arrays.stream(MODE_KEYS).map(LocalizationManager::translate).toList(),
            GAME_MODES,
            v -> { leaveIdx = v; rebuild(); });
        y += 30;

        // === Section: Bounds ===
        drawSection(guiGraphics(), y - 8, "Bounds");
        y += 6;

        // Bounds type toggle
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate("gui.bounds") + ": " +
                LocalizationManager.translate(rectangleMode ? "gui.bounds_rectangle" : "gui.bounds_circle")),
            b -> { rectangleMode = !rectangleMode; rebuild(); })
            .pos(lx, y).size(vw + 65, 20).build());
        y += 26;

        if (rectangleMode) {
            addStaticLabel(lx, y, "gui.bounds_min");
            bx1 = addCoordInput(vx, y, "X"); bz1 = addCoordInput(vx + 70, y, "Z");
            addUsePosButton(vx + 140, y, () -> { bx1.setValue(fmt(getPlayerX())); bz1.setValue(fmt(getPlayerZ())); });
            y += 24;

            addStaticLabel(lx, y, "gui.bounds_max");
            bx2 = addCoordInput(vx, y, "X"); bz2 = addCoordInput(vx + 70, y, "Z");
            addUsePosButton(vx + 140, y, () -> { bx2.setValue(fmt(getPlayerX())); bz2.setValue(fmt(getPlayerZ())); });
            y += 28;
        } else {
            addStaticLabel(lx, y, "gui.bounds_center");
            bx1 = addCoordInput(vx, y, "X"); bz1 = addCoordInput(vx + 70, y, "Z");
            addUsePosButton(vx + 140, y, () -> { bx1.setValue(fmt(getPlayerX())); bz1.setValue(fmt(getPlayerZ())); });
            y += 24;

            addStaticLabel(lx, y, "gui.bounds_radius");
            bx2 = addCoordInput(vx, y, "R");
            addRenderableWidget(bx2);
            y += 28;
        }
        y += 2;

        // Enabled toggle
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate(enabled ? "area.enabled" : "area.disabled")),
            b -> { enabled = !enabled; rebuild(); })
            .pos(lx, y).size(vw + 65, 20).build());
        y += 32;

        // === Section: Other Settings ===
        drawSection(guiGraphics(), y - 8, "Other");
        y += 6;

        addQuickLink(lx, y, "gui.protection_settings",
            "/areamonitor protect " + entry.name() + " info");
        y += 24;
        addQuickLink(lx, y, "gui.trigger_settings",
            "/areamonitor trigger " + entry.name() + " enter info");
        y += 24;
        addQuickLink(lx, y, "gui.whitelist_settings",
            "/areamonitor whitelist list");
        y += 24;
        addQuickLink(lx, y, "gui.restriction_settings",
            "/areamonitor blacklist area " + entry.name() + " list");
        y += 36;

        // Save / Cancel
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate("gui.save")), b -> onSave())
            .pos(lx, y).size(75, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate("gui.cancel")), b -> onClose())
            .pos(lx + 85, y).size(75, 20).build());
    }

    // --- helper methods ---

    private void addStaticLabel(int x, int y, String key) {
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate(key) + ":"),
            b -> {}).pos(x, y).size(60, 20).build());
    }

    private void addCycleButtons(int x, int w, int y, int idx, List<String> options,
                                  List<String> rawValues, java.util.function.IntConsumer onChange) {
        List<String> display = rawValues != null ? rawValues : options;
        addRenderableWidget(Button.builder(Component.literal("\u25C0"),
            b -> { int v = (idx - 1 + options.size()) % options.size(); onChange.accept(v); })
            .pos(x, y).size(20, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal(display.get(idx)), b -> {})
            .pos(x + 23, y).size(w - 48, 20).build());
        addRenderableWidget(Button.builder(Component.literal("\u25B6"),
            b -> { int v = (idx + 1) % options.size(); onChange.accept(v); })
            .pos(x + w - 23, y).size(20, 20).build());
    }

    private EditBox addCoordInput(int x, int y, String hint) {
        EditBox box = new EditBox(this.font, x, y, 64, 18, Component.literal(hint));
        box.setMaxLength(9);
        box.setValue("0");
        addRenderableWidget(box);
        return box;
    }

    private void addUsePosButton(int x, int y, Runnable action) {
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate("gui.bounds_use_pos")),
            b -> action.run())
            .pos(x, y).size(50, 18).build());
    }

    private void addQuickLink(int x, int y, String labelKey, String command) {
        addRenderableWidget(Button.builder(
            Component.literal("▶ " + LocalizationManager.translate(labelKey)),
            b -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.connection.sendCommand(command);
                }
            })
            .pos(x, y).size(220, 20).build());
    }

    private void drawSection(GuiGraphics g, int y, String section) {
        // Rendered as text separator in render(), coordinate stored for render method
    }

    private double getPlayerX() { return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getX() : 0; }
    private double getPlayerZ() { return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getZ() : 0; }
    private String fmt(double v) { return String.valueOf((int) Math.floor(v)); }
    private GuiGraphics guiGraphics() { return null; }

    private void rebuild() {
        this.clearWidgets();
        init();
    }

    private void onSave() {
        var json = new com.google.gson.JsonObject();
        String dn = displayNameInput != null ? displayNameInput.getValue().trim() : "";
        if (!dn.isEmpty() && !dn.equals(entry.name())) {
            json.addProperty("displayName", dn);
        }
        json.addProperty("dimension", DIMENSIONS.get(dimIdx));
        json.addProperty("enterMode", GAME_MODES.get(enterIdx));
        json.addProperty("leaveMode", GAME_MODES.get(leaveIdx));
        json.addProperty("enabled", enabled);

        // Bounds
        json.addProperty("boundsType", rectangleMode ? "RECTANGLE" : "CIRCLE");
        try {
            if (rectangleMode && bx1 != null && bz1 != null && bx2 != null && bz2 != null) {
                json.addProperty("minX", Integer.parseInt(bx1.getValue()));
                json.addProperty("minZ", Integer.parseInt(bz1.getValue()));
                json.addProperty("maxX", Integer.parseInt(bx2.getValue()));
                json.addProperty("maxZ", Integer.parseInt(bz2.getValue()));
            } else if (!rectangleMode && bx1 != null && bz1 != null && bx2 != null) {
                json.addProperty("centerX", Integer.parseInt(bx1.getValue()));
                json.addProperty("centerZ", Integer.parseInt(bz1.getValue()));
                json.addProperty("radius", Integer.parseInt(bx2.getValue()));
            }
        } catch (NumberFormatException ignored) {}

        ModNetwork.sendToServer(new C2SAreaActionPacket(
            C2SAreaActionPacket.Action.UPDATE, entry.name(), json.toString()));
        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parentScreen);
            parentScreen.updateAfterEdit();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        // Section separators rendered as horizontal lines via text
        int cx = this.width / 2;

        guiGraphics.drawString(this.font, "── Basic ──", cx - 25, 40, 0x888888);
        guiGraphics.drawString(this.font, "── Bounds ──", cx - 28,
            32 + 26*4 + 6, 0x888888);
        guiGraphics.drawString(this.font, "── Other ──", cx - 25, 
            32 + 26*4 + 6 + 26 + 2 + 26*2 + 28 + 32 + 2, 0x888888);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
