package com.kavinshi.areamonitor.client.gui;

import com.kavinshi.areamonitor.network.C2SAreaActionPacket;
import com.kavinshi.areamonitor.network.C2SRequestAreaListPacket;
import com.kavinshi.areamonitor.network.ModNetwork;
import com.kavinshi.areamonitor.network.S2CAreaListPacket;
import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.client.gui.panel.AreaEditPanel;
import com.kavinshi.areamonitor.client.gui.widget.GlassButton;
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

    // === Warm Parchment palette ===
    private static final int PARCH_DARK   = 0xD03A2A1A; // title bar (deep brown)
    private static final int PARCH_PANEL  = 0xC0C4A882; // panel bg (cream parchment)
    private static final int PARCH_LIGHT  = 0xA0D4B896; // light parchment overlay
    private static final int BORDER_GOLD  = 0x808B6914; // gold-brown accent
    private static final int BORDER_SHADOW = 0x405C4033; // subtle brown shadow
    private static final int TEXT_PRIMARY = 0xFFEFEDEB; // light cream
    private static final int TEXT_DIM     = 0xFFFFFFFF; // white
    private static final int ROW_ALT      = 0x18E8D5B7; // warm tint stripe
    private static final int ACCENT_GREEN  = 0x604B8C3E; // muted green
    private static final int ACCENT_RED    = 0x608C3E3E; // muted red

    private List<S2CAreaListPacket.AreaEntry> areas = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean creating = false;
    private EditBox nameInput;

    // Toast feedback
    private String toastMessage = "";
    private long toastEndMs = 0;

    // Dynamic layout (recalculated in init)
    private int itemHeight = 27;
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
        listLeft   = this.width / 14;
        listWidth  = Math.min(this.width - listLeft * 2 - 130, 413);
        listTop    = 46;
        itemsPerPage = Math.max(3, (this.height - 162) / itemHeight);
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
        addRenderableWidget(GlassButton.create(x, y, w, 20, text, b -> action.run()));
    }

    void onToggle(String name) {
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.TOGGLE, name));
        showToast(LocalizationManager.translate("gui.toast_saved"));
    }
    void onDelete(String name) {
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.DELETE, name));
        showToast(LocalizationManager.translate("gui.toast_deleted"));
    }
    void onRefresh() {
        ModNetwork.sendToServer(new C2SRequestAreaListPacket());
    }

    void onEdit(S2CAreaListPacket.AreaEntry entry) {
        if (this.minecraft != null) this.minecraft.setScreen(new AreaEditPanel(this, entry));
    }

    public void updateAfterEdit() { ModNetwork.sendToServer(new C2SRequestAreaListPacket()); }

    void onCreateConfirm() {
        if (nameInput != null) {
            String name = nameInput.getValue().trim();
            if (!name.isEmpty()) {
                ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.CREATE, name));
                showToast(LocalizationManager.translate("gui.toast_created"));
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

        // === Title bar ===
        g.fill(0, 0, this.width, 31, PARCH_DARK);
        g.fill(0, 30, this.width, 31, BORDER_GOLD);
        g.fill(0, 0, this.width, 1, BORDER_GOLD);
        g.drawCenteredString(this.font,
            Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE), cx, 10, 0xFFF5DEB3);

        // === Column header ===
        int hdrW = listWidth + 115;
        int hdrY = listTop - 16;
        g.fill(listLeft - 6, hdrY, listLeft + hdrW, hdrY + 15, PARCH_PANEL);
        g.fill(listLeft - 6, hdrY, listLeft + hdrW, hdrY + 1, BORDER_GOLD);
        g.fill(listLeft - 6, hdrY + 14, listLeft + hdrW, hdrY + 15, BORDER_SHADOW);
        g.drawString(this.font, LocalizationManager.translate("gui.header_info"),
            listLeft, listTop - 13, TEXT_DIM);
        g.drawString(this.font, LocalizationManager.translate("gui.header_action"),
            listLeft + listWidth - 60, listTop - 13, TEXT_DIM);

        if (!areas.isEmpty()) {
            String info = (scrollOffset + 1) + "-" +
                Math.min(scrollOffset + itemsPerPage, areas.size()) + "  /  " + areas.size();
            g.drawString(this.font, info, listLeft + listWidth + 20, listTop - 13, TEXT_DIM);
        }

        // === Area rows ===
        int visible = Math.min(Math.max(0, areas.size() - scrollOffset), itemsPerPage);
        for (int i = 0; i < visible; i++) {
            int idx = scrollOffset + i;
            S2CAreaListPacket.AreaEntry entry = areas.get(idx);
            int rowY = listTop + i * itemHeight;

            int rowBg = (i % 2 == 0) ? 0 : ROW_ALT;
            g.fill(listLeft - 2, rowY, listLeft + listWidth + 112, rowY + itemHeight - 1, rowBg);
            g.fill(listLeft - 2, rowY + 2, listLeft - 1, rowY + itemHeight - 3,
                entry.enabled() ? ACCENT_GREEN : ACCENT_RED);

            boolean hasProt = entry.protBlockBreak() || entry.protBlockPlace() || entry.protBlockInteract()
                || entry.protPvp() || entry.protExplosion() || entry.protEntityDamage();
            if (hasProt) {
                g.fill(listLeft - 2, rowY, listLeft - 2 + 8, rowY + 7, entry.enabled() ? ACCENT_GREEN : 0x60808080);
                g.drawString(this.font, "\u26E8", listLeft - 1, rowY, entry.enabled() ? 0x3A6B3A : TEXT_DIM);
            }
        }

        // === List frame ===
        int listBottom = listTop + itemsPerPage * itemHeight;
        g.fill(listLeft - 6, listTop - 2, listLeft - 5, listBottom, BORDER_SHADOW);
        g.fill(listLeft + listWidth + 110, listTop - 2, listLeft + listWidth + 111, listBottom, BORDER_SHADOW);

        // === Status bar ===
        int statusY = this.height - 72;
        g.fill(0, statusY, this.width, statusY + 18, PARCH_DARK);
        g.fill(0, statusY, this.width, statusY + 1, BORDER_GOLD);
        String status = "\u25C6 " + LocalizationManager.translate("gui.status_monitoring") + ": " +
            areas.size() + " " + LocalizationManager.translate("gui.status_areas");
        g.drawCenteredString(this.font,
            Component.literal(status).withStyle(ChatFormatting.GRAY), cx, statusY + 5, TEXT_DIM);

        // === Toast ===
        renderToast(g, cx);

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

    // ===== Toast =====

    private void showToast(String msg) {
        this.toastMessage = msg;
        this.toastEndMs = System.currentTimeMillis() + 2000;
    }

    private void renderToast(GuiGraphics g, int cx) {
        if (toastMessage.isEmpty() || System.currentTimeMillis() > toastEndMs) {
            if (!toastMessage.isEmpty()) toastMessage = "";
            return;
        }
        long remaining = toastEndMs - System.currentTimeMillis();
        int alpha = remaining > 500 ? 0xD0 : (int)(0xD0 * remaining / 500);
        int bg = (alpha << 24) | 0x3A2A1A;
        int brd = ((Math.min(alpha + 0x10, 0xFF)) << 24) | 0x8B6914;

        int tw = this.font.width(toastMessage) + 24;
        int tx = cx - tw / 2;
        int ty = this.height / 2 - 10;

        g.fill(tx, ty, tx + tw, ty + 20, bg);
        g.fill(tx, ty, tx + tw, ty + 1, brd);
        g.fill(tx, ty + 19, tx + tw, ty + 20, brd);
        g.drawCenteredString(this.font, Component.literal(toastMessage),
            cx, ty + 5, 0xFFF5DEB3);
    }

    // ===== Static inner widget classes (glass-styled) =====

    static class AreaRowButton extends Button {
        final String areaName;
        final AreaManagementScreen parent;
        AreaRowButton(int x, int y, int w, int h, Component msg, String areaName, AreaManagementScreen parent) {
            super(x, y, w, h, msg, b -> {}, DEFAULT_NARRATION);
            this.areaName = areaName; this.parent = parent;
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            g.drawString(net.minecraft.client.Minecraft.getInstance().font, getMessage(),
                getX() + 2, getY() + (height - 8) / 2, 0xFFEFEDEB);
        }
    }

    static class AreaEditButton extends Button {
        AreaEditButton(int x, int y, int w, int h, S2CAreaListPacket.AreaEntry entry, AreaManagementScreen parent) {
            super(x, y, w, h,
                Component.literal("\u2699").withStyle(ChatFormatting.GRAY),
                b -> parent.onEdit(entry), DEFAULT_NARRATION);
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int bg = isHoveredOrFocused() ? 0x80B89B6A : 0x50C4A882;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.fill(getX(), getY(), getX() + width, getY() + 1, 0x608B6914);
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, isHoveredOrFocused() ? 0xFFEFEDEB : 0xFFFFFFFF);
        }
    }

    static class AreaToggleButton extends Button {
        AreaToggleButton(int x, int y, int w, int h, String name, boolean enabled, AreaManagementScreen parent) {
            super(x, y, w, h,
                Component.literal(enabled ? LocalizationManager.translate("gui.disable") : LocalizationManager.translate("gui.enable")),
                b -> parent.onToggle(name), DEFAULT_NARRATION);
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int c = getMessage().getString().equals(LocalizationManager.translate("gui.enable")) ? 0x504B8C3E : 0x508C3E3E;
            int bg = isHoveredOrFocused() ? c | 0x20000000 : c;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.fill(getX(), getY(), getX() + width, getY() + 1, 0x608B6914);
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, 0xFFEFEDEB);
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
        @Override public void onPress() {
            if (!confirming) { confirming = true; setMessage(Component.literal("\u2715?").withStyle(ChatFormatting.RED)); }
            else { parent.onDelete(areaName); }
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int bg = confirming ? 0x808C3E3E : isHoveredOrFocused() ? 0x80B89B6A : 0x50C4A882;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.fill(getX(), getY(), getX() + width, getY() + 1, 0x608B6914);
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, confirming ? 0xFFC0392B : 0xFFFFFFFF);
        }
    }
}
