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
 * Client-side GUI for managing monitor areas — Glass Morphism theme.
 */
public class AreaManagementScreen extends Screen {

    // === Glass Morphism palette ===
    private static final int GLASS_DARK   = 0xA0000000; // title bar
    private static final int GLASS_PANEL  = 0x60000000; // section bg
    private static final int GLASS_SUBTLE = 0x30000000; // sub-bg
    private static final int BORDER_FAINT = 0x20FFFFFF; // subtle border
    private static final int BORDER_SOFT  = 0x40FFFFFF; // visible border
    private static final int TEXT_DIM     = 0xB0B0B0B0; // secondary text
    private static final int ROW_ODD      = 0x0CFFFFFF; // alternating row

    private List<S2CAreaListPacket.AreaEntry> areas = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean creating = false;
    private EditBox nameInput;

    // Dynamic layout (recalculated in init)
    private int itemHeight = 24;
    private int itemsPerPage = 8;
    private int listLeft, listTop, listWidth;
    private int btnY;

    public AreaManagementScreen() {
        super(Component.literal(LocalizationManager.translate("gui.title")));
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        ModNetwork.sendToServer(new C2SRequestAreaListPacket());
        rebuildRows();
    }

    private void calculateLayout() {
        listLeft   = this.width / 8;
        listWidth  = Math.min(this.width - listLeft * 2 - 120, 420);
        listTop    = 42;
        itemsPerPage = Math.max(3, (this.height - 158) / itemHeight);
        btnY = this.height - 35;
    }

    public void updateAreaList(List<S2CAreaListPacket.AreaEntry> newAreas) {
        this.areas = newAreas;
        this.scrollOffset = 0;
        rebuildRows();
    }

    private void rebuildRows() {
        this.clearWidgets();
        int cx = this.width / 2;

        if (creating) {
            nameInput = new EditBox(this.font, cx - 110, btnY - 23, 220, 18,
                Component.literal(LocalizationManager.translate("gui.area_name_hint")));
            nameInput.setMaxLength(48);
            addRenderableWidget(nameInput);

            glassBtn(LocalizationManager.translate("gui.create"), cx - 110, btnY, 70, this::onCreateConfirm);
            glassBtn(LocalizationManager.translate("gui.cancel"), cx - 30, btnY, 70, () -> {
                creating = false; rebuildRows(); });
        } else {
            glassBtn(LocalizationManager.translate("gui.create_new_area"), cx - 110, btnY, 70, () -> {
                creating = true; rebuildRows(); });
            glassBtn(LocalizationManager.translate("gui.refresh"), cx - 30, btnY, 70, this::onRefresh);
        }
        glassBtn(LocalizationManager.translate("gui.close"), cx + 50, btnY, 70, this::onClose);

        // Area rows
        int visible = Math.min(Math.max(0, areas.size() - scrollOffset), itemsPerPage);
        for (int i = 0; i < visible; i++) {
            int idx = scrollOffset + i;
            S2CAreaListPacket.AreaEntry entry = areas.get(idx);
            int y = listTop + 2 + i * itemHeight;

            Component label = entry.enabled()
                ? Component.literal("\u2714 ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(entry.name() + " ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("[" + entry.dimension() + "] " + entry.boundsType())
                        .withStyle(ChatFormatting.GRAY))
                : Component.literal("\u2718 ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(entry.name() + " ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("[" + entry.dimension() + "] " + entry.boundsType())
                        .withStyle(ChatFormatting.GRAY));

            int infoW = listWidth - 75;
            addRenderableWidget(new AreaRowButton(listLeft, y, infoW, itemHeight - 2, label, entry.name(), this));
            addRenderableWidget(new AreaEditButton(listLeft + infoW + 5, y, 20, itemHeight - 2, entry, this));
            addRenderableWidget(new AreaToggleButton(listLeft + infoW + 30, y, 50, itemHeight - 2,
                entry.name(), entry.enabled(), this));
            addRenderableWidget(new AreaDeleteButton(listLeft + infoW + 85, y, 20, itemHeight - 2,
                entry.name(), this));
        }
    }

    private void glassBtn(String text, int x, int y, int w, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> action.run())
            .pos(x, y).size(w, 20).build());
    }

    void onToggle(String name) { ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.TOGGLE, name)); }
    void onDelete(String name) { ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.DELETE, name)); }
    void onRefresh()            { ModNetwork.sendToServer(new C2SRequestAreaListPacket()); }

    void onEdit(S2CAreaListPacket.AreaEntry entry) {
        if (this.minecraft != null) this.minecraft.setScreen(new AreaEditPanel(this, entry));
    }

    public void updateAfterEdit() { ModNetwork.sendToServer(new C2SRequestAreaListPacket()); }

    void onCreateConfirm() {
        if (nameInput != null) {
            String name = nameInput.getValue().trim();
            if (!name.isEmpty()) {
                ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.CREATE, name));
                creating = false; nameInput = null; rebuildRows();
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 && creating && nameInput != null && nameInput.isFocused()) {
            onCreateConfirm(); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int cx = this.width / 2;

        // === Title bar (glass dark) ===
        g.fill(0, 0, this.width, 24, GLASS_DARK);
        g.fill(0, 23, this.width, 24, BORDER_SOFT);
        g.drawCenteredString(this.font,
            Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE), cx, 6, 0xFFFFFF);

        // === Column header (glass panel) ===
        int hdrW = listWidth + 110;
        int hdrY = listTop - 14;
        g.fill(listLeft - 4, hdrY, listLeft + hdrW, hdrY + 13, GLASS_PANEL);
        g.fill(listLeft - 4, hdrY + 12, listLeft + hdrW, hdrY + 13, BORDER_FAINT);
        g.drawString(this.font, LocalizationManager.translate("gui.header_info"),
            listLeft, listTop - 12, TEXT_DIM);
        g.drawString(this.font, LocalizationManager.translate("gui.header_action"),
            listLeft + listWidth - 65, listTop - 12, TEXT_DIM);

        if (!areas.isEmpty()) {
            String info = (scrollOffset + 1) + "-" +
                Math.min(scrollOffset + itemsPerPage, areas.size()) + " / " + areas.size();
            g.drawString(this.font, info, listLeft + listWidth - 50, listTop - 12, TEXT_DIM);
        }

        // === Alternating row backgrounds (glass subtle) ===
        int visible = Math.min(Math.max(0, areas.size() - scrollOffset), itemsPerPage);
        for (int i = 0; i < visible; i++) {
            if (i % 2 == 1) {
                g.fill(listLeft, listTop + i * itemHeight,
                    listLeft + listWidth + 105, listTop + (i + 1) * itemHeight - 1, ROW_ODD);
            }
        }

        // === Status bar (glass dark + border) ===
        int statusY = this.height - 72;
        g.fill(0, statusY, this.width, statusY + 16, GLASS_DARK);
        g.fill(0, statusY, this.width, statusY + 1, BORDER_SOFT);
        String status = LocalizationManager.translate("gui.status_monitoring") + ": " +
            areas.size() + " " + LocalizationManager.translate("gui.status_areas");
        g.drawCenteredString(this.font,
            Component.literal(status).withStyle(ChatFormatting.GRAY), cx, statusY + 4, TEXT_DIM);

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset--; rebuildRows();
        } else if (delta < 0 && scrollOffset < areas.size() - itemsPerPage) {
            scrollOffset++; rebuildRows();
        }
        return true;
    }

    // ===== Static inner widget classes =====

    static class AreaRowButton extends Button {
        final String areaName;
        final AreaManagementScreen parent;
        AreaRowButton(int x, int y, int w, int h, Component msg, String areaName, AreaManagementScreen parent) {
            super(x, y, w, h, msg, b -> {}, DEFAULT_NARRATION);
            this.areaName = areaName; this.parent = parent;
        }
    }

    static class AreaEditButton extends Button {
        AreaEditButton(int x, int y, int w, int h, S2CAreaListPacket.AreaEntry entry, AreaManagementScreen parent) {
            super(x, y, w, h,
                Component.literal("\u2699").withStyle(ChatFormatting.GRAY),
                b -> parent.onEdit(entry), DEFAULT_NARRATION);
        }
    }

    static class AreaToggleButton extends Button {
        AreaToggleButton(int x, int y, int w, int h, String name, boolean enabled, AreaManagementScreen parent) {
            super(x, y, w, h,
                Component.literal(enabled
                    ? LocalizationManager.translate("gui.disable")
                    : LocalizationManager.translate("gui.enable"))
                    .withStyle(enabled ? ChatFormatting.RED : ChatFormatting.GREEN),
                b -> parent.onToggle(name), DEFAULT_NARRATION);
        }
    }

    static class AreaDeleteButton extends Button {
        private boolean confirming = false;
        private final String areaName;
        private final AreaManagementScreen parent;
        AreaDeleteButton(int x, int y, int w, int h, String areaName, AreaManagementScreen parent) {
            super(x, y, w, h,
                Component.literal("\u2715").withStyle(ChatFormatting.GRAY),
                b -> {}, DEFAULT_NARRATION);
            this.areaName = areaName; this.parent = parent;
        }
        @Override
        public void onPress() {
            if (!confirming) {
                confirming = true;
                setMessage(Component.literal("\u2715?").withStyle(ChatFormatting.RED));
            } else {
                parent.onDelete(areaName);
            }
        }
    }
}
