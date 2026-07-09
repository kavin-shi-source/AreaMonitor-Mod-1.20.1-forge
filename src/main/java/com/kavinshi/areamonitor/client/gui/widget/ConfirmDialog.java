package com.kavinshi.areamonitor.client.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A reusable confirmation dialog overlay rendered within existing screens.
 * Matches the Warm Parchment / Glass Morphism theme.
 */
@OnlyIn(Dist.CLIENT)
public class ConfirmDialog {

    // Parchment palette
    private static final int PARCH_DARK   = 0xD03A2A1A;
    private static final int PARCH_PANEL  = 0xF0C4A882;
    private static final int BORDER_GOLD  = 0xC08B6914;
    private static final int OVERLAY_BG   = 0x80000000;
    private static final int TEXT_COLOR    = 0xFFEFEDEB;
    private static final int BTN_CONFIRM  = 0x808C3E3E; // muted red
    private static final int BTN_CANCEL   = 0x60B89B6A; // warm amber

    private boolean visible = false;
    private String title = "";
    private String message = "";
    // : defaults are only used if show() is called without explicit text — keep as empty
    // strings so a forgotten arg shows blank rather than English fallback text on a non-en client.
    private String confirmText = "";
    private String cancelText = "";
    private Runnable onConfirm;
    private Runnable onCancel;

    // Cached layout
    private int dialogX, dialogY, dialogW, dialogH;
    private int confirmX, confirmY, confirmW, confirmH;
    private int cancelX, cancelY, cancelW, cancelH;

    public boolean isVisible() {
        return visible;
    }

    public void show(String title, String message, String confirmText, String cancelText, Runnable onConfirm, Runnable onCancel) {
        this.title = title;
        this.message = message;
        this.confirmText = confirmText;
        this.cancelText = cancelText;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.visible = true;
    }

    public void hide() {
        this.visible = false;
        this.onConfirm = null;
        this.onCancel = null;
    }

    /**
     * Render the confirmation dialog overlay.
     * Call this AFTER drawing the main screen content (super.render).
     */
    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        if (!visible) return;

        Font font = Minecraft.getInstance().font;

        // Dialog dimensions
        dialogW = Math.max(200, font.width(message) + 48);
        dialogH = 90;
        dialogX = (screenWidth - dialogW) / 2;
        dialogY = (screenHeight - dialogH) / 2;

        // Button layout
        int btnH = 18;
        int btnW1 = Math.max(60, font.width(confirmText) + 16);
        int btnW2 = Math.max(60, font.width(cancelText) + 16);
        int btnY = dialogY + dialogH - btnH - 10;
        confirmX = dialogX + dialogW - btnW1 - 12;
        confirmY = btnY;
        confirmW = btnW1;
        confirmH = btnH;
        cancelX = dialogX + 12;
        cancelY = btnY;
        cancelW = btnW2;
        cancelH = btnH;

        // Screen dim overlay
        g.fill(0, 0, screenWidth, screenHeight, OVERLAY_BG);

        // Dialog background
        g.fill(dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, PARCH_PANEL);
        // Inner dark fill
        g.fill(dialogX + 1, dialogY + 1, dialogX + dialogW - 1, dialogY + dialogH - 1, 0xE02A1F14);
        // Gold border
        g.fill(dialogX, dialogY, dialogX + dialogW, dialogY + 2, BORDER_GOLD);
        g.fill(dialogX, dialogY, dialogX + 2, dialogY + dialogH, BORDER_GOLD);
        g.fill(dialogX + dialogW - 2, dialogY, dialogX + dialogW, dialogY + dialogH, BORDER_GOLD);
        g.fill(dialogX, dialogY + dialogH - 2, dialogX + dialogW, dialogY + dialogH, BORDER_GOLD);

        // Title bar
        g.fill(dialogX + 3, dialogY + 3, dialogX + dialogW - 3, dialogY + 27, PARCH_DARK);
        g.fill(dialogX + 3, dialogY + 26, dialogX + dialogW - 3, dialogY + 27, BORDER_GOLD);
        g.drawCenteredString(font, title, dialogX + dialogW / 2, dialogY + 9, TEXT_COLOR);

        // Message
        g.drawCenteredString(font, message, dialogX + dialogW / 2, dialogY + 40, 0xFFD4B896);

        // Confirm button (red/danger style)
        g.fill(confirmX, confirmY, confirmX + confirmW, confirmY + confirmH, BTN_CONFIRM);
        g.fill(confirmX, confirmY, confirmX + confirmW, confirmY + 1, BORDER_GOLD);
        g.drawCenteredString(font, confirmText, confirmX + confirmW / 2, confirmY + (confirmH - 8) / 2, TEXT_COLOR);

        // Cancel button (amber style)
        g.fill(cancelX, cancelY, cancelX + cancelW, cancelY + cancelH, BTN_CANCEL);
        g.fill(cancelX, cancelY, cancelX + cancelW, cancelY + 1, BORDER_GOLD);
        g.drawCenteredString(font, cancelText, cancelX + cancelW / 2, cancelY + (cancelH - 8) / 2, TEXT_COLOR);
    }

    /**
     * Handle mouse click. Returns true if the click was consumed by the dialog.
     */
    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) return false;

        // Capture callbacks before hide() clears them.
        Runnable confirmAction = onConfirm;
        Runnable cancelAction = onCancel;

        // Click outside dialog = cancel
        if (mx < dialogX || mx > dialogX + dialogW || my < dialogY || my > dialogY + dialogH) {
            hide();
            if (cancelAction != null) cancelAction.run();
            return true;
        }

        // Confirm button
        if (mx >= confirmX && mx <= confirmX + confirmW && my >= confirmY && my <= confirmY + confirmH) {
            hide();
            if (confirmAction != null) confirmAction.run();
            return true;
        }

        // Cancel button
        if (mx >= cancelX && mx <= cancelX + cancelW && my >= cancelY && my <= cancelY + cancelH) {
            hide();
            if (cancelAction != null) cancelAction.run();
            return true;
        }

        return true; // click inside dialog but not on buttons — consume it
    }

    /**
     * Handle keyboard input when the dialog is visible.
     * Esc -> cancel; Enter -> confirm; any other key is consumed to prevent
     * the parent screen from acting on it (e.g. closing on Esc).
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;

        Runnable confirmAction = onConfirm;
        Runnable cancelAction = onCancel;

        if (keyCode == InputConstants.KEY_ESCAPE) {
            hide();
            if (cancelAction != null) cancelAction.run();
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            hide();
            if (confirmAction != null) confirmAction.run();
            return true;
        }
        // Consume all other keys so the parent panel does not react
        // (e.g. parent's Esc handler, EditBox input, hotkeys).
        return true;
    }
}
