package com.kavinshi.areamonitor.client.gui.panel;

import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.client.gui.AreaManagementScreen;
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

    private static final int GLASS_DARK  = 0xA0000000;
    private static final int GLASS_PANEL = 0x60000000;
    private static final int BORDER_SOFT = 0x40FFFFFF;
    private static final Gson GSON = new Gson();

    private final AreaManagementScreen parentScreen;
    private final S2CAreaListPacket.AreaEntry entry;
    private final List<String> players = new ArrayList<>();
    private EditBox nameInput;

    public WhitelistEditPanel(AreaManagementScreen parent, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.whitelist_settings") + ": " + entry.name()));
        this.parentScreen = parent;
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
        int cx = this.width / 2;
        int lx = Math.max(10, cx - this.width / 4);
        int vx = lx + 10;
        int y = 30;

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
        int listHeight = Math.min(players.size() * 22, this.height - 140);
        int listY_start = y;
        int scrollable = Math.max(0, (this.height - 140) / 22);
        int shown = Math.min(players.size(), scrollable);

        for (int i = 0; i < shown; i++) {
            final int idx = i;
            String p = players.get(i);
            addRenderableWidget(Button.builder(
                Component.literal("  \u2022 " + p + "  \u2715"), b -> {
                    players.remove(idx);
                    sendUpdate();
                    rebuild();
                }).pos(lx, y).size(160, 20).build());
            y += 22;
        }

        if (players.size() > scrollable) {
            addRenderableWidget(Button.builder(
                Component.literal("... " + (players.size() - scrollable) + " more"), b -> {})
                .pos(lx, y).size(160, 20).build());
        }
        y = listY_start + listHeight + 8;

        // Save / Cancel
        addButton(lx, y, 70, LocalizationManager.translate("gui.save"), this::onClose);
        addButton(lx + 80, y, 70, LocalizationManager.translate("gui.cancel"), this::onClose);
    }

    private void addButton(int x, int y, int w, String text, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> action.run())
            .pos(x, y).size(w, 20).build());
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
        if (this.minecraft != null) {
            this.minecraft.setScreen(parentScreen);
            parentScreen.updateAfterEdit();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int cx = this.width / 2;
        g.fill(0, 0, this.width, 24, GLASS_DARK);
        g.fill(0, 23, this.width, 24, BORDER_SOFT);
        g.drawCenteredString(this.font,
            Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE), cx, 6, 0xFFFFFF);

        int lx = Math.max(10, cx - this.width / 4);
        // Glass panel around list area
        g.fill(lx - 4, 56, lx + 230, this.height - 70, GLASS_PANEL);
        g.fill(lx - 4, 56, lx + 230, 57, BORDER_SOFT);
        g.drawString(this.font, Component.literal("  Players").withStyle(ChatFormatting.DARK_GRAY),
            lx, 57, 0x888888);

        super.render(g, mx, my, pt);
    }
}
