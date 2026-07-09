package com.kavinshi.areamonitor.client.gui;

import com.kavinshi.areamonitor.network.C2SAreaActionPacket;
import com.kavinshi.areamonitor.network.C2SRequestAreaListPacket;
import com.kavinshi.areamonitor.network.ModNetwork;
import com.kavinshi.areamonitor.network.S2CAreaListPacket;
import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.util.DimensionUtils;
import com.kavinshi.areamonitor.client.gui.panel.AreaEditPanel;
import com.kavinshi.areamonitor.client.gui.widget.ConfirmDialog;
import com.kavinshi.areamonitor.client.gui.widget.GlassButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side GUI for managing monitor areas — Glass Morphism theme.
 */
@OnlyIn(Dist.CLIENT)
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
    private List<S2CAreaListPacket.AreaEntry> filteredAreas = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean creating = false;
    private EditBox nameInput;
    private EditBox searchInput;
    private String searchTerm = "";
    private boolean sortAsc = true; // toggle sort direction

    // Confirmation dialog
    private final ConfirmDialog confirmDialog = new ConfirmDialog();

    // Dynamic layout (recalculated in init)
    private int itemHeight = 27;
    private int itemsPerPage = 8;
    private int listLeft, listTop, listWidth;
    private int btnY;
    private int winX, winY, winW, winH;
    // : track whether we've already requested the area list to avoid re-requesting on resize
    private boolean listRequested = false;
    // : buffer for area list updates that arrive while a sub-panel is visible
    private static volatile List<S2CAreaListPacket.AreaEntry> pendingAreaList = null;
    // : cached Component objects to avoid per-frame allocation
    private Component cachedTitleComponent;
    private Component cachedStatusComponent;
    private int cachedStatusAreaCount = -1;

    public AreaManagementScreen() {
        super(Component.literal(LocalizationManager.translate("gui.title")));
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        // : apply any buffered update before refreshing UI
        if (pendingAreaList != null) {
            updateAreaList(pendingAreaList);
            pendingAreaList = null;
        }
        if (!listRequested) {
            ModNetwork.sendToServer(new C2SRequestAreaListPacket());
            listRequested = true;
        }
        // : (re)build cached title component on init in case locale changed
        cachedTitleComponent = Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE);
        cachedStatusAreaCount = -1; // force status bar rebuild
        rebuildRows();
    }

    /**
     * : buffer an area list update when the management screen is not the
     * active screen (e.g. an edit sub-panel is open). Applied on next init().
     */
    public static void bufferAreaList(List<S2CAreaListPacket.AreaEntry> areas) {
        pendingAreaList = areas;
    }

    private void calculateLayout() {
        winW = Math.min(this.width * 78 / 100, 560);
        winH = Math.min(this.height * 82 / 100, 480);
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;
        listLeft   = winX + 12;
        listWidth  = Math.min(winW - 24 - 100, 380);
        // Layout rows (top to bottom):
        //   title bar      winY+3  ~ winY+31
        //   search row     winY+36 ~ winY+54 (search box 14h + sort button 20h)
        //   column header  winY+56 ~ winY+71 (header bg + gold line)
        //   area list      winY+72 ~ ...
        listTop    = winY + 72;
        itemsPerPage = Math.max(3, (winH - 140) / itemHeight);
        btnY = winY + winH - 40;
    }

    public void updateAreaList(List<S2CAreaListPacket.AreaEntry> newAreas) {
        this.areas = newAreas;
        applyFilterAndSort();
        // : clamp scrollOffset instead of resetting to 0 — preserve user's scroll position
        int maxScroll = Math.max(0, filteredAreas.size() - itemsPerPage);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        rebuildRows();
    }

    private void applyFilterAndSort() {
        // Sort
        filteredAreas = new ArrayList<>(areas);
        filteredAreas.sort((a, b) -> sortAsc ? a.name().compareToIgnoreCase(b.name()) : b.name().compareToIgnoreCase(a.name()));
        // Filter
        if (!searchTerm.isEmpty()) {
            String term = searchTerm.toLowerCase();
            filteredAreas.removeIf(e -> !e.name().toLowerCase().contains(term));
        }
    }

    private void rebuildRows() {
        boolean searchFocused = searchInput != null && searchInput.isFocused();
        boolean nameFocused = nameInput != null && nameInput.isFocused();
        this.clearWidgets();
        int cx = winX + winW / 2;

        // Search bar — placed in the row between title bar (winY+3..31) and column header (winY+56..71).
        // searchY = listTop - 36 = winY + 36; bottom = winY + 50 (clears header top winY+56 by 6px).
        int searchY = listTop - 36;
        searchInput = new EditBox(this.font, listLeft, searchY, 150, 14, Component.literal(LocalizationManager.translate("gui.search")));
        searchInput.setMaxLength(32);
        searchInput.setValue(searchTerm);
        searchInput.setResponder(s -> { searchTerm = s; applyFilterAndSort(); scrollOffset = 0; rebuildRows(); });
        addRenderableWidget(searchInput);
        if (searchFocused) searchInput.setFocused(true);

        // Sort toggle — same row as search, aligned to search box top.
        String sortIcon = sortAsc ? "\u25B2" : "\u25BC"; // ▲ / ▼
        glassBtn(sortIcon, listLeft + 156, searchY - 2, 22, () -> { sortAsc = !sortAsc; applyFilterAndSort(); rebuildRows(); });

        if (creating) {
            nameInput = new EditBox(this.font, cx - 110, btnY - 23, 220, 18,
                Component.literal(LocalizationManager.translate("gui.area_name_hint")));
            nameInput.setMaxLength(32);
            addRenderableWidget(nameInput);
            if (nameFocused) nameInput.setFocused(true);
            glassBtn(LocalizationManager.translate("gui.create"), cx - 80, btnY, 70, this::onCreateConfirm);
            glassBtn(LocalizationManager.translate("gui.cancel"), cx + 10, btnY, 70, () -> { creating = false; rebuildRows(); });
        } else {
            glassBtn(LocalizationManager.translate("gui.create_new_area"), cx - 120, btnY, 80, () -> { creating = true; rebuildRows(); });
            glassBtn(LocalizationManager.translate("gui.refresh"), cx - 30, btnY, 60, this::onRefresh);
            glassBtn(LocalizationManager.translate("gui.close"), cx + 40, btnY, 60, this::onClose);
        }

        // Area rows
        int visible = Math.min(Math.max(0, filteredAreas.size() - scrollOffset), itemsPerPage);
        for (int i = 0; i < visible; i++) {
            int idx = scrollOffset + i;
            S2CAreaListPacket.AreaEntry entry = filteredAreas.get(idx);
            int y = listTop + 2 + i * itemHeight;
            int infoW = listWidth - 70;

            Component label = entry.enabled()
                ? Component.literal("\u2714 ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(entry.name() + " ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("[" + DimensionUtils.shortDim(entry.dimension()) + "] " + entry.boundsType()).withStyle(ChatFormatting.GRAY))                : Component.literal("\u2718 ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(entry.name() + " ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("[" + DimensionUtils.shortDim(entry.dimension()) + "] " + entry.boundsType()).withStyle(ChatFormatting.GRAY));
            addRenderableWidget(new AreaRowButton(listLeft, y, infoW, itemHeight - 2, label, entry.name(), this));
            addRenderableWidget(new AreaEditButton(listLeft + infoW + 2, y, 22, itemHeight - 2, entry, this));
            addRenderableWidget(new AreaToggleButton(listLeft + infoW + 26, y, 44, itemHeight - 2, entry.name(), entry.enabled(), this));
            addRenderableWidget(new AreaDeleteButton(listLeft + infoW + 72, y, 22, itemHeight - 2, entry.name(), this));
        }
    }

    private void glassBtn(String text, int x, int y, int w, Runnable action) {
        addRenderableWidget(GlassButton.create(x, y, w, 20, text, b -> action.run()));
    }

    void onToggle(String name) {
        // : do not show "saved" toast optimistically — wait for the refreshed area list as confirmation
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.TOGGLE, name));
    }
    void onDelete(String name) {
        // : do not show "deleted" toast optimistically — wait for the refreshed area list as confirmation
        ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.DELETE, name));
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
                // : do not show "created" toast optimistically
                ModNetwork.sendToServer(new C2SAreaActionPacket(C2SAreaActionPacket.Action.CREATE, name));
                creating = false; nameInput = null; rebuildRows();
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // : confirmation dialog must consume keyboard input when visible
        if (confirmDialog.isVisible()) {
            return confirmDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == 257 && creating && nameInput != null && nameInput.isFocused()) {
            onCreateConfirm(); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        // : consume text input while confirmation dialog is visible
        if (confirmDialog.isVisible()) return true;
        return super.charTyped(c, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        // Dimmed overlay — game world visible through dark tint
        g.fill(0, 0, this.width, this.height, 0x80000000);

        // === Window background ===
        g.fill(winX, winY, winX + winW, winY + winH, PARCH_PANEL);
        g.fill(winX + 1, winY + 1, winX + winW - 1, winY + winH - 1, 0xE02A1F14);
        // Window border
        g.fill(winX, winY, winX + winW, winY + 2, BORDER_GOLD);
        g.fill(winX, winY, winX + 2, winY + winH, BORDER_GOLD);
        g.fill(winX + winW - 2, winY, winX + winW, winY + winH, BORDER_GOLD);
        g.fill(winX, winY + winH - 2, winX + winW, winY + winH, BORDER_GOLD);

        int cx = winX + winW / 2;

        // === Title bar ===
        g.fill(winX + 3, winY + 3, winX + winW - 3, winY + 31, PARCH_DARK);
        g.fill(winX + 3, winY + 30, winX + winW - 3, winY + 31, BORDER_GOLD);
        // : use cached title component instead of allocating per frame
        if (cachedTitleComponent == null) {
            cachedTitleComponent = Component.literal(this.title.getString()).withStyle(ChatFormatting.WHITE);
        }
        g.drawCenteredString(this.font, cachedTitleComponent, cx, winY + 10, 0xFFF5DEB3);

        // === Column header ===
        int hdrW = listWidth + 100;
        g.fill(listLeft - 4, listTop - 16, listLeft + hdrW, listTop - 1, 0x30C4A882);
        g.fill(listLeft - 4, listTop - 1, listLeft + hdrW, listTop, BORDER_GOLD);
        g.drawString(this.font, LocalizationManager.translate("gui.header_info"), listLeft, listTop - 13, TEXT_DIM);
        g.drawString(this.font, LocalizationManager.translate("gui.header_action"), listLeft + listWidth - 50, listTop - 13, TEXT_DIM);
        if (!filteredAreas.isEmpty()) {
            String info = (scrollOffset + 1) + "-" + Math.min(scrollOffset + itemsPerPage, filteredAreas.size()) + " / " + filteredAreas.size();
            g.drawString(this.font, info, listLeft + listWidth + 10, listTop - 13, TEXT_DIM);
        }

        // === Area rows ===
        int visible = Math.min(Math.max(0, filteredAreas.size() - scrollOffset), itemsPerPage);
        for (int i = 0; i < visible; i++) {
            int idx = scrollOffset + i;
            S2CAreaListPacket.AreaEntry entry = filteredAreas.get(idx);
            int rowY = listTop + i * itemHeight;
            int rowBg = (i % 2 == 0) ? 0 : ROW_ALT;
            g.fill(listLeft - 2, rowY, listLeft + listWidth + 85, rowY + itemHeight - 1, rowBg);
            g.fill(listLeft - 2, rowY + 2, listLeft - 1, rowY + itemHeight - 3, entry.enabled() ? ACCENT_GREEN : ACCENT_RED);
            boolean hasProt = entry.protBlockBreak() || entry.protBlockPlace() || entry.protBlockInteract()
                || entry.protPvp() || entry.protExplosion() || entry.protEntityDamage();
            if (hasProt) {
                g.fill(listLeft - 2, rowY, listLeft + 6, rowY + 8, entry.enabled() ? ACCENT_GREEN : 0x60808080);
                g.drawString(this.font, "\u26E8", listLeft, rowY, entry.enabled() ? 0x3A6B3A : TEXT_DIM);
            }
        }

        // === Status bar ===
        int statusY = btnY - 30;
        g.fill(winX + 3, statusY - 4, winX + winW - 3, statusY + 16, 0x303A2A1A);
        g.fill(winX + 3, statusY - 4, winX + winW - 3, statusY - 3, BORDER_GOLD);
        // : rebuild status Component only when area count changes
        if (cachedStatusAreaCount != areas.size()) {
            cachedStatusComponent = Component.literal("\u25C6 " + LocalizationManager.translate("gui.status_monitoring") + ": " + areas.size() + " " + LocalizationManager.translate("gui.status_areas")).withStyle(ChatFormatting.GRAY);
            cachedStatusAreaCount = areas.size();
        }
        g.drawCenteredString(this.font, cachedStatusComponent, cx, statusY, TEXT_DIM);

        super.render(g, mx, my, pt);

        // === Confirm dialog (on top of everything) ===
        if (confirmDialog.isVisible()) {
            confirmDialog.render(g, this.width, this.height);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (confirmDialog.isVisible()) return true;
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset--; rebuildRows();
        } else if (delta < 0 && scrollOffset < filteredAreas.size() - itemsPerPage) {
            scrollOffset++; rebuildRows();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (confirmDialog.isVisible()) {
            confirmDialog.mouseClicked(mx, my, button);
            return true;
        }
        return super.mouseClicked(mx, my, button);
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
        private final boolean areaEnabled;
        AreaToggleButton(int x, int y, int w, int h, String name, boolean enabled, AreaManagementScreen parent) {
            super(x, y, w, h,
                Component.literal(enabled ? LocalizationManager.translate("gui.disable") : LocalizationManager.translate("gui.enable")),
                b -> parent.onToggle(name), DEFAULT_NARRATION);
            this.areaEnabled = enabled;
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int c = areaEnabled ? 0x508C3E3E : 0x504B8C3E;
            int bg = isHoveredOrFocused() ? c | 0x20000000 : c;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.fill(getX(), getY(), getX() + width, getY() + 1, 0x608B6914);
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, 0xFFEFEDEB);
        }
    }

    static class AreaDeleteButton extends Button {
        private final String areaName;
        private final ConfirmDialog confirmDialog;
        private final AreaManagementScreen parent;
        AreaDeleteButton(int x, int y, int w, int h, String areaName, AreaManagementScreen parent) {
            super(x, y, w, h,
                Component.literal("\u2715").withStyle(ChatFormatting.GRAY),
                b -> {}, DEFAULT_NARRATION);
            this.areaName = areaName; this.parent = parent; this.confirmDialog = parent.confirmDialog;
        }
        @Override public void onPress() {
            confirmDialog.show(
                LocalizationManager.translate("gui.confirm_delete_title"),
                LocalizationManager.translate("gui.confirm_delete_msg").replace("%s", areaName),
                LocalizationManager.translate("gui.confirm"),
                LocalizationManager.translate("gui.cancel"),
                () -> parent.onDelete(areaName),
                () -> {});
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int bg = isHoveredOrFocused() ? 0x808C3E3E : 0x40604040;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.fill(getX(), getY(), getX() + width, getY() + 1, 0x608B6914);
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, isHoveredOrFocused() ? 0xFFCC4444 : 0xFFAA6666);
        }
    }
}
