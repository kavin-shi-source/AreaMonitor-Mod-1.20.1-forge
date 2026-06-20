package com.kavinshi.areamonitor.client.gui.panel;

import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.client.gui.AreaManagementScreen;
import com.kavinshi.areamonitor.network.C2SAreaActionPacket;
import com.kavinshi.areamonitor.network.S2CAreaListPacket;
import com.kavinshi.areamonitor.network.ModNetwork;
import com.kavinshi.areamonitor.client.gui.widget.GlassButton;
import com.google.gson.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Trigger editor — enter/leave triggers with commands, sound, title, teleport.
 * Glass Morphism theme, clean label rendering in drawSectionLabels().
 */
public class TriggerEditPanel extends Screen {

    private static final int PARCH_DARK   = 0xD03A2A1A;
    private static final int PARCH_PANEL  = 0xC0C4A882;
    private static final int BORDER_GOLD  = 0x808B6914;
    private static final int BORDER_SHADOW = 0x405C4033;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Screen returnScreen;
    private final AreaManagementScreen mainScreen;
    private final S2CAreaListPacket.AreaEntry entry;

    private List<EditBox> enterCmds = new ArrayList<>();
    private EditBox enterSound, enterSoundVol, enterSoundPitch;
    private EditBox enterTitleM, enterTitleS;
    private EditBox enterTpDim, enterTpX, enterTpY, enterTpZ;

    private List<EditBox> leaveCmds = new ArrayList<>();
    private EditBox leaveSound, leaveSoundVol, leaveSoundPitch;
    private EditBox leaveTitleM, leaveTitleS;
    private EditBox leaveTpDim, leaveTpX, leaveTpY, leaveTpZ;

    // Layout state
    private int lx, vx, panelW;
    private final List<Section> sections = new ArrayList<>();
    private final List<LabelPos> labels = new ArrayList<>();

    public TriggerEditPanel(Screen returnScreen, AreaManagementScreen mainScreen, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.trigger_settings") + ": " + entry.name()));
        this.returnScreen = returnScreen;
        this.mainScreen = mainScreen;
        this.entry = entry;
    }

    @Override
    protected void init() {
        super.init();
        sections.clear();
        labels.clear();
        enterCmds.clear(); leaveCmds.clear();

        int cx = this.width / 2;
        lx = clamp(cx - this.width / 4, 10, cx - 40);
        vx = lx + 90;
        panelW = Math.min(290, this.width - lx * 2);
        int y = 30;

        JsonObject enterJ = safeParse(entry.enterTriggerJson());
        JsonObject leaveJ = safeParse(entry.leaveTriggerJson());

        // === Enter ===
        int top = y - 8;
        y = buildSection(y, "Enter", enterJ, enterCmds,
            e -> enterSound = e, e -> enterSoundVol = e, e -> enterSoundPitch = e,
            e -> enterTitleM = e, e -> enterTitleS = e,
            e -> enterTpDim = e, e -> enterTpX = e, e -> enterTpY = e, e -> enterTpZ = e);
        sections.add(new Section("  Enter Trigger  ", top, y - top + 2));

        // === Leave ===
        top = y - 8;
        y = buildSection(y, "Leave", leaveJ, leaveCmds,
            e -> leaveSound = e, e -> leaveSoundVol = e, e -> leaveSoundPitch = e,
            e -> leaveTitleM = e, e -> leaveTitleS = e,
            e -> leaveTpDim = e, e -> leaveTpX = e, e -> leaveTpY = e, e -> leaveTpZ = e);
        sections.add(new Section("  Leave Trigger  ", top, y - top + 2));

        int btnY = Math.max(y + 4, this.height - 40);
        addBtn(lx, btnY, 70, LocalizationManager.translate("gui.save"), () -> { sendUpdate(); onClose(); });
        addBtn(lx + 80, btnY, 70, LocalizationManager.translate("gui.cancel"), this::onClose);
    }

    private int buildSection(int y, String label, JsonObject json, List<EditBox> cmds,
            java.util.function.Consumer<EditBox> snd, java.util.function.Consumer<EditBox> vol,
            java.util.function.Consumer<EditBox> pitch,
            java.util.function.Consumer<EditBox> tM, java.util.function.Consumer<EditBox> tS,
            java.util.function.Consumer<EditBox> tD, java.util.function.Consumer<EditBox> tX,
            java.util.function.Consumer<EditBox> tY, java.util.function.Consumer<EditBox> tZ) {

        // Commands header
        addBtn(lx, y, panelW, "▶ " + LocalizationManager.translate("command.commands") + " (" + label + ")", () -> {});
        y += 22;

        // Command inputs
        if (json.has("commands") && json.get("commands").isJsonArray()) {
            for (var e : json.getAsJsonArray("commands")) {
                cmds.add(newEditBox(vx, y, 150, e.getAsString()));
                y += 18;
            }
        }
        if (cmds.isEmpty()) cmds.add(newEditBox(vx, y, 150, ""));

        final int addY = y;
        addBtn(vx + 155, y - 18, 50, "+ " + LocalizationManager.translate("command.add"), () -> {
            cmds.add(newEditBox(vx, addY, 150, "")); rebuild();
        });
        y += 6;

        // Sound
        labels.add(new LabelPos(lx, y, "Sound"));
        EditBox se = newEditBox(vx, y, 80, getStr(json, "soundEvent", "")); snd.accept(se);
        labels.add(new LabelPos(vx + 85, y, "Vol"));
        EditBox sv = newEditBox(vx + 110, y, 30, getStr(json, "soundVolume", "1.0")); vol.accept(sv);
        labels.add(new LabelPos(vx + 145, y, "Pitch"));
        EditBox sp = newEditBox(vx + 175, y, 30, getStr(json, "soundPitch", "1.0")); pitch.accept(sp);
        y += 22;

        // Title
        labels.add(new LabelPos(lx, y, "Title"));
        EditBox tm = newEditBox(vx, y, 80, getStr(json, "titleMain", "")); tM.accept(tm);
        labels.add(new LabelPos(vx + 85, y, "Sub"));
        EditBox ts = newEditBox(vx + 110, y, 95, getStr(json, "titleSub", "")); tS.accept(ts);
        y += 22;

        // TP
        labels.add(new LabelPos(lx, y, "TP"));
        EditBox td = newEditBox(vx, y, 80, getStr(json, "teleportTarget", "")); tD.accept(td);
        String[] xy = safeTpParts(td.getValue());
        EditBox tx = newEditBox(vx + 85, y, 35, xy[0]); tX.accept(tx);
        EditBox ty = newEditBox(vx + 125, y, 35, xy[1]); tY.accept(ty);
        EditBox tz = newEditBox(vx + 165, y, 35, xy[2]); tZ.accept(tz);

        return y + 24;
    }

    private EditBox newEditBox(int x, int y, int w, String val) {
        EditBox eb = new EditBox(this.font, x, y, w, 16, Component.empty());
        eb.setMaxLength(200); eb.setValue(val); addRenderableWidget(eb); return eb;
    }

    private void addBtn(int x, int y, int w, String text, Runnable action) {
        addRenderableWidget(GlassButton.create(x, y, w, 18, text, b -> action.run()));
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    // === JSON helpers ===
    private String getStr(JsonObject j, String key, String def) {
        return j != null && j.has(key) && !j.get(key).isJsonNull() ? j.get(key).getAsString() : def;
    }

    private String[] safeTpParts(String tp) {
        if (tp == null || tp.isEmpty()) return new String[]{"", "", ""};
        String[] p = tp.split(" ");
        if (p.length >= 4) return new String[]{p[1], p[2], p[3]};
        if (p.length == 3) return new String[]{p[0], p[1], p[2]};
        return new String[]{"", "", ""};
    }

    private JsonObject safeParse(String json) {
        try { return json != null ? GSON.fromJson(json, JsonObject.class) : new JsonObject(); }
        catch (Exception e) { return new JsonObject(); }
    }

    private void rebuild() { this.clearWidgets(); init(); }

    private String buildTrigJson(List<EditBox> cmds, EditBox snd, EditBox vol, EditBox pitch,
                                   EditBox tM, EditBox tS, EditBox tD, EditBox tX, EditBox tY, EditBox tZ) {
        JsonObject j = new JsonObject();
        var arr = new JsonArray();
        for (EditBox b : cmds) { String v = b.getValue().trim(); if (!v.isEmpty()) arr.add(v); }
        if (arr.size() > 0) j.add("commands", arr);
        if (snd != null && !snd.getValue().trim().isEmpty()) j.addProperty("soundEvent", snd.getValue().trim());
        if (vol != null) try { j.addProperty("soundVolume", Float.parseFloat(vol.getValue())); } catch (Exception ig) {}
        if (pitch != null) try { j.addProperty("soundPitch", Float.parseFloat(pitch.getValue())); } catch (Exception ig) {}
        if (tM != null && !tM.getValue().trim().isEmpty()) j.addProperty("titleMain", tM.getValue().trim());
        if (tS != null && !tS.getValue().trim().isEmpty()) j.addProperty("titleSub", tS.getValue().trim());
        if (tD != null && !tD.getValue().trim().isEmpty()) {
            String dim = tD.getValue().trim();
            if (tX != null && tY != null && tZ != null &&
                !tX.getValue().trim().isEmpty() && !tY.getValue().trim().isEmpty() && !tZ.getValue().trim().isEmpty())
                dim += " " + tX.getValue().trim() + " " + tY.getValue().trim() + " " + tZ.getValue().trim();
            j.addProperty("teleportTarget", dim);
        }
        return j.toString();
    }

    private void sendUpdate() {
        var json = new JsonObject();
        try { json.add("enterTrigger", GSON.fromJson(buildTrigJson(enterCmds,
            enterSound, enterSoundVol, enterSoundPitch, enterTitleM, enterTitleS,
            enterTpDim, enterTpX, enterTpY, enterTpZ), JsonObject.class));
        } catch (Exception e) { json.add("enterTrigger", new JsonObject()); }
        try { json.add("leaveTrigger", GSON.fromJson(buildTrigJson(leaveCmds,
            leaveSound, leaveSoundVol, leaveSoundPitch, leaveTitleM, leaveTitleS,
            leaveTpDim, leaveTpX, leaveTpY, leaveTpZ), JsonObject.class));
        } catch (Exception e) { json.add("leaveTrigger", new JsonObject()); }
        ModNetwork.sendToServer(new C2SAreaActionPacket(
            C2SAreaActionPacket.Action.UPDATE, entry.name(), json.toString()));
    }

    @Override
    public void onClose() {
        mainScreen.updateAfterEdit();
        if (this.minecraft != null) this.minecraft.setScreen(returnScreen);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int cx = this.width / 2;
        g.fill(0, 0, this.width, 31, PARCH_DARK);
        g.fill(0, 30, this.width, 31, BORDER_GOLD);
        g.drawCenteredString(this.font,
            Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE), cx, 10, 0xFFF5DEB3);

        for (Section s : sections) {
            g.fill(lx - 6, s.y, lx + panelW + 10, s.y + s.h, PARCH_PANEL);
            g.fill(lx - 6, s.y, lx + panelW + 10, s.y + 1, BORDER_GOLD);
            g.fill(lx - 6, s.y + s.h - 1, lx + panelW + 10, s.y + s.h, BORDER_SHADOW);
            g.drawString(this.font, Component.literal(s.title).withStyle(ChatFormatting.DARK_GRAY),
                lx + 2, s.y + 2, 0xFF8B6914);
        }

        // Render labels as plain text (no widgets)
        for (LabelPos l : labels) {
            g.drawString(this.font, Component.literal(l.text).withStyle(ChatFormatting.GRAY),
                l.x, l.y + 2, 0xFFFFFFFF);
        }

        super.render(g, mx, my, pt);
    }

    private record Section(String title, int y, int h) {}
    private record LabelPos(int x, int y, String text) {}
}
