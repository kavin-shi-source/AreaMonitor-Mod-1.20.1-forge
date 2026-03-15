package com.kavinshi.areamonitor;

import com.kavinshi.areamonitor.util.GameModeUtils;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Extended command system supporting multi-area management, visual editing, and more.
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExtendedCommands {
    private static final List<String> GAME_MODES = List.of("survival", "creative", "adventure", "spectator");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Register all commands under /areamonitor
        dispatcher.register(Commands.literal("areamonitor")
            .requires(source -> source.hasPermission(2))

            // Toggle monitoring (from ModCommands)
            .then(Commands.literal("toggle")
                .executes(ExtendedCommands::toggleMonitor)
            )

            // Whitelist management (from ModCommands)
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
                    .executes(ExtendedCommands::listWhitelist)
                )
                .then(Commands.literal("clear")
                    .executes(ExtendedCommands::clearWhitelist)
                )
            )

            // Help command (from ModCommands)
            .then(Commands.literal("help")
                .executes(ExtendedCommands::showHelp)
            )

            // Language settings (from ModCommands)
            .then(Commands.literal("language")
                .then(Commands.literal("en")
                    .executes(ExtendedCommands::setLanguageEnglish)
                )
                .then(Commands.literal("zh")
                    .executes(ExtendedCommands::setLanguageChinese)
                )
                .executes(ExtendedCommands::showLanguageStatus)
            )

            // Area management commands
            .then(Commands.literal("area")
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> createArea(
                            StringArgumentType.getString(context, "name"),
                            context
                        )))
                )
                .then(Commands.literal("delete")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String areaName : AreaManager.getInstance().getAreaNames()) {
                                if (areaName.startsWith(builder.getRemaining().toLowerCase())) {
                                    builder.suggest(areaName);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> deleteArea(
                            StringArgumentType.getString(context, "name"),
                            context
                        )))
                )
                .then(Commands.literal("list")
                    .executes(ExtendedCommands::listAreas)
                )
                .then(Commands.literal("toggle")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String areaName : AreaManager.getInstance().getAreaNames()) {
                                if (areaName.startsWith(builder.getRemaining().toLowerCase())) {
                                    builder.suggest(areaName);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> toggleArea(
                            StringArgumentType.getString(context, "name"),
                            context
                        )))
                )
                .then(Commands.literal("info")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String areaName : AreaManager.getInstance().getAreaNames()) {
                                if (areaName.startsWith(builder.getRemaining().toLowerCase())) {
                                    builder.suggest(areaName);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> showAreaInfo(
                            StringArgumentType.getString(context, "name"),
                            context
                        )))
                )
                .then(Commands.literal("setEnterMode")
                    .then(Commands.argument("areaName", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String areaName : AreaManager.getInstance().getAreaNames()) {
                                if (areaName.startsWith(builder.getRemaining().toLowerCase())) {
                                    builder.suggest(areaName);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("mode", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                for (String mode : GAME_MODES) {
                                    if (mode.startsWith(builder.getRemaining().toLowerCase())) {
                                        builder.suggest(mode);
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> setAreaEnterMode(
                                StringArgumentType.getString(context, "areaName"),
                                StringArgumentType.getString(context, "mode"),
                                context
                            ))))
                )
                .then(Commands.literal("setLeaveMode")
                    .then(Commands.argument("areaName", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String areaName : AreaManager.getInstance().getAreaNames()) {
                                if (areaName.startsWith(builder.getRemaining().toLowerCase())) {
                                    builder.suggest(areaName);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("mode", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                for (String mode : GAME_MODES) {
                                    if (mode.startsWith(builder.getRemaining().toLowerCase())) {
                                        builder.suggest(mode);
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> setAreaLeaveMode(
                                StringArgumentType.getString(context, "areaName"),
                                StringArgumentType.getString(context, "mode"),
                                context
                            ))))
                )
            )

            // 可视化编辑器命令
            .then(Commands.literal("visual")
                .then(Commands.literal("tool")
                    .executes(ExtendedCommands::giveVisualTool)
                )
                .then(Commands.literal("show")
                    .then(Commands.argument("area", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String areaName : AreaManager.getInstance().getAreaNames()) {
                                if (areaName.startsWith(builder.getRemaining().toLowerCase())) {
                                    builder.suggest(areaName);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> showAreaVisual(
                            StringArgumentType.getString(context, "area"),
                            context
                        )))
                )
                .then(Commands.literal("hide")
                    .executes(ExtendedCommands::hideAreaVisual)
                )
            )

            // 性能监控命令
            .then(Commands.literal("performance")
                .executes(ExtendedCommands::showPerformance)
            )

            // 黑名单管理命令
            .then(Commands.literal("blacklist")
                .then(Commands.literal("info")
                    .executes(ExtendedCommands::showBlacklistInfo)
                )
                .then(Commands.literal("area")
                    .then(Commands.argument("areaName", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String areaName : AreaManager.getInstance().getAreaNames()) {
                                if (areaName.startsWith(builder.getRemaining().toLowerCase())) {
                                    builder.suggest(areaName);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.literal("add")
                            .then(Commands.argument("item", StringArgumentType.greedyString())
                                .suggests(ExtendedCommands::suggestItems)
                                .executes(context -> addItemToAreaBlacklist(
                                    StringArgumentType.getString(context, "areaName"),
                                    StringArgumentType.getString(context, "item"),
                                    context
                                ))
                            )
                        )
                        .then(Commands.literal("remove")
                            .then(Commands.argument("item", StringArgumentType.greedyString())
                                .suggests(ExtendedCommands::suggestItems)
                                .executes(context -> removeItemFromAreaBlacklist(
                                    StringArgumentType.getString(context, "areaName"),
                                    StringArgumentType.getString(context, "item"),
                                    context
                                ))
                            )
                        )
                        .then(Commands.literal("list")
                            .executes(context -> listAreaBlacklist(
                                StringArgumentType.getString(context, "areaName"),
                                context
                            ))
                        )
                        .then(Commands.literal("toggle")
                            .executes(context -> toggleAreaBlacklist(
                                StringArgumentType.getString(context, "areaName"),
                                context
                            ))
                        )
                    )
                )
                .then(Commands.literal("reload")
                    .executes(ExtendedCommands::reloadBlacklistConfig)
                )
            )


            // 选择工具命令
            .then(Commands.literal("selection")
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> createAreaFromSelection(
                            StringArgumentType.getString(context, "name"),
                            context
                        )))
                )
                .then(Commands.literal("cancel")
                    .executes(ExtendedCommands::cancelSelection)
                )
                .then(Commands.literal("info")
                    .executes(ExtendedCommands::showSelectionInfo)
                )
                .then(Commands.literal("tutorial")
                    .executes(ExtendedCommands::showTutorial)
                )
            )

            // 配置管理命令
            .then(Commands.literal("config")
                .then(Commands.literal("reload")
                    .executes(ExtendedCommands::reloadConfigs)
                )
                .then(Commands.literal("generate")
                    .executes(ExtendedCommands::generateConfigs)
                )
            )
        );
    }

    // 区域创建命令
    private static int createArea(String areaName, CommandContext<CommandSourceStack> context) {
        if (AreaManager.getInstance().getArea(areaName) != null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.exists", areaName);
            return 0;
        }

        MonitorArea area = new MonitorArea(areaName);
        AreaManager.getInstance().addArea(area);
        ConfigManager.saveAreasConfig();

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.area.created", true, areaName);
        return 1;
    }

    // 区域删除命令
    private static int deleteArea(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        AreaManager.getInstance().removeArea(areaName);
        ConfigManager.saveAreasConfig();

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.area.deleted", true, areaName);
        return 1;
    }

    // 区域列表命令 - 显示详细信息
    private static int listAreas(CommandContext<CommandSourceStack> context) {
        Set<String> areaNames = AreaManager.getInstance().getAreaNames();

        if (areaNames.isEmpty()) {
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.area.list.empty", false);
            return 1;
        }

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.area.list.header", false);

        int index = 1;
        for (String areaName : areaNames) {
            MonitorArea area = AreaManager.getInstance().getArea(areaName);
            if (area == null) continue;

            String status = area.isEnabled() ? "area.enabled" : "area.disabled";
            String coordinates = "Not set";

            if (area.getBounds() instanceof MonitorArea.RectangleBounds rect) {
                coordinates = String.format("X[%d ~ %d], Z[%d ~ %d]",
                    rect.getMinX(), rect.getMaxX(), rect.getMinZ(), rect.getMaxZ());
            } else if (area.getBounds() instanceof MonitorArea.CircleBounds circle) {
                coordinates = String.format("Center(%d, %d), Radius %d",
                    circle.getCenterX(), circle.getCenterZ(), circle.getRadius());
            }

            final int currentIndex = index++;
            final String finalCoordinates = coordinates;
            final String finalStatus = status;

            context.getSource().sendSuccess(
                () -> MessageUtils.smartComponent(context.getSource(), "area.status", 
                    String.format("§e%d. §f%s", currentIndex, area.getDisplayName()) + " " + finalStatus),
                false
            );

            MessageUtils.sendSuccess(context.getSource(), "area.coordinates_format", false, finalCoordinates);
            MessageUtils.sendSuccess(context.getSource(), "area.dimension", false, area.getDimension());
            MessageUtils.sendSuccess(context.getSource(), "area.enter_mode", false, area.getEnterMode().getName());
            MessageUtils.sendSuccess(context.getSource(), "area.leave_mode", false, area.getLeaveMode().getName());

            context.getSource().sendSuccess(
                () -> Component.literal("   §7────────────────"),
                false
            );
        }

        MessageUtils.sendSuccess(context.getSource(), "area.count", false, areaNames.size());

        return 1;
    }

    // 区域开关命令
    private static int toggleArea(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        boolean currentState = area.isEnabled();
        area.setEnabled(!currentState);
        ConfigManager.saveAreasConfig();

        String newState = !currentState ? "area.enabled" : "area.disabled";
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.area.toggled", true, area.getDisplayName(), newState);

        return 1;
    }

    // 区域信息显示命令
    private static int showAreaInfo(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.area.info.header", false, area.getDisplayName());

        String statusKey = area.isEnabled() ? "area.enabled" : "area.disabled";
        String status = LocalizationManager.translate(statusKey);
        MessageUtils.sendSuccess(context.getSource(), "area.status", false, status);
        MessageUtils.sendSuccess(context.getSource(), "area.dimension", false, area.getDimension());

        if (area.getBounds() instanceof MonitorArea.RectangleBounds rect) {
            String coords = String.format("X[%d ~ %d], Z[%d ~ %d]",
                rect.getMinX(), rect.getMaxX(), rect.getMinZ(), rect.getMaxZ());
            MessageUtils.sendSuccess(context.getSource(), "area.coordinates_format", false, coords);
        } else if (area.getBounds() instanceof MonitorArea.CircleBounds circle) {
            String coords = String.format("Center(%d, %d), Radius %d",
                circle.getCenterX(), circle.getCenterZ(), circle.getRadius());
            MessageUtils.sendSuccess(context.getSource(), "area.coordinates_format", false, coords);
        }

        String enterMode = LocalizationManager.getGameModeDisplayName(area.getEnterMode());
        String leaveMode = LocalizationManager.getGameModeDisplayName(area.getLeaveMode());
        MessageUtils.sendSuccess(context.getSource(), "area.enter_mode", false, enterMode);
        MessageUtils.sendSuccess(context.getSource(), "area.leave_mode", false, leaveMode);

        return 1;
    }

    // 给予可视化工具
    private static int giveVisualTool(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        SelectionTool.giveSelectionTool(player);
        return 1;
    }

    // 显示区域可视化
    private static int showAreaVisual(String areaName, CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        AreaVisualizer.startPersistentVisualization(player, area);
        MessageUtils.sendSuccess(context.getSource(), "area.start_showing_boundary", true, area.getDisplayName());
        return 1;
    }

    // 隐藏区域可视化
    private static int hideAreaVisual(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        AreaVisualizer.stopPersistentVisualization(player);
        MessageUtils.sendSuccess(context.getSource(), "area.stop_showing_boundary", true);
        return 1;
    }

    // 性能监控显示
    private static int showPerformance(CommandContext<CommandSourceStack> context) {
        Map<String, String> stats = PerformanceMonitor.getPerformanceStats();

        MessageUtils.sendSuccess(context.getSource(), "performance.header", false);

        for (Map.Entry<String, String> entry : stats.entrySet()) {
            String key = "performance." + entry.getKey();
            String label = LocalizationManager.translate(key);
            context.getSource().sendSuccess(
                () -> Component.literal(label + ": §f" + entry.getValue()),
                false
            );
        }

        return 1;
    }

    // 黑名单信息显示
    private static int showBlacklistInfo(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        ItemBlacklistManager.showPlayerRestrictions(player);
        return 1;
    }

    // 添加物品到区域黑名单
    private static int addItemToAreaBlacklist(String areaName, String itemName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        Item item = parseItem(itemName);
        if (item == null) {
            MessageUtils.sendFailure(context.getSource(), "blacklist.invalid_item", itemName);
            return 0;
        }

        Set<Item> areaBlacklist = ItemBlacklistManager.getAreaBlacklist(areaName);
        if (areaBlacklist.isEmpty()) {
            areaBlacklist = new HashSet<>();
        }

        if (areaBlacklist.contains(item)) {
            MessageUtils.sendSuccess(context.getSource(), "blacklist.item_already_blacklisted", true, getItemDisplayName(item));
        } else {
            areaBlacklist.add(item);
            ItemBlacklistManager.addAreaBlacklist(areaName, areaBlacklist);
            ItemBlacklistManager.saveBlacklistConfig();

            MessageUtils.sendSuccess(context.getSource(), "blacklist.item_added", true, getItemDisplayName(item), areaName);
        }
        return 1;
    }

    // 从区域黑名单移除物品
    private static int removeItemFromAreaBlacklist(String areaName, String itemName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        Item item = parseItem(itemName);
        if (item == null) {
            MessageUtils.sendFailure(context.getSource(), "blacklist.invalid_item", itemName);
            return 0;
        }

        Set<Item> areaBlacklist = ItemBlacklistManager.getAreaBlacklist(areaName);
        if (areaBlacklist.remove(item)) {
            if (areaBlacklist.isEmpty()) {
                ItemBlacklistManager.removeAreaBlacklist(areaName);
            } else {
                ItemBlacklistManager.addAreaBlacklist(areaName, areaBlacklist);
            }
            ItemBlacklistManager.saveBlacklistConfig();

            MessageUtils.sendSuccess(context.getSource(), "blacklist.item_removed", true, getItemDisplayName(item), areaName);
        } else {
            MessageUtils.sendSuccess(context.getSource(), "blacklist.item_not_found", true, getItemDisplayName(item));
        }
        return 1;
    }

    // 列出区域黑名单
    private static int listAreaBlacklist(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        Set<Item> areaBlacklist = ItemBlacklistManager.getAreaBlacklist(areaName);

        MessageUtils.sendSuccess(context.getSource(), "blacklist.area_header", false, areaName);

        if (areaBlacklist.isEmpty()) {
            MessageUtils.sendSuccess(context.getSource(), "blacklist.area_empty", false);
        } else {
            for (Item item : areaBlacklist) {
                context.getSource().sendSuccess(
                    () -> Component.literal("§c- " + getItemDisplayName(item)),
                    false
                );
            }
        }

        Set<Item> globalBlacklist = ItemBlacklistManager.getGlobalBlacklist();
        if (!globalBlacklist.isEmpty()) {
            MessageUtils.sendSuccess(context.getSource(), "blacklist.global_items", false);
            for (Item item : globalBlacklist) {
                context.getSource().sendSuccess(
                    () -> Component.literal("§7- " + getItemDisplayName(item)),
                    false
                );
            }
        }

        return 1;
    }

    // 切换区域黑名单启用状态
    private static int toggleAreaBlacklist(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        boolean currentState = area.getRestrictions().isEnableItemBlacklist();
        area.getRestrictions().setEnableItemBlacklist(!currentState);
        ConfigManager.saveAreasConfig();

        String newState = !currentState ? "area.enabled" : "area.disabled";
        MessageUtils.sendSuccess(context.getSource(), "blacklist.area_toggle", true, areaName, newState);

        return 1;
    }

    // 热加载黑名单配置
    private static int reloadBlacklistConfig(CommandContext<CommandSourceStack> context) {
        try {
            ItemBlacklistManager.loadBlacklistConfig();
            MessageUtils.sendSuccess(context.getSource(), "blacklist.reloaded", true);
        } catch (Exception e) {
            MessageUtils.sendFailure(context.getSource(), "blacklist.reload_failed", e.getMessage());
        }
        return 1;
    }

    // 解析物品名称
    private static Item parseItem(String itemName) {
        try {
            // 尝试通过注册名解析
            if (!itemName.contains(":")) {
                itemName = "minecraft:" + itemName;
            }
            ResourceLocation location = new ResourceLocation(itemName);
            Item item = BuiltInRegistries.ITEM.get(location);
            // 检查是否找到有效的物品（不是空气）
            if (item == Items.AIR && !itemName.equals("minecraft:air")) {
                return null; // 无效的物品名称
            }
            return item;
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.debug("Failed to parse item name: {}", itemName);
            return null;
        }
    }

    // 获取物品显示名称
    private static String getItemDisplayName(Item item) {
        return item.getDescriptionId(); // 简化实现，实际应该使用翻译键
    }

    // 物品名称建议提供者
    private static CompletableFuture<Suggestions> suggestItems(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        // 遍历所有注册物品
        BuiltInRegistries.ITEM.entrySet().forEach(entry -> {
            String itemId = entry.getKey().location().toString();
            String itemName = entry.getKey().location().getPath();

            // 支持完整ID匹配和部分名称匹配
            if (itemId.startsWith(remaining) || itemName.startsWith(remaining) ||
                itemName.contains(remaining.replace("minecraft:", ""))) {
                // 过滤掉空气等特殊物品
                if (entry.getValue() != Items.AIR || remaining.contains("air")) {
                    builder.suggest(itemId);
                }
            }
        });

        return builder.buildFuture();
    }

    // 从选择创建区域
    private static int createAreaFromSelection(String areaName, CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        SelectionTool.createAreaFromSelection(player, areaName);
        return 1;
    }

    // 取消选择
    private static int cancelSelection(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        SelectionTool.cancelSelection(player);
        return 1;
    }

    // 显示选择信息
    private static int showSelectionInfo(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        SelectionTool.showCurrentSelection(player);
        return 1;
    }

    // 显示教程
    private static int showTutorial(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        showTutorialMessage(player);
        return 1;
    }

    private static void showTutorialMessage(ServerPlayer player) {
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.tutorial.header").withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step1"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step1.command"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step1.description"), false);

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step2"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step2.action"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step2.feedback"), false);

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step3"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step3.action"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step3.feedback"), false);

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step4"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step4.command"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step4.example"), false);

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.step5"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.set_enter_mode"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.set_leave_mode"), false);

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.other_commands"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.view_area"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.show_boundary"), false);
        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.cancel_selection"), false);

        player.displayClientMessage(MessageUtils.smartComponent(player, "selection.tutorial.end"), false);
    }

    // 设置区域进入模式
    private static int setAreaEnterMode(String areaName, String mode, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        String modeLower = mode.toLowerCase();
        if (!GAME_MODES.contains(modeLower)) {
            MessageUtils.sendFailure(context.getSource(), "area.invalid_gamemode", mode, String.join(", ", GAME_MODES));
            return 0;
        }

        GameType gameMode = GameModeUtils.fromName(modeLower);

        area.setEnterMode(gameMode);
        ConfigManager.saveAreasConfig();

        MessageUtils.sendSuccess(context.getSource(), "area.enter_mode_set", true, areaName, modeLower);

        return 1;
    }

    // 设置区域离开模式
    private static int setAreaLeaveMode(String areaName, String mode, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            MessageUtils.sendFailure(context.getSource(), "command.areamonitor.area.not_found", areaName);
            return 0;
        }

        String modeLower = mode.toLowerCase();
        if (!GAME_MODES.contains(modeLower)) {
            MessageUtils.sendFailure(context.getSource(), "area.invalid_gamemode", mode, String.join(", ", GAME_MODES));
            return 0;
        }

        GameType gameMode = GameModeUtils.fromName(modeLower);

        area.setLeaveMode(gameMode);
        ConfigManager.saveAreasConfig();

        MessageUtils.sendSuccess(context.getSource(), "area.leave_mode_set", true, areaName, modeLower);

        return 1;
    }

    // 重新加载配置文件
    private static int reloadConfigs(CommandContext<CommandSourceStack> context) {
        try {
            ConfigManager.loadAreasConfig();
            ItemBlacklistManager.loadBlacklistConfig();
            ConfigManager.validateConfig();

            MessageUtils.sendSuccess(context.getSource(), "config.reloaded", true);
        } catch (Exception e) {
            MessageUtils.sendFailure(context.getSource(), "config.reload_failed", e.getMessage());
        }
        return 1;
    }

    // 生成配置文件
    private static int generateConfigs(CommandContext<CommandSourceStack> context) {
        try {
            ConfigManager.ensureConfigFiles();

            MessageUtils.sendSuccess(context.getSource(), "config.generated", true);
            MessageUtils.sendSuccess(context.getSource(), "config.path_info", false);
            MessageUtils.sendSuccess(context.getSource(), "config.areas_path", false);
            MessageUtils.sendSuccess(context.getSource(), "config.blacklist_path", false);

        } catch (Exception e) {
            MessageUtils.sendFailure(context.getSource(), "config.regenerate_failed", e.getMessage());
        }
        return 1;
    }

    // === Methods from ModCommands ===

    private static int toggleMonitor(CommandContext<CommandSourceStack> context) {
        boolean current = ConfigManager.CONFIG.isEnabled.get();
        boolean newState = !current;

        ConfigManager.CONFIG.isEnabled.set(newState);
        ConfigManager.CONFIG.isEnabled.save();

        String messageKey = newState ?
                "command.areamonitor.toggle.enabled" :
                "command.areamonitor.toggle.disabled";

        MessageUtils.sendSuccess(context.getSource(), messageKey, true);

        return 1;
    }

    private static int addToWhitelist(String player, CommandContext<CommandSourceStack> context) {
        if (WhitelistManager.addToWhitelist(player)) {
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.add.success", true, player);
        } else {
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.add.exists", true, player);
        }
        return 1;
    }

    private static int removeFromWhitelist(String player, CommandContext<CommandSourceStack> context) {
        if (WhitelistManager.removeFromWhitelist(player)) {
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.remove.success", true, player);
        } else {
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.remove.not_found", true, player);
        }
        return 1;
    }

    private static int listWhitelist(CommandContext<CommandSourceStack> context) {
        Set<String> whitelist = WhitelistManager.getWhitelist();

        if (whitelist.isEmpty()) {
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.list.empty", false);
        } else {
            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.list.header", false);

            List<String> whitelistArray = new ArrayList<>(whitelist);

            for (int i = 0; i < whitelistArray.size(); i++) {
                final int index = i + 1;
                final String player = whitelistArray.get(i);

                context.getSource().sendSuccess(
                        () -> Component.literal(String.format("§e%d. §f%s", index, player)),
                        false
                );
            }

            MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.list.count", false, whitelist.size());
        }
        return 1;
    }

    private static int clearWhitelist(CommandContext<CommandSourceStack> context) {
        WhitelistManager.clearWhitelist();
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.whitelist.clear.success", true);
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.header", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.basic", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.toggle", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.list", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.area", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.create", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.delete", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.list", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.toggle", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.info", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.set_enter_mode", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.area.set_leave_mode", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.visual", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.visual.tool", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.visual.show", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.visual.hide", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.selection", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.selection.create", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.selection.cancel", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.selection.info", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.selection.tutorial", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.whitelist", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.whitelist.add", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.whitelist.remove", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.whitelist.list", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.whitelist.clear", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.other", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.performance", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.blacklist.info", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.blacklist.add", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.blacklist.remove", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.blacklist.list", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.blacklist.toggle", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.blacklist.reload", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.config", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.config.reload", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.config.generate", false);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.section.language", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.language.show", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.language.english", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.language.chinese", false);

        return 1;
    }

    private static int setLanguageEnglish(CommandContext<CommandSourceStack> context) {
        LocalizationManager.switchLanguage(LocalizationManager.LANGUAGE_ENGLISH);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.language.english.success", false);
        return 1;
    }

    private static int setLanguageChinese(CommandContext<CommandSourceStack> context) {
        LocalizationManager.switchLanguage(LocalizationManager.LANGUAGE_CHINESE);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.language.chinese.success", true);
        return 1;
    }

    private static int showLanguageStatus(CommandContext<CommandSourceStack> context) {
        String currentLang = LocalizationManager.getCurrentLanguage();
        String displayName = LocalizationManager.getLanguageDisplayName(currentLang);

        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.language.current", false, displayName);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.language.usage", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.language.english", false);
        MessageUtils.sendSuccess(context.getSource(), "command.areamonitor.help.language.chinese", false);

        return 1;
    }
}
