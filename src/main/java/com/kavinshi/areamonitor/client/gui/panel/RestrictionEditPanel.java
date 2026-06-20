package com.kavinshi.areamonitor.client.gui.panel;

import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.client.gui.AreaManagementScreen;
import com.kavinshi.areamonitor.client.gui.widget.GlassButton;
import com.kavinshi.areamonitor.network.C2SAreaActionPacket;
import com.kavinshi.areamonitor.network.S2CAreaListPacket;
import com.kavinshi.areamonitor.network.ModNetwork;
import com.google.gson.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public class RestrictionEditPanel extends Screen {

    private static final int PARCH_DARK  = 0xD03A2A1A;
    private static final int PARCH_PANEL = 0xC0C4A882;
    private static final int BORDER_GOLD = 0x808B6914;
    private static final Gson GSON = new Gson();

    private final Screen returnScreen;
    private final AreaManagementScreen mainScreen;
    private final S2CAreaListPacket.AreaEntry entry;

    private boolean enableItemBlacklist = true, blockTeleportCommands = true;
    private boolean itemsExp = true, cmdsExp = true;
    private final List<String> blockedItems = new ArrayList<>(), blockedCommands = new ArrayList<>();
    private EditBox addItemBox, addCmdBox;
    private int wx, wy, ww, wh, lx;

    public RestrictionEditPanel(Screen returnScreen, AreaManagementScreen mainScreen, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.restriction_settings") + ": " + entry.name()));
        this.returnScreen = returnScreen; this.mainScreen = mainScreen; this.entry = entry;
        if (entry.restrictionsJson() != null) try {
            JsonObject obj = GSON.fromJson(entry.restrictionsJson(), JsonObject.class);
            enableItemBlacklist = zb(obj, "enableItemBlacklist", true);
            blockTeleportCommands = zb(obj, "blockTeleportCommands", true);
            zl(obj, "blockedItems", blockedItems); zl(obj, "blockedCommands", blockedCommands);
        } catch (Exception ignored) {}
    }

    @Override protected void init() {
        super.init();
        ww = Math.min(this.width * 78 / 100, 560); wh = Math.min(this.height * 82 / 100, 480);
        wx = (this.width - ww) / 2; wy = (this.height - wh) / 2;
        lx = wx + 8;
        int y = wy + 36;

        // Item Blacklist
        zbtn(lx, y, 260, LocalizationManager.translate("gui.restriction_items") +
            (enableItemBlacklist ? " " + LocalizationManager.translate("gui.prot_enabled") : " " + LocalizationManager.translate("gui.prot_disabled")) +
            (itemsExp ? "  \u25BC" : "  \u25B6"), () -> { itemsExp = !itemsExp; rebuild(); }); y += 20;
        if (itemsExp) {
            zbtn(lx, y, 74, LocalizationManager.translate(enableItemBlacklist ? "gui.disable" : "gui.enable"), () -> { enableItemBlacklist = !enableItemBlacklist; rebuild(); }); y += 20;
            if (enableItemBlacklist) {
                addItemBox = new EditBox(this.font, lx, y, 150, 16, Component.empty()); addItemBox.setMaxLength(100); addRenderableWidget(addItemBox);
                zbtn(lx + 155, y, 36, "+", () -> { String v = addItemBox.getValue().trim().toLowerCase(); if (!v.isEmpty() && !blockedItems.contains(v)) { blockedItems.add(v); addItemBox.setValue(""); sendUpdate(); rebuild(); } }); y += 20;
                int max = Math.min(blockedItems.size(), 6);
                for (int i = 0; i < max; i++) { final int idx = i; zbtn(lx + 6, y, 150, "  \u2022 " + blockedItems.get(i) + "  \u2715", () -> { blockedItems.remove(idx); sendUpdate(); rebuild(); }); y += 18; }
            }
        }
        y += 8;

        // Cmd restrictions
        zbtn(lx, y, 260, LocalizationManager.translate("gui.restriction_commands") +
            (blockTeleportCommands ? " " + LocalizationManager.translate("gui.prot_enabled") : " " + LocalizationManager.translate("gui.prot_disabled")) +
            (cmdsExp ? "  \u25BC" : "  \u25B6"), () -> { cmdsExp = !cmdsExp; rebuild(); }); y += 20;
        if (cmdsExp) {
            zbtn(lx, y, 74, LocalizationManager.translate(blockTeleportCommands ? "gui.disable" : "gui.enable"), () -> { blockTeleportCommands = !blockTeleportCommands; rebuild(); }); y += 20;
            addCmdBox = new EditBox(this.font, lx, y, 150, 16, Component.empty()); addCmdBox.setMaxLength(100); addRenderableWidget(addCmdBox);
            zbtn(lx + 155, y, 36, "+", () -> { String v = addCmdBox.getValue().trim(); if (!v.isEmpty() && !blockedCommands.contains(v)) { blockedCommands.add(v); addCmdBox.setValue(""); sendUpdate(); rebuild(); } }); y += 20;
            int max = Math.min(blockedCommands.size(), 5);
            for (int i = 0; i < max; i++) { final int idx = i; zbtn(lx + 6, y, 150, "  \u2022 " + blockedCommands.get(i) + "  \u2715", () -> { blockedCommands.remove(idx); sendUpdate(); rebuild(); }); y += 18; }
        }
        y += 10;

        int btnY = Math.max(y, wy + wh - 38);
        zbtn(lx, btnY, 70, LocalizationManager.translate("gui.save"), this::onClose);
        zbtn(lx + 78, btnY, 70, LocalizationManager.translate("gui.cancel"), this::onClose);
    }

    private void zbtn(int x, int y, int w, String text, Runnable a) { addRenderableWidget(GlassButton.create(x, y, w, 18, text, b -> a.run())); }
    private boolean zb(JsonObject o, String k, boolean d) { return o.has(k) ? o.get(k).getAsBoolean() : d; }
    private void zl(JsonObject o, String k, List<String> d) { if (o.has(k) && o.get(k).isJsonArray()) for (var e : o.getAsJsonArray(k)) d.add(e.getAsString()); }
    private void rebuild() { clearWidgets(); init(); }
    private void sendUpdate() {
        var json = new JsonObject(); var rest = new JsonObject();
        rest.addProperty("enableItemBlacklist", enableItemBlacklist); rest.addProperty("blockTeleportCommands", blockTeleportCommands);
        JsonArray items = new JsonArray(); for (String s : blockedItems) items.add(s); rest.add("blockedItems", items);
        JsonArray cmds = new JsonArray(); for (String s : blockedCommands) cmds.add(s); rest.add("blockedCommands", cmds);
        json.add("restrictions", rest);
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
        super.render(g, mx, my, pt);
    }
}
