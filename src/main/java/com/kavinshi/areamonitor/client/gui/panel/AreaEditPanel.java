package com.kavinshi.areamonitor.client.gui.panel;

import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.client.gui.AreaManagementScreen;
import com.kavinshi.areamonitor.network.C2SAreaActionPacket;
import com.kavinshi.areamonitor.network.S2CAreaListPacket;
import com.kavinshi.areamonitor.network.ModNetwork;
import com.kavinshi.areamonitor.client.gui.widget.GlassButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class AreaEditPanel extends Screen {

    private static final int PARCH_DARK   = 0xD03A2A1A;
    private static final int PARCH_PANEL  = 0xC0C4A882;
    private static final int BORDER_GOLD  = 0x808B6914;
    private static final int BORDER_SHADOW = 0x405C4033;

    private final AreaManagementScreen parentScreen;
    private final S2CAreaListPacket.AreaEntry entry;
    private EditBox displayNameInput;
    private EditBox bx1, bz1, bx2, bz2;
    private boolean enabled;
    private boolean rectangleMode = true;
    private boolean protectionExpanded = false;
    private boolean protBreak, protPlace, protInteract, protPvp, protExplosion, protDamage;

    private static final List<String> DIMENSIONS = List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    private static final List<String> GAME_MODES = List.of("survival", "creative", "adventure", "spectator");
    private static final String[] MODE_KEYS = {"gameMode.survival", "gameMode.creative", "gameMode.adventure", "gameMode.spectator"};
    private int dimIdx = 0, enterIdx = 0, leaveIdx = 0;

    private int protCount() { return (protBreak?1:0)+(protPlace?1:0)+(protInteract?1:0)+(protPvp?1:0)+(protExplosion?1:0)+(protDamage?1:0); }

    private int lx, vx, vw;
    private int winX, winY, winW, winH;
    private final List<SectionPos> sections = new ArrayList<>();
    private final List<LabelPos> labels = new ArrayList<>();

    public AreaEditPanel(AreaManagementScreen parent, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.edit_area") + ": " + entry.name()));
        this.parentScreen = parent;
        this.entry = entry;
        this.enabled = entry.enabled();
        this.rectangleMode = !"CIRCLE".equals(entry.boundsType());
        this.protBreak = entry.protBlockBreak(); this.protPlace = entry.protBlockPlace();
        this.protInteract = entry.protBlockInteract(); this.protPvp = entry.protPvp();
        this.protExplosion = entry.protExplosion(); this.protDamage = entry.protEntityDamage();
        dimIdx = Math.max(0, DIMENSIONS.indexOf(entry.dimension()));
        enterIdx = Math.max(0, GAME_MODES.indexOf(entry.enterMode()));
        leaveIdx = Math.max(0, GAME_MODES.indexOf(entry.leaveMode()));
    }

    @Override protected void init() {
        super.init(); sections.clear(); labels.clear();
        winW = Math.min(this.width * 78 / 100, 560);
        winH = Math.min(this.height * 82 / 100, 480);
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;
        lx = winX + 12;
        vx = lx + 75; vw = Math.min(200, winW - 40);
        int y = winY + 38;

        // === Basic ===
        int top = y - 6;
        addLabel("gui.display", y); displayNameInput = addBox(vx, y, vw, entry.displayName() != null ? entry.displayName() : entry.name()); y += 24;
        addLabel("gui.dimension", y); addCycle(vx, vw, y, dimIdx, DIMENSIONS, v -> { dimIdx = v; rebuild(); }); y += 24;
        addLabel("gui.enter_mode", y); addCycle(vx, vw, y, enterIdx, toModeNames(), v -> { enterIdx = v; rebuild(); }); y += 24;
        addLabel("gui.leave_mode", y); addCycle(vx, vw, y, leaveIdx, toModeNames(), v -> { leaveIdx = v; rebuild(); }); y += 28;
        sections.add(new SectionPos("  Basic  ", top, y - top + 2));

        // === Bounds ===
        top = y - 6;
        addBtn(LocalizationManager.translate("gui.bounds") + ": " + LocalizationManager.translate(rectangleMode ? "gui.bounds_rectangle" : "gui.bounds_circle"),
            lx, y, vw + 40, () -> { rectangleMode = !rectangleMode; rebuild(); }); y += 24;
        if (rectangleMode) {
            addLabel("gui.bounds_min", y); bx1 = addCoord(vx, y, "X"); bz1 = addCoord(vx + 53, y, "Z"); addPosBtn(vx + 106, y); y += 22;
            addLabel("gui.bounds_max", y); bx2 = addCoord(vx, y, "X"); bz2 = addCoord(vx + 53, y, "Z"); addPosBtn(vx + 106, y, () -> { bx2.setValue(fmtPx()); bz2.setValue(fmtPz()); });
        } else {
            addLabel("gui.bounds_center", y); bx1 = addCoord(vx, y, "X"); bz1 = addCoord(vx + 53, y, "Z"); addPosBtn(vx + 106, y); y += 22;
            addLabel("gui.bounds_radius", y); bx2 = addCoord(vx, y, "R"); addRenderableWidget(bx2);
        }
        y += 30;
        sections.add(new SectionPos("  Bounds  ", top, y - top + 2));

        // === Protection (collapsible) ===
        top = y - 6;
        String foldIcon = protectionExpanded ? "  \u25BC" : "  \u25B6";
        addBtn(LocalizationManager.translate("gui.protection") + ": " + protCount() + "/6 " + LocalizationManager.translate("gui.prot_enabled") + foldIcon,
            lx, y, vw + 40, () -> { protectionExpanded = !protectionExpanded; rebuild(); }); y += 22;
        if (protectionExpanded) {
            addBtn(LocalizationManager.translate("gui.prot_enable_all"), lx, y, 70, () -> { setAllProt(true); rebuild(); });
            addBtn(LocalizationManager.translate("gui.prot_disable_all"), lx + 78, y, 70, () -> { setAllProt(false); rebuild(); }); y += 22;
            addProtToggle(lx, y, "gui.prot_block_break", protBreak, v -> protBreak = v);
            addProtToggle(lx + 115, y, "gui.prot_pvp", protPvp, v -> protPvp = v); y += 18;
            addProtToggle(lx, y, "gui.prot_block_place", protPlace, v -> protPlace = v);
            addProtToggle(lx + 115, y, "gui.prot_explosion", protExplosion, v -> protExplosion = v); y += 18;
            addProtToggle(lx, y, "gui.prot_block_interact", protInteract, v -> protInteract = v);
            addProtToggle(lx + 115, y, "gui.prot_entity_damage", protDamage, v -> protDamage = v);
        }
        y += 6;
        addBtn(LocalizationManager.translate(enabled ? "area.enabled" : "area.disabled"), lx, y, vw + 40, () -> { enabled = !enabled; rebuild(); }); y += 32;
        sections.add(new SectionPos("  Protection  ", top, y - top + 2));

        // === Other ===
        top = y - 6;
        addQuickR(lx, y, "gui.trigger_settings", () -> { if (this.minecraft != null) this.minecraft.setScreen(new TriggerEditPanel(AreaEditPanel.this, parentScreen, entry)); }); y += 22;
        addQuickR(lx, y, "gui.whitelist_settings", () -> { if (this.minecraft != null) this.minecraft.setScreen(new WhitelistEditPanel(AreaEditPanel.this, parentScreen, entry)); }); y += 22;
        addQuickR(lx, y, "gui.restriction_settings", () -> { if (this.minecraft != null) this.minecraft.setScreen(new RestrictionEditPanel(AreaEditPanel.this, parentScreen, entry)); });
        y += 34;
        sections.add(new SectionPos("  Other  ", top, y - top + 2));

        // Save / Cancel
        int btnY = Math.max(y + 4, winY + winH - 40);
        addBtn("[" + LocalizationManager.translate("gui.save") + "]", lx, btnY, 70, this::onSaveAction);
        addBtn("[" + LocalizationManager.translate("gui.cancel") + "]", lx + 80, btnY, 70, this::onClose);
    }

    private void addLabel(String key, int y) { labels.add(new LabelPos(lx, y, key)); }
    private EditBox addBox(int x, int y, int w, String val) { EditBox b = new EditBox(this.font, x, y, w, 18, Component.empty()); b.setMaxLength(48); b.setValue(val); addRenderableWidget(b); return b; }
    private EditBox addCoord(int x, int y, String hint) { EditBox b = new EditBox(this.font, x, y, 48, 18, Component.empty()); b.setMaxLength(9); b.setValue("0"); addRenderableWidget(b); return b; }

    private void addPosBtn(int x, int y) { addBtn(LocalizationManager.translate("gui.bounds_use_pos"), x, y, 50, () -> { bx1.setValue(fmtPx()); bz1.setValue(fmtPz()); }); }
    private void addPosBtn(int x, int y, Runnable r) { addBtn(LocalizationManager.translate("gui.bounds_use_pos"), x, y, 50, r); }

    private void addBtn(String text, int x, int y, int w, Runnable action) { addRenderableWidget(GlassButton.create(x, y, w, 18, text, b -> action.run())); }

    private void addCycle(int vx, int vw, int y, int idx, List<String> options, java.util.function.IntConsumer onChange) {
        addRenderableWidget(GlassButton.create(vx, y, 20, 18, "\u25C0", b -> onChange.accept((idx - 1 + options.size()) % options.size())));
        addRenderableWidget(GlassButton.create(vx + 22, y, vw - 44, 18, options.get(idx), b -> {}));
        addRenderableWidget(GlassButton.create(vx + vw - 22, y, 20, 18, "\u25B6", b -> onChange.accept((idx + 1) % options.size())));
    }

    private void addProtToggle(int x, int y, String key, boolean val, java.util.function.Consumer<Boolean> setter) {
        addRenderableWidget(GlassButton.create(x, y, 108, 17,
            LocalizationManager.translate(key) + "  " + (val ? LocalizationManager.translate("gui.prot_enabled") : LocalizationManager.translate("gui.prot_disabled")),
            b -> { setter.accept(!val); rebuild(); }));
    }

    private void addQuickR(int x, int y, String key, Runnable action) { addBtn("+ " + LocalizationManager.translate(key), x, y, 230, action); }

    private List<String> toModeNames() { return java.util.Arrays.stream(MODE_KEYS).map(LocalizationManager::translate).toList(); }
    private void setAllProt(boolean v) { protBreak = protPlace = protInteract = protPvp = protExplosion = protDamage = v; }

    private String fmtPx() { return String.valueOf((int)Math.floor(this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getX() : 0)); }
    private String fmtPz() { return String.valueOf((int)Math.floor(this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getZ() : 0)); }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private void rebuild() { this.clearWidgets(); init(); }

    private void onSaveAction() {
        var json = new com.google.gson.JsonObject();
        String dn = displayNameInput.getValue().trim();
        if (!dn.isEmpty() && !dn.equals(entry.name())) json.addProperty("displayName", dn);
        json.addProperty("dimension", DIMENSIONS.get(dimIdx));
        json.addProperty("enterMode", GAME_MODES.get(enterIdx));
        json.addProperty("leaveMode", GAME_MODES.get(leaveIdx));
        json.addProperty("enabled", enabled);
        json.addProperty("protBlockBreak", protBreak); json.addProperty("protBlockPlace", protPlace);
        json.addProperty("protBlockInteract", protInteract); json.addProperty("protPvp", protPvp);
        json.addProperty("protExplosion", protExplosion); json.addProperty("protEntityDamage", protDamage);
        json.addProperty("boundsType", rectangleMode ? "RECTANGLE" : "CIRCLE");
        try {
            if (rectangleMode) { json.addProperty("minX", v(bx1)); json.addProperty("minZ", v(bz1)); json.addProperty("maxX", v(bx2)); json.addProperty("maxZ", v(bz2)); }
            else { json.addProperty("centerX", v(bx1)); json.addProperty("centerZ", v(bz1)); json.addProperty("radius", v(bx2)); }
        } catch (Exception ignored) {}
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.UPDATE, entry.name(), json.toString()));
        onClose();
    }
    private static int v(EditBox b) { return Integer.parseInt(b.getValue()); }

    @Override public void onClose() { if (this.minecraft != null) { this.minecraft.setScreen(parentScreen); parentScreen.updateAfterEdit(); } }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0x80000000);
        g.fill(winX, winY, winX + winW, winY + winH, PARCH_PANEL);
        g.fill(winX + 1, winY + 1, winX + winW - 1, winY + winH - 1, 0xE02A1F14);
        g.fill(winX, winY, winX + winW, winY + 2, BORDER_GOLD);
        g.fill(winX, winY, winX + 2, winY + winH, BORDER_GOLD);
        g.fill(winX + winW - 2, winY, winX + winW, winY + winH, BORDER_GOLD);
        g.fill(winX, winY + winH - 2, winX + winW, winY + winH, BORDER_GOLD);

        g.fill(winX + 3, winY + 3, winX + winW - 3, winY + 31, PARCH_DARK);
        g.fill(winX + 3, winY + 30, winX + winW - 3, winY + 31, BORDER_GOLD);
        g.drawCenteredString(this.font, Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE),
            winX + winW / 2, winY + 10, 0xFFF5DEB3);

        for (SectionPos s : sections) {
            g.fill(lx - 6, s.y, lx + vw + 46, s.y + s.h, 0x30C4A882);
            g.fill(lx - 6, s.y, lx + vw + 46, s.y + 1, BORDER_GOLD);
            g.drawString(this.font, Component.literal(s.title).withStyle(ChatFormatting.DARK_GRAY), lx + 2, s.y + 2, 0xFF8B6914);
        }
        for (LabelPos l : labels) g.drawString(this.font, Component.literal(LocalizationManager.translate(l.key)).withStyle(ChatFormatting.GRAY), l.x, l.y + 2, 0xFFFFFFFF);
        super.render(g, mx, my, pt);
    }

    private record SectionPos(String title, int y, int h) {}
    private record LabelPos(int x, int y, String key) {}
}
