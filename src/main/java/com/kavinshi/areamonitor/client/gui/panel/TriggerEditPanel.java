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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class TriggerEditPanel extends Screen {

    private static final int GLASS_DARK    = 0xC0000000;
    private static final int GLASS_PANEL   = 0x70000000;
    private static final int BORDER_SOFT   = 0x50FFFFFF;
    private static final int BORDER_BRIGHT = 0x80FFFFFF;
    private static final int BORDER_FAINT  = 0x20FFFFFF;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final AreaManagementScreen parentScreen;
    private final S2CAreaListPacket.AreaEntry entry;

    private List<EditBox> enterCmds = new ArrayList<>();
    private EditBox enterSound, enterSoundVol, enterSoundPitch;
    private EditBox enterTitleM, enterTitleS;
    private EditBox enterTpDim, enterTpX, enterTpY, enterTpZ;

    private List<EditBox> leaveCmds = new ArrayList<>();
    private EditBox leaveSound, leaveSoundVol, leaveSoundPitch;
    private EditBox leaveTitleM, leaveTitleS;
    private EditBox leaveTpDim, leaveTpX, leaveTpY, leaveTpZ;

    private final List<Section> sections = new ArrayList<>();

    public TriggerEditPanel(AreaManagementScreen parent, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.trigger_settings") + ": " + entry.name()));
        this.parentScreen = parent;
        this.entry = entry;
    }

    @Override
    protected void init() {
        super.init();
        sections.clear();
        enterCmds.clear();
        leaveCmds.clear();

        int cx = this.width / 2;
        int lx = Math.max(10, cx - this.width / 4);
        int vx = lx + 80;
        int vw = Math.min(200, this.width / 2 - 40);
        int y = 30;

        JsonObject enterJ = safeParse(entry.enterTriggerJson());
        JsonObject leaveJ = safeParse(entry.leaveTriggerJson());

        // === Enter Trigger ===
        int s1top = y - 8;
        y = addTriggerSection(y, lx, vx, "Enter", enterJ, enterCmds,
            s -> enterSound = s, s -> enterSoundVol = s, s -> enterSoundPitch = s,
            s -> enterTitleM = s, s -> enterTitleS = s,
            s -> enterTpDim = s, s -> enterTpX = s, s -> enterTpY = s, s -> enterTpZ = s);
        sections.add(new Section("  Enter Trigger  ", s1top, y - s1top - 4));

        // === Leave Trigger ===
        int s2top = y - 8;
        y = addTriggerSection(y, lx, vx, "Leave", leaveJ, leaveCmds,
            s -> leaveSound = s, s -> leaveSoundVol = s, s -> leaveSoundPitch = s,
            s -> leaveTitleM = s, s -> leaveTitleS = s,
            s -> leaveTpDim = s, s -> leaveTpX = s, s -> leaveTpY = s, s -> leaveTpZ = s);
        sections.add(new Section("  Leave Trigger  ", s2top, y - s2top - 4));

        // Save / Cancel
        y += 4;
        addButton(lx, y, 70, LocalizationManager.translate("gui.save"), () -> { sendUpdate(); onClose(); });
        addButton(lx + 80, y, 70, LocalizationManager.translate("gui.cancel"), this::onClose);
    }

    private int addTriggerSection(int y, int lx, int vx, String label, JsonObject json,
            List<EditBox> cmdBoxes,
            java.util.function.Consumer<EditBox> snd, java.util.function.Consumer<EditBox> vol,
            java.util.function.Consumer<EditBox> pitch,
            java.util.function.Consumer<EditBox> tM, java.util.function.Consumer<EditBox> tS,
            java.util.function.Consumer<EditBox> tD, java.util.function.Consumer<EditBox> tX,
            java.util.function.Consumer<EditBox> tY, java.util.function.Consumer<EditBox> tZ) {

        // Header
        addRenderableWidget(Button.builder(
            Component.literal("\u25B6 " + LocalizationManager.translate("command.commands") + " (" + label + ")"),
            b -> {}).pos(lx, y).size(210, 18).build());
        y += 20;

        // Load commands from JSON
        if (json.has("commands") && json.get("commands").isJsonArray()) {
            for (var e : json.getAsJsonArray("commands")) {
                EditBox eb = new EditBox(this.font, vx, y, 160, 16, Component.empty());
                eb.setMaxLength(200);
                eb.setValue(e.getAsString());
                addRenderableWidget(eb);
                cmdBoxes.add(eb);
                y += 18;
            }
        }
        if (cmdBoxes.isEmpty()) {
            EditBox eb = new EditBox(this.font, vx, y, 160, 16, Component.empty());
            eb.setMaxLength(200);
            eb.setValue("");
            addRenderableWidget(eb);
            cmdBoxes.add(eb);
        }

        // Add command button
        int lastCmdY = y - 18;
        final int nextCmdY = y;
        addButton(vx + 165, lastCmdY, 45, "+", () -> {
            cmdBoxes.add(createCmdBox(vx, nextCmdY, ""));
            rebuild();
        });
        y += 4;

        // Sound row
        addLabel2(lx, y, "Sound");
        EditBox se = createCmdBox(vx, y, getStr(json, "soundEvent", "")); se.setWidth(90); snd.accept(se);
        addLabel2(vx + 95, y, "Vol");
        EditBox sv = createCmdBox(vx + 125, y, getFloat(json, "soundVolume", 1.0f)); sv.setWidth(30); vol.accept(sv);
        addLabel2(vx + 160, y, "Pitch");
        EditBox sp = createCmdBox(vx + 195, y, getFloat(json, "soundPitch", 1.0f)); sp.setWidth(30); pitch.accept(sp);
        y += 22;

        // Title row
        addLabel2(lx, y, "Title");
        EditBox tm = createCmdBox(vx, y, getStr(json, "titleMain", "")); tm.setWidth(90); tM.accept(tm);
        addLabel2(vx + 95, y, "Sub");
        EditBox ts = createCmdBox(vx + 125, y, getStr(json, "titleSub", "")); ts.setWidth(90); tS.accept(ts);
        y += 22;

        // TP row
        addLabel2(lx, y, "TP");
        EditBox td = createCmdBox(vx, y, getStr(json, "teleportTarget", "")); td.setWidth(90); tD.accept(td);
        y += 6;
        String[] tpParts = safeTpParts(getStr(json, "teleportTarget", ""));
        addLabel2(lx, y, "X/Y/Z");
        EditBox tx = createCmdBox(vx, y, tpParts[0]); tx.setWidth(40); tX.accept(tx);
        EditBox ty = createCmdBox(vx + 45, y, tpParts[1]); ty.setWidth(40); tY.accept(ty);
        EditBox tz = createCmdBox(vx + 90, y, tpParts[2]); tz.setWidth(40); tZ.accept(tz);

        return y + 24;
    }

    private EditBox createCmdBox(int x, int y, String val) {
        EditBox b = new EditBox(this.font, x, y, 160, 16, Component.empty());
        b.setMaxLength(200); b.setValue(val); addRenderableWidget(b); return b;
    }

    private void addButton(int x, int y, int w, String text, Runnable action) {
        addRenderableWidget(GlassButton.create(x, y, w, 18, text, b -> action.run()));
    }

    private void addLabel2(int x, int y, String text) {
        addRenderableWidget(Button.builder(
            Component.literal(text).withStyle(ChatFormatting.GRAY), b -> {})
            .pos(x, y).size(30, 16).build());
    }

    private String getStr(JsonObject j, String key, String def) {
        return j != null && j.has(key) && !j.get(key).isJsonNull() ? j.get(key).getAsString() : def;
    }

    private String getFloat(JsonObject j, String key, float def) {
        return j != null && j.has(key) ? String.valueOf(j.get(key).getAsFloat()) : String.valueOf(def);
    }

    private String[] safeTpParts(String tp) {
        String[] def = {"", "", ""};
        if (tp == null || tp.isEmpty()) return def;
        String[] p = tp.split(" ");
        if (p.length >= 4) return new String[]{p[1], p[2], p[3]};
        if (p.length == 3) return new String[]{p[0], p[1], p[2]};
        return def;
    }

    private JsonObject safeParse(String json) {
        try { return json != null ? GSON.fromJson(json, JsonObject.class) : new JsonObject(); }
        catch (Exception e) { return new JsonObject(); }
    }

    private void rebuild() { this.clearWidgets(); init(); }

    private String buildTriggerJson(List<EditBox> cmds, EditBox snd, EditBox vol, EditBox pitch,
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
                !tX.getValue().trim().isEmpty() && !tY.getValue().trim().isEmpty() && !tZ.getValue().trim().isEmpty()) {
                dim += " " + tX.getValue().trim() + " " + tY.getValue().trim() + " " + tZ.getValue().trim();
            }
            j.addProperty("teleportTarget", dim);
        }
        return j.toString();
    }

    private void sendUpdate() {
        var json = new JsonObject();
        try { json.add("enterTrigger", GSON.fromJson(
            buildTriggerJson(enterCmds, enterSound, enterSoundVol, enterSoundPitch,
                enterTitleM, enterTitleS, enterTpDim, enterTpX, enterTpY, enterTpZ), JsonObject.class));
        } catch (Exception e) { json.add("enterTrigger", new JsonObject()); }
        try { json.add("leaveTrigger", GSON.fromJson(
            buildTriggerJson(leaveCmds, leaveSound, leaveSoundVol, leaveSoundPitch,
                leaveTitleM, leaveTitleS, leaveTpDim, leaveTpX, leaveTpY, leaveTpZ), JsonObject.class));
        } catch (Exception e) { json.add("leaveTrigger", new JsonObject()); }
        ModNetwork.sendToServer(new C2SAreaActionPacket(
            C2SAreaActionPacket.Action.UPDATE, entry.name(), json.toString()));
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
        g.fill(0, 0, this.width, 28, GLASS_DARK);
        g.fill(0, 27, this.width, 28, BORDER_BRIGHT);
        g.fill(0, 0, this.width, 1, BORDER_SOFT);
        g.drawCenteredString(this.font,
            Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE), cx, 8, 0xFFFFFF);

        int lx = Math.max(10, cx - this.width / 4);
        for (Section s : sections) {
            g.fill(lx - 6, s.y, lx + 282, s.y + s.h, GLASS_PANEL);
            g.fill(lx - 6, s.y, lx + 282, s.y + 1, BORDER_BRIGHT);
            g.fill(lx - 6, s.y + s.h - 1, lx + 282, s.y + s.h, BORDER_FAINT);
            g.drawString(this.font, Component.literal(s.title).withStyle(ChatFormatting.DARK_GRAY),
                lx + 2, s.y + 2, 0x999999);
        }
        super.render(g, mx, my, pt);
    }

    private record Section(String title, int y, int h) {}
}
