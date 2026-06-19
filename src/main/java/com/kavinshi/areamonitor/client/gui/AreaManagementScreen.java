package com.kavinshi.areamonitor.client.gui;

import com.kavinshi.areamonitor.network.C2SAreaActionPacket;
import com.kavinshi.areamonitor.network.C2SRequestAreaListPacket;
import com.kavinshi.areamonitor.network.ModNetwork;
import com.kavinshi.areamonitor.network.S2CAreaListPacket;
import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.client.gui.panel.AreaEditPanel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side GUI for managing monitor areas.
 * Only accessible when the player client has this mod installed.
 */
public class AreaManagementScreen extends Screen {

    private List<S2CAreaListPacket.AreaEntry> areas = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean creating = false;
    private EditBox nameInput;
    private static final int ITEMS_PER_PAGE = 8;
    private static final int ITEM_HEIGHT = 28;
    private static final int LIST_TOP = 40;
    private static final int LIST_LEFT = 30;
    private static final int LIST_WIDTH = 280;

    public AreaManagementScreen() {
        super(Component.literal(LocalizationManager.translate("gui.title")));
    }

    @Override
    protected void init() {
        super.init();

        // Request area list from server
        ModNetwork.sendToServer(new C2SRequestAreaListPacket());

        rebuildRows();
    }

    public void updateAreaList(List<S2CAreaListPacket.AreaEntry> newAreas) {
        this.areas = newAreas;
        this.scrollOffset = 0;
        rebuildRows();
    }

    private void rebuildRows() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int btnY = this.height - 35;

        if (creating) {
            // Name input field above the buttons
            nameInput = new EditBox(this.font, centerX - 110, this.height - 58, 220, 18,
                Component.literal(LocalizationManager.translate("gui.area_name_hint")));
            nameInput.setMaxLength(48);
            addRenderableWidget(nameInput);

            addRenderableWidget(
                Button.builder(Component.literal(LocalizationManager.translate("gui.create")), b -> onCreateConfirm())
                    .pos(centerX - 110, btnY).size(70, 20).build());
            addRenderableWidget(
                Button.builder(Component.literal(LocalizationManager.translate("gui.cancel")), b -> {
                    creating = false;
                    rebuildRows();
                }).pos(centerX - 30, btnY).size(70, 20).build());
        } else {
            addRenderableWidget(
                Button.builder(Component.literal(LocalizationManager.translate("gui.create_new_area")), b -> {
                    creating = true;
                    rebuildRows();
                }).pos(centerX - 110, btnY).size(70, 20).build());
            addRenderableWidget(
                Button.builder(Component.literal(LocalizationManager.translate("gui.refresh")), b -> onRefresh())
                    .pos(centerX - 30, btnY).size(70, 20).build());
        }

        // Close button (always visible, same row)
        addRenderableWidget(
            Button.builder(Component.literal(LocalizationManager.translate("gui.close")), b -> onClose())
                .pos(centerX + 50, btnY).size(70, 20).build());

        int visible = Math.min(Math.max(0, areas.size() - scrollOffset), ITEMS_PER_PAGE);
        for (int i = 0; i < visible; i++) {
            int idx = scrollOffset + i;
            S2CAreaListPacket.AreaEntry entry = areas.get(idx);
            int y = LIST_TOP + 4 + i * ITEM_HEIGHT;

            Component label = entry.enabled()
                ? Component.literal("✔ ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(entry.name() + " ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("[" + entry.dimension() + "] " + entry.boundsType())
                        .withStyle(ChatFormatting.GRAY))
                : Component.literal("✘ ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(entry.name() + " ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("[" + entry.dimension() + "] " + entry.boundsType())
                        .withStyle(ChatFormatting.GRAY));
            // Info-only row button (click does nothing — edit/delete are separate buttons)
            addRenderableWidget(new AreaRowButton(LIST_LEFT, y, LIST_WIDTH - 75, ITEM_HEIGHT - 2,
                label, entry.name(), this));

            // Edit button
            addRenderableWidget(new AreaEditButton(LIST_LEFT + LIST_WIDTH - 70, y,
                20, ITEM_HEIGHT - 2, entry, this));

            // Toggle button
            addRenderableWidget(new AreaToggleButton(LIST_LEFT + LIST_WIDTH - 45, y,
                50, ITEM_HEIGHT - 2, entry.name(), entry.enabled(), this));

            // Delete button with confirmation
            addRenderableWidget(new AreaDeleteButton(LIST_LEFT + LIST_WIDTH + 10, y,
                20, ITEM_HEIGHT - 2, entry.name(), this));
        }
    }

    void onToggle(String areaName) {
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.TOGGLE, areaName));
    }

    void onDelete(String areaName) {
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.DELETE, areaName));
    }

    void onRefresh() {
        ModNetwork.sendToServer(new C2SRequestAreaListPacket());
    }

    void onEdit(S2CAreaListPacket.AreaEntry entry) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new AreaEditPanel(this, entry));
        }
    }

    public void updateAfterEdit() {
        ModNetwork.sendToServer(new C2SRequestAreaListPacket());
    }

    void onCreateConfirm() {
        if (nameInput != null) {
            String name = nameInput.getValue().trim();
            if (!name.isEmpty()) {
                ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.CREATE, name));
                creating = false;
                nameInput = null;
                rebuildRows();
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 && creating && nameInput != null && nameInput.isFocused()) { // Enter key
            onCreateConfirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int cx = this.width / 2;

        // Title bar background
        g.fill(0, 0, this.width, 22, 0x30000000);
        g.drawCenteredString(this.font,
            Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE),
            cx, 6, 0xFFFFFF);

        // Column header background
        g.fill(LIST_LEFT - 2, LIST_TOP - 14, LIST_LEFT + LIST_WIDTH + 120, LIST_TOP - 14 + 12, 0x20000000);
        g.drawString(this.font,
            LocalizationManager.translate("gui.header_info"),
            LIST_LEFT + 3, LIST_TOP - 12, 0xAAAAAA);
        g.drawString(this.font,
            LocalizationManager.translate("gui.header_action"),
            LIST_LEFT + LIST_WIDTH + 10, LIST_TOP - 12, 0xAAAAAA);

        // Scroll info
        if (!areas.isEmpty()) {
            String info = (scrollOffset + 1) + "-" +
                Math.min(scrollOffset + ITEMS_PER_PAGE, areas.size()) + " / " + areas.size();
            g.drawString(this.font, info, LIST_LEFT + LIST_WIDTH - 50, LIST_TOP - 12, 0x888888);
        }

        // Status bar between list and buttons
        int statusY = this.height - 70;
        g.fill(0, statusY, this.width, statusY + 14, 0x20000000);
        String status = LocalizationManager.translate("gui.status_monitoring") + ": " +
            areas.size() + " " + LocalizationManager.translate("gui.status_areas");
        g.drawCenteredString(this.font, Component.literal(status).withStyle(ChatFormatting.GRAY),
            cx, statusY + 3, 0xAAAAAA);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset--;
            rebuildRows();
        } else if (delta < 0 && scrollOffset < areas.size() - ITEMS_PER_PAGE) {
            scrollOffset++;
            rebuildRows();
        }
        return true;
    }

    // ---- Inner classes for custom buttons ----

    static class AreaRowButton extends Button {
        final String areaName;
        final AreaManagementScreen parent;

        AreaRowButton(int x, int y, int w, int h, Component msg, String areaName, AreaManagementScreen parent) {
            super(x, y, w, h, msg, b -> {}, DEFAULT_NARRATION); // Click does nothing
            this.areaName = areaName;
            this.parent = parent;
        }
    }

    static class AreaToggleButton extends Button {
        final String areaName;
        final AreaManagementScreen parent;

        AreaToggleButton(int x, int y, int w, int h, String areaName, boolean enabled, AreaManagementScreen parent) {
            super(x, y, w, h,
                Component.literal(LocalizationManager.translate(enabled ? "gui.disable" : "gui.enable")),
                b -> parent.onToggle(areaName),
                DEFAULT_NARRATION);
            this.areaName = areaName;
            this.parent = parent;
        }
    }

    static class AreaDeleteButton extends Button {
        private final String areaName;
        private final AreaManagementScreen parent;
        private boolean confirming = false;

        AreaDeleteButton(int x, int y, int w, int h, String areaName, AreaManagementScreen parent) {
            super(x, y, w, h,
                Component.literal(LocalizationManager.translate("gui.delete")).withStyle(ChatFormatting.DARK_GRAY),
                b -> {},
                DEFAULT_NARRATION);
            this.areaName = areaName;
            this.parent = parent;
        }

        @Override
        public void onPress() {
            if (!confirming) {
                confirming = true;
                setMessage(Component.literal(LocalizationManager.translate("gui.delete_confirm")).withStyle(ChatFormatting.RED));
            } else {
                parent.onDelete(areaName);
            }
        }

        public void resetConfirmation() {
            confirming = false;
            setMessage(Component.literal(LocalizationManager.translate("gui.delete")).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    static class AreaEditButton extends Button {
        final S2CAreaListPacket.AreaEntry entry;
        final AreaManagementScreen parent;

        AreaEditButton(int x, int y, int w, int h, S2CAreaListPacket.AreaEntry entry, AreaManagementScreen parent) {
            super(x, y, w, h,
                Component.literal("⚙").withStyle(ChatFormatting.DARK_GRAY),
                b -> parent.onEdit(entry),
                DEFAULT_NARRATION);
            this.entry = entry;
            this.parent = parent;
        }
    }
}
