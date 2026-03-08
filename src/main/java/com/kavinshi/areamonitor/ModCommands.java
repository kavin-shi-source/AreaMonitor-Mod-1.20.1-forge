package com.kavinshi.areamonitor;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {



    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("areamonitor")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("toggle")
                        .executes(ModCommands::toggleMonitor)
                )


                .then(Commands.literal("whitelist")
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .executes(context -> addToWhitelist(
                                                StringArgumentType.getString(context, "player"),
                                                context
                                        )))
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .executes(context -> removeFromWhitelist(
                                                StringArgumentType.getString(context, "player"),
                                                context
                                        )))
                        )
                        .then(Commands.literal("list")
                                .executes(ModCommands::listWhitelist)
                        )
                        .then(Commands.literal("clear")
                                .executes(ModCommands::clearWhitelist)
                        )
                )

                .then(Commands.literal("help")
                        .executes(ModCommands::showHelp)
                )

                .then(Commands.literal("language")
                        .then(Commands.literal("en")
                                .executes(ModCommands::setLanguageEnglish)
                        )
                        .then(Commands.literal("zh")
                                .executes(ModCommands::setLanguageChinese)
                        )
                        .executes(ModCommands::showLanguageStatus)
                )
        );
    }

    private static int toggleMonitor(CommandContext<CommandSourceStack> context) {
        boolean current = ConfigManager.CONFIG.isEnabled.get();
        boolean newState = !current;

        ConfigManager.CONFIG.isEnabled.set(newState);
        ConfigManager.CONFIG.isEnabled.save();

        String messageKey = newState ?
                "command.areamonitor.toggle.enabled" :
                "command.areamonitor.toggle.disabled";

        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate(messageKey)),
                true
        );

        return 1;
    }


    private static int addToWhitelist(String player, CommandContext<CommandSourceStack> context) {
        if (WhitelistManager.addToWhitelist(player)) {
            context.getSource().sendSuccess(
                    () -> Component.literal(LocalizationManager.translate("command.areamonitor.whitelist.add.success", player)),
                    true
            );
        } else {
            context.getSource().sendSuccess(
                    () -> Component.literal(LocalizationManager.translate("command.areamonitor.whitelist.add.exists", player)),
                    true
            );
        }
        return 1;
    }

    private static int removeFromWhitelist(String player, CommandContext<CommandSourceStack> context) {
        if (WhitelistManager.removeFromWhitelist(player)) {
            context.getSource().sendSuccess(
                    () -> Component.literal(LocalizationManager.translate("command.areamonitor.whitelist.remove.success", player)),
                    true
            );
        } else {
            context.getSource().sendSuccess(
                    () -> Component.literal(LocalizationManager.translate("command.areamonitor.whitelist.remove.not_found", player)),
                    true
            );
        }
        return 1;
    }

    private static int listWhitelist(CommandContext<CommandSourceStack> context) {
        Set<String> whitelist = WhitelistManager.getWhitelist();

        if (whitelist.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.literal(LocalizationManager.translate("command.areamonitor.whitelist.list.empty")),
                    false
            );
        } else {
            context.getSource().sendSuccess(
                    () -> Component.literal(LocalizationManager.translate("command.areamonitor.whitelist.list.header")),
                    false
            );

            List<String> whitelistArray = new ArrayList<>(whitelist);

            for (int i = 0; i < whitelistArray.size(); i++) {
                final int index = i + 1;
                final String player = whitelistArray.get(i);

                context.getSource().sendSuccess(
                        () -> Component.literal(String.format("§e%d. §f%s", index, player)),
                        false
                );
            }

            context.getSource().sendSuccess(
                    () -> Component.literal(LocalizationManager.translate("command.areamonitor.whitelist.list.count", whitelist.size())),
                    false
            );
        }
        return 1;
    }

    private static int clearWhitelist(CommandContext<CommandSourceStack> context) {
        WhitelistManager.clearWhitelist();
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.whitelist.clear.success")),
                true
        );
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.header")), false
        );

        // 基础命令
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.section.basic")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.toggle")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.area.list")), false
        );

        // 区域管理命令
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.section.area")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.area.create")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.area.delete")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.area.list")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.area.toggle")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.area.info")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.area.set_enter_mode")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.area.set_leave_mode")), false
        );

        // 可视化工具命令
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.section.visual")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.visual.tool")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.visual.show")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.visual.hide")), false
        );

        // 选择工具命令
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.section.selection")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.selection.create")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.selection.cancel")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.selection.info")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.selection.tutorial")), false
        );

        // 白名单命令
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.section.whitelist")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.whitelist.add")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.whitelist.remove")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.whitelist.list")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.whitelist.clear")), false
        );

        // 其他命令
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.section.other")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.performance")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.blacklist.info")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.blacklist.add")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.blacklist.remove")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.blacklist.list")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.blacklist.toggle")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.blacklist.reload")), false
        );

        // 配置管理命令
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.section.config")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.config.reload")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.config.generate")), false
        );

        // 语言设置命令
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.section.language")), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.language.show")),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.language.english")),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.language.chinese")),
                false
        );

        return 1;
    }

    private static int setLanguageEnglish(CommandContext<CommandSourceStack> context) {
        if (LocalizationManager.setLanguage(LocalizationManager.LANGUAGE_ENGLISH)) {
            context.getSource().sendSuccess(
                    () -> Component.literal("§aLanguage switched to English"),
                    false
            );
        } else {
            context.getSource().sendSuccess(
                    () -> Component.literal("§cFailed to switch language"),
                    false
            );
        }
        return 1;
    }

    private static int setLanguageChinese(CommandContext<CommandSourceStack> context) {
        if (LocalizationManager.setLanguage(LocalizationManager.LANGUAGE_CHINESE)) {
            context.getSource().sendSuccess(
                    () -> Component.literal(LocalizationManager.translate("command.areamonitor.language.chinese.success")),
                    false
            );
        } else {
            context.getSource().sendSuccess(
                    () -> Component.literal(LocalizationManager.translate("command.areamonitor.language.failed")),
                    false
            );
        }
        return 1;
    }

    private static int showLanguageStatus(CommandContext<CommandSourceStack> context) {
        String currentLang = LocalizationManager.getCurrentLanguage();
        String displayName = LocalizationManager.getLanguageDisplayName(currentLang);

        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.language.current", displayName)),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.language.usage")),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.language.english")),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(LocalizationManager.translate("command.areamonitor.help.language.chinese")),
                false
        );

        return 1;
    }
}