package com.kavinshi.areamonitor.client.gui.panel;

import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.client.gui.AreaManagementScreen;
import com.kavinshi.areamonitor.network.C2SAreaActionPacket;
import com.kavinshi.areamonitor.network.S2CAreaListPacket;
import com.kavinshi.areamonitor.network.ModNetwork;
import com.kavinshi.areamonitor.client.gui.widget.ConfirmDialog;
import com.kavinshi.areamonitor.client.gui.widget.GlassButton;
import com.google.gson.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class TriggerEditPanel extends Screen {

    private static final int PARCH_DARK  = 0xD03A2A1A;
    private static final int PARCH_PANEL = 0xC0C4A882;
    private static final int BORDER_GOLD = 0x808B6914;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Screen returnScreen;
    private final AreaManagementScreen mainScreen;
    private final S2CAreaListPacket.AreaEntry entry;

    private List<EditBox> enterCmds = new ArrayList<>(), leaveCmds = new ArrayList<>();
    private EditBox enterSound, enterSoundVol, enterSoundPitch, enterTitleM, enterTitleS, enterTpDim, enterTpX, enterTpY, enterTpZ;
    private EditBox leaveSound, leaveSoundVol, leaveSoundPitch, leaveTitleM, leaveTitleS, leaveTpDim, leaveTpX, leaveTpY, leaveTpZ;

    private int wx, wy, ww, wh, lx, vx, panelW;
    // Scroll support
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int contentTopY, contentBottomY;
    // Unsaved-change tracking & confirm dialog
    private boolean dirty = false;
    private final ConfirmDialog confirmDialog = new ConfirmDialog();
    private GlassButton saveBtn, cancelBtn;

    private final List<Section> sections = new ArrayList<>();
    private final List<LabelPos> labels = new ArrayList<>();
    private final List<TooltipZone> tooltips = new ArrayList<>();

    // ===== State persistence =====
    private boolean initializedData = false;
    private boolean enterExpanded = true;
    private boolean leaveExpanded = false;

    private final List<String> enterCmdValues = new ArrayList<>();
    private String enterSoundVal = "", enterVolVal = "1.0", enterPitchVal = "1.0";
    private String enterTitleMVal = "", enterTitleSVal = "";
    private String enterTpDimVal = "", enterTpXVal = "", enterTpYVal = "", enterTpZVal = "";

    private final List<String> leaveCmdValues = new ArrayList<>();
    private String leaveSoundVal = "", leaveVolVal = "1.0", leavePitchVal = "1.0";
    private String leaveTitleMVal = "", leaveTitleSVal = "";
    private String leaveTpDimVal = "", leaveTpXVal = "", leaveTpYVal = "", leaveTpZVal = "";

    public TriggerEditPanel(Screen returnScreen, AreaManagementScreen mainScreen, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.trigger_settings") + ": " + entry.name()));
        this.returnScreen = returnScreen; this.mainScreen = mainScreen; this.entry = entry;
    }

    @Override
    protected void init() {
        super.init();
        sections.clear(); labels.clear(); tooltips.clear(); enterCmds.clear(); leaveCmds.clear();

        ww = Math.min(this.width * 78 / 100, 560);
        wh = Math.min(this.height * 82 / 100, 480);
        wx = (this.width - ww) / 2;
        wy = (this.height - wh) / 2;
        lx = wx + 8;
        vx = lx + 85;
        panelW = ww - 45;
        int titleBarHeight = 26, topPadding = 10;
        contentTopY = wy + 3 + titleBarHeight + topPadding;
        contentBottomY = wy + wh - 38;
        int y = contentTopY - scrollOffset;

        if (!initializedData) {
            JsonObject enterJ = safeParse(entry.enterTriggerJson());
            if (enterJ.has("commands") && enterJ.get("commands").isJsonArray()) {
                for (var e : enterJ.getAsJsonArray("commands")) enterCmdValues.add(e.getAsString());
            }
            enterSoundVal = getStr(enterJ, "soundEvent", "");
            enterVolVal = getStr(enterJ, "soundVolume", "1.0");
            enterPitchVal = getStr(enterJ, "soundPitch", "1.0");
            enterTitleMVal = getStr(enterJ, "titleMain", "");
            enterTitleSVal = getStr(enterJ, "titleSub", "");
            String[] enterTp = safeTpParts(getStr(enterJ, "teleportTarget", ""));
            enterTpDimVal = enterTp[0]; enterTpXVal = enterTp[1]; enterTpYVal = enterTp[2]; enterTpZVal = enterTp[3];

            JsonObject leaveJ = safeParse(entry.leaveTriggerJson());
            if (leaveJ.has("commands") && leaveJ.get("commands").isJsonArray()) {
                for (var e : leaveJ.getAsJsonArray("commands")) leaveCmdValues.add(e.getAsString());
            }
            leaveSoundVal = getStr(leaveJ, "soundEvent", "");
            leaveVolVal = getStr(leaveJ, "soundVolume", "1.0");
            leavePitchVal = getStr(leaveJ, "soundPitch", "1.0");
            leaveTitleMVal = getStr(leaveJ, "titleMain", "");
            leaveTitleSVal = getStr(leaveJ, "titleSub", "");
            String[] leaveTp = safeTpParts(getStr(leaveJ, "teleportTarget", ""));
            leaveTpDimVal = leaveTp[0]; leaveTpXVal = leaveTp[1]; leaveTpYVal = leaveTp[2]; leaveTpZVal = leaveTp[3];

            initializedData = true;
        }

        // === 1. Enter Trigger collapsible section ===
        int top = y; y += 14; // section title row (avoid overlap with button below)
        String enterTitle = "\u00A7a" + (enterExpanded ? "\u25BC " : "\u25B6 ") + "\u00A7f" + LocalizationManager.translate("gui.trigger_enter");
        zbtn(lx, y, panelW, enterTitle, () -> { captureCurrentState(); enterExpanded = !enterExpanded; saveAndRebuild(); }); y += 20;
        if (enterExpanded) {
            y = zsec(y, enterCmds, enterCmdValues,
                e -> enterSound = e, e -> enterSoundVol = e, e -> enterSoundPitch = e,
                e -> enterTitleM = e, e -> enterTitleS = e,
                e -> enterTpDim = e, e -> enterTpX = e, e -> enterTpY = e, e -> enterTpZ = e,
                enterSoundVal, enterVolVal, enterPitchVal, enterTitleMVal, enterTitleSVal, enterTpDimVal, enterTpXVal, enterTpYVal, enterTpZVal);
        }
        y += 6;
        sections.add(new Section("  " + LocalizationManager.translate("gui.trigger_enter") + "  ", top, y - top));

        // === 2. Leave Trigger collapsible section ===
        top = y; y += 14;
        String leaveTitle = "\u00A7c" + (leaveExpanded ? "\u25BC " : "\u25B6 ") + "\u00A7f" + LocalizationManager.translate("gui.trigger_leave");
        zbtn(lx, y, panelW, leaveTitle, () -> { captureCurrentState(); leaveExpanded = !leaveExpanded; saveAndRebuild(); }); y += 20;
        if (leaveExpanded) {
            y = zsec(y, leaveCmds, leaveCmdValues,
                e -> leaveSound = e, e -> leaveSoundVol = e, e -> leaveSoundPitch = e,
                e -> leaveTitleM = e, e -> leaveTitleS = e,
                e -> leaveTpDim = e, e -> leaveTpX = e, e -> leaveTpY = e, e -> leaveTpZ = e,
                leaveSoundVal, leaveVolVal, leavePitchVal, leaveTitleMVal, leaveTitleSVal, leaveTpDimVal, leaveTpXVal, leaveTpYVal, leaveTpZVal);
        }
        sections.add(new Section("  " + LocalizationManager.translate("gui.trigger_leave") + "  ", top, y - top));

        // Record content height and clamp scrollOffset
        contentHeight = (y + scrollOffset) - contentTopY;
        int visibleH = contentBottomY - contentTopY;
        int maxScroll = Math.max(0, contentHeight - visibleH);
        if (scrollOffset > maxScroll) { scrollOffset = maxScroll; }

        // Save / Cancel — centered at bottom (kept as fields so render() can draw them outside the scissor clip)
        int btnY = wy + wh - 30;
        int cx = wx + ww / 2;
        saveBtn = GlassButton.create(cx - 78, btnY, 70, 18, "[" + LocalizationManager.translate("gui.save") + "]", b -> { captureCurrentState(); sendUpdate(); dirty = false; mainScreen.updateAfterEdit(); onClose(); });
        cancelBtn = GlassButton.create(cx + 8, btnY, 70, 18, "[" + LocalizationManager.translate("gui.cancel") + "]", b -> onClose());
        // P1-3 fix: do not register to addRenderableWidget — rendered/hit-tested manually to avoid double rendering.
    }

    private int zsec(int y, List<EditBox> cmds, List<String> cmdValues,
            java.util.function.Consumer<EditBox> snd, java.util.function.Consumer<EditBox> vol, java.util.function.Consumer<EditBox> pitch,
            java.util.function.Consumer<EditBox> tM, java.util.function.Consumer<EditBox> tS,
            java.util.function.Consumer<EditBox> tD, java.util.function.Consumer<EditBox> tX, java.util.function.Consumer<EditBox> tY, java.util.function.Consumer<EditBox> tZ,
            String sndV, String volV, String pitchV, String tMV, String tSV, String tDV, String tXV, String tYV, String tZV) {

        final int cmdW = Math.min(panelW - 60, 340);
        if (cmdValues.isEmpty()) cmdValues.add("");

        for (int i = 0; i < cmdValues.size(); i++) {
            EditBox box = zbox(vx, y, cmdW, cmdValues.get(i));
            cmds.add(box);

            final int idx = i;
            if (cmdValues.size() > 1) {
                zbtn(vx + cmdW + 4, y, 24, "[\u00D7]", () -> {
                    captureCurrentState(); cmdValues.remove(idx); dirty = true; saveAndRebuild();
                });
            }
            y += 18;
        }

        zbtn(vx, y, 60, "+ " + LocalizationManager.translate("command.add"), () -> {
            captureCurrentState();
            if (cmdValues.size() < 20) cmdValues.add("");
            dirty = true; saveAndRebuild();
        });
        y += 22;

        // Sound row
        addLab(lx + 4, y, "gui.trigger_sound"); EditBox se = zbox(vx, y, 160, sndV); snd.accept(se);
        addLab(vx + 166, y, "gui.trigger_volume"); EditBox sv = znum(vx + 188, y, 30, volV); vol.accept(sv);
        addLab(vx + 224, y, "gui.trigger_pitch");  EditBox sp = znum(vx + 240, y, 30, pitchV); pitch.accept(sp);
        tooltips.add(new TooltipZone(vx, y, 160, 16, "gui.tooltip_trigger_sound"));
        y += 20;

        // Title row
        addLab(lx + 4, y, "gui.trigger_title"); EditBox tm = zbox(vx, y, 110, tMV); tM.accept(tm);
        addLab(vx + 116, y, "gui.trigger_subtitle"); EditBox ts = zbox(vx + 140, y, 130, tSV); tS.accept(ts);
        y += 20;

        // TP row
        addLab(lx + 4, y, "gui.trigger_tp_dim"); EditBox td = zbox(vx, y, 90, tDV);
        td.setFilter(s -> s.isEmpty() || s.matches("[a-zA-Z0-9_\\-:]*")); tD.accept(td);
        addLab(vx + 96, y, "gui.trigger_tp_x");   EditBox tx = znum(vx + 106, y, 32, tXV); tX.accept(tx);
        addLab(vx + 142, y, "gui.trigger_tp_y");  EditBox ty = znum(vx + 152, y, 32, tYV); tY.accept(ty);
        addLab(vx + 188, y, "gui.trigger_tp_z");  EditBox tz = znum(vx + 198, y, 32, tZV); tZ.accept(tz);
        tooltips.add(new TooltipZone(vx, y, 90, 16, "gui.tooltip_trigger_tp"));

        return y + 24;
    }

    private void captureCurrentState() {
        if (!enterCmds.isEmpty() && enterSound != null) {
            enterCmdValues.clear();
            for (EditBox b : enterCmds) enterCmdValues.add(b.getValue());
            enterSoundVal = enterSound.getValue(); enterVolVal = enterSoundVol.getValue(); enterPitchVal = enterSoundPitch.getValue();
            enterTitleMVal = enterTitleM.getValue(); enterTitleSVal = enterTitleS.getValue();
            enterTpDimVal = enterTpDim.getValue(); enterTpXVal = enterTpX.getValue(); enterTpYVal = enterTpY.getValue(); enterTpZVal = enterTpZ.getValue();
        }
        if (!leaveCmds.isEmpty() && leaveSound != null) {
            leaveCmdValues.clear();
            for (EditBox b : leaveCmds) leaveCmdValues.add(b.getValue());
            leaveSoundVal = leaveSound.getValue(); leaveVolVal = leaveSoundVol.getValue(); leavePitchVal = leaveSoundPitch.getValue();
            leaveTitleMVal = leaveTitleM.getValue(); leaveTitleSVal = leaveTitleS.getValue();
            leaveTpDimVal = leaveTpDim.getValue(); leaveTpXVal = leaveTpX.getValue(); leaveTpYVal = leaveTpY.getValue(); leaveTpZVal = leaveTpZ.getValue();
        }
    }

    private void saveAndRebuild() {
        clearWidgets();
        init();
    }

    private void zbtn(int x, int y, int w, String text, Runnable a) { addRenderableWidget(GlassButton.create(x, y, w, 18, text, b -> a.run())); }
    private EditBox zbox(int x, int y, int w, String v) {
        EditBox e = new EditBox(this.font, x, y, w, 16, Component.empty());
        e.setMaxLength(200);
        e.setValue(v);
        // Allow formatting character (§) in the edit box
        e.setFilter(s -> true);
        e.setResponder(v2 -> dirty = true);
        addRenderableWidget(e);
        return e;
    }
    private EditBox znum(int x, int y, int w, String v) {
        EditBox e = new EditBox(this.font, x, y, w, 16, Component.empty());
        e.setMaxLength(12);
        e.setValue(v);
        e.setFilter(s -> s.isEmpty() || s.equals("-") || s.equals(".") || s.equals("-.")
            || java.util.regex.Pattern.matches("-?\\d*\\.?\\d*", s));
        e.setResponder(v2 -> dirty = true);
        addRenderableWidget(e);
        return e;
    }
    private void addLab(int x, int y, String key) { labels.add(new LabelPos(x, y, LocalizationManager.translate(key))); }
    private String getStr(JsonObject j, String k, String d) { return j != null && j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsString() : d; }

    private String[] safeTpParts(String tp) {
        if (tp == null || tp.isEmpty()) return new String[]{"", "", "", ""};
        // handle both space-separated (current) and comma-separated (legacy) formats.
        String[] p = tp.contains(" ") ? tp.split("\\s+") : tp.split(",");
        if (p.length >= 4) return new String[]{p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim()};
        if (p.length == 3) return new String[]{"", p[0].trim(), p[1].trim(), p[2].trim()};
        return new String[]{tp.trim(), "", "", ""};
    }

    private JsonObject safeParse(String j) {
        try { return j != null ? GSON.fromJson(j, JsonObject.class) : new JsonObject(); }
        catch (Exception e) {
            // : log parse failures instead of silently swallowing
            com.kavinshi.areamonitor.AreaMonitorMod.LOGGER.warn("Failed to parse trigger JSON: {}", e.getMessage());
            return new JsonObject();
        }
    }
    private void rebuild() { captureCurrentState(); saveAndRebuild(); }

    private java.util.List<EditBox> getAllBoxes() {
        java.util.List<EditBox> all = new java.util.ArrayList<>();
        if (enterCmds != null) all.addAll(enterCmds);
        if (leaveCmds != null) all.addAll(leaveCmds);
        java.util.stream.Stream.of(enterSound, enterSoundVol, enterSoundPitch, enterTitleM, enterTitleS,
            enterTpDim, enterTpX, enterTpY, enterTpZ,
            leaveSound, leaveSoundVol, leaveSoundPitch, leaveTitleM, leaveTitleS,
            leaveTpDim, leaveTpX, leaveTpY, leaveTpZ)
            .filter(Objects::nonNull).forEach(all::add);
        return all;
    }

    private String trigJsonFromValues(List<String> cmdVals, String snd, String vol, String pitch,
            String tM, String tS, String tD, String tX, String tY, String tZ) {
        JsonObject j = new JsonObject(); var arr = new JsonArray();
        if (cmdVals != null) {
            for (String v : cmdVals) { String t = v.trim(); if (!t.isEmpty()) arr.add(t); }
        }
        // always write keys so server can clear fields (empty array / JsonNull clears server-side value)
        j.add("commands", arr);
        if (snd != null && !snd.trim().isEmpty()) j.addProperty("soundEvent", snd.trim());
        else j.add("soundEvent", JsonNull.INSTANCE);
        if (vol != null) try { j.addProperty("soundVolume", Float.parseFloat(vol)); }
        catch (NumberFormatException e) {
            com.kavinshi.areamonitor.AreaMonitorMod.LOGGER.warn("Invalid soundVolume '{}' for trigger, ignoring", vol);
        }
        if (pitch != null) try { j.addProperty("soundPitch", Float.parseFloat(pitch)); }
        catch (NumberFormatException e) {
            com.kavinshi.areamonitor.AreaMonitorMod.LOGGER.warn("Invalid soundPitch '{}' for trigger, ignoring", pitch);
        }
        if (tM != null && !tM.trim().isEmpty()) j.addProperty("titleMain", tM.trim());
        else j.add("titleMain", JsonNull.INSTANCE);
        if (tS != null && !tS.trim().isEmpty()) j.addProperty("titleSub", tS.trim());
        else j.add("titleSub", JsonNull.INSTANCE);
        if (tD != null && !tD.trim().isEmpty()) {
            String dim = tD.trim();
            if (tX != null && tY != null && tZ != null && !tX.trim().isEmpty() && !tY.trim().isEmpty() && !tZ.trim().isEmpty())
                dim += " " + tX.trim() + " " + tY.trim() + " " + tZ.trim();
            j.addProperty("teleportTarget", dim);
        } else {
            j.add("teleportTarget", JsonNull.INSTANCE);
        }
        return j.toString();
    }

    private void sendUpdate() {
        var json = new JsonObject();
        try { json.add("enterTrigger", GSON.fromJson(trigJsonFromValues(enterCmdValues, enterSoundVal, enterVolVal, enterPitchVal, enterTitleMVal, enterTitleSVal, enterTpDimVal, enterTpXVal, enterTpYVal, enterTpZVal), JsonObject.class)); } catch (Exception e) { json.add("enterTrigger", new JsonObject()); }
        try { json.add("leaveTrigger", GSON.fromJson(trigJsonFromValues(leaveCmdValues, leaveSoundVal, leaveVolVal, leavePitchVal, leaveTitleMVal, leaveTitleSVal, leaveTpDimVal, leaveTpXVal, leaveTpYVal, leaveTpZVal), JsonObject.class)); } catch (Exception e) { json.add("leaveTrigger", new JsonObject()); }
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.UPDATE, entry.name(), json.toString()));
    }

    @Override public void onClose() { if (this.minecraft != null) this.minecraft.setScreen(returnScreen); }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // : confirmation dialog must consume keyboard input when visible
        if (confirmDialog.isVisible()) {
            return confirmDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        for (EditBox box : getAllBoxes()) {
            if (box.isFocused()) return box.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char c, int modifiers) {
        // : consume text input while confirmation dialog is visible
        if (confirmDialog.isVisible()) return true;
        for (EditBox box : getAllBoxes()) {
            if (box.isFocused()) return box.charTyped(c, modifiers);
        }
        return super.charTyped(c, modifiers);
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

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (confirmDialog.isVisible()) {
            confirmDialog.mouseClicked(mx, my, button);
            return true;
        }
        if (mx < wx || mx > wx + ww || my < wy || my > wy + wh) {
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

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0x80000000);
        g.fill(wx, wy, wx + ww, wy + wh, PARCH_PANEL);
        g.fill(wx + 1, wy + 1, wx + ww - 1, wy + wh - 1, 0xE02A1F14);
        g.fill(wx, wy, wx + ww, wy + 2, BORDER_GOLD); g.fill(wx, wy, wx + 2, wy + wh, BORDER_GOLD);
        g.fill(wx + ww - 2, wy, wx + ww, wy + wh, BORDER_GOLD); g.fill(wx, wy + wh - 2, wx + ww, wy + wh, BORDER_GOLD);
        g.fill(wx + 3, wy + 3, wx + ww - 3, wy + 29, PARCH_DARK);
        g.fill(wx + 3, wy + 28, wx + ww - 3, wy + 29, BORDER_GOLD);
        g.drawCenteredString(this.font, Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE), wx + ww / 2, wy + 9, 0xFFF5DEB3);

        // Clip content area so scrolling doesn't bleed into title/bottom-button rows
        g.enableScissor(wx + 2, contentTopY, wx + ww - 2, contentBottomY);
        int margin = 8;
        int secW = ww - (margin * 2);
        int startX = wx + margin;
        for (Section s : sections) {
            g.fill(startX, s.y, startX + secW, s.y + s.h, 0x30C4A882);
            g.fill(startX, s.y, startX + secW, s.y + 1, BORDER_GOLD);
            g.drawString(this.font, Component.literal(s.title).withStyle(ChatFormatting.DARK_GRAY), startX + 2, s.y + 2, 0xFF8B6914);
        }
        for (LabelPos l : labels) g.drawString(this.font, Component.literal(l.text).withStyle(ChatFormatting.GRAY), l.x, l.y + 1, 0xFFFFFFFF);
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
                if (tip.equals(t.key)) continue;
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
        if (ty < 4) ty = my + 12;
        g.fill(tx, ty, tx + tw, ty + th, 0xE03A2A1A);
        g.fill(tx, ty, tx + tw, ty + 1, 0xC08B6914);
        g.fill(tx, ty + th - 1, tx + tw, ty + th, 0xC08B6914);
        g.fill(tx, ty, tx + 1, ty + th, 0xC08B6914);
        g.fill(tx + tw - 1, ty, tx + tw, ty + th, 0xC08B6914);
        g.drawString(this.font, text, tx + padding, ty + padding, 0xFFD4B896);
    }

    private record Section(String title, int y, int h) {}
    private record LabelPos(int x, int y, String text) {}
    private record TooltipZone(int x, int y, int w, int h, String key) {}
}
