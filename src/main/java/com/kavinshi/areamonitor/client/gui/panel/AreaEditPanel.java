package com.kavinshi.areamonitor.client.gui.panel;

import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.client.gui.AreaManagementScreen;
import com.kavinshi.areamonitor.network.C2SAreaActionPacket;
import com.kavinshi.areamonitor.network.S2CAreaListPacket;
import com.kavinshi.areamonitor.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Overlay panel for editing an area's basic properties.
 * Covers: display name, dimension, enter/leave mode, enabled toggle.
 */
public class AreaEditPanel extends Screen {

    private final AreaManagementScreen parentScreen;
    private final S2CAreaListPacket.AreaEntry entry;
    private EditBox displayNameInput;
    private boolean enabled;

    private static final List<String> DIMENSIONS = List.of(
        "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    private static final List<String> GAME_MODES = List.of(
        "survival", "creative", "adventure", "spectator");
    private static final String[] MODE_KEYS = {
        "gameMode.survival", "gameMode.creative", "gameMode.adventure", "gameMode.spectator"
    };

    private int dimIdx = 0;
    private int enterIdx = 0;
    private int leaveIdx = 0;

    public AreaEditPanel(AreaManagementScreen parent, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.title") + " - " + entry.name()));
        this.parentScreen = parent;
        this.entry = entry;
        this.enabled = entry.enabled();

        // Find current indices
        dimIdx = Math.max(0, DIMENSIONS.indexOf(entry.dimension()));
        enterIdx = Math.max(0, GAME_MODES.indexOf(entry.enterMode()));
        leaveIdx = Math.max(0, GAME_MODES.indexOf(entry.leaveMode()));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int y = 45;

        // Display Name
        addRenderableWidget(Button.builder(
            Component.literal("Display:"), b -> {}).pos(cx - 110, y).size(60, 20).build());
        displayNameInput = new EditBox(this.font, cx - 45, y, 155, 20,
            Component.literal(LocalizationManager.translate("gui.area_name_hint")));
        displayNameInput.setMaxLength(48);
        displayNameInput.setValue(entry.displayName() != null ? entry.displayName() : entry.name());
        addRenderableWidget(displayNameInput);
        y += 28;

        // Dimension cycler
        addRenderableWidget(Button.builder(
            Component.literal("Dim:"), b -> {}).pos(cx - 110, y).size(60, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("\u25C0"), b -> { dimIdx = (dimIdx - 1 + DIMENSIONS.size()) % DIMENSIONS.size(); rebuildRows(); })
            .pos(cx - 45, y).size(20, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal(DIMENSIONS.get(dimIdx)), b -> {})
            .pos(cx - 22, y).size(130, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("\u25B6"), b -> { dimIdx = (dimIdx + 1) % DIMENSIONS.size(); rebuildRows(); })
            .pos(cx + 111, y).size(20, 20).build());
        y += 28;

        // Enter Mode cycler
        String enterLabel = LocalizationManager.translate(MODE_KEYS[enterIdx]);
        addRenderableWidget(Button.builder(
            Component.literal("Enter:"), b -> {}).pos(cx - 110, y).size(60, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("\u25C0"), b -> { enterIdx = (enterIdx - 1 + GAME_MODES.size()) % GAME_MODES.size(); rebuildRows(); })
            .pos(cx - 45, y).size(20, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal(enterLabel), b -> {})
            .pos(cx - 22, y).size(130, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("\u25B6"), b -> { enterIdx = (enterIdx + 1) % GAME_MODES.size(); rebuildRows(); })
            .pos(cx + 111, y).size(20, 20).build());
        y += 28;

        // Leave Mode cycler
        String leaveLabel = LocalizationManager.translate(MODE_KEYS[leaveIdx]);
        addRenderableWidget(Button.builder(
            Component.literal("Leave:"), b -> {}).pos(cx - 110, y).size(60, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("\u25C0"), b -> { leaveIdx = (leaveIdx - 1 + GAME_MODES.size()) % GAME_MODES.size(); rebuildRows(); })
            .pos(cx - 45, y).size(20, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal(leaveLabel), b -> {})
            .pos(cx - 22, y).size(130, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("\u25B6"), b -> { leaveIdx = (leaveIdx + 1) % GAME_MODES.size(); rebuildRows(); })
            .pos(cx + 111, y).size(20, 20).build());
        y += 28;

        // Enabled toggle
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate(enabled ? "area.enabled" : "area.disabled")),
            b -> { enabled = !enabled; rebuildRows(); })
            .pos(cx - 110, y).size(155, 20).build());
        y += 38;

        // Save button
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate("gui.create")), b -> onSave())
            .pos(cx - 110, y).size(75, 20).build());
        // Cancel button
        addRenderableWidget(Button.builder(
            Component.literal(LocalizationManager.translate("gui.cancel")), b -> onClose())
            .pos(cx - 25, y).size(75, 20).build());
    }

    private void rebuildRows() {
        this.clearWidgets();
        init();
    }

    private void onSave() {
        // Build JSON payload with modified fields
        String displayName = displayNameInput != null ? displayNameInput.getValue().trim() : "";
        var json = new com.google.gson.JsonObject();
        if (!displayName.isEmpty() && !displayName.equals(entry.name())) {
            json.addProperty("displayName", displayName);
        }
        json.addProperty("dimension", DIMENSIONS.get(dimIdx));
        json.addProperty("enterMode", GAME_MODES.get(enterIdx));
        json.addProperty("leaveMode", GAME_MODES.get(leaveIdx));
        json.addProperty("enabled", enabled);

        ModNetwork.sendToServer(new C2SAreaActionPacket(
            C2SAreaActionPacket.Action.UPDATE, entry.name(), json.toString()));
        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parentScreen);
            parentScreen.updateAfterEdit();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
