package com.kavinshi.areamonitor.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Button styled with Glass Morphism theme.
 * Renders a semi-transparent dark background with subtle borders.
 */
public class GlassButton extends Button {

    private static final int BG_DEFAULT  = 0x40000000;
    private static final int BG_HOVER    = 0x60000000;
    private static final int BORDER_TOP  = 0x40FFFFFF;
    private static final int BORDER_BOT  = 0x10FFFFFF;

    public GlassButton(int x, int y, int w, int h, Component message, OnPress onPress) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
    }

    public static GlassButton create(int x, int y, int w, int h, String text, OnPress onPress) {
        return new GlassButton(x, y, w, h, Component.literal(text), onPress);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        // Glass background
        int bg = isHoveredOrFocused() ? BG_HOVER : BG_DEFAULT;
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        g.fill(getX(), getY(), getX() + width, getY() + 1, BORDER_TOP);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, BORDER_BOT);

        // Draw text centered
        int textColor = isHoveredOrFocused() ? 0xFFFFFF : 0xCCCCCC;
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
            getX() + width / 2, getY() + (height - 8) / 2, textColor);
    }
}
