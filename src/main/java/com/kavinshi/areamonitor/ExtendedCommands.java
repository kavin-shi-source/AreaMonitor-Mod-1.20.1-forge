package com.kavinshi.areamonitor;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

/**
 * 扩展命令系统，支持多区域管理、可视化编辑等功能
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExtendedCommands {


    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // 区域管理命令
        dispatcher.register(Commands.literal("areamonitor")
            .requires(source -> source.hasPermission(2))

            // 区域管理子命令
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
                                for (String mode : ModConstants.GAME_MODE_SUGGESTIONS) {
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
                                for (String mode : ModConstants.GAME_MODE_SUGGESTIONS) {
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
            context.getSource().sendFailure(
                Component.translatable("command.areamonitor.area.exists", areaName)
            );
            return 0;
        }

        MonitorArea area = new MonitorArea(areaName);
        AreaManager.getInstance().addArea(area);
        ConfigManager.saveAreasConfig();

        context.getSource().sendSuccess(
            () -> Component.translatable("command.areamonitor.area.created", areaName),
            true
        );
        return 1;
    }

    // 区域删除命令
    private static int deleteArea(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            context.getSource().sendFailure(
                Component.translatable("command.areamonitor.area.not_found", areaName)
            );
            return 0;
        }

        AreaManager.getInstance().removeArea(areaName);
        ConfigManager.saveAreasConfig();

        context.getSource().sendSuccess(
            () -> Component.translatable("command.areamonitor.area.deleted", areaName),
            true
        );
        return 1;
    }

    // 区域列表命令 - 显示详细信息
    private static int listAreas(CommandContext<CommandSourceStack> context) {
        Set<String> areaNames = AreaManager.getInstance().getAreaNames();

        if (areaNames.isEmpty()) {
            context.getSource().sendSuccess(
                () -> Component.translatable("command.areamonitor.area.list.empty"),
                false
            );
            return 1;
        }

        context.getSource().sendSuccess(
            () -> Component.translatable("command.areamonitor.area.list.header"),
            false
        );

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

            // 显示区域基本信息
            context.getSource().sendSuccess(
                () -> Component.literal(String.format("§e%d. §f%s §7(%s)",
                    currentIndex, area.getDisplayName(), status)),
                false
            );

            // 显示详细信息
            context.getSource().sendSuccess(
                () -> Component.translatable("area.coordinates_format", finalCoordinates),
                false
            );

            context.getSource().sendSuccess(
                () -> Component.translatable("area.dimension", area.getDimension()),
                false
            );

            context.getSource().sendSuccess(
                () -> Component.translatable("area.enter_mode", area.getEnterMode().getName()),
                false
            );

            context.getSource().sendSuccess(
                () -> Component.translatable("area.leave_mode", area.getLeaveMode().getName()),
                false
            );

            // 分隔线
            context.getSource().sendSuccess(
                () -> Component.literal("   §7────────────────"),
                false
            );
        }

        context.getSource().sendSuccess(
            () -> Component.translatable("area.count", areaNames.size()),
            false
        );

        return 1;
    }

    // 区域开关命令
    private static int toggleArea(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            context.getSource().sendFailure(
                Component.translatable("command.areamonitor.area.not_found", areaName)
            );
            return 0;
        }

        boolean currentState = area.isEnabled();
        area.setEnabled(!currentState);
        ConfigManager.saveAreasConfig();

        String newState = !currentState ? "area.enabled" : "area.disabled";
        String message = String.format("§6区域 '%s' %s", area.getDisplayName(), newState);

        context.getSource().sendSuccess(
            () -> Component.translatable("command.areamonitor.area.toggled", area.getDisplayName(), newState),
            true
        );

        return 1;
    }

    // 区域信息显示命令
    private static int showAreaInfo(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            context.getSource().sendFailure(
                Component.translatable("command.areamonitor.area.not_found", areaName)
            );
            return 0;
        }

        context.getSource().sendSuccess(
            () -> Component.translatable("command.areamonitor.area.info.header", area.getDisplayName()),
            false
        );

        String status = area.isEnabled() ? "area.enabled" : "area.disabled";
        context.getSource().sendSuccess(
            () -> Component.translatable("area.status", status),
            false
        );

        context.getSource().sendSuccess(
            () -> Component.translatable("area.dimension", area.getDimension()),
            false
        );

        if (area.getBounds() instanceof MonitorArea.RectangleBounds rect) {
            context.getSource().sendSuccess(
                () -> Component.translatable("area.coordinates_format",
                    rect.getMinX(), rect.getMaxX(), rect.getMinZ(), rect.getMaxZ()),
                false
            );
        }

        context.getSource().sendSuccess(
            () -> Component.translatable("area.enter_mode", area.getEnterMode().getName()),
            false
        );

        context.getSource().sendSuccess(
            () -> Component.translatable("area.leave_mode", area.getLeaveMode().getName()),
            false
        );

        return 1;
    }

    // 给予可视化工具
    private static int giveVisualTool(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(
                Component.translatable("player.only_command")
            );
            return 0;
        }

        SelectionTool.giveSelectionTool(player);
        return 1;
    }

    // 显示区域可视化
    private static int showAreaVisual(String areaName, CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(
                Component.translatable("player.only_command")
            );
            return 0;
        }

        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            context.getSource().sendFailure(
                Component.translatable("command.areamonitor.area.not_found", areaName)
            );
            return 0;
        }

        AreaVisualizer.startPersistentVisualization(player, area);
        context.getSource().sendSuccess(
            () -> Component.translatable("area.start_showing_boundary", area.getDisplayName()),
            true
        );
        return 1;
    }

    // 隐藏区域可视化
    private static int hideAreaVisual(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(
                Component.translatable("player.only_command")
            );
            return 0;
        }

        AreaVisualizer.stopPersistentVisualization(player);
        context.getSource().sendSuccess(
            () -> Component.translatable("area.stop_showing_boundary"),
            true
        );
        return 1;
    }

    // 性能监控显示
    private static int showPerformance(CommandContext<CommandSourceStack> context) {
        Map<String, String> stats = PerformanceMonitor.getPerformanceStats();

        context.getSource().sendSuccess(
            () -> Component.translatable("performance.header"),
            false
        );

        for (Map.Entry<String, String> entry : stats.entrySet()) {
            context.getSource().sendSuccess(
                () -> Component.literal("§e" + entry.getKey() + ": §f" + entry.getValue()),
                false
            );
        }

        return 1;
    }

    // 黑名单信息显示
    private static int showBlacklistInfo(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(
                Component.translatable("player.only_command")
            );
            return 0;
        }

        ItemBlacklistManager.showPlayerRestrictions(player);
        return 1;
    }

    // 添加物品到区域黑名单
    private static int addItemToAreaBlacklist(String areaName, String itemName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            context.getSource().sendFailure(
                Component.translatable("command.areamonitor.area.not_found", areaName)
            );
            return 0;
        }

        // 解析物品
        Item item = parseItem(itemName);
        if (item == null) {
            context.getSource().sendFailure(
                Component.translatable("blacklist.invalid_item", itemName)
            );
            return 0;
        }

        // 获取或创建区域黑名单
        Set<Item> areaBlacklist = ItemBlacklistManager.getAreaBlacklist(areaName);
        if (areaBlacklist.isEmpty()) {
            areaBlacklist = new HashSet<>();
        }

        if (areaBlacklist.contains(item)) {
            context.getSource().sendSuccess(
                () -> Component.translatable("blacklist.item_already_blacklisted", getItemDisplayName(item)),
                true
            );
        } else {
            areaBlacklist.add(item);
            ItemBlacklistManager.addAreaBlacklist(areaName, areaBlacklist);
            ItemBlacklistManager.saveBlacklistConfig();

            context.getSource().sendSuccess(
                () -> Component.translatable("blacklist.item_added", getItemDisplayName(item), areaName),
                true
            );
        }
        return 1;
    }

    // 从区域黑名单移除物品
    private static int removeItemFromAreaBlacklist(String areaName, String itemName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            context.getSource().sendFailure(
                Component.translatable("command.areamonitor.area.not_found", areaName)
            );
            return 0;
        }

        Item item = parseItem(itemName);
        if (item == null) {
            context.getSource().sendFailure(
                Component.translatable("blacklist.invalid_item", itemName)
            );
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

            context.getSource().sendSuccess(
                () -> Component.translatable("blacklist.item_removed", getItemDisplayName(item), areaName),
                true
            );
        } else {
            context.getSource().sendSuccess(
                () -> Component.translatable("blacklist.item_not_found", getItemDisplayName(item)),
                true
            );
        }
        return 1;
    }

    // 列出区域黑名单
    private static int listAreaBlacklist(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            context.getSource().sendFailure(
                Component.translatable("command.areamonitor.area.not_found", areaName)
            );
            return 0;
        }

        Set<Item> areaBlacklist = ItemBlacklistManager.getAreaBlacklist(areaName);

        context.getSource().sendSuccess(
            () -> Component.translatable("blacklist.area_header", areaName),
            false
        );

        if (areaBlacklist.isEmpty()) {
            context.getSource().sendSuccess(
                () -> Component.translatable("blacklist.area_empty"),
                false
            );
        } else {
            for (Item item : areaBlacklist) {
                context.getSource().sendSuccess(
                    () -> Component.literal("§c- " + getItemDisplayName(item)),
                    false
                );
            }
        }

        // 显示全局黑名单
        Set<Item> globalBlacklist = ItemBlacklistManager.getGlobalBlacklist();
        if (!globalBlacklist.isEmpty()) {
            context.getSource().sendSuccess(
                () -> Component.translatable("blacklist.global_items"),
                false
            );
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
            context.getSource().sendFailure(
                Component.translatable("command.areamonitor.area.not_found", areaName)
            );
            return 0;
        }

        boolean currentState = area.getRestrictions().isEnableItemBlacklist();
        area.getRestrictions().setEnableItemBlacklist(!currentState);
        ConfigManager.saveAreasConfig();

        String newState = !currentState ? "area.enabled" : "area.disabled";
        context.getSource().sendSuccess(
            () -> Component.translatable("blacklist.area_toggle", areaName, newState),
            true
        );

        return 1;
    }

    // 热加载黑名单配置
    private static int reloadBlacklistConfig(CommandContext<CommandSourceStack> context) {
        try {
            ItemBlacklistManager.loadBlacklistConfig();
            context.getSource().sendSuccess(
                () -> Component.translatable("blacklist.reloaded"),
                true
            );
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.translatable("blacklist.reload_failed", e.getMessage())
            );
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

    // 添加物品到黑名单
    private static int addToBlacklist(String itemName, CommandContext<CommandSourceStack> context) {
        // 这里需要解析物品名称并添加到黑名单
        context.getSource().sendSuccess(
            () -> Component.translatable("feature.development"),
            true
        );
        return 1;
    }

    // 从黑名单移除物品
    private static int removeFromBlacklist(String itemName, CommandContext<CommandSourceStack> context) {
        // 这里需要解析物品名称并从黑名单移除
        context.getSource().sendSuccess(
            () -> Component.translatable("feature.development"),
            true
        );
        return 1;
    }

    // 从选择创建区域
    private static int createAreaFromSelection(String areaName, CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(
                Component.translatable("player.only_command")
            );
            return 0;
        }

        SelectionTool.createAreaFromSelection(player, areaName);
        return 1;
    }

    // 取消选择
    private static int cancelSelection(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(
                Component.translatable("player.only_command")
            );
            return 0;
        }

        SelectionTool.cancelSelection(player);
        return 1;
    }

    // 显示选择信息
    private static int showSelectionInfo(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(
                Component.translatable("player.only_command")
            );
            return 0;
        }

        SelectionTool.showCurrentSelection(player);
        return 1;
    }

    // 显示教程
    private static int showTutorial(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(
                Component.translatable("player.only_command")
            );
            return 0;
        }

        showTutorialMessage(player);
        return 1;
    }

    private static void showTutorialMessage(ServerPlayer player) {
        player.displayClientMessage(
            Component.translatable("selection.tutorial.header").withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );

        player.displayClientMessage(
            Component.translatable("selection.tutorial.step1"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.step1.command"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.step1.description"),
            false
        );

        player.displayClientMessage(
            Component.translatable("selection.tutorial.step2"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.step2.action"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.step2.feedback"),
            false
        );

        player.displayClientMessage(
            Component.translatable("selection.tutorial.step3"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.step3.action"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.step3.feedback"),
            false
        );

        player.displayClientMessage(
            Component.translatable("selection.tutorial.step4"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.step4.command"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.step4.example"),
            false
        );

        player.displayClientMessage(
            Component.translatable("selection.tutorial.step5"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.set_enter_mode"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.set_leave_mode"),
            false
        );

        player.displayClientMessage(
            Component.translatable("selection.tutorial.other_commands"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.view_area"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.show_boundary"),
            false
        );
        player.displayClientMessage(
            Component.translatable("selection.tutorial.cancel_selection"),
            false
        );

        player.displayClientMessage(
            Component.translatable("selection.tutorial.end"),
            false
        );
    }

    // 设置区域进入模式
    private static int setAreaEnterMode(String areaName, String mode, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            context.getSource().sendFailure(
                Component.translatable("command.areamonitor.area.not_found", areaName)
            );
            return 0;
        }

        String modeLower = mode.toLowerCase();
        if (!ModConstants.GAME_MODE_SUGGESTIONS.contains(modeLower)) {
            context.getSource().sendFailure(
                Component.translatable("area.invalid_gamemode", mode, String.join(", ", ModConstants.GAME_MODE_SUGGESTIONS))
            );
            return 0;
        }

        GameType gameMode = switch (modeLower) {
            case "creative" -> GameType.CREATIVE;
            case "adventure" -> GameType.ADVENTURE;
            case "spectator" -> GameType.SPECTATOR;
            default -> GameType.SURVIVAL;
        };

        area.setEnterMode(gameMode);
        ConfigManager.saveAreasConfig();

        context.getSource().sendSuccess(
            () -> Component.translatable("area.enter_mode_set", areaName, modeLower),
            true
        );

        return 1;
    }

    // 设置区域离开模式
    private static int setAreaLeaveMode(String areaName, String mode, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaManager.getInstance().getArea(areaName);
        if (area == null) {
            context.getSource().sendFailure(
                Component.translatable("command.areamonitor.area.not_found", areaName)
            );
            return 0;
        }

        String modeLower = mode.toLowerCase();
        if (!ModConstants.GAME_MODE_SUGGESTIONS.contains(modeLower)) {
            context.getSource().sendFailure(
                Component.translatable("area.invalid_gamemode", mode, String.join(", ", ModConstants.GAME_MODE_SUGGESTIONS))
            );
            return 0;
        }

        GameType gameMode = switch (modeLower) {
            case "creative" -> GameType.CREATIVE;
            case "adventure" -> GameType.ADVENTURE;
            case "spectator" -> GameType.SPECTATOR;
            default -> GameType.SURVIVAL;
        };

        area.setLeaveMode(gameMode);
        ConfigManager.saveAreasConfig();

        context.getSource().sendSuccess(
            () -> Component.translatable("area.leave_mode_set", areaName, modeLower),
            true
        );

        return 1;
    }

    // 重新加载配置文件
    private static int reloadConfigs(CommandContext<CommandSourceStack> context) {
        try {
            ConfigManager.loadAreasConfig();
            ItemBlacklistManager.loadBlacklistConfig();
            ConfigManager.validateConfig();

            context.getSource().sendSuccess(
                () -> Component.translatable("config.reloaded"),
                true
            );
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.translatable("config.reload_failed", e.getMessage())
            );
        }
        return 1;
    }

    // 生成配置文件
    private static int generateConfigs(CommandContext<CommandSourceStack> context) {
        try {
            ConfigManager.ensureConfigFiles();

            context.getSource().sendSuccess(
                () -> Component.translatable("config.generated"),
                true
            );

            // 显示配置文件路径信息
            context.getSource().sendSuccess(
                () -> Component.translatable("config.path_info"),
                false
            );
            context.getSource().sendSuccess(
                () -> Component.translatable("config.areas_path"),
                false
            );
            context.getSource().sendSuccess(
                () -> Component.translatable("config.blacklist_path"),
                false
            );

        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.translatable("config.regenerate_failed", e.getMessage())
            );
        }
        return 1;
    }
}
