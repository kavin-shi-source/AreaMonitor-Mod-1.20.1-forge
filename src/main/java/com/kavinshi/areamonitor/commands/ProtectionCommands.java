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
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "error.area_not_found", areaName);
            return 0;
        }
        ProtectionSettings p = area.getProtection();
        switch (type) {
            case "blockBreak" -> p.setBlockBreak(enable);
            case "blockPlace" -> p.setBlockPlace(enable);
            case "blockInteract" -> p.setBlockInteract(enable);
            case "pvp" -> p.setPvp(enable);
            case "explosion" -> p.setExplosion(enable);
            case "entityDamage" -> p.setEntityDamage(enable);
            case "containerInteract" -> p.setContainerInteract(enable);
            case "fluidPlace" -> p.setFluidPlace(enable);
            case "itemDrop" -> p.setItemDrop(enable);
            default -> {
                MessageUtils.sendFailure(context.getSource(), "protection.invalid_type", type);
                return 0;
            }
        }
        ConfigManager.saveAreasConfig();
        String typeDisplay = LocalizationManager.translate("protection." + type);
        MessageUtils.sendSuccess(context.getSource(),
            enable ? "protection.enabled" : "protection.disabled", true, typeDisplay, areaName);
        return 1;
    }

    public static int setAllProtection(String areaName, boolean enable, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "error.area_not_found", areaName);
            return 0;
        }
        ProtectionSettings p = area.getProtection();
        p.setBlockBreak(enable);
        p.setBlockPlace(enable);
        p.setBlockInteract(enable);
        p.setPvp(enable);
        p.setExplosion(enable);
        p.setEntityDamage(enable);
        p.setContainerInteract(enable);
        p.setFluidPlace(enable);
        p.setItemDrop(enable);
        ConfigManager.saveAreasConfig();
        MessageUtils.sendSuccess(context.getSource(),
            enable ? "protection.all_enabled" : "protection.all_disabled", true, areaName);
        return 1;
    }

    public static int showProtectionInfo(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "error.area_not_found", areaName);
            return 0;
        }
        ProtectionSettings p = area.getProtection();
        context.getSource().sendSystemMessage(
            Component.literal(String.format(
                LocalizationManager.translate("protection.info_header"), areaName)));
        context.getSource().sendSystemMessage(
            Component.literal(formatLine(LocalizationManager.translate("protection.blockBreak"), p.isBlockBreak())));
        context.getSource().sendSystemMessage(
            Component.literal(formatLine(LocalizationManager.translate("protection.blockPlace"), p.isBlockPlace())));
        context.getSource().sendSystemMessage(
            Component.literal(formatLine(LocalizationManager.translate("protection.blockInteract"), p.isBlockInteract())));
        context.getSource().sendSystemMessage(
            Component.literal(formatLine(LocalizationManager.translate("protection.pvp"), p.isPvp())));
        context.getSource().sendSystemMessage(
            Component.literal(formatLine(LocalizationManager.translate("protection.explosion"), p.isExplosion())));
        context.getSource().sendSystemMessage(
            Component.literal(formatLine(LocalizationManager.translate("protection.entityDamage"), p.isEntityDamage())));
        context.getSource().sendSystemMessage(
            Component.literal(formatLine(LocalizationManager.translate("protection.containerInteract"), p.isContainerInteract())));
        context.getSource().sendSystemMessage(
            Component.literal(formatLine(LocalizationManager.translate("protection.fluidPlace"), p.isFluidPlace())));
        context.getSource().sendSystemMessage(
            Component.literal(formatLine(LocalizationManager.translate("protection.itemDrop"), p.isItemDrop())));
        return 1;
    }

    private static String formatLine(String label, boolean enabled) {
        return (enabled ? "§a\u2713 " : "§7\u2717 ") + label;
    }
}
