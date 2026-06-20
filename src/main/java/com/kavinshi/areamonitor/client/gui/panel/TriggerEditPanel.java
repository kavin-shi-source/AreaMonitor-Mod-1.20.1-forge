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

    // ===== 核心重构：引入界面运行时状态持久化，防止 rebuild() 时数据丢失 =====
    private boolean initializedData = false;
    private boolean enterExpanded = true;  // 默认展开 Enter 面板
    private boolean leaveExpanded = false; // 默认收起 Leave 面板，防止纵向撑爆屏幕

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
        sections.clear(); labels.clear(); enterCmds.clear(); leaveCmds.clear();
        
        ww = Math.min(this.width * 78 / 100, 560); 
        wh = Math.min(this.height * 82 / 100, 480);
        wx = (this.width - ww) / 2; 
        wy = (this.height - wh) / 2;
        lx = wx + 8; 
        vx = lx + 85; 
        panelW = ww - 45;
        int titleBarHeight = 26, topPadding = 10;
        int y = wy + 3 + titleBarHeight + topPadding;

        // 仅在初次打开 GUI 时从原始封包中解析 Json，后续的刷新使用缓存数据
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

        // === 1. Enter Trigger 折叠栏主按钮 ===
        String enterTitle = (enterExpanded ? "▼ " : "▶ ") + LocalizationManager.translate("command.commands") + " (Enter)";
        zbtn(lx, y, panelW, enterTitle, () -> { enterExpanded = !enterExpanded; saveAndRebuild(); });
        y += 20;
        if (enterExpanded) {
            int top = y - 2;
            y = zsec(y, enterCmds, enterCmdValues, 
                e -> enterSound = e, e -> enterSoundVol = e, e -> enterSoundPitch = e,
                e -> enterTitleM = e, e -> enterTitleS = e, 
                e -> enterTpDim = e, e -> enterTpX = e, e -> enterTpY = e, e -> enterTpZ = e,
                enterSoundVal, enterVolVal, enterPitchVal, enterTitleMVal, enterTitleSVal, enterTpDimVal, enterTpXVal, enterTpYVal, enterTpZVal);
            sections.add(new Section("", top, y - top));
        }
        y += 6;

        // === 2. Leave Trigger 折叠栏主按钮 ===
        String leaveTitle = (leaveExpanded ? "▼ " : "▶ ") + LocalizationManager.translate("command.commands") + " (Leave)";
        zbtn(lx, y, panelW, leaveTitle, () -> { leaveExpanded = !leaveExpanded; saveAndRebuild(); });
        y += 20;
        if (leaveExpanded) {
            int top = y - 2;
            y = zsec(y, leaveCmds, leaveCmdValues, 
                e -> leaveSound = e, e -> leaveSoundVol = e, e -> leaveSoundPitch = e,
                e -> leaveTitleM = e, e -> leaveTitleS = e, 
                e -> leaveTpDim = e, e -> leaveTpX = e, e -> leaveTpY = e, e -> leaveTpZ = e,
                leaveSoundVal, leaveVolVal, leavePitchVal, leaveTitleMVal, leaveTitleSVal, leaveTpDimVal, leaveTpXVal, leaveTpYVal, leaveTpZVal);
            sections.add(new Section("", top, y - top));
        }

        // === 3. 固定底部控制按钮，绝对安全，永不被挤出屏幕外 ===
        int btnY = wy + wh - 24;
        zbtn(lx, btnY, 70, LocalizationManager.translate("gui.save"), () -> { captureCurrentState(); sendUpdate(); onClose(); });
        zbtn(lx + 78, btnY, 70, LocalizationManager.translate("gui.cancel"), this::onClose);
    }

    // 重构核心方法：重绘次级区域，彻底重构坐标系统并引入命令删除键
    private int zsec(int y, List<EditBox> cmds, List<String> cmdValues,
            java.util.function.Consumer<EditBox> snd, java.util.function.Consumer<EditBox> vol, java.util.function.Consumer<EditBox> pitch,
            java.util.function.Consumer<EditBox> tM, java.util.function.Consumer<EditBox> tS,
            java.util.function.Consumer<EditBox> tD, java.util.function.Consumer<EditBox> tX, java.util.function.Consumer<EditBox> tY, java.util.function.Consumer<EditBox> tZ,
            String sndV, String volV, String pitchV, String tMV, String tSV, String tDV, String tXV, String tYV, String tZV) {
        
        final int cmdW = Math.min(panelW - 60, 340);
        if (cmdValues.isEmpty()) cmdValues.add("");

        // 循环渲染多行指令，并添加行尾的简易删除按钮 "×"
        for (int i = 0; i < cmdValues.size(); i++) {
            EditBox box = zbox(vx, y, cmdW, cmdValues.get(i));
            cmds.add(box);
            
            final int idx = i;
            if (cmdValues.size() > 1) { // 只有大于1行时才提供删除按钮
                zbtn(vx + cmdW + 4, y, 16, "×", () -> {
                    captureCurrentState();
                    cmdValues.remove(idx);
                    saveAndRebuild();
                });
            }
            y += 18;
        }

        // 贴片放置 "+ Add" 按钮
        zbtn(vx, y, 60, "+ " + LocalizationManager.translate("command.add"), () -> {
            captureCurrentState();
            if (cmdValues.size() < 20) cmdValues.add(""); // 限制最大 20 条防止面板溢出
            saveAndRebuild();
        });
        y += 22;

        // Sound 音效行优化：大幅拉宽空间，增加小标题对齐，避免重叠
        addLab(lx + 4, y, "Sound"); EditBox se = zbox(vx, y, 160, sndV); snd.accept(se);
        addLab(vx + 166, y, "Vol"); EditBox sv = zbox(vx + 188, y, 30, volV); vol.accept(sv);
        addLab(vx + 224, y, "Pt");  EditBox sp = zbox(vx + 240, y, 30, pitchV); pitch.accept(sp);
        y += 20;

        // Title 标题行优化：提供合理的编辑长度
        addLab(lx + 4, y, "Title"); EditBox tm = zbox(vx, y, 110, tMV); tM.accept(tm);
        addLab(vx + 116, y, "Sub"); EditBox ts = zbox(vx + 140, y, 130, tSV); tS.accept(ts);
        y += 20;

        // TP 传送行优化：维度与 X/Y/Z 文本彻底独立隔离，互不干扰
        addLab(lx + 4, y, "TP Dim"); EditBox td = zbox(vx, y, 90, tDV); tD.accept(td);
        addLab(vx + 96, y, "X");   EditBox tx = zbox(vx + 106, y, 32, tXV); tX.accept(tx);
        addLab(vx + 142, y, "Y");  EditBox ty = zbox(vx + 152, y, 32, tYV); tY.accept(ty);
        addLab(vx + 188, y, "Z");  EditBox tz = zbox(vx + 198, y, 32, tZV); tZ.accept(tz);
        
        return y + 24;
    }

    // 核心工具方法：在擦除小部件重构前，捕获并实时更新内存中的文本状态值
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
    private EditBox zbox(int x, int y, int w, String v) { EditBox e = new EditBox(this.font, x, y, w, 16, Component.empty()); e.setMaxLength(200); e.setValue(v); addRenderableWidget(e); return e; }
    private void addLab(int x, int y, String t) { labels.add(new LabelPos(x, y, t)); }
    private String getStr(JsonObject j, String k, String d) { return j != null && j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsString() : d; }
    
    // 改良版的4段传送解析器：[0]->维度, [1]->X, [2]->Y, [3]->Z
    private String[] safeTpParts(String tp) { 
        if (tp == null || tp.isEmpty()) return new String[]{"", "", "", ""};
        String[] p = tp.split(" "); 
        if (p.length >= 4) return new String[]{p[0], p[1], p[2], p[3]}; 
        if (p.length == 3) return new String[]{"", p[0], p[1], p[2]}; 
        return new String[]{tp, "", "", ""}; 
    }
    
    private JsonObject safeParse(String j) { try { return j != null ? GSON.fromJson(j, JsonObject.class) : new JsonObject(); } catch (Exception e) { return new JsonObject(); } }
    private void rebuild() { captureCurrentState(); saveAndRebuild(); }

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

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        java.util.List<EditBox> all = new java.util.ArrayList<>();
        if (enterCmds != null) all.addAll(enterCmds);
        if (leaveCmds != null) all.addAll(leaveCmds);
        java.util.stream.Stream.of(enterSound, enterSoundVol, enterSoundPitch, enterTitleM, enterTitleS,
            enterTpDim, enterTpX, enterTpY, enterTpZ,
            leaveSound, leaveSoundVol, leaveSoundPitch, leaveTitleM, leaveTitleS,
            leaveTpDim, leaveTpX, leaveTpY, leaveTpZ)
            .filter(Objects::nonNull).forEach(all::add);
        for (EditBox box : all) {
            if (box.isFocused()) return box.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (mx < wx || mx > wx + ww || my < wy || my > wy + wh) {
            this.onClose();
            return true;
        }
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

        int secW = Math.min(vx - lx + panelW + 4, ww - 16);
        for (Section s : sections) {
            g.fill(lx - 4, s.y, lx + secW, s.y + s.h, 0x30C4A882);
            g.fill(lx - 4, s.y, lx + secW, s.y + 1, BORDER_GOLD);
        }
        for (LabelPos l : labels) g.drawString(this.font, Component.literal(l.text).withStyle(ChatFormatting.GRAY), l.x, l.y + 1, 0xFFFFFFFF);
        super.render(g, mx, my, pt);
    }

    private record Section(String title, int y, int h) {}
    private record LabelPos(int x, int y, String text) {}
}