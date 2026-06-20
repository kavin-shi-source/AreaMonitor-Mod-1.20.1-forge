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
    private final List<Section> sections = new ArrayList<>();
    private final List<LabelPos> labels = new ArrayList<>();

    public TriggerEditPanel(Screen returnScreen, AreaManagementScreen mainScreen, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.trigger_settings") + ": " + entry.name()));
        this.returnScreen = returnScreen; this.mainScreen = mainScreen; this.entry = entry;
    }

    @Override protected void init() {
        super.init(); sections.clear(); labels.clear(); enterCmds.clear(); leaveCmds.clear();
        ww = Math.min(this.width * 78 / 100, 560); wh = Math.min(this.height * 82 / 100, 480);
        wx = (this.width - ww) / 2; wy = (this.height - wh) / 2;
        lx = wx + 8; vx = lx + 85; panelW = Math.min(272, ww - 32);
        int y = wy + 36;

        JsonObject enterJ = safeParse(entry.enterTriggerJson());
        JsonObject leaveJ = safeParse(entry.leaveTriggerJson());

        int top = y - 4;
        y = zsec(y, "Enter", enterJ, enterCmds, e -> enterSound = e, e -> enterSoundVol = e, e -> enterSoundPitch = e,
            e -> enterTitleM = e, e -> enterTitleS = e, e -> enterTpDim = e, e -> enterTpX = e, e -> enterTpY = e, e -> enterTpZ = e);
        sections.add(new Section("  Enter Trigger  ", top, y - top));

        top = y - 4;
        y = zsec(y, "Leave", leaveJ, leaveCmds, e -> leaveSound = e, e -> leaveSoundVol = e, e -> leaveSoundPitch = e,
            e -> leaveTitleM = e, e -> leaveTitleS = e, e -> leaveTpDim = e, e -> leaveTpX = e, e -> leaveTpY = e, e -> leaveTpZ = e);
        sections.add(new Section("  Leave Trigger  ", top, y - top));

        int btnY = Math.max(y + 6, wy + wh - 38);
        zbtn(lx, btnY, 70, LocalizationManager.translate("gui.save"), () -> { sendUpdate(); onClose(); });
        zbtn(lx + 78, btnY, 70, LocalizationManager.translate("gui.cancel"), this::onClose);
    }

    private int zsec(int y, String label, JsonObject json, List<EditBox> cmds,
            java.util.function.Consumer<EditBox> snd, java.util.function.Consumer<EditBox> vol, java.util.function.Consumer<EditBox> pitch,
            java.util.function.Consumer<EditBox> tM, java.util.function.Consumer<EditBox> tS,
            java.util.function.Consumer<EditBox> tD, java.util.function.Consumer<EditBox> tX, java.util.function.Consumer<EditBox> tY, java.util.function.Consumer<EditBox> tZ) {
        zbtn(lx, y, panelW, "▶ " + LocalizationManager.translate("command.commands") + " (" + label + ")", () -> {}); y += 20;
        if (json.has("commands") && json.get("commands").isJsonArray())
            for (var e : json.getAsJsonArray("commands")) { cmds.add(zbox(vx, y, 150, e.getAsString())); y += 18; }
        if (cmds.isEmpty()) cmds.add(zbox(vx, y, 150, ""));
        final int addY = y;
        zbtn(vx + 155, y - 18, 44, "+ " + LocalizationManager.translate("command.add"), () -> { cmds.add(zbox(vx, addY, 150, "")); rebuild(); });
        y += 6;

        // Sound row
        addLab(lx, y, "Sound"); EditBox se = zbox(vx, y, 72, getStr(json, "soundEvent", "")); snd.accept(se);
        addLab(vx + 76, y, "Vol"); EditBox sv = zbox(vx + 100, y, 26, getStr(json, "soundVolume", "1.0")); vol.accept(sv);
        addLab(vx + 130, y, "Pt"); EditBox sp = zbox(vx + 148, y, 26, getStr(json, "soundPitch", "1.0")); pitch.accept(sp);
        y += 20;

        // Title row
        addLab(lx, y, "Title"); EditBox tm = zbox(vx, y, 72, getStr(json, "titleMain", "")); tM.accept(tm);
        addLab(vx + 76, y, "Sub"); EditBox ts = zbox(vx + 100, y, 74, getStr(json, "titleSub", "")); tS.accept(ts);
        y += 20;

        // TP row
        addLab(lx, y, "TP"); EditBox td = zbox(vx, y, 72, getStr(json, "teleportTarget", "")); tD.accept(td);
        String[] xy = safeTpParts(td.getValue());
        EditBox tx = zbox(vx + 76, y, 32, xy[0]); tX.accept(tx);
        EditBox ty = zbox(vx + 112, y, 32, xy[1]); tY.accept(ty);
        EditBox tz = zbox(vx + 148, y, 32, xy[2]); tZ.accept(tz);
        return y + 22;
    }

    private void zbtn(int x, int y, int w, String text, Runnable a) { addRenderableWidget(GlassButton.create(x, y, w, 18, text, b -> a.run())); }
    private EditBox zbox(int x, int y, int w, String v) { EditBox e = new EditBox(this.font, x, y, w, 16, Component.empty()); e.setMaxLength(200); e.setValue(v); addRenderableWidget(e); return e; }
    private void addLab(int x, int y, String t) { labels.add(new LabelPos(x, y, t)); }
    private String getStr(JsonObject j, String k, String d) { return j != null && j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsString() : d; }
    private String[] safeTpParts(String tp) { if (tp == null || tp.isEmpty()) return new String[]{"","",""};
        String[] p = tp.split(" "); if (p.length >= 4) return new String[]{p[1],p[2],p[3]}; if (p.length == 3) return new String[]{p[0],p[1],p[2]}; return new String[]{"","",""}; }
    private JsonObject safeParse(String j) { try { return j != null ? GSON.fromJson(j, JsonObject.class) : new JsonObject(); } catch (Exception e) { return new JsonObject(); } }
    private void rebuild() { clearWidgets(); init(); }

    private String trigJson(List<EditBox> cmds, EditBox snd, EditBox vol, EditBox pitch, EditBox tM, EditBox tS, EditBox tD, EditBox tX, EditBox tY, EditBox tZ) {
        JsonObject j = new JsonObject(); var arr = new JsonArray();
        for (EditBox b : cmds) { String v = b.getValue().trim(); if (!v.isEmpty()) arr.add(v); }
        if (arr.size() > 0) j.add("commands", arr);
        if (snd != null && !snd.getValue().trim().isEmpty()) j.addProperty("soundEvent", snd.getValue().trim());
        if (vol != null) try { j.addProperty("soundVolume", Float.parseFloat(vol.getValue())); } catch (Exception ig) {}
        if (pitch != null) try { j.addProperty("soundPitch", Float.parseFloat(pitch.getValue())); } catch (Exception ig) {}
        if (tM != null && !tM.getValue().trim().isEmpty()) j.addProperty("titleMain", tM.getValue().trim());
        if (tS != null && !tS.getValue().trim().isEmpty()) j.addProperty("titleSub", tS.getValue().trim());
        if (tD != null && !tD.getValue().trim().isEmpty()) {
            String dim = tD.getValue().trim();
            if (tX != null && tY != null && tZ != null && !tX.getValue().trim().isEmpty() && !tY.getValue().trim().isEmpty() && !tZ.getValue().trim().isEmpty())
                dim += " " + tX.getValue().trim() + " " + tY.getValue().trim() + " " + tZ.getValue().trim();
            j.addProperty("teleportTarget", dim);
        }
        return j.toString();
    }

    private void sendUpdate() {
        var json = new JsonObject();
        try { json.add("enterTrigger", GSON.fromJson(trigJson(enterCmds, enterSound, enterSoundVol, enterSoundPitch, enterTitleM, enterTitleS, enterTpDim, enterTpX, enterTpY, enterTpZ), JsonObject.class)); } catch (Exception e) { json.add("enterTrigger", new JsonObject()); }
        try { json.add("leaveTrigger", GSON.fromJson(trigJson(leaveCmds, leaveSound, leaveSoundVol, leaveSoundPitch, leaveTitleM, leaveTitleS, leaveTpDim, leaveTpX, leaveTpY, leaveTpZ), JsonObject.class)); } catch (Exception e) { json.add("leaveTrigger", new JsonObject()); }
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.UPDATE, entry.name(), json.toString()));
    }

    @Override public void onClose() { mainScreen.updateAfterEdit(); if (this.minecraft != null) this.minecraft.setScreen(returnScreen); }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0x80000000);
        g.fill(wx, wy, wx + ww, wy + wh, PARCH_PANEL);
        g.fill(wx + 1, wy + 1, wx + ww - 1, wy + wh - 1, 0xE02A1F14);
        g.fill(wx, wy, wx + ww, wy + 2, BORDER_GOLD); g.fill(wx, wy, wx + 2, wy + wh, BORDER_GOLD);
        g.fill(wx + ww - 2, wy, wx + ww, wy + wh, BORDER_GOLD); g.fill(wx, wy + wh - 2, wx + ww, wy + wh, BORDER_GOLD);
        g.fill(wx + 3, wy + 3, wx + ww - 3, wy + 29, PARCH_DARK);
        g.fill(wx + 3, wy + 28, wx + ww - 3, wy + 29, BORDER_GOLD);
        g.drawCenteredString(this.font, Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE), wx + ww / 2, wy + 9, 0xFFF5DEB3);

        int secW = vx - lx + panelW + 4;
        for (Section s : sections) {
            g.fill(lx - 4, s.y, lx + secW, s.y + s.h, 0x30C4A882);
            g.fill(lx - 4, s.y, lx + secW, s.y + 1, BORDER_GOLD);
            g.drawString(this.font, Component.literal(s.title).withStyle(ChatFormatting.DARK_GRAY), lx + 2, s.y + 2, 0xFF8B6914);
        }
        for (LabelPos l : labels) g.drawString(this.font, Component.literal(l.text).withStyle(ChatFormatting.GRAY), l.x, l.y + 1, 0xFFFFFFFF);
        super.render(g, mx, my, pt);
    }

    private record Section(String title, int y, int h) {}
    private record LabelPos(int x, int y, String text) {}
}
