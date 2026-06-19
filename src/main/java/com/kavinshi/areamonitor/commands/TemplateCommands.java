package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.TemplateManager;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class TemplateCommands {

    private TemplateCommands() {}

    public static int listTemplates(CommandContext<CommandSourceStack> context) {
        List<TemplateManager.TemplateData> templates = TemplateManager.loadAllTemplates();
        context.getSource().sendSystemMessage(Component.literal("§6=== Available Templates ==="));
        if (templates.isEmpty()) {
            context.getSource().sendSystemMessage(Component.literal("§eNo templates found"));
        } else {
            for (TemplateManager.TemplateData t : templates) {
                context.getSource().sendSystemMessage(
                    Component.literal("§b  " + t.name + " §7- " + t.displayName +
                        " §7(enter: " + t.enterMode + ", leave: " + t.leaveMode + ")"));
            }
        }
        return 1;
    }

    public static int showTemplateInfo(String name, CommandContext<CommandSourceStack> context) {
        TemplateManager.TemplateData template = TemplateManager.loadTemplate(name);
        if (template == null) {
            MessageUtils.sendFailure(context.getSource(), "template.not_found", name);
            return 0;
        }
        context.getSource().sendSystemMessage(Component.literal("§6=== Template: " + template.name + " ==="));
        context.getSource().sendSystemMessage(Component.literal("§eDisplay Name: §f" + template.displayName));
        context.getSource().sendSystemMessage(Component.literal("§eEnter Mode: §b" + template.enterMode));
        context.getSource().sendSystemMessage(Component.literal("§eLeave Mode: §b" + template.leaveMode));
        if (template.protection != null) {
            context.getSource().sendSystemMessage(Component.literal("§eProtection: §f" +
                (template.protection.isAnyProtectionEnabled() ? "§aEnabled" : "§7None")));
        }
        return 1;
    }

    public static int createFromTemplate(String templateName, String areaName, CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }
        TemplateManager.createFromTemplate(templateName, areaName, player);
        return 1;
    }
}
