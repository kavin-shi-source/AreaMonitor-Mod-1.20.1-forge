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
    private final boolean protMode;
    private final List<String> players = new ArrayList<>();
    private EditBox nameInput;
    private int wx, wy, ww, wh, lx;
    private final ConfirmDialog confirmDialog = new ConfirmDialog();

    // Scroll support
    private int scrollOffset = 0;
    private int contentTopY, contentBottomY;
    // Unsaved-change tracking
    private boolean dirty = false;
    // Tooltips
    private final List<TooltipZone> tooltips = new ArrayList<>();
    // Bottom buttons kept as fields so render() can draw them outside the scissor clip
    private GlassButton saveBtn, cancelBtn, clearAllBtn;

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
        tooltips.clear();
        ww = Math.min(this.width * 78 / 100, 560); wh = Math.min(this.height * 82 / 100, 480);
        wx = (this.width - ww) / 2; wy = (this.height - wh) / 2;
        lx = wx + 8;
        int titleBarHeight = 26, topPadding = 10;
        contentTopY = wy + 3 + titleBarHeight + topPadding;
        contentBottomY = wy + wh - 38;

        int y = contentTopY;

        // Input row
        nameInput = new EditBox(this.font, lx, y, 256, 16, Component.empty());
        nameInput.setMaxLength(16);
        nameInput.setResponder(s -> dirty = true);
        addRenderableWidget(nameInput);
        zbtn(lx + 262, y, 50, "[" + LocalizationManager.translate("command.add") + "]", () -> {
            String nm = nameInput.getValue().trim();
            if (!nm.isEmpty() && !players.contains(nm.toLowerCase()) && players.size() < 50) {
                players.add(nm.toLowerCase()); nameInput.setValue(""); dirty = true; rebuild();
            } else if (players.size() >= 50) { nameInput.setValue(""); }
        });
        tooltips.add(new TooltipZone(lx, y, 256, 18, "gui.tooltip_whitelist_input"));
        y += 24;

        // Player list with scroll
        int listTop = y;
        int listH = contentBottomY - listTop;
        int visible = Math.max(0, listH / 20);
        int totalToShow = players.size();
        int maxScroll = Math.max(0, totalToShow * 20 - listH);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        int firstVisible = Math.max(0, scrollOffset / 20);
        int lastVisible = Math.min(totalToShow, firstVisible + visible + 1);
        for (int i = firstVisible; i < lastVisible; i++) {
            final int idx = i; String p = players.get(i);
            int py = listTop + i * 20 - scrollOffset;
            if (py + 18 <= contentBottomY) {
                zbtn(lx, py, 256, "  \u2022 " + p + "  \u2715", () -> { players.remove(idx); dirty = true; rebuild(); });
                tooltips.add(new TooltipZone(lx, py, 256, 18, "gui.tooltip_whitelist_item"));
            }
        }

        // Bottom buttons — centered (kept as fields; rendered outside the scissor clip in render())
        int btnY = wy + wh - 30;
        int cx = wx + ww / 2;
        saveBtn = GlassButton.create(cx - 78, btnY, 70, 18, "[" + LocalizationManager.translate("gui.save") + "]", b -> {
            if (dirty) { sendUpdate(); dirty = false; }
            onClose();
        });
        cancelBtn = GlassButton.create(cx + 8, btnY, 70, 18, "[" + LocalizationManager.translate("gui.cancel") + "]", b -> {
            dirty = false;
            onClose();
        });
        addRenderableWidget(saveBtn);
        addRenderableWidget(cancelBtn);
        if (!players.isEmpty()) {
            clearAllBtn = GlassButton.create(wx + ww - 78, btnY, 70, 18, "[" + LocalizationManager.translate("gui.clear_all") + "]", b -> {
                confirmDialog.show(
                    LocalizationManager.translate("gui.confirm_clear_title"),
                    LocalizationManager.translate("gui.confirm_clear_msg"),
                    LocalizationManager.translate("gui.confirm"),
                    LocalizationManager.translate("gui.cancel"),
                    () -> { players.clear(); dirty = true; rebuild(); },
                    () -> {});
            });
            addRenderableWidget(clearAllBtn);
        }
    }

    private void zbtn(int x, int y, int w, String text, Runnable a) { addRenderableWidget(GlassButton.create(x, y, w, 18, text, b -> a.run())); }

    private void rebuild() {
        String saved = nameInput != null ? nameInput.getValue() : "";
        clearWidgets();
        init();
        if (nameInput != null) nameInput.setValue(saved);
    }

    private void sendUpdate() {
        var json = new JsonObject(); JsonArray arr = new JsonArray();
        for (String p : players) arr.add(p);
        json.add(protMode ? "protWhitelist" : "whitelist", arr);
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.UPDATE, entry.name(), json.toString()));
    }

    @Override public void onClose() { mainScreen.updateAfterEdit(); if (this.minecraft != null) this.minecraft.setScreen(returnScreen); }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (confirmDialog.isVisible()) return true;
        if (this.nameInput != null && this.nameInput.isFocused())
            return this.nameInput.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char c, int modifiers) {
        if (this.nameInput != null && this.nameInput.isFocused())
            return this.nameInput.charTyped(c, modifiers);
        return super.charTyped(c, modifiers);
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        int listH = contentBottomY - (contentTopY + 24);
        int maxScroll = Math.max(0, players.size() * 20 - listH);
        if (maxScroll > 0) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) delta * 20));
            rebuild();
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
        ChatFormatting titleColor = protMode ? ChatFormatting.RED : ChatFormatting.GREEN;
        g.drawCenteredString(this.font, Component.literal(this.title.getString()).withStyle(titleColor), wx + ww / 2, wy + 9, 0xFFF5DEB3);

        int listTop = contentTopY + 24;
        int panelX = lx - 4, panelY = listTop - 2, panelW = ww - 12, panelH = contentBottomY - listTop + 4;
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x30C4A882);
        g.fill(panelX, panelY, panelX + panelW, panelY + 1, BORDER_GOLD);
        g.drawString(this.font, Component.literal("  " + LocalizationManager.translate("gui.whitelist_players")).withStyle(ChatFormatting.DARK_GRAY), lx + 2, panelY + 2, 0xFF8B6914);

        g.enableScissor(wx + 2, contentTopY, wx + ww - 2, contentBottomY);
        super.render(g, mx, my, pt);
        g.disableScissor();

        // Re-draw bottom buttons manually (they sit below the scissor clip)
        if (saveBtn != null) saveBtn.render(g, mx, my, pt);
        if (cancelBtn != null) cancelBtn.render(g, mx, my, pt);
        if (clearAllBtn != null) clearAllBtn.render(g, mx, my, pt);

        // Scrollbar
        int listH = contentBottomY - listTop;
        int contentH = players.size() * 20;
        if (contentH > listH) {
            int barX = lx + 264;
            int thumbH = Math.max(20, listH * listH / contentH);
            int thumbY = listTop + (listH - thumbH) * scrollOffset / Math.max(1, contentH - listH);
            g.fill(barX, listTop, barX + 4, listTop + listH, 0x408B6914);
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xC08B6914);
        }

        // Tooltips
        for (TooltipZone t : tooltips) {
            if (mx >= t.x && mx <= t.x + t.w && my >= t.y && my <= t.y + t.h) {
                String tip = LocalizationManager.translate(t.key);
                if (!tip.equals(t.key)) {
                    renderTooltip(g, mx, my, tip);
                    break;
                }
            }
        }

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

    private record TooltipZone(int x, int y, int w, int h, String key) {}
}
