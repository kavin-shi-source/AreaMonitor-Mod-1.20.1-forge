package com.kavinshi.areamonitor;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

@Mod.EventBusSubscriber(modid = AreaMonitorMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("areamonitor")
                .requires(source -> source.hasPermission(2))

                // 只保留 toggle 命令，删除 on/off 命令
                .then(Commands.literal("toggle")
                        .executes(ModCommands::toggleMonitor)
                )

                .then(Commands.literal("setArea")
                        .then(Commands.argument("minX", IntegerArgumentType.integer())
                                .then(Commands.argument("minZ", IntegerArgumentType.integer())
                                        .then(Commands.argument("maxX", IntegerArgumentType.integer())
                                                .then(Commands.argument("maxZ", IntegerArgumentType.integer())
                                                        .executes(context -> setArea(
                                                                IntegerArgumentType.getInteger(context, "minX"),
                                                                IntegerArgumentType.getInteger(context, "minZ"),
                                                                IntegerArgumentType.getInteger(context, "maxX"),
                                                                IntegerArgumentType.getInteger(context, "maxZ"),
                                                                context
                                                        ))))))
                )

                .then(Commands.literal("setDimension")
                        .then(Commands.argument("dimension", StringArgumentType.string())
                                .executes(context -> setDimension(
                                        StringArgumentType.getString(context, "dimension"),
                                        context
                                )))
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

                .then(Commands.literal("status")
                        .executes(ModCommands::showStatus)
                )
                .then(Commands.literal("help")
                        .executes(ModCommands::showHelp)
                )
        );
    }

    private static int toggleMonitor(CommandContext<CommandSourceStack> context) {
        boolean current = ConfigManager.CONFIG.isEnabled.get();
        boolean newState = !current;

        ConfigManager.CONFIG.isEnabled.set(newState);
        ConfigManager.CONFIG.isEnabled.save();

        String state = newState ? "§a启用" : "§c禁用";
        String message = newState ?
                "§6区域监控已§a启用§6，使用 /areamonitor status 查看状态" :
                "§6区域监控已§c禁用§6，使用 /areamonitor toggle 重新启用";

        context.getSource().sendSuccess(
                () -> Component.literal(message),
                true
        );

        AreaMonitorMod.LOGGER.info("区域监控状态已切换为: {}", newState ? "启用" : "禁用");
        return 1;
    }

    private static int setArea(int minX, int minZ, int maxX, int maxZ,
                               CommandContext<CommandSourceStack> context) {
        ConfigManager.CONFIG.minX.set(minX);
        ConfigManager.CONFIG.minZ.set(minZ);
        ConfigManager.CONFIG.maxX.set(maxX);
        ConfigManager.CONFIG.maxZ.set(maxZ);

        ConfigManager.CONFIG.minX.save();
        ConfigManager.CONFIG.minZ.save();
        ConfigManager.CONFIG.maxX.save();
        ConfigManager.CONFIG.maxZ.save();

        context.getSource().sendSuccess(
                () -> Component.literal(String.format(
                        "§a监控区域已设置为: X[%d ~ %d], Z[%d ~ %d]",
                        minX, maxX, minZ, maxZ
                )),
                true
        );
        return 1;
    }

    private static int setDimension(String dimension, CommandContext<CommandSourceStack> context) {
        ConfigManager.CONFIG.targetDimension.set(dimension);
        ConfigManager.CONFIG.targetDimension.save();

        context.getSource().sendSuccess(
                () -> Component.literal("§a目标维度已设置为: " + dimension),
                true
        );
        return 1;
    }

    private static int addToWhitelist(String player, CommandContext<CommandSourceStack> context) {
        if (WhitelistManager.addToWhitelist(player)) {
            context.getSource().sendSuccess(
                    () -> Component.literal("§a玩家 " + player + " 已添加到白名单"),
                    true
            );
        } else {
            context.getSource().sendSuccess(
                    () -> Component.literal("§e玩家 " + player + " 已在白名单中"),
                    true
            );
        }
        return 1;
    }

    private static int removeFromWhitelist(String player, CommandContext<CommandSourceStack> context) {
        if (WhitelistManager.removeFromWhitelist(player)) {
            context.getSource().sendSuccess(
                    () -> Component.literal("§a玩家 " + player + " 已从白名单移除"),
                    true
            );
        } else {
            context.getSource().sendSuccess(
                    () -> Component.literal("§e玩家 " + player + " 不在白名单中"),
                    true
            );
        }
        return 1;
    }

    private static int listWhitelist(CommandContext<CommandSourceStack> context) {
        Set<String> whitelist = WhitelistManager.getWhitelist();

        if (whitelist.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.literal("§e白名单为空"),
                    false
            );
        } else {
            context.getSource().sendSuccess(
                    () -> Component.literal("§6=== 白名单列表 ==="),
                    false
            );

            // 修复：将白名单转换为List，使用索引访问
            List<String> whitelistArray = new ArrayList<>(whitelist);

            for (int i = 0; i < whitelistArray.size(); i++) {
                final int index = i + 1; // 创建final变量
                final String player = whitelistArray.get(i); // 创建final变量

                context.getSource().sendSuccess(
                        () -> Component.literal(String.format("§e%d. §f%s", index, player)),
                        false
                );
            }

            context.getSource().sendSuccess(
                    () -> Component.literal(String.format("§e共 %d 名玩家", whitelist.size())),
                    false
            );
        }
        return 1;
    }

    private static int clearWhitelist(CommandContext<CommandSourceStack> context) {
        WhitelistManager.clearWhitelist();
        context.getSource().sendSuccess(
                () -> Component.literal("§a白名单已清空"),
                true
        );
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        ConfigManager.Config config = ConfigManager.CONFIG;

        String status = config.isEnabled.get() ? "§a运行中" : "§c已停止";

        context.getSource().sendSuccess(
                () -> Component.literal("§6=== 区域监控模组状态 ==="), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§e状态: " + status), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal(String.format(
                        "§e监控区域: X[%d ~ %d], Z[%d ~ %d]",
                        config.minX.get(), config.maxX.get(),
                        config.minZ.get(), config.maxZ.get()
                )), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§e目标维度: " + config.targetDimension.get()), false
        );

        Set<String> whitelist = WhitelistManager.getWhitelist();
        context.getSource().sendSuccess(
                () -> Component.literal("§e白名单玩家: " + whitelist.size() + " 名"), false
        );

        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                () -> Component.literal("§6=== 区域监控模组命令 ==="), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor toggle §f- 切换监控状态"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor setArea <minX> <minZ> <maxX> <maxZ> §f- 设置区域"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor setDimension <维度ID> §f- 设置目标维度"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor whitelist add <玩家名> §f- 添加白名单"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor whitelist remove <玩家名> §f- 移除白名单"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor whitelist list §f- 查看白名单"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor whitelist clear §f- 清空白名单"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor status §f- 显示状态"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor help §f- 显示帮助"), false
        );

        return 1;
    }
}