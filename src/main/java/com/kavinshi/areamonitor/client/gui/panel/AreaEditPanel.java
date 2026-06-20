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

    private final AreaManagementScreen parentScreen;
    private final S2CAreaListPacket.AreaEntry entry;
    private EditBox displayNameInput;
    private EditBox bx1, bz1, bx2, bz2;
    private boolean enabled, rectangleMode = true, protectionExpanded = false;
    private boolean protBreak, protPlace, protInteract, protPvp, protExplosion, protDamage;

    private static final List<String> DIMENSIONS = List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    private static final List<String> GAME_MODES = List.of("survival", "creative", "adventure", "spectator");
    private static final String[] MODE_KEYS = {"gameMode.survival", "gameMode.creative", "gameMode.adventure", "gameMode.spectator"};
    private int dimIdx, enterIdx, leaveIdx;

    private int protCount() { return (protBreak?1:0)+(protPlace?1:0)+(protInteract?1:0)+(protPvp?1:0)+(protExplosion?1:0)+(protDamage?1:0); }

    // Window coords
    private int wx, wy, ww, wh;
    // Content area (inside window, below title)
    private int lx, vx, vw;
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
        ww = Math.min(this.width * 78 / 100, 560);
        wh = Math.min(this.height * 82 / 100, 480);
        wx = (this.width - ww) / 2;
        wy = (this.height - wh) / 2;
        lx = wx + 8;
        vx = lx + 70;
        vw = Math.min(ww - 110, 320);  // fill most of window width
        int y = wy + 36;

        // === Basic ===
        int top = y - 4;
        addLabel("gui.display", y); displayNameInput = zbox(vx, y, vw, entry.displayName() != null ? entry.displayName() : entry.name()); y += 22;
        addLabel("gui.dimension", y); zcycle(vx, vw, y, dimIdx, DIMENSIONS, v -> { dimIdx = v; rebuild(); }); y += 22;
        addLabel("gui.enter_mode", y); zcycle(vx, vw, y, enterIdx, toModeNames(), v -> { enterIdx = v; rebuild(); }); y += 22;
        addLabel("gui.leave_mode", y); zcycle(vx, vw, y, leaveIdx, toModeNames(), v -> { leaveIdx = v; rebuild(); }); y += 22;
        sections.add(new SectionPos("  Basic  ", top, y - top));

        // === Bounds ===
        top = y - 4;
        zbtn(lx, y, vw + 40, LocalizationManager.translate("gui.bounds") + ": " + LocalizationManager.translate(rectangleMode ? "gui.bounds_rectangle" : "gui.bounds_circle"),
            () -> { rectangleMode = !rectangleMode; rebuild(); }); y += 22;
        if (rectangleMode) {
            addLabel("gui.bounds_min", y); bx1 = zcoord(vx, y); bz1 = zcoord(vx + 48, y); zbtn(vx + 96, y, 44, LocalizationManager.translate("gui.bounds_use_pos"), () -> { bx1.setValue(fmtPx()); bz1.setValue(fmtPz()); }); y += 20;
            addLabel("gui.bounds_max", y); bx2 = zcoord(vx, y); bz2 = zcoord(vx + 48, y); zbtn(vx + 96, y, 44, LocalizationManager.translate("gui.bounds_use_pos"), () -> { bx2.setValue(fmtPx()); bz2.setValue(fmtPz()); });
        } else {
            addLabel("gui.bounds_center", y); bx1 = zcoord(vx, y); bz1 = zcoord(vx + 48, y); zbtn(vx + 96, y, 44, LocalizationManager.translate("gui.bounds_use_pos"), () -> { bx1.setValue(fmtPx()); bz1.setValue(fmtPz()); }); y += 20;
            addLabel("gui.bounds_radius", y); bx2 = zcoord(vx, y); addRenderableWidget(bx2);
        }
        y += 26;
        sections.add(new SectionPos("  Bounds  ", top, y - top));

        // === Protection (collapsible) ===
        top = y - 4;
        String fold = protectionExpanded ? "  \u25BC" : "  \u25B6";
        zbtn(lx, y, vw + 40, LocalizationManager.translate("gui.protection") + ": " + protCount() + "/6 " + LocalizationManager.translate("gui.prot_enabled") + fold,
            () -> { protectionExpanded = !protectionExpanded; rebuild(); }); y += 20;
        if (protectionExpanded) {
            zbtn(lx, y, 68, LocalizationManager.translate("gui.prot_enable_all"), () -> { setAllProt(true); rebuild(); });
            zbtn(lx + 72, y, 68, LocalizationManager.translate("gui.prot_disable_all"), () -> { setAllProt(false); rebuild(); }); y += 20;
            zprot(lx, y, "gui.prot_block_break", protBreak, v -> protBreak = v);
            zprot(lx + 120, y, "gui.prot_pvp", protPvp, v -> protPvp = v); y += 17;
            zprot(lx, y, "gui.prot_block_place", protPlace, v -> protPlace = v);
            zprot(lx + 120, y, "gui.prot_explosion", protExplosion, v -> protExplosion = v); y += 17;
            zprot(lx, y, "gui.prot_block_interact", protInteract, v -> protInteract = v);
            zprot(lx + 120, y, "gui.prot_entity_damage", protDamage, v -> protDamage = v);
        }
        y += 8;
        zbtn(lx, y, vw + 40, LocalizationManager.translate(enabled ? "area.enabled" : "area.disabled"), () -> { enabled = !enabled; rebuild(); }); y += 28;
        sections.add(new SectionPos("  Protection  ", top, y - top));

        // === Other ===
        top = y - 4;
        zbtn(lx, y, vw + 40, "+ " + LocalizationManager.translate("gui.trigger_settings"),
            () -> { if (this.minecraft != null) this.minecraft.setScreen(new TriggerEditPanel(AreaEditPanel.this, parentScreen, entry)); }); y += 20;
        zbtn(lx, y, vw + 40, "+ " + LocalizationManager.translate("gui.whitelist_settings"),
            () -> { if (this.minecraft != null) this.minecraft.setScreen(new WhitelistEditPanel(AreaEditPanel.this, parentScreen, entry)); }); y += 20;
        zbtn(lx, y, vw + 40, "+ " + LocalizationManager.translate("gui.restriction_settings"),
            () -> { if (this.minecraft != null) this.minecraft.setScreen(new RestrictionEditPanel(AreaEditPanel.this, parentScreen, entry)); });
        y += 28;
        sections.add(new SectionPos("  Other  ", top, y - top));

        // Save / Cancel
        int btnY = Math.max(y + 6, wy + wh - 38);
        zbtn(lx, btnY, 70, "[" + LocalizationManager.translate("gui.save") + "]", this::onSaveAction);
        zbtn(lx + 78, btnY, 70, "[" + LocalizationManager.translate("gui.cancel") + "]", this::onClose);
    }

    // === Widget builders (all 18px height) ===
    private void zbtn(int x, int y, int w, String text, Runnable a) { addRenderableWidget(GlassButton.create(x, y, w, 18, text, b -> a.run())); }
    private EditBox zbox(int x, int y, int w, String v) { EditBox e = new EditBox(this.font, x, y, w, 16, Component.empty()); e.setMaxLength(48); e.setValue(v); addRenderableWidget(e); return e; }
    private EditBox zcoord(int x, int y) { EditBox e = new EditBox(this.font, x, y, 44, 16, Component.empty()); e.setMaxLength(9); e.setValue("0"); addRenderableWidget(e); return e; }
    private void zprot(int x, int y, String key, boolean val, java.util.function.Consumer<Boolean> s) {
        zbtn(x, y, 108, LocalizationManager.translate(key) + "  " + (val ? LocalizationManager.translate("gui.prot_enabled") : LocalizationManager.translate("gui.prot_disabled")), () -> { s.accept(!val); rebuild(); });
    }
    private void zcycle(int vx, int vw, int y, int idx, List<String> opts, java.util.function.IntConsumer cb) {
        zbtn(vx, y, 18, "\u25C0", () -> cb.accept((idx - 1 + opts.size()) % opts.size()));
        zbtn(vx + 20, y, vw - 40, opts.get(idx), () -> {});
        zbtn(vx + vw - 20, y, 18, "\u25B6", () -> cb.accept((idx + 1) % opts.size()));
    }
    private void addLabel(String key, int y) { labels.add(new LabelPos(lx, y, key)); }
    private List<String> toModeNames() { return java.util.Arrays.stream(MODE_KEYS).map(LocalizationManager::translate).toList(); }
    private void setAllProt(boolean v) { protBreak = protPlace = protInteract = protPvp = protExplosion = protDamage = v; }
    private String fmtPx() { return String.valueOf((int)Math.floor(this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getX() : 0)); }
    private String fmtPz() { return String.valueOf((int)Math.floor(this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getZ() : 0)); }
    private void rebuild() { this.clearWidgets(); init(); }
    private static int v(EditBox b) { return Integer.parseInt(b.getValue()); }

    private void onSaveAction() {
        var json = new com.google.gson.JsonObject();
        String dn = displayNameInput.getValue().trim();
        if (!dn.isEmpty() && !dn.equals(entry.name())) json.addProperty("displayName", dn);
        json.addProperty("dimension", DIMENSIONS.get(dimIdx)); json.addProperty("enterMode", GAME_MODES.get(enterIdx));
        json.addProperty("leaveMode", GAME_MODES.get(leaveIdx)); json.addProperty("enabled", enabled);
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

    @Override public void onClose() { if (this.minecraft != null) { this.minecraft.setScreen(parentScreen); parentScreen.updateAfterEdit(); } }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0x80000000);
        // Window
        g.fill(wx, wy, wx + ww, wy + wh, PARCH_PANEL);
        g.fill(wx + 1, wy + 1, wx + ww - 1, wy + wh - 1, 0xE02A1F14);
        g.fill(wx, wy, wx + ww, wy + 2, BORDER_GOLD);
        g.fill(wx, wy, wx + 2, wy + wh, BORDER_GOLD);
        g.fill(wx + ww - 2, wy, wx + ww, wy + wh, BORDER_GOLD);
        g.fill(wx, wy + wh - 2, wx + ww, wy + wh, BORDER_GOLD);
        // Title bar
        g.fill(wx + 3, wy + 3, wx + ww - 3, wy + 29, PARCH_DARK);
        g.fill(wx + 3, wy + 28, wx + ww - 3, wy + 29, BORDER_GOLD);
        g.drawCenteredString(this.font, Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE),
            wx + ww / 2, wy + 9, 0xFFF5DEB3);
        // Section backgrounds
        int secW = vx - lx + vw + 4;
        for (SectionPos s : sections) {
            g.fill(lx - 4, s.y, lx + secW, s.y + s.h, 0x30C4A882);
            g.fill(lx - 4, s.y, lx + secW, s.y + 1, BORDER_GOLD);
            g.drawString(this.font, Component.literal(s.title).withStyle(ChatFormatting.DARK_GRAY), lx + 2, s.y + 2, 0xFF8B6914);
        }
        for (LabelPos l : labels) g.drawString(this.font, Component.literal(LocalizationManager.translate(l.key)).withStyle(ChatFormatting.GRAY), l.x, l.y + 1, 0xFFFFFFFF);
        super.render(g, mx, my, pt);
    }

    private record SectionPos(String title, int y, int h) {}
    private record LabelPos(int x, int y, String key) {}
}
