package com.kavinshi.areamonitor.client.gui.panel;

import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.client.gui.AreaManagementScreen;
import com.kavinshi.areamonitor.client.gui.widget.ConfirmDialog;
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

public class WhitelistEditPanel extends Screen {

    private static final int PARCH_DARK  = 0xD03A2A1A;
    private static final int PARCH_PANEL = 0xC0C4A882;
    private static final int BORDER_GOLD = 0x808B6914;
    private static final Gson GSON = new Gson();

    private final Screen returnScreen;
    private final AreaManagementScreen mainScreen;
    private final S2CAreaListPacket.AreaEntry entry;
    private final boolean protMode;  // true = protection whitelist, false = global whitelist
    private final List<String> players = new ArrayList<>();
    private EditBox nameInput;
    private int wx, wy, ww, wh, lx;
    private final ConfirmDialog confirmDialog = new ConfirmDialog();

    public WhitelistEditPanel(Screen returnScreen, AreaManagementScreen mainScreen, S2CAreaListPacket.AreaEntry entry) {
        this(returnScreen, mainScreen, entry, false);
    }

    public WhitelistEditPanel(Screen returnScreen, AreaManagementScreen mainScreen, S2CAreaListPacket.AreaEntry entry, boolean protMode) {
        super(Component.literal(LocalizationManager.translate(protMode ? "gui.prot_whitelist" : "gui.whitelist_settings") + ": " + entry.name()));
        this.returnScreen = returnScreen; this.mainScreen = mainScreen; this.entry = entry; this.protMode = protMode;
        String json = protMode ? entry.protWhitelistJson() : entry.whitelistJson();
        if (json != null) try {
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            for (var e : arr) players.add(e.getAsString());
        } catch (Exception ignored) {}
    }

    @Override protected void init() {
        super.init();
        ww = Math.min(this.width * 78 / 100, 560); wh = Math.min(this.height * 82 / 100, 480);
        wx = (this.width - ww) / 2; wy = (this.height - wh) / 2;
        lx = wx + 8;
        int titleBarHeight = 26, topPadding = 10;
        int y = wy + 3 + titleBarHeight + topPadding;

        nameInput = new EditBox(this.font, lx, y, 256, 16, Component.empty());
        nameInput.setMaxLength(16); addRenderableWidget(nameInput);
        zbtn(lx + 262, y, 40, LocalizationManager.translate("command.add"), () -> {
            String nm = nameInput.getValue().trim();
            if (!nm.isEmpty() && !players.contains(nm.toLowerCase()) && players.size() < 50) { players.add(nm.toLowerCase()); nameInput.setValue(""); sendUpdate(); rebuild(); }
            else if (players.size() >= 50) { nameInput.setValue(""); }
        });
        y += 22;

        int visible = Math.max(0, (wh - 120) / 20);
        int shown = Math.min(players.size(), visible);
        for (int i = 0; i < shown; i++) {
            final int idx = i; String p = players.get(i);
            zbtn(lx, y, 256, "  \u2022 " + p + "  \u2715", () -> { players.remove(idx); sendUpdate(); rebuild(); });
            y += 20;
        }
        if (players.size() > visible) zbtn(lx, y, 256, "... " + (players.size() - visible) + " more", () -> {});
        y += 10;

        int btnY = Math.max(y, wy + wh - 38);
        // Clear all button — only show when there are players
        if (!players.isEmpty()) {
            zbtn(lx + 200, btnY, 70, LocalizationManager.translate("gui.clear_all"), () -> {
                confirmDialog.show(
                    LocalizationManager.translate("gui.confirm_clear_title"),
                    LocalizationManager.translate("gui.confirm_clear_msg"),
                    LocalizationManager.translate("gui.confirm"),
                    LocalizationManager.translate("gui.cancel"),
                    () -> { players.clear(); sendUpdate(); rebuild(); },
                    () -> {});
            });
        }
        zbtn(lx, btnY, 70, LocalizationManager.translate("gui.save"), this::onClose);
        zbtn(lx + 78, btnY, 70, LocalizationManager.translate("gui.cancel"), this::onClose);
    }

    private void zbtn(int x, int y, int w, String text, Runnable a) { addRenderableWidget(GlassButton.create(x, y, w, 18, text, b -> a.run())); }
    private void rebuild() { clearWidgets(); init(); }
    private void sendUpdate() {
        var json = new JsonObject(); JsonArray arr = new JsonArray();
        for (String p : players) arr.add(p);
        json.add(protMode ? "protWhitelist" : "whitelist", arr);
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.UPDATE, entry.name(), json.toString()));
    }

    @Override public void onClose() { mainScreen.updateAfterEdit(); if (this.minecraft != null) this.minecraft.setScreen(returnScreen); }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameInput != null && this.nameInput.isFocused())
            return this.nameInput.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (confirmDialog.isVisible()) {
            confirmDialog.mouseClicked(mx, my, button);
            return true;
        }
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

        int panelX = lx - 4, panelY = wy + 3 + 26 + 8, panelW = ww - 12, panelH = wy + wh - 76 - panelY;
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x30C4A882);
        g.fill(panelX, panelY, panelX + panelW, panelY + 1, BORDER_GOLD);
        g.drawString(this.font, Component.literal("  Players").withStyle(ChatFormatting.DARK_GRAY), lx + 2, panelY + 2, 0xFF8B6914);
        super.render(g, mx, my, pt);

        if (confirmDialog.isVisible()) {
            confirmDialog.render(g, this.width, this.height);
        }
    }
}
