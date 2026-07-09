package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.ConfigManager;
import com.kavinshi.areamonitor.MonitorArea;
import com.kavinshi.areamonitor.ProtectionSettings;
import com.kavinshi.areamonitor.LocalizationManager;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * Protection command handlers for /areamonitor protect area &lt;type&gt; on|off.
 */
public class ProtectionCommands {

    private ProtectionCommands() {}

    public static int setProtection(String areaName, String type, boolean enable, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName);
        if (area == null) return 0;
        ProtectionSettings protection = area.getProtection();
        switch (type) {
            case "blockBreak" -> protection.setBlockBreak(enable);
            case "blockPlace" -> protection.setBlockPlace(enable);
            case "blockInteract" -> protection.setBlockInteract(enable);
            case "pvp" -> protection.setPvp(enable);
            case "explosion" -> protection.setExplosion(enable);
            case "entityDamage" -> protection.setEntityDamage(enable);
            case "containerInteract" -> protection.setContainerInteract(enable);
            case "fluidPlace" -> protection.setFluidPlace(enable);
            case "itemDrop" -> protection.setItemDrop(enable);
            default -> {
                MessageUtils.sendFailure(context.getSource(), "protection.invalid_type", type);
                return 0;
            }
        }
        ConfigManager.safeSaveConfig();
        String typeDisplay = LocalizationManager.translate("protection." + type);
        MessageUtils.sendSuccess(context.getSource(),
            enable ? "protection.enabled" : "protection.disabled", true, typeDisplay, areaName);
        return 1;
    }

    public static int setAllProtection(String areaName, boolean enable, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName);
        if (area == null) return 0;
        ProtectionSettings protection = area.getProtection();
        protection.setBlockBreak(enable);
        protection.setBlockPlace(enable);
        protection.setBlockInteract(enable);
        protection.setPvp(enable);
        protection.setExplosion(enable);
        protection.setEntityDamage(enable);
        protection.setContainerInteract(enable);
        protection.setFluidPlace(enable);
        protection.setItemDrop(enable);
        ConfigManager.safeSaveConfig();
        MessageUtils.sendSuccess(context.getSource(),
            enable ? "protection.all_enabled" : "protection.all_disabled", true, areaName);
        return 1;
    }

    public static int showProtectionInfo(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName);
        if (area == null) return 0;
        ProtectionSettings protection = area.getProtection();
        context.getSource().sendSuccess(
            () -> MessageUtils.smartComponent(context.getSource(), "protection.info_header", areaName), false);
        sendProtectionLine(context.getSource(), "protection.blockBreak", protection.isBlockBreak());
        sendProtectionLine(context.getSource(), "protection.blockPlace", protection.isBlockPlace());
        sendProtectionLine(context.getSource(), "protection.blockInteract", protection.isBlockInteract());
        sendProtectionLine(context.getSource(), "protection.pvp", protection.isPvp());
        sendProtectionLine(context.getSource(), "protection.explosion", protection.isExplosion());
        sendProtectionLine(context.getSource(), "protection.entityDamage", protection.isEntityDamage());
        sendProtectionLine(context.getSource(), "protection.containerInteract", protection.isContainerInteract());
        sendProtectionLine(context.getSource(), "protection.fluidPlace", protection.isFluidPlace());
        sendProtectionLine(context.getSource(), "protection.itemDrop", protection.isItemDrop());
        return 1;
    }

    private static void sendProtectionLine(CommandSourceStack source, String key, boolean enabled) {
        source.sendSuccess(
            () -> MessageUtils.smartComponent(source, key)
                .append(Component.literal(enabled ? " §a\u2713" : " §7\u2717")),
            false);
    }
}
