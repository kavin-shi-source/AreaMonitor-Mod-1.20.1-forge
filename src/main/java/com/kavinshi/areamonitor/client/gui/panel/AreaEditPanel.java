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
 * Full area edit panel — Glass Morphism theme.
 * Sections: Basic / Bounds / Protection(foldable) / Other(quick links) / Actions
 */
public class AreaEditPanel extends Screen {

    // Enhanced Glass Morphism palette
    private static final int GLASS_DARK    = 0xC0000000;
    private static final int GLASS_PANEL   = 0x70000000;
    private static final int BORDER_SOFT   = 0x50FFFFFF;
    private static final int BORDER_BRIGHT = 0x80FFFFFF;
    private static final int BORDER_FAINT  = 0x20FFFFFF;

    private final AreaManagementScreen parentScreen;
    private final S2CAreaListPacket.AreaEntry entry;
    private EditBox displayNameInput;
    private EditBox bx1, bz1, bx2, bz2;
    private boolean enabled;
    private boolean rectangleMode = true;
    private boolean protectionExpanded = false;
    private boolean protBreak, protPlace, protInteract, protPvp, protExplosion, protDamage;

    private static final List<String> DIMENSIONS = List.of(
        "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    private static final List<String> GAME_MODES = List.of("survival", "creative", "adventure", "spectator");
    private static final String[] MODE_KEYS = {
        "gameMode.survival", "gameMode.creative", "gameMode.adventure", "gameMode.spectator"
    };
    private int dimIdx = 0, enterIdx = 0, leaveIdx = 0;

    private int protCount() {
        return (protBreak?1:0)+(protPlace?1:0)+(protInteract?1:0)+(protPvp?1:0)+(protExplosion?1:0)+(protDamage?1:0);
    }

    // Dynamic layout
    private int lx, vx, vw;

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
        lx = Math.max(10, cx - this.width / 4);
        vx = cx - 40;
        vw = Math.min(200, this.width / 2);
        int y = 30;

        // ===== Section 1: Basic =====
        sections.add(new SectionPos("  Basic  ", y - 6, 26 * 4 + 4));
        addLabel(lx, y, "gui.display");
        displayNameInput = addTextBox(vx, y, vw, entry.displayName() != null ? entry.displayName() : entry.name());
        y += 24;
        addLabel(lx, y, "gui.dimension");
        addCycle(vx, vw, y, dimIdx, DIMENSIONS, v -> { dimIdx = v; rebuild(); });
        y += 24;
        addLabel(lx, y, "gui.enter_mode");
        addCycle(vx, vw, y, enterIdx, toModeNames(), v -> { enterIdx = v; rebuild(); });
        y += 24;
        addLabel(lx, y, "gui.leave_mode");
        addCycle(vx, vw, y, leaveIdx, toModeNames(), v -> { leaveIdx = v; rebuild(); });
        y += 28;

        // ===== Section 2: Bounds =====
        sections.add(new SectionPos("  Bounds  ", y - 6, 26 + 22 + 30));

        addGlassBtn(LocalizationManager.translate("gui.bounds") + ": " +
            LocalizationManager.translate(rectangleMode ? "gui.bounds_rectangle" : "gui.bounds_circle"),
            lx, y, vw + 40, () -> { rectangleMode = !rectangleMode; rebuild(); });
        y += 24;

        if (rectangleMode) {
            addLabel(lx, y, "gui.bounds_min");
            bx1 = addCoord(vx, y); bz1 = addCoord(vx + 70, y);
            addPosBtn(vx + 140, y, () -> { bx1.setValue(fmt(getPX())); bz1.setValue(fmt(getPZ())); }); y += 22;
            addLabel(lx, y, "gui.bounds_max");
            bx2 = addCoord(vx, y); bz2 = addCoord(vx + 70, y);
            addPosBtn(vx + 140, y, () -> { bx2.setValue(fmt(getPX())); bz2.setValue(fmt(getPZ())); });
        } else {
            addLabel(lx, y, "gui.bounds_center");
            bx1 = addCoord(vx, y); bz1 = addCoord(vx + 70, y);
            addPosBtn(vx + 140, y, () -> { bx1.setValue(fmt(getPX())); bz1.setValue(fmt(getPZ())); }); y += 22;
            addLabel(lx, y, "gui.bounds_radius");
            bx2 = addCoord(vx, y); addGlassBtn("", vx, y, 66, () -> {});
        }
        y += 30;

        // ===== Section 3: Protection (collapsible) =====
        int protTop = y - 6;
        int protH = protectionExpanded ? (20 + 22 + 56 + 4) : (20 + 4);
        sections.add(new SectionPos("  Protection  ", protTop, protH));

        String foldIcon = protectionExpanded ? "  \u25BC" : "  \u25B6";
        String summ = LocalizationManager.translate("gui.protection") + ": " +
            protCount() + "/6 " + LocalizationManager.translate("gui.prot_enabled") + foldIcon;
        addGlassBtn(summ, lx, y, vw + 40, () -> { protectionExpanded = !protectionExpanded; rebuild(); });
        y += 22;

        if (protectionExpanded) {
            addGlassBtn(LocalizationManager.translate("gui.prot_enable_all"), lx, y, 70,
                () -> { setAllProt(true); rebuild(); });
            addGlassBtn(LocalizationManager.translate("gui.prot_disable_all"), lx + 78, y, 70,
                () -> { setAllProt(false); rebuild(); });
            y += 22;
            addProtToggle(lx, y, "gui.prot_block_break", protBreak, v -> protBreak = v);
            addProtToggle(lx + 110, y, "gui.prot_pvp", protPvp, v -> protPvp = v); y += 18;
            addProtToggle(lx, y, "gui.prot_block_place", protPlace, v -> protPlace = v);
            addProtToggle(lx + 110, y, "gui.prot_explosion", protExplosion, v -> protExplosion = v); y += 18;
            addProtToggle(lx, y, "gui.prot_block_interact", protInteract, v -> protInteract = v);
            addProtToggle(lx + 110, y, "gui.prot_entity_damage", protDamage, v -> protDamage = v);
        }
        y += 6;

        // Enabled
        addGlassBtn(LocalizationManager.translate(enabled ? "area.enabled" : "area.disabled"),
            lx, y, vw + 40, () -> { enabled = !enabled; rebuild(); });
        y += 32;

        // ===== Section 4: Other =====
        sections.add(new SectionPos("  Other  ", y - 6, 22 * 4 + 36));

        addQuickR(lx, y, "gui.trigger_settings", () -> {
            if (this.minecraft != null) this.minecraft.setScreen(new TriggerEditPanel(parentScreen, entry));
        }); y += 22;
        addQuickR(lx, y, "gui.whitelist_settings", () -> {
            if (this.minecraft != null) this.minecraft.setScreen(new WhitelistEditPanel(parentScreen, entry));
        }); y += 22;
        addQuickR(lx, y, "gui.restriction_settings", () -> {
            if (this.minecraft != null) this.minecraft.setScreen(new RestrictionEditPanel(parentScreen, entry));
        });
        y += 34;

        // Save / Cancel
        addRenderableWidget(Button.builder(
            Component.literal("[" + LocalizationManager.translate("gui.save") + "]").withStyle(ChatFormatting.GREEN),
            b -> onSave()).pos(lx, y).size(70, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("[" + LocalizationManager.translate("gui.cancel") + "]").withStyle(ChatFormatting.GRAY),
            b -> onClose()).pos(lx + 80, y).size(70, 20).build());
    }

    // === Section position tracking ===
    private final java.util.ArrayList<SectionPos> sections = new java.util.ArrayList<>();
    private record SectionPos(String title, int y, int h) {}

    // === Widget helpers ===

    private void addLabel(int x, int y, String key) {
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate(key)).withStyle(ChatFormatting.GRAY),
            b -> {}).pos(x, y).size(60, 20).build());
    }

    private EditBox addTextBox(int x, int y, int w, String val) {
        EditBox b = new EditBox(this.font, x, y, w, 18, Component.empty());
        b.setMaxLength(48); b.setValue(val); addRenderableWidget(b); return b;
    }

    private EditBox addCoord(int x, int y) {
        EditBox b = new EditBox(this.font, x, y, 66, 18, Component.empty());
        b.setMaxLength(9); b.setValue("0"); addRenderableWidget(b); return b;
    }

    private void addPosBtn(int x, int y, Runnable action) {
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate("gui.bounds_use_pos")),
            b -> action.run()).pos(x, y).size(50, 18).build());
    }

    private void addGlassBtn(String text, int x, int y, int w, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> action.run())
            .pos(x, y).size(w, 20).build());
    }

    private void addCycle(int vx, int vw, int y, int idx, List<String> options,
                          java.util.function.IntConsumer onChange) {
        addRenderableWidget(Button.builder(
            Component.literal("\u25C0").withStyle(ChatFormatting.DARK_GRAY),
            b -> onChange.accept((idx - 1 + options.size()) % options.size()))
            .pos(vx, y).size(20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(options.get(idx)), b -> {})
            .pos(vx + 22, y).size(vw - 44, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("\u25B6").withStyle(ChatFormatting.DARK_GRAY),
            b -> onChange.accept((idx + 1) % options.size()))
            .pos(vx + vw - 22, y).size(20, 20).build());
    }

    private void addProtToggle(int x, int y, String key, boolean val,
                                java.util.function.Consumer<Boolean> setter) {
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate(key) + "  " +
                (val ? LocalizationManager.translate("gui.prot_enabled") : LocalizationManager.translate("gui.prot_disabled"))),
            b -> { setter.accept(!val); rebuild(); }).pos(x, y).size(105, 17).build());
    }

    private void addQuickR(int x, int y, String key, Runnable action) {
        addRenderableWidget(Button.builder(
            Component.literal("+ " + LocalizationManager.translate(key)).withStyle(ChatFormatting.DARK_GRAY),
            b -> action.run())
            .pos(x, y).size(220, 20).build());
    }

    private void addQuickC(int x, int y, String key, String cmd) {
        addRenderableWidget(Button.builder(
            Component.literal("+ " + LocalizationManager.translate(key)).withStyle(ChatFormatting.DARK_GRAY),
            b -> { if (this.minecraft != null && this.minecraft.player != null)
                this.minecraft.player.connection.sendCommand(cmd); })
            .pos(x, y).size(220, 20).build());
    }

    private List<String> toModeNames() {
        return java.util.Arrays.stream(MODE_KEYS).map(LocalizationManager::translate).toList();
    }

    private void setAllProt(boolean v) {
        protBreak = protPlace = protInteract = protPvp = protExplosion = protDamage = v;
    }

    private double getPX() { return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getX() : 0; }
    private double getPZ() { return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getZ() : 0; }
    private String fmt(double v) { return String.valueOf((int) Math.floor(v)); }
    private void rebuild() { this.clearWidgets(); sections.clear(); init(); }

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

        // Title bar (double-line)
        g.fill(0, 0, this.width, 28, GLASS_DARK);
        g.fill(0, 27, this.width, 28, BORDER_BRIGHT);
        g.fill(0, 0, this.width, 1, BORDER_SOFT);
        g.drawCenteredString(this.font,
            Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE), cx, 8, 0xFFFFFF);

        // Section backgrounds with bottom shadow edge
        for (SectionPos s : sections) {
            g.fill(lx - 6, s.y, lx + vw + 46, s.y + s.h, GLASS_PANEL);
            // Bright top accent
            g.fill(lx - 6, s.y, lx + vw + 46, s.y + 1, BORDER_BRIGHT);
            // Faint bottom shadow
            g.fill(lx - 6, s.y + s.h - 1, lx + vw + 46, s.y + s.h, BORDER_FAINT);
            g.drawString(this.font,
                Component.literal(s.title).withStyle(ChatFormatting.DARK_GRAY),
                lx + 2, s.y + 2, 0x999999);
        }

        super.render(g, mx, my, pt);
    }
}
