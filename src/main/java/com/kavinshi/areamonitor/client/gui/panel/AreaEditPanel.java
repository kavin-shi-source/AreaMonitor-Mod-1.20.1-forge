package com.kavinshi.areamonitor.client.gui.panel;

import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.client.gui.AreaManagementScreen;
import com.kavinshi.areamonitor.network.C2SAreaActionPacket;
import com.kavinshi.areamonitor.network.S2CAreaListPacket;
import com.kavinshi.areamonitor.network.ModNetwork;
import com.kavinshi.areamonitor.client.gui.widget.ConfirmDialog;
import com.kavinshi.areamonitor.client.gui.widget.GlassButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class AreaEditPanel extends Screen {

    private static final int PARCH_DARK   = 0xD03A2A1A;
    private static final int PARCH_PANEL  = 0xC0C4A882;
    private static final int BORDER_GOLD  = 0x808B6914;
    private static final int ACCENT_GREEN = 0x604B8C3E;
    private static final int ACCENT_GRAY  = 0x60606060;

    private final AreaManagementScreen parentScreen;
    private final S2CAreaListPacket.AreaEntry entry;
    private EditBox displayNameInput;
    private EditBox bx1, bz1, bx2, bz2;
    private boolean enabled, protectionExpanded = false, schedExpanded = false;
    // boundsMode: "RECTANGLE", "CIRCLE", or "POLYGON"
    private String boundsMode = "RECTANGLE";
    private String prevBoundsMode = "RECTANGLE";
    private boolean protBreak, protPlace, protInteract, protPvp, protExplosion, protDamage;
    private boolean protContainer, protFluid, protItemDrop;
    private boolean schedEnabled;
    private EditBox schedMin, schedMax;
    // Condition activation
    private boolean condEnabled, condExpanded = false;
    private EditBox condMinPlayers, condRequirePlayer;
    // Chain
    private boolean chainExpanded = false;
    private EditBox chainNextInput;

    // Initial values parsed from entry (for backfilling inputs on open)
    private String initBx1 = "0", initBz1 = "0", initBx2 = "0", initBz2 = "0";
    private String initSchedMin = "0", initSchedMax = "0";
    private String initCondMinPlayers = "0", initCondRequirePlayer = "";
    private String initChainNext = "";

    private static final List<String> DIMENSIONS = List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    private static final List<String> GAME_MODES = List.of("survival", "creative", "adventure", "spectator");
    private static final String[] MODE_KEYS = {"gameMode.survival", "gameMode.creative", "gameMode.adventure", "gameMode.spectator"};
    private int dimIdx, enterIdx, leaveIdx;

    private int protCount() { return (protBreak?1:0)+(protPlace?1:0)+(protInteract?1:0)+(protPvp?1:0)+(protExplosion?1:0)+(protDamage?1:0)+(protContainer?1:0)+(protFluid?1:0)+(protItemDrop?1:0); }

    // Window coords
    private int wx, wy, ww, wh;
    // Content area (inside window, below title)
    private int lx, vx, vw;
    // Scroll support
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int contentTopY;    // absolute y where content begins (below title bar)
    private int contentBottomY; // absolute y where content ends (above bottom buttons)
    // All EditBox instances for unified keyPressed handling
    private final List<EditBox> allBoxes = new ArrayList<>();
    // Unsaved-change tracking & confirm dialog
    private boolean dirty = false;
    private final ConfirmDialog confirmDialog = new ConfirmDialog();
    // P1-4 fix: saving state — wait briefly so server-side rejection (action bar) is visible before closing
    private boolean saving = false;
    private long savingStartMs = 0L;

    private final List<SectionPos> sections = new ArrayList<>();
    private final List<LabelPos> labels = new ArrayList<>();
    private final List<TooltipZone> tooltips = new ArrayList<>();

    public AreaEditPanel(AreaManagementScreen parent, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.edit_area") + ": " + entry.name()));
        this.parentScreen = parent;
        this.entry = entry;
        this.enabled = entry.enabled();
        {
            String bt = entry.boundsType() != null ? entry.boundsType() : "RECTANGLE";
            this.boundsMode = "POLYGON".equals(bt) ? "POLYGON" : "CIRCLE".equals(bt) ? "CIRCLE" : "RECTANGLE";
        }
        this.protBreak = entry.protBlockBreak(); this.protPlace = entry.protBlockPlace();
        this.protInteract = entry.protBlockInteract(); this.protPvp = entry.protPvp();
        this.protExplosion = entry.protExplosion(); this.protDamage = entry.protEntityDamage();
        this.protContainer = entry.protContainerInteract(); this.protFluid = entry.protFluidPlace();
        this.protItemDrop = entry.protItemDrop();
        // schedule
        if (entry.scheduleJson() != null) try {
            var sched = new com.google.gson.Gson().fromJson(entry.scheduleJson(), com.google.gson.JsonObject.class);
            schedEnabled = sched.has("enabled") && sched.get("enabled").getAsBoolean();
            if (sched.has("timeMin")) initSchedMin = String.valueOf(sched.get("timeMin").getAsInt());
            if (sched.has("timeMax")) initSchedMax = String.valueOf(sched.get("timeMax").getAsInt());
        } catch (Exception e) {
            // P2 #39: log parse failures instead of silently swallowing
            com.kavinshi.areamonitor.AreaMonitorMod.LOGGER.warn("Failed to parse schedule JSON for area '{}': {}", entry.name(), e.getMessage());
            schedEnabled = false;
        }
        // condition
        if (entry.conditionJson() != null) try {
            var cond = new com.google.gson.Gson().fromJson(entry.conditionJson(), com.google.gson.JsonObject.class);
            condEnabled = cond.has("enabled") && cond.get("enabled").getAsBoolean();
            if (cond.has("minPlayers")) initCondMinPlayers = String.valueOf(cond.get("minPlayers").getAsInt());
            if (cond.has("requirePlayer")) initCondRequirePlayer = cond.get("requirePlayer").getAsString();
        } catch (Exception e) {
            com.kavinshi.areamonitor.AreaMonitorMod.LOGGER.warn("Failed to parse condition JSON for area '{}': {}", entry.name(), e.getMessage());
            condEnabled = false;
        }
        // chain
        if (entry.chainJson() != null) try {
            var chain = new com.google.gson.Gson().fromJson(entry.chainJson(), com.google.gson.JsonObject.class);
            if (chain.has("chainNext")) initChainNext = chain.get("chainNext").getAsString();
        } catch (Exception e) {
            com.kavinshi.areamonitor.AreaMonitorMod.LOGGER.warn("Failed to parse chain JSON for area '{}': {}", entry.name(), e.getMessage());
        }
        // bounds coordinates
        if (entry.boundsCoordsJson() != null) try {
            var bc = new com.google.gson.Gson().fromJson(entry.boundsCoordsJson(), com.google.gson.JsonObject.class);
            if ("CIRCLE".equals(boundsMode)) {
                if (bc.has("centerX")) initBx1 = String.valueOf(bc.get("centerX").getAsInt());
                if (bc.has("centerZ")) initBz1 = String.valueOf(bc.get("centerZ").getAsInt());
                if (bc.has("radius")) initBx2 = String.valueOf(bc.get("radius").getAsInt());
            } else if (!"POLYGON".equals(boundsMode)) {
                if (bc.has("minX")) initBx1 = String.valueOf(bc.get("minX").getAsInt());
                if (bc.has("minZ")) initBz1 = String.valueOf(bc.get("minZ").getAsInt());
                if (bc.has("maxX")) initBx2 = String.valueOf(bc.get("maxX").getAsInt());
                if (bc.has("maxZ")) initBz2 = String.valueOf(bc.get("maxZ").getAsInt());
            }
        } catch (Exception e) {
            com.kavinshi.areamonitor.AreaMonitorMod.LOGGER.warn("Failed to parse bounds coords JSON for area '{}': {}", entry.name(), e.getMessage());
        }
        dimIdx = Math.max(0, DIMENSIONS.indexOf(entry.dimension()));
        enterIdx = Math.max(0, GAME_MODES.indexOf(entry.enterMode()));
        leaveIdx = Math.max(0, GAME_MODES.indexOf(entry.leaveMode()));
    }

    @Override protected void init() {
        super.init(); sections.clear(); labels.clear(); tooltips.clear(); allBoxes.clear();
        ww = Math.min(this.width * 78 / 100, 560);
        wh = Math.min(this.height * 82 / 100, 480);
        wx = (this.width - ww) / 2;
        wy = (this.height - wh) / 2;
        lx = wx + 8;
        vx = lx + 70;
        vw = Math.min(ww - 110, 320);
        int titleBarHeight = 26, topPadding = 10;
        contentTopY = wy + 3 + titleBarHeight + topPadding;
        contentBottomY = wy + wh - 38; // reserve bottom row for save/cancel
        int y = contentTopY - scrollOffset;

        // === Basic ===
        int top = y; y += 14;
        addLabel("gui.display", y); displayNameInput = zbox(vx, y, vw, entry.displayName() != null ? entry.displayName() : entry.name());
        displayNameInput.setResponder(s -> dirty = true); y += 22;
        addLabel("gui.dimension", y); zcycle(vx, vw, y, dimIdx, dimDisplayNames(), v -> { dimIdx = v; dirty = true; rebuild(); }); y += 22;
        addLabel("gui.enter_mode", y); zcycle(vx, vw, y, enterIdx, toModeNames(), v -> { enterIdx = v; dirty = true; rebuild(); }); y += 22;
        addLabel("gui.leave_mode", y); zcycle(vx, vw, y, leaveIdx, toModeNames(), v -> { leaveIdx = v; dirty = true; rebuild(); }); y += 22;
        sections.add(new SectionPos("  " + LocalizationManager.translate("gui.section_basic") + "  ", top, y - top));

        // === Bounds ===
        top = y; y += 14;
        zbtn(lx, y, vw + 40, LocalizationManager.translate("gui.bounds") + ": " + boundsDisplayName(),
            () -> { nextBoundsMode(); dirty = true; rebuild(); }); y += 22;
        if ("POLYGON".equals(boundsMode)) {
            String vInfo = entry.boundsCoordsJson() != null ? entry.boundsCoordsJson() : "[]";
            var polyMsg = net.minecraft.network.chat.Component.literal(
                String.format(LocalizationManager.translate("bounds.polygon"), countPolygonVertices(vInfo)));
            zlabel(vx, y, polyMsg.getString()); y += 20;
            zlabel(vx, y, "  " + LocalizationManager.translate("gui.bounds_polygon_hint")); y += 20;
        } else if ("CIRCLE".equals(boundsMode)) {
            addLabel("gui.bounds_center", y); bx1 = zcoord(vx, y, initBx1); bz1 = zcoord(vx + 64, y, initBz1);
            bx1.setResponder(s -> { dirty = true; initBx1 = s; }); bz1.setResponder(s -> { dirty = true; initBz1 = s; });
            zbtn(vx + 128, y, 56, LocalizationManager.translate("gui.bounds_use_pos"), () -> { bx1.setValue(fmtPx()); bz1.setValue(fmtPz()); }); y += 20;
            addLabel("gui.bounds_radius", y); bx2 = zcoord(vx, y, initBx2);
            bx2.setResponder(s -> { dirty = true; initBx2 = s; });
        } else {
            addLabel("gui.bounds_min", y); bx1 = zcoord(vx, y, initBx1); bz1 = zcoord(vx + 64, y, initBz1);
            bx1.setResponder(s -> { dirty = true; initBx1 = s; }); bz1.setResponder(s -> { dirty = true; initBz1 = s; });
            zbtn(vx + 128, y, 56, LocalizationManager.translate("gui.bounds_use_pos"), () -> { bx1.setValue(fmtPx()); bz1.setValue(fmtPz()); }); y += 20;
            addLabel("gui.bounds_max", y); bx2 = zcoord(vx, y, initBx2); bz2 = zcoord(vx + 64, y, initBz2);
            bx2.setResponder(s -> { dirty = true; initBx2 = s; }); bz2.setResponder(s -> { dirty = true; initBz2 = s; });
            zbtn(vx + 128, y, 56, LocalizationManager.translate("gui.bounds_use_pos"), () -> { bx2.setValue(fmtPx()); bz2.setValue(fmtPz()); });
        }
        y += 26;
        sections.add(new SectionPos("  " + LocalizationManager.translate("gui.section_bounds") + "  ", top, y - top));

        // === Protection (collapsible) ===
        top = y; y += 14;
        String fold = protectionExpanded ? "  \u25BC" : "  \u25B6";
        zbtn(lx, y, vw + 40, LocalizationManager.translate("gui.protection") + ": " + protCount() + "/9 " + LocalizationManager.translate("gui.prot_enabled") + fold,
            () -> { protectionExpanded = !protectionExpanded; rebuild(); }); y += 20;
        if (protectionExpanded) {
            zbtn(lx, y, 68, LocalizationManager.translate("gui.prot_enable_all"), () -> { setAllProt(true); dirty = true; rebuild(); });
            zbtn(lx + 72, y, 68, LocalizationManager.translate("gui.prot_disable_all"), () -> { setAllProt(false); dirty = true; rebuild(); }); y += 20;
            // 3-column grid for 9 protection toggles (button shows short name, color = state)
            int colW = (vw + 40 - 8) / 3;
            zprot(lx,             y, colW, "gui.prot_block_break",    protBreak,    v -> { protBreak = v; dirty = true; rebuild(); });
            zprot(lx + colW + 4,  y, colW, "gui.prot_pvp",            protPvp,      v -> { protPvp = v; dirty = true; rebuild(); });
            zprot(lx + 2*(colW+4), y, colW, "gui.prot_explosion",     protExplosion,v -> { protExplosion = v; dirty = true; rebuild(); }); y += 18;
            zprot(lx,             y, colW, "gui.prot_block_place",    protPlace,    v -> { protPlace = v; dirty = true; rebuild(); });
            zprot(lx + colW + 4,  y, colW, "gui.prot_entity_damage",  protDamage,   v -> { protDamage = v; dirty = true; rebuild(); });
            zprot(lx + 2*(colW+4), y, colW, "gui.prot_container",     protContainer,v -> { protContainer = v; dirty = true; rebuild(); }); y += 18;
            zprot(lx,             y, colW, "gui.prot_block_interact", protInteract, v -> { protInteract = v; dirty = true; rebuild(); });
            zprot(lx + colW + 4,  y, colW, "gui.prot_fluid_place",    protFluid,    v -> { protFluid = v; dirty = true; rebuild(); });
            zprot(lx + 2*(colW+4), y, colW, "gui.prot_item_drop",     protItemDrop, v -> { protItemDrop = v; dirty = true; rebuild(); }); y += 18;
        }
        y += 8;
        zbtn(lx, y, vw + 40, LocalizationManager.translate(enabled ? "area.enabled" : "area.disabled"), () -> { enabled = !enabled; dirty = true; rebuild(); }); y += 28;
        sections.add(new SectionPos("  " + LocalizationManager.translate("gui.section_protection") + "  ", top, y - top));

        // === Schedule (collapsible) ===
        top = y; y += 14;
        String schedFold = schedExpanded ? "  \u25BC" : "  \u25B6";
        String schedLabel = "  " + LocalizationManager.translate("gui.section_schedule") + (schedEnabled ? " \u2714" : " \u2718");
        zbtn(lx, y, vw + 40, schedLabel + schedFold,
            () -> { schedExpanded = !schedExpanded; rebuild(); });
        tooltips.add(new TooltipZone(lx, y, vw + 40, 18, "gui.tooltip_schedule"));
        y += 20;
        if (schedExpanded) {
            zbtn(lx, y, vw + 40, LocalizationManager.translate(schedEnabled ? "area.enabled" : "area.disabled"),
                () -> { schedEnabled = !schedEnabled; dirty = true; rebuild(); }); y += 20;
            addLabel("gui.schedule_time_min", y); schedMin = ztime(vx, y, initSchedMin);
            schedMin.setResponder(s -> { dirty = true; initSchedMin = s; }); y += 20;
            addLabel("gui.schedule_time_max", y); schedMax = ztime(vx, y, initSchedMax);
            schedMax.setResponder(s -> { dirty = true; initSchedMax = s; });
        }
        y += 8;
        sections.add(new SectionPos("  " + LocalizationManager.translate("gui.section_schedule") + "  ", top, y - top));

        // === Condition (collapsible) ===
        top = y; y += 14;
        String condFold = condExpanded ? "  \u25BC" : "  \u25B6";
        String condLabel = "  " + LocalizationManager.translate("gui.section_condition") + (condEnabled ? " \u2714" : " \u2718");
        zbtn(lx, y, vw + 40, condLabel + condFold,
            () -> { condExpanded = !condExpanded; rebuild(); });
        tooltips.add(new TooltipZone(lx, y, vw + 40, 18, "gui.tooltip_condition"));
        y += 20;
        if (condExpanded) {
            zbtn(lx, y, vw + 40, LocalizationManager.translate(condEnabled ? "area.enabled" : "area.disabled"),
                () -> { condEnabled = !condEnabled; dirty = true; rebuild(); }); y += 20;
            addLabel("gui.cond_min_players", y); condMinPlayers = zcoord(vx, y, initCondMinPlayers);
            condMinPlayers.setResponder(s -> { dirty = true; initCondMinPlayers = s; }); y += 20;
            addLabel("gui.cond_require_player", y);
            condRequirePlayer = zbox(vx, y, 120, initCondRequirePlayer);
            condRequirePlayer.setResponder(s -> { dirty = true; initCondRequirePlayer = s; });
        }
        y += 8;
        sections.add(new SectionPos("  " + LocalizationManager.translate("gui.section_condition") + "  ", top, y - top));

        // === Chain (collapsible) ===
        top = y; y += 14;
        String chainFold = chainExpanded ? "  \u25BC" : "  \u25B6";
        zbtn(lx, y, vw + 40, "  " + LocalizationManager.translate("gui.section_chain") + chainFold,
            () -> { chainExpanded = !chainExpanded; rebuild(); });
        tooltips.add(new TooltipZone(lx, y, vw + 40, 18, "gui.tooltip_chain"));
        y += 20;
        if (chainExpanded) {
            addLabel("gui.chain_next", y);
            chainNextInput = zbox(vx, y, 160, initChainNext);
            chainNextInput.setResponder(s -> { dirty = true; initChainNext = s; });
        }
        y += 8;
        sections.add(new SectionPos("  " + LocalizationManager.translate("gui.section_chain") + "  ", top, y - top));

        // === Other ===
        top = y; y += 14;
        zbtn(lx, y, vw + 40, "+ " + LocalizationManager.translate("gui.trigger_settings"),
            () -> { if (this.minecraft != null) this.minecraft.setScreen(new TriggerEditPanel(AreaEditPanel.this, parentScreen, entry)); });
        tooltips.add(new TooltipZone(lx, y, vw + 40, 18, "gui.tooltip_trigger_settings")); y += 20;
        zbtn(lx, y, vw + 40, "+ " + LocalizationManager.translate("gui.whitelist_settings"),
            () -> { if (this.minecraft != null) this.minecraft.setScreen(new WhitelistEditPanel(AreaEditPanel.this, parentScreen, entry)); });
        tooltips.add(new TooltipZone(lx, y, vw + 40, 18, "gui.tooltip_whitelist_settings")); y += 20;
        zbtn(lx, y, vw + 40, "+ " + LocalizationManager.translate("gui.prot_whitelist"),
            () -> { if (this.minecraft != null) this.minecraft.setScreen(new WhitelistEditPanel(AreaEditPanel.this, parentScreen, entry, true)); });
        tooltips.add(new TooltipZone(lx, y, vw + 40, 18, "gui.tooltip_prot_whitelist")); y += 20;
        zbtn(lx, y, vw + 40, "+ " + LocalizationManager.translate("gui.restriction_settings"),
            () -> { if (this.minecraft != null) this.minecraft.setScreen(new RestrictionEditPanel(AreaEditPanel.this, parentScreen, entry)); });
        tooltips.add(new TooltipZone(lx, y, vw + 40, 18, "gui.tooltip_restriction_settings"));
        y += 28;
        sections.add(new SectionPos("  " + LocalizationManager.translate("gui.section_other") + "  ", top, y - top));

        // Record content height and clamp scrollOffset
        contentHeight = (y + scrollOffset) - contentTopY;
        int visibleH = contentBottomY - contentTopY;
        int maxScroll = Math.max(0, contentHeight - visibleH);
        if (scrollOffset > maxScroll) { scrollOffset = maxScroll; }

        // Save / Cancel — centered at bottom (kept as fields so render() can draw them outside the scissor clip)
        int btnY = wy + wh - 30;
        int cx = wx + ww / 2;
        saveBtn = GlassButton.create(cx - 78, btnY, 70, 18, "[" + LocalizationManager.translate("gui.save") + "]", b -> onSaveAction());
        cancelBtn = GlassButton.create(cx + 8, btnY, 70, 18, "[" + LocalizationManager.translate("gui.cancel") + "]", b -> onClose());
        // P1-3 fix: do not register to addRenderableWidget — they live below the scissor clip and are rendered/hit-tested manually to avoid double rendering.
        prevBoundsMode = boundsMode;
    }

    private GlassButton saveBtn, cancelBtn;

    // === Widget builders ===
    private void zbtn(int x, int y, int w, String text, Runnable a) { addRenderableWidget(GlassButton.create(x, y, w, 18, text, b -> a.run())); }
    private EditBox zbox(int x, int y, int w, String v) {
        EditBox e = new EditBox(this.font, x, y, w, 16, Component.empty()); e.setMaxLength(48); e.setValue(v);
        // Allow formatting character (§) in the edit box
        e.setFilter(s -> true);
        addRenderableWidget(e); allBoxes.add(e); return e;
    }
    private EditBox zcoord(int x, int y, String initial) {
        EditBox e = new EditBox(this.font, x, y, 60, 16, Component.empty()); e.setMaxLength(9); e.setValue(initial);
        e.setFilter(s -> s.isEmpty() || s.equals("-") || java.util.regex.Pattern.matches("-?\\d{0,8}", s));
        addRenderableWidget(e); allBoxes.add(e); return e;
    }
    private EditBox ztime(int x, int y, String initial) {
        EditBox e = new EditBox(this.font, x, y, 60, 16, Component.literal(LocalizationManager.translate("gui.schedule_time_hint")));
        e.setMaxLength(9); e.setValue(initial);
        e.setFilter(s -> s.isEmpty() || java.util.regex.Pattern.matches("\\d{0,9}", s));
        addRenderableWidget(e); allBoxes.add(e); return e;
    }
    private void zprot(int x, int y, int w, String key, boolean val, java.util.function.Consumer<Boolean> s) {
        // Short label only; color distinguishes enabled (green) vs disabled (gray)
        String label = LocalizationManager.translate(key) + (val ? " \u2714" : " \u2718");
        addRenderableWidget(GlassButton.create(x, y, w, 18, label, b -> s.accept(!val)));
        String tooltipKey = "gui.tooltip_" + key.substring(4);
        tooltips.add(new TooltipZone(x, y, w, 18, tooltipKey));
    }
    private void zcycle(int vx, int vw, int y, int idx, List<String> opts, java.util.function.IntConsumer cb) {
        zbtn(vx, y, 18, "\u25C0", () -> cb.accept((idx - 1 + opts.size()) % opts.size()));
        zbtn(vx + 20, y, vw - 40, opts.get(idx), () -> {});
        zbtn(vx + vw - 20, y, 18, "\u25B6", () -> cb.accept((idx + 1) % opts.size()));
    }
    private void addLabel(String key, int y) { labels.add(new LabelPos(lx, y, key)); }
    private List<String> toModeNames() { return java.util.Arrays.stream(MODE_KEYS).map(LocalizationManager::translate).toList(); }
    private List<String> dimDisplayNames() {
        return DIMENSIONS.stream().map(AreaEditPanel::shortDim).toList();
    }
    private static String shortDim(String dim) {
        return switch (dim) {
            case "minecraft:overworld" -> "overworld";
            case "minecraft:the_nether" -> "nether";
            case "minecraft:the_end" -> "end";
            default -> dim;
        };
    }
    private void setAllProt(boolean v) { protBreak = protPlace = protInteract = protPvp = protExplosion = protDamage = protContainer = protFluid = protItemDrop = v; }
    private String fmtPx() { return String.valueOf((int)Math.floor(this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getX() : 0)); }
    private String fmtPz() { return String.valueOf((int)Math.floor(this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getZ() : 0)); }
    private String boundsDisplayName() {
        return LocalizationManager.translate(
            "CIRCLE".equals(boundsMode) ? "gui.bounds_circle" :
            "POLYGON".equals(boundsMode) ? "gui.bounds_polygon" : "gui.bounds_rectangle");
    }
    private void nextBoundsMode() {
        if ("RECTANGLE".equals(boundsMode)) boundsMode = "CIRCLE";
        else if ("CIRCLE".equals(boundsMode)) boundsMode = "POLYGON";
        else boundsMode = "RECTANGLE";
    }
    private static int countPolygonVertices(String json) {
        try {
            var obj = new com.google.gson.Gson().fromJson(json, com.google.gson.JsonObject.class);
            if (obj.has("vertices") && obj.get("vertices").isJsonArray())
                return obj.getAsJsonArray("vertices").size();
        } catch (Exception ignored) {}
        return 0;
    }
    private void zlabel(int x, int y, String text) {
        var lbl = new net.minecraft.client.gui.components.AbstractWidget(x, y, vw, 14, Component.literal(text)) {
            @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                g.drawString(AreaEditPanel.this.font, getMessage(), getX(), getY(), 0xC0C4A882);
            }
            @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) {}
        };
        addRenderableWidget(lbl);
    }
    private void rebuild() {
        // Save EditBox values by field reference before clearing widgets
        boolean modeChanged = !prevBoundsMode.equals(boundsMode);
        String savedDisplayName = displayNameInput != null ? displayNameInput.getValue() : null;
        String savedBx1 = bx1 != null ? bx1.getValue() : null;
        String savedBz1 = bz1 != null ? bz1.getValue() : null;
        String savedBx2 = bx2 != null ? bx2.getValue() : null;
        String savedBz2 = bz2 != null ? bz2.getValue() : null;
        String savedSchedMin = schedMin != null ? schedMin.getValue() : null;
        String savedSchedMax = schedMax != null ? schedMax.getValue() : null;
        String savedCondMinPlayers = condMinPlayers != null ? condMinPlayers.getValue() : null;
        String savedCondRequirePlayer = condRequirePlayer != null ? condRequirePlayer.getValue() : null;
        String savedChainNext = chainNextInput != null ? chainNextInput.getValue() : null;

        this.clearWidgets();
        init();

        // Restore values by field reference (skip bx2/bz2 if bounds mode changed — semantic meaning differs)
        if (savedDisplayName != null && displayNameInput != null) displayNameInput.setValue(savedDisplayName);
        if (savedBx1 != null && bx1 != null) bx1.setValue(savedBx1);
        if (savedBz1 != null && bz1 != null) bz1.setValue(savedBz1);
        if (!modeChanged && savedBx2 != null && bx2 != null) bx2.setValue(savedBx2);
        if (!modeChanged && savedBz2 != null && bz2 != null) bz2.setValue(savedBz2);
        if (savedSchedMin != null && schedMin != null) schedMin.setValue(savedSchedMin);
        if (savedSchedMax != null && schedMax != null) schedMax.setValue(savedSchedMax);
        if (savedCondMinPlayers != null && condMinPlayers != null) condMinPlayers.setValue(savedCondMinPlayers);
        if (savedCondRequirePlayer != null && condRequirePlayer != null) condRequirePlayer.setValue(savedCondRequirePlayer);
        if (savedChainNext != null && chainNextInput != null) chainNextInput.setValue(savedChainNext);
    }
    private static int v(EditBox b) { return Integer.parseInt(b.getValue()); }

    private void showValidationError(String msg) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(msg).withStyle(ChatFormatting.RED), true);
        }
    }

    private void onSaveAction() {
        var json = new com.google.gson.JsonObject();
        String dn = displayNameInput.getValue().trim();
        if (!dn.isEmpty() && !dn.equals(entry.name())) json.addProperty("displayName", dn);
        json.addProperty("dimension", DIMENSIONS.get(dimIdx)); json.addProperty("enterMode", GAME_MODES.get(enterIdx));
        json.addProperty("leaveMode", GAME_MODES.get(leaveIdx)); json.addProperty("enabled", enabled);
        json.addProperty("protBlockBreak", protBreak); json.addProperty("protBlockPlace", protPlace);
        json.addProperty("protBlockInteract", protInteract); json.addProperty("protPvp", protPvp);
        json.addProperty("protExplosion", protExplosion); json.addProperty("protEntityDamage", protDamage);
        json.addProperty("protContainerInteract", protContainer); json.addProperty("protFluidPlace", protFluid);
        json.addProperty("protItemDrop", protItemDrop);
        json.addProperty("boundsType", boundsMode);
        try {
            if ("POLYGON".equals(boundsMode)) {
                // POLYGON: preserve existing vertices, skip coordinate editing
            } else if ("CIRCLE".equals(boundsMode)) {
                if (bx1 == null || bz1 == null || bx2 == null
                    || bx1.getValue().trim().isEmpty() || bz1.getValue().trim().isEmpty()
                    || bx2.getValue().trim().isEmpty()) {
                    showValidationError(LocalizationManager.translate("gui.error_bounds_required"));
                    return;
                }
                int radius = v(bx2);
                if (radius <= 0) {
                    showValidationError(LocalizationManager.translate("gui.error_radius_positive"));
                    return;
                }
                json.addProperty("centerX", v(bx1)); json.addProperty("centerZ", v(bz1));
                json.addProperty("radius", radius);
            } else {
                if (bx1 == null || bz1 == null || bx2 == null || bz2 == null
                    || bx1.getValue().trim().isEmpty() || bz1.getValue().trim().isEmpty()
                    || bx2.getValue().trim().isEmpty() || bz2.getValue().trim().isEmpty()) {
                    showValidationError(LocalizationManager.translate("gui.error_bounds_required"));
                    return;
                }
                int minX = v(bx1), minZ = v(bz1), maxX = v(bx2), maxZ = v(bz2);
                if (minX > maxX || minZ > maxZ) {
                    showValidationError(LocalizationManager.translate("gui.error_bounds_range"));
                    return;
                }
                json.addProperty("minX", minX); json.addProperty("minZ", minZ);
                json.addProperty("maxX", maxX); json.addProperty("maxZ", maxZ);
            }
        } catch (NumberFormatException ignored) {
            showValidationError(LocalizationManager.translate("gui.error_bounds_number"));
            return;
        }
        // Schedule — always write so folded sections preserve current server values (P1-1 fix)
        {
            var schedObj = new com.google.gson.JsonObject();
            schedObj.addProperty("enabled", schedEnabled);
            String smin = schedMin != null ? schedMin.getValue() : initSchedMin;
            String smax = schedMax != null ? schedMax.getValue() : initSchedMax;
            // P2 #35: validate schedule time range (0..24000 Minecraft ticks); min must be <= max
            int tmin = -1, tmax = -1;
            try { if (smin != null && !smin.isEmpty()) tmin = Integer.parseInt(smin); }
            catch (NumberFormatException e) {
                showValidationError(LocalizationManager.translate("gui.error_bounds_number"));
                return;
            }
            try { if (smax != null && !smax.isEmpty()) tmax = Integer.parseInt(smax); }
            catch (NumberFormatException e) {
                showValidationError(LocalizationManager.translate("gui.error_bounds_number"));
                return;
            }
            if (tmin != -1 && (tmin < 0 || tmin > 24000)) {
                showValidationError(LocalizationManager.translate("gui.error.schedule_range"));
                return;
            }
            if (tmax != -1 && (tmax < 0 || tmax > 24000)) {
                showValidationError(LocalizationManager.translate("gui.error.schedule_range"));
                return;
            }
            if (tmin != -1 && tmax != -1 && tmin > tmax) {
                showValidationError(LocalizationManager.translate("gui.error.schedule_order"));
                return;
            }
            if (tmin != -1) schedObj.addProperty("timeMin", tmin);
            if (tmax != -1) schedObj.addProperty("timeMax", tmax);
            json.add("schedule", schedObj);
        }
        // Condition — always write (P1-1 fix)
        {
            var condObj = new com.google.gson.JsonObject();
            condObj.addProperty("enabled", condEnabled);
            String cmp = condMinPlayers != null ? condMinPlayers.getValue() : initCondMinPlayers;
            String crp = condRequirePlayer != null ? condRequirePlayer.getValue() : initCondRequirePlayer;
            try { if (cmp != null && !cmp.isEmpty()) condObj.addProperty("minPlayers", Integer.parseInt(cmp)); }
            catch (NumberFormatException e) {
                // P2 #39: surface number parse errors to user instead of silently dropping
                showValidationError(LocalizationManager.translate("gui.error_bounds_number"));
                return;
            }
            if (crp != null && !crp.trim().isEmpty()) condObj.addProperty("requirePlayer", crp.trim().toLowerCase());
            json.add("condition", condObj);
        }
        // Chain — always write; empty chainNext clears server chain (P1-10 fix)
        {
            var chainObj = new com.google.gson.JsonObject();
            String cn = chainNextInput != null ? chainNextInput.getValue().trim() : initChainNext.trim();
            if (!cn.isEmpty()) chainObj.addProperty("chainNext", cn);
            json.add("chain", chainObj);
        }
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.UPDATE, entry.name(), json.toString()));
        dirty = false;
        // P1-4 fix: do not close immediately — give the server a brief window to reject (action bar msg) before closing
        saving = true;
        savingStartMs = System.currentTimeMillis();
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(LocalizationManager.translate("gui.saving")).withStyle(ChatFormatting.YELLOW), true);
        }
    }

    @Override public void onClose() { if (this.minecraft != null) { this.minecraft.setScreen(parentScreen); } }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        // P1-4 fix: auto-close after brief saving window so server rejection (action bar) is visible
        if (saving && System.currentTimeMillis() - savingStartMs > 1500) {
            saving = false;
            // P2 #30: only refresh parent list when an actual save happened
            parentScreen.updateAfterEdit();
            onClose();
            return;
        }
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

        // Clip content area so scrolling doesn't bleed into title/bottom-button rows
        g.enableScissor(wx + 2, contentTopY, wx + ww - 2, contentBottomY);
        // Section backgrounds — unified width based on panel size
        int margin = 8;
        int secW = ww - (margin * 2);
        int startX = wx + margin;
        for (SectionPos s : sections) {
            g.fill(startX, s.y, startX + secW, s.y + s.h, 0x30C4A882);
            g.fill(startX, s.y, startX + secW, s.y + 1, BORDER_GOLD);
            g.drawString(this.font, Component.literal(s.title).withStyle(ChatFormatting.DARK_GRAY), startX + 2, s.y + 2, 0xFF8B6914);
        }
        for (LabelPos l : labels) g.drawString(this.font, Component.literal(LocalizationManager.translate(l.key)).withStyle(ChatFormatting.GRAY), l.x, l.y + 1, 0xFFFFFFFF);
        super.render(g, mx, my, pt);
        g.disableScissor();

        // Re-draw save/cancel manually (they sit below the scissor clip)
        if (saveBtn != null) saveBtn.render(g, mx, my, pt);
        if (cancelBtn != null) cancelBtn.render(g, mx, my, pt);

        // Scrollbar
        int visibleH = contentBottomY - contentTopY;
        int maxScroll = Math.max(0, contentHeight - visibleH);
        if (maxScroll > 0) {
            int barX = wx + ww - 6;
            int barH = Math.max(20, visibleH * visibleH / Math.max(1, contentHeight));
            int barY = contentTopY + (visibleH - barH) * scrollOffset / Math.max(1, maxScroll);
            g.fill(barX, contentTopY, barX + 4, contentBottomY, 0x408B6914);
            g.fill(barX, barY, barX + 4, barY + barH, 0xC08B6914);
        }

        // Tooltip rendering
        for (TooltipZone t : tooltips) {
            if (mx >= t.x && mx <= t.x + t.w && my >= t.y && my <= t.y + t.h) {
                String tip = LocalizationManager.translate(t.key);
                if (tip.equals(t.key)) continue; // translation not found, skip
                renderTooltip(g, mx, my, tip);
                break;
            }
        }

        // Confirm dialog (on top of everything)
        if (confirmDialog.isVisible()) {
            confirmDialog.render(g, this.width, this.height);
        }
    }

    private void renderTooltip(GuiGraphics g, int mx, int my, String text) {
        int padding = 4;
        int tw = this.font.width(text) + padding * 2;
        int th = this.font.lineHeight + padding * 2;
        int tx = Math.min(mx + 8, this.width - tw - 4);
        int ty = my - th - 4;
        if (ty < 4) ty = my + 12; // flip below if too close to top

        g.fill(tx, ty, tx + tw, ty + th, 0xE03A2A1A);
        g.fill(tx, ty, tx + tw, ty + 1, 0xC08B6914);
        g.fill(tx, ty + th - 1, tx + tw, ty + th, 0xC08B6914);
        g.fill(tx, ty, tx + 1, ty + th, 0xC08B6914);
        g.fill(tx + tw - 1, ty, tx + tw, ty + th, 0xC08B6914);
        g.drawString(this.font, text, tx + padding, ty + padding, 0xFFD4B896);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset = Math.max(0, scrollOffset - 20);
            rebuild();
        } else if (delta < 0) {
            int visibleH = contentBottomY - contentTopY;
            int maxScroll = Math.max(0, contentHeight - visibleH);
            if (scrollOffset < maxScroll) {
                scrollOffset = Math.min(maxScroll, scrollOffset + 20);
                rebuild();
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // P2 #29: confirmation dialog must consume keyboard input when visible
        if (confirmDialog.isVisible()) {
            return confirmDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        // Forward to whichever EditBox is focused (covers all input boxes uniformly)
        for (EditBox box : allBoxes) {
            if (box.isFocused()) {
                return box.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        // P2 #29: consume text input while confirmation dialog is visible
        if (confirmDialog.isVisible()) return true;
        for (EditBox box : allBoxes) {
            if (box.isFocused()) {
                return box.charTyped(c, modifiers);
            }
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Confirm dialog intercepts all clicks when visible
        if (confirmDialog.isVisible()) {
            confirmDialog.mouseClicked(mx, my, button);
            return true;
        }
        if (mx < wx || mx > wx + ww || my < wy || my > wy + wh) {
            // Clicked outside window — confirm before discarding unsaved edits
            if (dirty) {
                confirmDialog.show(
                    LocalizationManager.translate("gui.confirm_discard_title"),
                    LocalizationManager.translate("gui.confirm_discard_msg"),
                    LocalizationManager.translate("gui.confirm"),
                    LocalizationManager.translate("gui.cancel"),
                    () -> { dirty = false; onClose(); },
                    () -> {});
            } else {
                onClose();
            }
            return true;
        }
        // P1-3 fix: manually dispatch click to unregistered saveBtn/cancelBtn
        if (saveBtn != null && saveBtn.isMouseOver(mx, my) && saveBtn.mouseClicked(mx, my, button)) return true;
        if (cancelBtn != null && cancelBtn.isMouseOver(mx, my) && cancelBtn.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    private record SectionPos(String title, int y, int h) {}
    private record LabelPos(int x, int y, String key) {}
    private record TooltipZone(int x, int y, int w, int h, String key) {}
}
