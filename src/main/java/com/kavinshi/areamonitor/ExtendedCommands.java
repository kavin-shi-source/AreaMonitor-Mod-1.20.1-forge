package com.kavinshi.areamonitor;

import com.kavinshi.areamonitor.commands.AreaCommands;
import com.kavinshi.areamonitor.commands.BlacklistCommands;
import com.kavinshi.areamonitor.commands.SelectionCommands;
import com.kavinshi.areamonitor.commands.VisualCommands;
import com.kavinshi.areamonitor.commands.WhitelistCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Central command dispatcher for the /areamonitor command tree.
 * Delegates sub-command registration to specialized command classes.
 *
 * <p>Command groups:
 * <ul>
 *   <li>{@link WhitelistCommands} — toggle, whitelist, help, language</li>
 *   <li>{@link AreaCommands} — area create/delete/list/toggle/info/mode</li>
 *   <li>{@link BlacklistCommands} — blacklist add/remove/list/toggle/reload</li>
 *   <li>{@link VisualCommands} — visual tool/show/hide, performance</li>
 *   <li>{@link SelectionCommands} — selection create/cancel/info/tutorial, config</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExtendedCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("areamonitor")
            .requires(source -> source.hasPermission(2))

            // --- Basic commands (from WhitelistCommands) ---
            .then(Commands.literal("toggle")
                .executes(WhitelistCommands::toggleMonitor)
            )

            .then(Commands.literal("whitelist")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .executes(context -> WhitelistCommands.addToWhitelist(
                            StringArgumentType.getString(context, "player"),
                            context
                        )))
                )
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .executes(context -> WhitelistCommands.removeFromWhitelist(
                            StringArgumentType.getString(context, "player"),
                            context
                        )))
                )
                .then(Commands.literal("list")
                    .executes(WhitelistCommands::listWhitelist)
                )
                .then(Commands.literal("clear")
                    .executes(WhitelistCommands::clearWhitelist)
                )
            )

            .then(Commands.literal("help")
                .executes(WhitelistCommands::showHelp)
            )

            .then(Commands.literal("language")
                .then(Commands.literal("en")
                    .executes(WhitelistCommands::setLanguageEnglish)
                )
                .then(Commands.literal("zh")
                    .executes(WhitelistCommands::setLanguageChinese)
                )
                .executes(WhitelistCommands::showLanguageStatus)
            )

            // --- Area commands ---
            .then(Commands.literal("area")
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> AreaCommands.createArea(
                            StringArgumentType.getString(context, "name"),
                            context
                        )))
                )
                .then(Commands.literal("delete")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(AreaCommands::suggestAreaNames)
                        .executes(context -> AreaCommands.deleteArea(
                            StringArgumentType.getString(context, "name"),
                            context
                        )))
                )
                .then(Commands.literal("list")
                    .executes(AreaCommands::listAreas)
                )
                .then(Commands.literal("toggle")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(AreaCommands::suggestAreaNames)
                        .executes(context -> AreaCommands.toggleArea(
                            StringArgumentType.getString(context, "name"),
                            context
                        )))
                )
                .then(Commands.literal("info")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(AreaCommands::suggestAreaNames)
                        .executes(context -> AreaCommands.showAreaInfo(
                            StringArgumentType.getString(context, "name"),
                            context
                        )))
                )
                .then(Commands.literal("setEnterMode")
                    .then(Commands.argument("areaName", StringArgumentType.string())
                        .suggests(AreaCommands::suggestAreaNames)
                        .then(Commands.argument("mode", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                for (String mode : AreaCommands.GAME_MODES) {
                                    if (mode.startsWith(builder.getRemaining().toLowerCase())) {
                                        builder.suggest(mode);
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> AreaCommands.setAreaEnterMode(
                                StringArgumentType.getString(context, "areaName"),
                                StringArgumentType.getString(context, "mode"),
                                context
                            ))))
                )
                .then(Commands.literal("setLeaveMode")
                    .then(Commands.argument("areaName", StringArgumentType.string())
                        .suggests(AreaCommands::suggestAreaNames)
                        .then(Commands.argument("mode", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                for (String mode : AreaCommands.GAME_MODES) {
                                    if (mode.startsWith(builder.getRemaining().toLowerCase())) {
                                        builder.suggest(mode);
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> AreaCommands.setAreaLeaveMode(
                                StringArgumentType.getString(context, "areaName"),
                                StringArgumentType.getString(context, "mode"),
                                context
                            ))))
                )
            )

            // --- Visual commands ---
            .then(Commands.literal("visual")
                .then(Commands.literal("tool")
                    .executes(VisualCommands::giveVisualTool)
                )
                .then(Commands.literal("show")
                    .then(Commands.argument("area", StringArgumentType.string())
                        .suggests(AreaCommands::suggestAreaNames)
                        .executes(context -> VisualCommands.showAreaVisual(
                            StringArgumentType.getString(context, "area"),
                            context
                        )))
                )
                .then(Commands.literal("hide")
                    .executes(VisualCommands::hideAreaVisual)
                )
            )

            .then(Commands.literal("performance")
                .executes(VisualCommands::showPerformance)
            )

            // --- Blacklist commands ---
            .then(Commands.literal("blacklist")
                .then(Commands.literal("info")
                    .executes(BlacklistCommands::showBlacklistInfo)
                )
                .then(Commands.literal("area")
                    .then(Commands.argument("areaName", StringArgumentType.string())
                        .suggests(AreaCommands::suggestAreaNames)
                        .then(Commands.literal("add")
                            .then(Commands.argument("item", StringArgumentType.greedyString())
                                .suggests(BlacklistCommands::suggestItems)
                                .executes(context -> BlacklistCommands.addItemToAreaBlacklist(
                                    StringArgumentType.getString(context, "areaName"),
                                    StringArgumentType.getString(context, "item"),
                                    context
                                ))
                            )
                        )
                        .then(Commands.literal("remove")
                            .then(Commands.argument("item", StringArgumentType.greedyString())
                                .suggests(BlacklistCommands::suggestItems)
                                .executes(context -> BlacklistCommands.removeItemFromAreaBlacklist(
                                    StringArgumentType.getString(context, "areaName"),
                                    StringArgumentType.getString(context, "item"),
                                    context
                                ))
                            )
                        )
                        .then(Commands.literal("list")
                            .executes(context -> BlacklistCommands.listAreaBlacklist(
                                StringArgumentType.getString(context, "areaName"),
                                context
                            ))
                        )
                        .then(Commands.literal("toggle")
                            .executes(context -> BlacklistCommands.toggleAreaBlacklist(
                                StringArgumentType.getString(context, "areaName"),
                                context
                            ))
                        )
                    )
                )
                .then(Commands.literal("reload")
                    .executes(BlacklistCommands::reloadBlacklistConfig)
                )
            )

            // --- Selection commands ---
            .then(Commands.literal("selection")
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> SelectionCommands.createAreaFromSelection(
                            StringArgumentType.getString(context, "name"),
                            context
                        )))
                )
                .then(Commands.literal("cancel")
                    .executes(SelectionCommands::cancelSelection)
                )
                .then(Commands.literal("info")
                    .executes(SelectionCommands::showSelectionInfo)
                )
                .then(Commands.literal("tutorial")
                    .executes(SelectionCommands::showTutorial)
                )
            )

            // --- Config commands ---
            .then(Commands.literal("config")
                .then(Commands.literal("reload")
                    .executes(SelectionCommands::reloadConfigs)
                )
                .then(Commands.literal("generate")
                    .executes(SelectionCommands::generateConfigs)
                )
            )
        );
    }
}
