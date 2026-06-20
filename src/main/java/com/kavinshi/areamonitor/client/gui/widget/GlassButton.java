package com.kavinshi.areamonitor.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Button styled with Warm Parchment theme.
 * Renders a semi-transparent amber/parchment background with gold-brown borders.
 */
public class GlassButton extends Button {

    // Parchment palette
    private static final int BG_DEFAULT  = 0x60B89B6A; // warm amber
    private static final int BG_HOVER    = 0x80B89B6A; // darker amber
    private static final int BORDER_TOP  = 0x608B6914; // gold-brown edge
    private static final int BORDER_BOT  = 0x305C4033; // subtle brown shadow

    public GlassButton(int x, int y, int w, int h, Component message, OnPress onPress) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
    }

    public static GlassButton create(int x, int y, int w, int h, String text, OnPress onPress) {
        return new GlassButton(x, y, w, h, Component.literal(text), onPress);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        int bg = isHoveredOrFocused() ? BG_HOVER : BG_DEFAULT;
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        g.fill(getX(), getY(), getX() + width, getY() + 1, BORDER_TOP);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, BORDER_BOT);

        int textColor = isHoveredOrFocused() ? 0xFF2A1A0A : 0xFF5C4033;
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
            getX() + width / 2, getY() + (height - 8) / 2, textColor);
    }
}
