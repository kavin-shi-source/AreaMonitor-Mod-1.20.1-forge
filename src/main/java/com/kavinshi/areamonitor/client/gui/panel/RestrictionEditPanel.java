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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.*;

@OnlyIn(Dist.CLIENT)
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

    // Scroll support
    private int scrollOffset = 0;
    private int contentTopY, contentBottomY;
    // Unsaved-change tracking
    private boolean dirty = false;
    private final ConfirmDialog confirmDialog = new ConfirmDialog();
    // Tooltips
    private final List<TooltipZone> tooltips = new ArrayList<>();
    // Bottom buttons kept as fields so render() can draw them outside the scissor clip
    private GlassButton saveBtn, cancelBtn;

    public RestrictionEditPanel(Screen returnScreen, AreaManagementScreen mainScreen, S2CAreaListPacket.AreaEntry entry) {
        super(Component.literal(LocalizationManager.translate("gui.restriction_settings") + ": " + entry.name()));
        this.returnScreen = returnScreen; this.mainScreen = mainScreen; this.entry = entry;
        if (entry.restrictionsJson() != null) try {
            JsonObject obj = GSON.fromJson(entry.restrictionsJson(), JsonObject.class);
            enableItemBlacklist = zb(obj, "enableItemBlacklist", true);
            blockTeleportCommands = zb(obj, "blockTeleportCommands", true);
            zl(obj, "blockedItems", blockedItems); zl(obj, "blockedCommands", blockedCommands);
        } catch (Exception e) {
            // P2 #39: log parse failures instead of silently swallowing
            com.kavinshi.areamonitor.AreaMonitorMod.LOGGER.warn("Failed to parse restrictions JSON for area '{}': {}", entry.name(), e.getMessage());
        }
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
        int y = contentTopY - scrollOffset;

        int secW = Math.min(ww - 16, 360);

        // === Section: Item Blacklist ===
        int top = y; y += 14;
        String itemFold = itemsExp ? "  \u25BC" : "  \u25B6";
        String itemLabel = "  " + LocalizationManager.translate("gui.section_item_blacklist") +
            (enableItemBlacklist ? " \u2714" : " \u2718") + itemFold;
        zbtn(lx, y, secW, itemLabel, () -> { itemsExp = !itemsExp; rebuild(); });
        tooltips.add(new TooltipZone(lx, y, secW, 18, "gui.tooltip_item_blacklist"));
        y += 20;
        if (itemsExp) {
            zbtn(lx, y, 90, "[" + LocalizationManager.translate(enableItemBlacklist ? "gui.disable" : "gui.enable") + "]",
                () -> { enableItemBlacklist = !enableItemBlacklist; dirty = true; rebuild(); }); y += 20;
            if (enableItemBlacklist) {
                addItemBox = new EditBox(this.font, lx, y, 220, 16, Component.empty());
                addItemBox.setMaxLength(100);
                addItemBox.setResponder(s -> dirty = true);
                addRenderableWidget(addItemBox);
                zbtn(lx + 226, y, 60, "[" + LocalizationManager.translate("command.add") + "]", () -> {
                    String v = addItemBox.getValue().trim().toLowerCase();
                    if (!v.isEmpty() && !blockedItems.contains(v) && blockedItems.size() < 12) {
                        blockedItems.add(v); addItemBox.setValue(""); dirty = true; rebuild();
                    }
                });
                tooltips.add(new TooltipZone(lx, y, 220, 18, "gui.tooltip_item_input"));
                y += 22;
                for (int i = 0; i < blockedItems.size(); i++) {
                    final int idx = i; String p = blockedItems.get(i);
                    zbtn(lx + 6, y, 220, "  \u2022 " + p + "  \u2715", () -> { blockedItems.remove(idx); dirty = true; rebuild(); });
                    tooltips.add(new TooltipZone(lx + 6, y, 220, 18, "gui.tooltip_item_row"));
                    y += 18;
                }
            }
        }
        y += 8;

        // === Section: Command Restrictions ===
        y += 14;
        String cmdFold = cmdsExp ? "  \u25BC" : "  \u25B6";
        String cmdLabel = "  " + LocalizationManager.translate("gui.section_cmd_restrict") +
            (blockTeleportCommands ? " \u2714" : " \u2718") + cmdFold;
        zbtn(lx, y, secW, cmdLabel, () -> { cmdsExp = !cmdsExp; rebuild(); });
        tooltips.add(new TooltipZone(lx, y, secW, 18, "gui.tooltip_cmd_restrict"));
        y += 20;
        if (cmdsExp) {
            zbtn(lx, y, 90, "[" + LocalizationManager.translate(blockTeleportCommands ? "gui.disable" : "gui.enable") + "]",
                () -> { blockTeleportCommands = !blockTeleportCommands; dirty = true; rebuild(); }); y += 20;
            addCmdBox = new EditBox(this.font, lx, y, 220, 16, Component.empty());
            addCmdBox.setMaxLength(100);
            addCmdBox.setResponder(s -> dirty = true);
            addRenderableWidget(addCmdBox);
            zbtn(lx + 226, y, 60, "[" + LocalizationManager.translate("command.add") + "]", () -> {
                String v = addCmdBox.getValue().trim();
                if (!v.isEmpty() && !blockedCommands.contains(v) && blockedCommands.size() < 12) {
                    blockedCommands.add(v); addCmdBox.setValue(""); dirty = true; rebuild();
                }
            });
            tooltips.add(new TooltipZone(lx, y, 220, 18, "gui.tooltip_cmd_input"));
            y += 22;
            for (int i = 0; i < blockedCommands.size(); i++) {
                final int idx = i; String p = blockedCommands.get(i);
                zbtn(lx + 6, y, 220, "  \u2022 " + p + "  \u2715", () -> { blockedCommands.remove(idx); dirty = true; rebuild(); });
                tooltips.add(new TooltipZone(lx + 6, y, 220, 18, "gui.tooltip_cmd_row"));
                y += 18;
            }
        }
        y += 8;

        // Record content height and clamp scrollOffset
        int contentHeight = (y + scrollOffset) - contentTopY;
        int visibleH = contentBottomY - contentTopY;
        int maxScroll = Math.max(0, contentHeight - visibleH);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        // Bottom buttons — centered (kept as fields; rendered outside the scissor clip in render())
        int btnY = wy + wh - 30;
        int cx = wx + ww / 2;
        saveBtn = GlassButton.create(cx - 78, btnY, 70, 18, "[" + LocalizationManager.translate("gui.save") + "]", b -> {
            if (dirty) { sendUpdate(); dirty = false; mainScreen.updateAfterEdit(); }
            onClose();
        });
        cancelBtn = GlassButton.create(cx + 8, btnY, 70, 18, "[" + LocalizationManager.translate("gui.cancel") + "]", b -> {
            dirty = false;
            onClose();
        });
        // P1-3 fix: do not register to addRenderableWidget — rendered/hit-tested manually to avoid double rendering.
    }

    private void zbtn(int x, int y, int w, String text, Runnable a) { addRenderableWidget(GlassButton.create(x, y, w, 18, text, b -> a.run())); }
    private boolean zb(JsonObject o, String k, boolean d) { return o.has(k) ? o.get(k).getAsBoolean() : d; }
    private void zl(JsonObject o, String k, List<String> d) { if (o.has(k) && o.get(k).isJsonArray()) for (var e : o.getAsJsonArray(k)) d.add(e.getAsString()); }

    private void rebuild() {
        String savedItem = addItemBox != null ? addItemBox.getValue() : "";
        String savedCmd = addCmdBox != null ? addCmdBox.getValue() : "";
        clearWidgets();
        init();
        if (addItemBox != null) addItemBox.setValue(savedItem);
        if (addCmdBox != null) addCmdBox.setValue(savedCmd);
    }

    private void sendUpdate() {
        var json = new JsonObject(); var rest = new JsonObject();
        rest.addProperty("enableItemBlacklist", enableItemBlacklist); rest.addProperty("blockTeleportCommands", blockTeleportCommands);
        JsonArray items = new JsonArray(); for (String s : blockedItems) items.add(s); rest.add("blockedItems", items);
        JsonArray cmds = new JsonArray(); for (String s : blockedCommands) cmds.add(s); rest.add("blockedCommands", cmds);
        json.add("restrictions", rest);
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.UPDATE, entry.name(), json.toString()));
    }

    @Override public void onClose() { if (this.minecraft != null) this.minecraft.setScreen(returnScreen); }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // P2 #29: delegate to confirm dialog for Esc/Enter handling
        if (confirmDialog.isVisible()) return confirmDialog.keyPressed(keyCode, scanCode, modifiers);
        if (this.addItemBox != null && this.addItemBox.isFocused())
            return this.addItemBox.keyPressed(keyCode, scanCode, modifiers);
        if (this.addCmdBox != null && this.addCmdBox.isFocused())
            return this.addCmdBox.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char c, int modifiers) {
        // P2 #29: consume text input while confirmation dialog is visible
        if (confirmDialog.isVisible()) return true;
        if (this.addItemBox != null && this.addItemBox.isFocused())
            return this.addItemBox.charTyped(c, modifiers);
        if (this.addCmdBox != null && this.addCmdBox.isFocused())
            return this.addCmdBox.charTyped(c, modifiers);
        return super.charTyped(c, modifiers);
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        int visibleH = contentBottomY - contentTopY;
        int contentH = 0;
        // Approximate content height: items section + cmd section
        contentH += 14 + 20; // item header
        if (itemsExp) { contentH += 20; if (enableItemBlacklist) { contentH += 22 + blockedItems.size() * 18; } }
        contentH += 14 + 20; // cmd header
        if (cmdsExp) { contentH += 20 + 22 + blockedCommands.size() * 18; }
        int maxScroll = Math.max(0, contentH - visibleH);
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
        // P1-3 fix: manually dispatch click to unregistered saveBtn/cancelBtn
        if (saveBtn != null && saveBtn.isMouseOver(mx, my) && saveBtn.mouseClicked(mx, my, button)) return true;
        if (cancelBtn != null && cancelBtn.isMouseOver(mx, my) && cancelBtn.mouseClicked(mx, my, button)) return true;
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

        g.enableScissor(wx + 2, contentTopY, wx + ww - 2, contentBottomY);
        super.render(g, mx, my, pt);
        g.disableScissor();

        // Re-draw bottom buttons manually (they sit below the scissor clip)
        if (saveBtn != null) saveBtn.render(g, mx, my, pt);
        if (cancelBtn != null) cancelBtn.render(g, mx, my, pt);

        // Scrollbar
        int visibleH = contentBottomY - contentTopY;
        int contentH = 0;
        contentH += 14 + 20;
        if (itemsExp) { contentH += 20; if (enableItemBlacklist) { contentH += 22 + blockedItems.size() * 18; } }
        contentH += 14 + 20;
        if (cmdsExp) { contentH += 20 + 22 + blockedCommands.size() * 18; }
        if (contentH > visibleH) {
            int barX = wx + ww - 10;
            int thumbH = Math.max(20, visibleH * visibleH / contentH);
            int thumbY = contentTopY + (visibleH - thumbH) * scrollOffset / Math.max(1, contentH - visibleH);
            g.fill(barX, contentTopY, barX + 4, contentTopY + visibleH, 0x408B6914);
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
