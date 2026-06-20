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
        this.renderBackground(g);
        int cx = this.width / 2;
        g.fill(0, 0, this.width, 31, PARCH_DARK);
        g.fill(0, 30, this.width, 31, BORDER_GOLD);
        g.drawCenteredString(this.font,
            Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE), cx, 10, 0xFFF5DEB3);

        int lx = Math.max(10, cx - this.width / 4);
        g.fill(lx - 6, 36, lx + 232, this.height - 70, PARCH_PANEL);
        g.fill(lx - 6, 36, lx + 232, 37, BORDER_GOLD);
        g.fill(lx - 6, this.height - 71, lx + 232, this.height - 70, BORDER_SHADOW);
        g.drawString(this.font, Component.literal("  Players").withStyle(ChatFormatting.DARK_GRAY),
            lx + 2, 58, 0xFF8B6914);

        super.render(g, mx, my, pt);
    }
}
