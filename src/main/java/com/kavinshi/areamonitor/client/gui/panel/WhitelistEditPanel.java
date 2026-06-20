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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Whitelist editor panel — manage list of allowed player names for this area.
 * Glass Morphism theme.
 */
public class WhitelistEditPanel extends Screen {

    private static final int PARCH_DARK   = 0xD03A2A1A;
    private static final int PARCH_PANEL  = 0xC0C4A882;
    private static final int BORDER_GOLD  = 0x808B6914;
    private static final int BORDER_SHADOW = 0x405C4033;
    private static final Gson GSON = new Gson();

    private final Screen returnScreen;
    private final AreaManagementScreen mainScreen;
    private final S2CAreaListPacket.AreaEntry entry;
    private final List<String> players = new ArrayList<>();
    private EditBox nameInput;
    private int winX, winY, winW, winH;

    public WhitelistEditPanel(Screen returnScreen, AreaManagementScreen mainScreen, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.whitelist_settings") + ": " + entry.name()));
        this.returnScreen = returnScreen;
        this.mainScreen = mainScreen;
        this.entry = entry;
        // Load existing players from JSON
        if (entry.whitelistJson() != null) {
            try {
                JsonArray arr = GSON.fromJson(entry.whitelistJson(), JsonArray.class);
                for (var e : arr) players.add(e.getAsString());
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void init() {
        super.init();
        winW = Math.min(this.width * 78 / 100, 560);
        winH = Math.min(this.height * 82 / 100, 480);
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;
        int lx = winX + 12;
        int y = winY + 38;

        // Add input field
        nameInput = new EditBox(this.font, lx, y, 180, 18, Component.literal("Player name"));
        nameInput.setMaxLength(16);
        addRenderableWidget(nameInput);

        addButton(lx + 185, y, 40, LocalizationManager.translate("command.add"), () -> {
            String nm = nameInput.getValue().trim();
            if (!nm.isEmpty() && !players.contains(nm.toLowerCase())) {
                players.add(nm.toLowerCase());
                nameInput.setValue("");
                sendUpdate();
                rebuild();
            }
        });
        y += 24;

        // Player list
        int listHeight = Math.min(players.size() * 22, winH - 140);
        int listY_start = y;
        int scrollable = Math.max(0, (winH - 140) / 22);
        int shown = Math.min(players.size(), scrollable);

        for (int i = 0; i < shown; i++) {
            final int idx = i;
            String p = players.get(i);
            addRenderableWidget(GlassButton.create(lx, y, 160, 18, "  \u2022 " + p + "  \u2715", b -> {
                    players.remove(idx);
                    sendUpdate();
                    rebuild();
                }));
            y += 22;
        }

        if (players.size() > scrollable) {
            addRenderableWidget(GlassButton.create(lx, y, 160, 18,
                "... " + (players.size() - scrollable) + " more", b -> {}));
        }
        y = listY_start + listHeight + 8;

        // Save / Cancel
        addButton(lx, y, 70, LocalizationManager.translate("gui.save"), this::onClose);
        addButton(lx + 80, y, 70, LocalizationManager.translate("gui.cancel"), this::onClose);
    }

    private void addButton(int x, int y, int w, String text, Runnable action) {
        addRenderableWidget(GlassButton.create(x, y, w, 18, text, b -> action.run()));
    }

    private void rebuild() { this.clearWidgets(); init(); }

    private void sendUpdate() {
        var json = new JsonObject();
        JsonArray arr = new JsonArray();
        for (String p : players) arr.add(p);
        json.add("whitelist", arr);
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
        g.fill(0, 0, this.width, this.height, 0x80000000);
        g.fill(winX, winY, winX + winW, winY + winH, PARCH_PANEL);
        g.fill(winX + 1, winY + 1, winX + winW - 1, winY + winH - 1, 0xE02A1F14);
        g.fill(winX, winY, winX + winW, winY + 2, BORDER_GOLD);
        g.fill(winX, winY, winX + 2, winY + winH, BORDER_GOLD);
        g.fill(winX + winW - 2, winY, winX + winW, winY + winH, BORDER_GOLD);
        g.fill(winX, winY + winH - 2, winX + winW, winY + winH, BORDER_GOLD);
        g.fill(winX + 3, winY + 3, winX + winW - 3, winY + 31, PARCH_DARK);
        g.fill(winX + 3, winY + 30, winX + winW - 3, winY + 31, BORDER_GOLD);
        g.drawCenteredString(this.font,
            Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE), winX + winW / 2, winY + 10, 0xFFF5DEB3);

        int lx = winX + 12;
        g.fill(lx - 6, winY + 36, lx + 232, winY + winH - 40, 0x30C4A882);
        g.fill(lx - 6, winY + 36, lx + 232, winY + 37, BORDER_GOLD);
        g.drawString(this.font, Component.literal("  Players").withStyle(ChatFormatting.DARK_GRAY), lx + 2, winY + 38, 0xFF8B6914);
        super.render(g, mx, my, pt);
    }
}
