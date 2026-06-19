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

public class AreaEditPanel extends Screen {

    private final AreaManagementScreen parentScreen;
    private final S2CAreaListPacket.AreaEntry entry;
    private EditBox displayNameInput;
    private EditBox bx1, bz1, bx2, bz2;
    private boolean enabled;
    private boolean rectangleMode = true;

    // Protection toggles
    private boolean protBreak, protPlace, protInteract, protPvp, protExplosion, protDamage;

    private static final List<String> DIMENSIONS = List.of(
        "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    private static final List<String> GAME_MODES = List.of(
        "survival", "creative", "adventure", "spectator");
    private static final String[] MODE_KEYS = {
        "gameMode.survival", "gameMode.creative", "gameMode.adventure", "gameMode.spectator"
    };

    private int dimIdx = 0, enterIdx = 0, leaveIdx = 0;

    public AreaEditPanel(AreaManagementScreen parent, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.edit_area") + ": " + entry.name()));
        this.parentScreen = parent;
        this.entry = entry;
        this.enabled = entry.enabled();
        this.rectangleMode = !"CIRCLE".equals(entry.boundsType());
        this.protBreak = entry.protBlockBreak();
        this.protPlace = entry.protBlockPlace();
        this.protInteract = entry.protBlockInteract();
        this.protPvp = entry.protPvp();
        this.protExplosion = entry.protExplosion();
        this.protDamage = entry.protEntityDamage();

        dimIdx = Math.max(0, DIMENSIONS.indexOf(entry.dimension()));
        enterIdx = Math.max(0, GAME_MODES.indexOf(entry.enterMode()));
        leaveIdx = Math.max(0, GAME_MODES.indexOf(entry.leaveMode()));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int lx = cx - 120;
        int vx = cx - 55;
        int vw = 175;
        int y = 30;

        // ===== Section 1: Basic Info =====
        sectionY = y - 6;
        addLabel(lx, y, "gui.display");
        displayNameInput = addTextBox(vx, y, vw, entry.displayName() != null ? entry.displayName() : entry.name());
        y += 24;

        addLabel(lx, y, "gui.dimension");
        addCycle(lx, vx, vw, y, dimIdx, DIMENSIONS, v -> { dimIdx = v; rebuild(); });
        y += 24;

        addLabel(lx, y, "gui.enter_mode");
        addCycle(lx, vx, vw, y, enterIdx,
            java.util.Arrays.stream(MODE_KEYS).map(LocalizationManager::translate).toList(),
            v -> { enterIdx = v; rebuild(); });
        y += 24;

        addLabel(lx, y, "gui.leave_mode");
        addCycle(lx, vx, vw, y, leaveIdx,
            java.util.Arrays.stream(MODE_KEYS).map(LocalizationManager::translate).toList(),
            v -> { leaveIdx = v; rebuild(); });
        y += 28;

        // ===== Section 2: Bounds =====
        boundsSectionY = y - 6;

        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate("gui.bounds") + ": " +
                LocalizationManager.translate(rectangleMode ? "gui.bounds_rectangle" : "gui.bounds_circle")),
            b -> { rectangleMode = !rectangleMode; rebuild(); })
            .pos(lx, y).size(vw + 55, 20).build());
        y += 24;

        if (rectangleMode) {
            addLabel(lx, y, "gui.bounds_min");
            bx1 = addCoord(vx, y); bz1 = addCoord(vx + 70, y);
            addPosBtn(vx + 140, y, () -> { bx1.setValue(fmt(getPX())); bz1.setValue(fmt(getPZ())); });
            y += 22;
            addLabel(lx, y, "gui.bounds_max");
            bx2 = addCoord(vx, y); bz2 = addCoord(vx + 70, y);
            addPosBtn(vx + 140, y, () -> { bx2.setValue(fmt(getPX())); bz2.setValue(fmt(getPZ())); });
        } else {
            addLabel(lx, y, "gui.bounds_center");
            bx1 = addCoord(vx, y); bz1 = addCoord(vx + 70, y);
            addPosBtn(vx + 140, y, () -> { bx1.setValue(fmt(getPX())); bz1.setValue(fmt(getPZ())); });
            y += 22;
            addLabel(lx, y, "gui.bounds_radius");
            bx2 = addCoord(vx, y); addRenderableWidget(bx2);
        }
        y += 28;

        // ===== Section 3: Protection =====
        protectionSectionY = y - 6;
        protY = y;

        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate("gui.prot_enable_all")),
            b -> { setAllProt(true); rebuild(); })
            .pos(lx, y).size(70, 18).build());
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate("gui.prot_disable_all")),
            b -> { setAllProt(false); rebuild(); })
            .pos(lx + 78, y).size(70, 18).build());
        y += 22;

        // 6 protection toggles in 2 columns
        addProtToggle(lx, y, "gui.prot_block_break", protBreak, v -> protBreak = v);
        addProtToggle(lx + 110, y, "gui.prot_pvp", protPvp, v -> protPvp = v);
        y += 18;
        addProtToggle(lx, y, "gui.prot_block_place", protPlace, v -> protPlace = v);
        addProtToggle(lx + 110, y, "gui.prot_explosion", protExplosion, v -> protExplosion = v);
        y += 18;
        addProtToggle(lx, y, "gui.prot_block_interact", protInteract, v -> protInteract = v);
        addProtToggle(lx + 110, y, "gui.prot_entity_damage", protDamage, v -> protDamage = v);
        y += 28;

        // Enabled toggle
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate(enabled ? "area.enabled" : "area.disabled")),
            b -> { enabled = !enabled; rebuild(); })
            .pos(lx, y).size(vw + 55, 20).build());
        y += 32;

        // ===== Section 4: Other =====
        otherSectionY = y - 6;

        addQuick(lx, y, "gui.protection_settings", "areamonitor protect " + entry.name() + " info"); y += 22;
        addQuick(lx, y, "gui.trigger_settings", "areamonitor trigger " + entry.name() + " enter info"); y += 22;
        addQuick(lx, y, "gui.whitelist_settings", "areamonitor whitelist list"); y += 22;
        addQuick(lx, y, "gui.restriction_settings", "areamonitor blacklist area " + entry.name() + " list");
        y += 34;

        // Save / Cancel
        addRenderableWidget(Button.builder(
            Component.literal("[" + LocalizationManager.translate("gui.save") + "]").withStyle(ChatFormatting.GREEN),
            b -> onSave()).pos(lx, y).size(70, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("[" + LocalizationManager.translate("gui.cancel") + "]").withStyle(ChatFormatting.GRAY),
            b -> onClose()).pos(lx + 80, y).size(70, 20).build());
    }

    // ===== Layout state for render sections =====
    private int sectionY, boundsSectionY, protectionSectionY, otherSectionY, protY;

    // ===== Widget helpers =====

    private void addLabel(int x, int y, String key) {
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate(key)).withStyle(ChatFormatting.GRAY),
            b -> {}).pos(x, y).size(60, 20).build());
    }

    private EditBox addTextBox(int x, int y, int w, String val) {
        EditBox box = new EditBox(this.font, x, y, w, 18, Component.empty());
        box.setMaxLength(48);
        box.setValue(val);
        addRenderableWidget(box);
        return box;
    }

    private EditBox addCoord(int x, int y) {
        EditBox box = new EditBox(this.font, x, y, 66, 18, Component.empty());
        box.setMaxLength(9);
        box.setValue("0");
        addRenderableWidget(box);
        return box;
    }

    private void addPosBtn(int x, int y, Runnable action) {
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate("gui.bounds_use_pos")),
            b -> action.run()).pos(x, y).size(50, 18).build());
    }

    private void addCycle(int lx, int vx, int vw, int y, int idx, List<String> options,
                          java.util.function.IntConsumer onChange) {
        addRenderableWidget(Button.builder(Component.literal("\u25C0").withStyle(ChatFormatting.DARK_GRAY),
            b -> onChange.accept((idx - 1 + options.size()) % options.size()))
            .pos(vx, y).size(20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(options.get(idx)),
            b -> {}).pos(vx + 22, y).size(vw - 44, 20).build());
        addRenderableWidget(Button.builder(Component.literal("\u25B6").withStyle(ChatFormatting.DARK_GRAY),
            b -> onChange.accept((idx + 1) % options.size()))
            .pos(vx + vw - 22, y).size(20, 20).build());
    }

    private void addProtToggle(int x, int y, String labelKey, boolean val,
                                java.util.function.Consumer<Boolean> setter) {
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate(labelKey) + "  " +
                (val
                    ? LocalizationManager.translate("gui.prot_enabled")
                    : LocalizationManager.translate("gui.prot_disabled"))),
            b -> { setter.accept(!val); rebuild(); })
            .pos(x, y).size(105, 17).build());
    }

    private void addQuick(int x, int y, String key, String cmd) {
        addRenderableWidget(Button.builder(
            Component.literal("+ " + LocalizationManager.translate(key)).withStyle(ChatFormatting.DARK_GRAY),
            b -> { if (this.minecraft != null && this.minecraft.player != null)
                this.minecraft.player.connection.sendCommand(cmd); })
            .pos(x, y).size(220, 20).build());
    }

    private void setAllProt(boolean v) {
        protBreak = protPlace = protInteract = protPvp = protExplosion = protDamage = v;
    }

    // ===== Data helpers =====
    private double getPX() { return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getX() : 0; }
    private double getPZ() { return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getZ() : 0; }
    private String fmt(double v) { return String.valueOf((int) Math.floor(v)); }
    private void rebuild() { this.clearWidgets(); init(); }

    private void onSave() {
        var json = new com.google.gson.JsonObject();
        String dn = displayNameInput != null ? displayNameInput.getValue().trim() : "";
        if (!dn.isEmpty() && !dn.equals(entry.name())) json.addProperty("displayName", dn);
        json.addProperty("dimension", DIMENSIONS.get(dimIdx));
        json.addProperty("enterMode", GAME_MODES.get(enterIdx));
        json.addProperty("leaveMode", GAME_MODES.get(leaveIdx));
        json.addProperty("enabled", enabled);
        json.addProperty("protBlockBreak", protBreak);
        json.addProperty("protBlockPlace", protPlace);
        json.addProperty("protBlockInteract", protInteract);
        json.addProperty("protPvp", protPvp);
        json.addProperty("protExplosion", protExplosion);
        json.addProperty("protEntityDamage", protDamage);
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
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int cx = this.width / 2;

        // Title
        g.drawCenteredString(this.font, Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE), cx, 8, 0xFFFFFF);

        // Section backgrounds and headers
        int lx = cx - 124;
        int rw = 250;

        drawSection(g, "  Basic  ", lx, sectionY, rw, 26*4 + 4, 0);
        drawSection(g, "  Bounds  ", lx, boundsSectionY, rw, 26 + 22 + 28 + 2, 0);
        drawSection(g, "  Protection  ", lx, protectionSectionY, rw, 20 + 22 + 18*3 + 28 + 4, protY);
        drawSection(g, "  Other  ", lx, otherSectionY, rw, 22*4 + 34, 0);

        super.render(g, mx, my, pt);
    }

    private void drawSection(GuiGraphics g, String title, int x, int y, int w, int h, int yOff) {
        // Semi-transparent background
        g.fill(x, y, x + w, y + h, 0x18181818);
        // Separator line at top
        g.fill(x, y, x + w, y + 1, 0x60888888);
        // Section title
        g.drawString(this.font, Component.literal(title).withStyle(ChatFormatting.DARK_GRAY),
            x + 4, y + 1, 0x888888);
    }
}
