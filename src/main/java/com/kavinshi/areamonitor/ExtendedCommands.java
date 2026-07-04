package com.kavinshi.areamonitor;

import com.kavinshi.areamonitor.commands.AreaCommands;
import com.kavinshi.areamonitor.commands.BlacklistCommands;
import com.kavinshi.areamonitor.commands.SelectionCommands;
import com.kavinshi.areamonitor.commands.VisualCommands;
import com.kavinshi.areamonitor.commands.ProtectionCommands;
import com.kavinshi.areamonitor.commands.TemplateCommands;
import com.kavinshi.areamonitor.commands.TriggerCommands;
import com.kavinshi.areamonitor.commands.WhitelistCommands;
import com.kavinshi.areamonitor.network.ModNetwork;
import com.kavinshi.areamonitor.network.S2COpenManagementScreenPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Central command dispatcher for the /areamonitor command tree.
 * Delegates sub-command registration to specialized command classes.
 *
 * <p>Command groups:
 * <ul>
 *   <li>{@link WhitelistCommands} — toggle, whitelist, help</li>
 *   <li>{@link AreaCommands} — area create/delete/list/toggle/info/mode</li>
 *   <li>{@link BlacklistCommands} — blacklist add/remove/list/toggle/reload</li>
 *   <li>{@link VisualCommands} — visual tool/show/hide, performance</li>
 *   <li>{@link SelectionCommands} — selection create/cancel/info/tutorial, config</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExtendedCommands {

    private static final java.util.List<String> PROTECTION_TYPES = java.util.List.of(
        "blockBreak", "blockPlace", "blockInteract", "pvp", "explosion", "entityDamage",
        "containerInteract", "fluidPlace", "itemDrop"
    );

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
                    .then(Commands.literal("export")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .suggests(AreaCommands::suggestAreaNames)
                            .executes(context -> AreaCommands.exportArea(
                                StringArgumentType.getString(context, "name"),
                                context
                            ))))
                    .then(Commands.literal("import")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("json", StringArgumentType.greedyString())
                                .executes(context -> AreaCommands.importArea(
                                    StringArgumentType.getString(context, "name"),
                                    StringArgumentType.getString(context, "json"),
                                    context
                                )))))
                    .then(Commands.literal("clone")
                        .then(Commands.argument("source", StringArgumentType.string())
                            .suggests(AreaCommands::suggestAreaNames)
                            .then(Commands.argument("target", StringArgumentType.string())
                                .executes(context -> AreaCommands.cloneArea(
                                    StringArgumentType.getString(context, "source"),
                                    StringArgumentType.getString(context, "target"),
                                    context
                                )))))
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
                .then(Commands.literal("polygon")
                    .then(Commands.literal("start")
                        .executes(SelectionCommands::startPolygonSelection))
                    .then(Commands.literal("finish")
                        .executes(SelectionCommands::finishPolygonSelection))
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

            // --- Protect commands ---
            .then(Commands.literal("protect")
                .then(Commands.argument("area", StringArgumentType.string())
                    .suggests(AreaCommands::suggestAreaNames)
                    .then(Commands.literal("all")
                        .then(Commands.literal("on")
                            .executes(context -> ProtectionCommands.setAllProtection(
                                StringArgumentType.getString(context, "area"), true, context)))
                        .then(Commands.literal("off")
                            .executes(context -> ProtectionCommands.setAllProtection(
                                StringArgumentType.getString(context, "area"), false, context))))
                    .then(Commands.literal("info")
                        .executes(context -> ProtectionCommands.showProtectionInfo(
                            StringArgumentType.getString(context, "area"), context)))
                    .then(Commands.argument("type", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String t : PROTECTION_TYPES) {
                                builder.suggest(t);
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.literal("on")
                            .executes(context -> ProtectionCommands.setProtection(
                                StringArgumentType.getString(context, "area"),
                                StringArgumentType.getString(context, "type"),
                                true, context)))
                        .then(Commands.literal("off")
                            .executes(context -> ProtectionCommands.setProtection(
                                StringArgumentType.getString(context, "area"),
                                StringArgumentType.getString(context, "type"),
                                false, context)))))
            )

            // --- Trigger commands ---
            .then(Commands.literal("trigger")
                .then(Commands.argument("area", StringArgumentType.string())
                    .suggests(AreaCommands::suggestAreaNames)
                    .then(Commands.literal("enter")
                        .then(Commands.literal("cmd")
                            .then(Commands.literal("add")
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                    .executes(context -> TriggerCommands.addCommand(
                                        StringArgumentType.getString(context, "area"), true,
                                        StringArgumentType.getString(context, "command"), context))))
                            .then(Commands.literal("remove")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                    .executes(context -> TriggerCommands.removeCommand(
                                        StringArgumentType.getString(context, "area"), true,
                                        IntegerArgumentType.getInteger(context, "index"), context))))
                            .then(Commands.literal("list")
                                .executes(context -> TriggerCommands.listCommands(
                                    StringArgumentType.getString(context, "area"), true, context)))
                            .then(Commands.literal("clear")
                                .executes(context -> TriggerCommands.clearCommands(
                                    StringArgumentType.getString(context, "area"), true, context))))
                        .then(Commands.literal("sound")
                            .then(Commands.argument("soundId", StringArgumentType.string())
                                .executes(context -> TriggerCommands.setSound(
                                    StringArgumentType.getString(context, "area"), true,
                                    StringArgumentType.getString(context, "soundId"), 1.0f, 1.0f, context)))
                            .then(Commands.literal("clear")
                                .executes(context -> TriggerCommands.clearSound(
                                    StringArgumentType.getString(context, "area"), true, context))))
                        .then(Commands.literal("title")
                            .then(Commands.argument("main", StringArgumentType.greedyString())
                                .executes(context -> TriggerCommands.setTitle(
                                    StringArgumentType.getString(context, "area"), true,
                                    StringArgumentType.getString(context, "main"), null, context)))
                            .then(Commands.literal("clear")
                                .executes(context -> TriggerCommands.clearTitle(
                                    StringArgumentType.getString(context, "area"), true, context))))
                        .then(Commands.literal("tp")
                            .then(Commands.argument("dim", ResourceLocationArgument.id())
                                .suggests((context, builder) -> {
                                    builder.suggest("minecraft:overworld");
                                    builder.suggest("minecraft:the_nether");
                                    builder.suggest("minecraft:the_end");
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                    .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                            .executes(context -> TriggerCommands.setTp(
                                                StringArgumentType.getString(context, "area"), true,
                                                ResourceLocationArgument.getId(context, "dim").toString(),
                                                DoubleArgumentType.getDouble(context, "x"),
                                                DoubleArgumentType.getDouble(context, "y"),
                                                DoubleArgumentType.getDouble(context, "z"), context))))))
                            .then(Commands.literal("clear")
                                .executes(context -> TriggerCommands.clearTp(
                                    StringArgumentType.getString(context, "area"), true, context))))
                        .then(Commands.literal("info")
                            .executes(context -> TriggerCommands.showInfo(
                                StringArgumentType.getString(context, "area"), true, context))))
                    .then(Commands.literal("leave")
                        .then(Commands.literal("cmd")
                            .then(Commands.literal("add")
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                    .executes(context -> TriggerCommands.addCommand(
                                        StringArgumentType.getString(context, "area"), false,
                                        StringArgumentType.getString(context, "command"), context))))
                            .then(Commands.literal("remove")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                    .executes(context -> TriggerCommands.removeCommand(
                                        StringArgumentType.getString(context, "area"), false,
                                        IntegerArgumentType.getInteger(context, "index"), context))))
                            .then(Commands.literal("list")
                                .executes(context -> TriggerCommands.listCommands(
                                    StringArgumentType.getString(context, "area"), false, context)))
                            .then(Commands.literal("clear")
                                .executes(context -> TriggerCommands.clearCommands(
                                    StringArgumentType.getString(context, "area"), false, context))))
                        .then(Commands.literal("sound")
                            .then(Commands.argument("soundId", StringArgumentType.string())
                                .executes(context -> TriggerCommands.setSound(
                                    StringArgumentType.getString(context, "area"), false,
                                    StringArgumentType.getString(context, "soundId"), 1.0f, 1.0f, context)))
                            .then(Commands.literal("clear")
                                .executes(context -> TriggerCommands.clearSound(
                                    StringArgumentType.getString(context, "area"), false, context))))
                        .then(Commands.literal("title")
                            .then(Commands.argument("main", StringArgumentType.greedyString())
                                .executes(context -> TriggerCommands.setTitle(
                                    StringArgumentType.getString(context, "area"), false,
                                    StringArgumentType.getString(context, "main"), null, context)))
                            .then(Commands.literal("clear")
                                .executes(context -> TriggerCommands.clearTitle(
                                    StringArgumentType.getString(context, "area"), false, context))))
                        .then(Commands.literal("tp")
                            .then(Commands.argument("dim", ResourceLocationArgument.id())
                                .suggests((context, builder) -> {
                                    builder.suggest("minecraft:overworld");
                                    builder.suggest("minecraft:the_nether");
                                    builder.suggest("minecraft:the_end");
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                    .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                            .executes(context -> TriggerCommands.setTp(
                                                StringArgumentType.getString(context, "area"), false,
                                                ResourceLocationArgument.getId(context, "dim").toString(),
                                                DoubleArgumentType.getDouble(context, "x"),
                                                DoubleArgumentType.getDouble(context, "y"),
                                                DoubleArgumentType.getDouble(context, "z"), context))))))
                            .then(Commands.literal("clear")
                                .executes(context -> TriggerCommands.clearTp(
                                    StringArgumentType.getString(context, "area"), false, context))))
                        .then(Commands.literal("info")
                            .executes(context -> TriggerCommands.showInfo(
                                StringArgumentType.getString(context, "area"), false, context))))))

            // --- Stats command ---
            .then(Commands.literal("stats")
                .executes(AreaCommands::showStats)
            )

            // --- Backup command ---
            .then(Commands.literal("backup")
                .executes(AreaCommands::backupConfigs)
            )

            // --- GUI command ---
            .then(Commands.literal("gui")
                .executes(context -> {
                    net.minecraft.server.level.ServerPlayer player =
                        context.getSource().getPlayerOrException();
                    ModNetwork.sendToPlayer(new S2COpenManagementScreenPacket(), player);
                    context.getSource().sendSystemMessage(
                        Component.literal(LocalizationManager.translate("gui.opening") + " ")
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(
                                LocalizationManager.translate("gui.no_client_mod"))
                                .withStyle(ChatFormatting.GRAY)));
                    return 1;
                })
            )

            // --- Template commands ---
            .then(Commands.literal("template")
                .then(Commands.literal("list")
                    .executes(TemplateCommands::listTemplates))
                .then(Commands.literal("info")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> TemplateCommands.showTemplateInfo(
                            StringArgumentType.getString(context, "name"), context))))
                .then(Commands.literal("create")
                    .then(Commands.argument("template", StringArgumentType.string())
                        .then(Commands.argument("areaName", StringArgumentType.string())
                            .executes(context -> TemplateCommands.createFromTemplate(
                                StringArgumentType.getString(context, "template"),
                                StringArgumentType.getString(context, "areaName"), context)))))
            )
        );
    }
}
