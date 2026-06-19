package com.kavinshi.areamonitor.client.gui;

import com.kavinshi.areamonitor.network.C2SAreaActionPacket;
import com.kavinshi.areamonitor.network.C2SRequestAreaListPacket;
import com.kavinshi.areamonitor.network.ModNetwork;
import com.kavinshi.areamonitor.network.S2CAreaListPacket;
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
        super(Component.literal("Area Monitor Management"));
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
                Component.literal("Area name"));
            nameInput.setMaxLength(48);
            addRenderableWidget(nameInput);

            addRenderableWidget(
                Button.builder(Component.literal("Create"), b -> onCreateConfirm())
                    .pos(centerX - 110, btnY).size(70, 20).build());
            addRenderableWidget(
                Button.builder(Component.literal("Cancel"), b -> {
                    creating = false;
                    rebuildRows();
                }).pos(centerX - 30, btnY).size(70, 20).build());
        } else {
            addRenderableWidget(
                Button.builder(Component.literal("Create New Area"), b -> {
                    creating = true;
                    rebuildRows();
                }).pos(centerX - 110, btnY).size(70, 20).build());
            addRenderableWidget(
                Button.builder(Component.literal("Refresh"), b -> onRefresh())
                    .pos(centerX - 30, btnY).size(70, 20).build());
        }

        // Close button (always visible, same row)
        addRenderableWidget(
            Button.builder(Component.literal("Close"), b -> onClose())
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
            addRenderableWidget(new AreaRowButton(LIST_LEFT, y, LIST_WIDTH, ITEM_HEIGHT - 2,
                label, entry.name(), this));

            // Toggle button
            addRenderableWidget(new AreaToggleButton(LIST_LEFT + LIST_WIDTH + 4, y,
                50, ITEM_HEIGHT - 2, entry.name(), entry.enabled(), this));
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        // Draw header
        String header = "Name / Info";
        guiGraphics.drawString(this.font, header, LIST_LEFT + 5, LIST_TOP - 12, 0xAAAAAA);
        guiGraphics.drawString(this.font, "Action", LIST_LEFT + LIST_WIDTH + 10, LIST_TOP - 12, 0xAAAAAA);

        // Draw scroll info
        if (!areas.isEmpty()) {
            String info = (scrollOffset + 1) + "-" +
                Math.min(scrollOffset + ITEMS_PER_PAGE, areas.size()) + " / " + areas.size();
            guiGraphics.drawString(this.font, info, LIST_LEFT + LIST_WIDTH - 50, LIST_TOP - 12, 0xAAAAAA);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
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
            super(x, y, w, h, msg, b -> parent.onDelete(areaName), DEFAULT_NARRATION);
            this.areaName = areaName;
            this.parent = parent;
        }
    }

    static class AreaToggleButton extends Button {
        final String areaName;
        final AreaManagementScreen parent;

        AreaToggleButton(int x, int y, int w, int h, String areaName, boolean enabled, AreaManagementScreen parent) {
            super(x, y, w, h,
                Component.literal(enabled ? "Disable" : "Enable"),
                b -> parent.onToggle(areaName),
                DEFAULT_NARRATION);
            this.areaName = areaName;
            this.parent = parent;
        }
    }
}
