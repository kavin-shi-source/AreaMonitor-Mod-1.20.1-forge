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
 * Restriction settings panel — item blacklist and command restrictions.
 * Glass Morphism theme.
 */
public class RestrictionEditPanel extends Screen {

    private static final int GLASS_DARK    = 0xC0000000;
    private static final int GLASS_PANEL   = 0x70000000;
    private static final int BORDER_SOFT   = 0x50FFFFFF;
    private static final int BORDER_BRIGHT = 0x80FFFFFF;
    private static final int BORDER_FAINT  = 0x20FFFFFF;
    private static final Gson GSON = new Gson();

    private final Screen returnScreen;
    private final AreaManagementScreen mainScreen;
    private final S2CAreaListPacket.AreaEntry entry;

    private boolean enableItemBlacklist = true;
    private boolean blockTeleportCommands = true;
    private final List<String> blockedItems = new ArrayList<>();
    private final List<String> blockedCommands = new ArrayList<>();
    private EditBox addItemBox, addCmdBox;
    private boolean itemsSectionExpanded = true;
    private boolean cmdsSectionExpanded = true;

    public RestrictionEditPanel(Screen returnScreen, AreaManagementScreen mainScreen, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.restriction_settings") + ": " + entry.name()));
        this.returnScreen = returnScreen;
        this.mainScreen = mainScreen;
        this.entry = entry;
        if (entry.restrictionsJson() != null) {
            try {
                JsonObject obj = GSON.fromJson(entry.restrictionsJson(), JsonObject.class);
                enableItemBlacklist = getBool(obj, "enableItemBlacklist", true);
                blockTeleportCommands = getBool(obj, "blockTeleportCommands", true);
                loadList(obj, "blockedItems", blockedItems);
                loadList(obj, "blockedCommands", blockedCommands);
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int lx = Math.max(10, cx - this.width / 4);
        int y = 30;

        // === Item Blacklist ===
        int s1top = y - 8;
        addGlassBtn(lx, y, 240, LocalizationManager.translate("gui.restriction_items") + (enableItemBlacklist ?
            " " + LocalizationManager.translate("gui.prot_enabled") : " " + LocalizationManager.translate("gui.prot_disabled")) +
            (itemsSectionExpanded ? "  \u25BC" : "  \u25B6"),
            () -> { itemsSectionExpanded = !itemsSectionExpanded; rebuild(); });
        y += 24;

        if (itemsSectionExpanded) {
            addGlassBtn(lx, y, 80,
                LocalizationManager.translate(enableItemBlacklist ? "gui.disable" : "gui.enable"),
                () -> { enableItemBlacklist = !enableItemBlacklist; rebuild(); });
            y += 24;

            if (enableItemBlacklist) {
                addItemBox = new EditBox(this.font, lx, y, 150, 18, Component.literal("item id"));
                addItemBox.setMaxLength(100);
                addRenderableWidget(addItemBox);
                addBtn(lx + 155, y, 40, "+", () -> {
                    String v = addItemBox.getValue().trim().toLowerCase();
                    if (!v.isEmpty() && !blockedItems.contains(v)) {
                        blockedItems.add(v); addItemBox.setValue(""); sendUpdate(); rebuild();
                    }
                });
                y += 22;
                int max = Math.min(blockedItems.size(), 8);
                for (int i = 0; i < max; i++) {
                    final int idx = i;
                    addRemovable(lx + 10, y, blockedItems.get(i), () -> {
                        blockedItems.remove(idx); sendUpdate(); rebuild(); });
                    y += 20;
                }
            }
        }
        y += 6;

        // === Command Restrictions ===
        int s2top = y - 8;
        addGlassBtn(lx, y, 240, LocalizationManager.translate("gui.restriction_commands") + (blockTeleportCommands ?
            " " + LocalizationManager.translate("gui.prot_enabled") : " " + LocalizationManager.translate("gui.prot_disabled")) +
            (cmdsSectionExpanded ? "  \u25BC" : "  \u25B6"),
            () -> { cmdsSectionExpanded = !cmdsSectionExpanded; rebuild(); });
        y += 24;

        if (cmdsSectionExpanded) {
            addGlassBtn(lx, y, 80,
                LocalizationManager.translate(blockTeleportCommands ? "gui.disable" : "gui.enable"),
                () -> { blockTeleportCommands = !blockTeleportCommands; rebuild(); });
            y += 24;

            addCmdBox = new EditBox(this.font, lx, y, 150, 18, Component.literal("command"));
            addCmdBox.setMaxLength(100);
            addRenderableWidget(addCmdBox);
            addBtn(lx + 155, y, 40, "+", () -> {
                String v = addCmdBox.getValue().trim();
                if (!v.isEmpty() && !blockedCommands.contains(v)) {
                    blockedCommands.add(v); addCmdBox.setValue(""); sendUpdate(); rebuild();
                }
            });
            y += 22;
            int max = Math.min(blockedCommands.size(), 6);
            for (int i = 0; i < max; i++) {
                final int idx = i;
                addRemovable(lx + 10, y, blockedCommands.get(i), () -> {
                    blockedCommands.remove(idx); sendUpdate(); rebuild(); });
                y += 20;
            }
        }
        y += 10;

        // Save / Cancel
        addBtn(lx, y, 70, LocalizationManager.translate("gui.save"), this::onClose);
        addBtn(lx + 80, y, 70, LocalizationManager.translate("gui.cancel"), this::onClose);
    }

    private void addGlassBtn(int x, int y, int w, String text, Runnable action) {
        addRenderableWidget(GlassButton.create(x, y, w, 20, text, b -> action.run()));
    }

    private void addBtn(int x, int y, int w, String text, Runnable action) {
        addRenderableWidget(GlassButton.create(x, y, w, 20, text, b -> action.run()));
    }

    private void addRemovable(int x, int y, String text, Runnable remove) {
        addRenderableWidget(GlassButton.create(x, y, 160, 18, "  \u2022 " + text + "  \u2715", b -> remove.run()));
    }

    private boolean getBool(JsonObject o, String k, boolean def) {
        return o.has(k) ? o.get(k).getAsBoolean() : def;
    }

    private void loadList(JsonObject o, String k, List<String> dest) {
        if (o.has(k) && o.get(k).isJsonArray()) {
            for (var e : o.getAsJsonArray(k)) dest.add(e.getAsString());
        }
    }

    private void rebuild() { this.clearWidgets(); init(); }

    private void sendUpdate() {
        var json = new JsonObject();
        var rest = new JsonObject();
        rest.addProperty("enableItemBlacklist", enableItemBlacklist);
        rest.addProperty("blockTeleportCommands", blockTeleportCommands);
        JsonArray items = new JsonArray();
        for (String s : blockedItems) items.add(s);
        rest.add("blockedItems", items);
        JsonArray cmds = new JsonArray();
        for (String s : blockedCommands) cmds.add(s);
        rest.add("blockedCommands", cmds);
        json.add("restrictions", rest);
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
        g.fill(0, 0, this.width, 24, GLASS_DARK);
        g.fill(0, 23, this.width, 24, BORDER_SOFT);
        g.drawCenteredString(this.font,
            Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE), cx, 6, 0xFFFFFF);

        int lx = Math.max(10, cx - this.width / 4);
        super.render(g, mx, my, pt);
    }
}
