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
import java.util.Arrays;
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
        );
    }

    private static int toggleMonitor(CommandContext<CommandSourceStack> context) {
        boolean current = ConfigManager.CONFIG.isEnabled.get();
        boolean newState = !current;

        ConfigManager.CONFIG.isEnabled.set(newState);
        ConfigManager.CONFIG.isEnabled.save();

        String message = newState ?
                "§6区域监控已§a启用§6，使用 /areamonitor status 查看状态" :
                "§6区域监控已§c禁用§6，使用 /areamonitor toggle 重新启用";

        context.getSource().sendSuccess(
                () -> Component.literal(message),
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

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                () -> Component.literal("§6=== AreaMonitor 区域监控模组命令 ==="), false
        );

        // 基础命令
        context.getSource().sendSuccess(
                () -> Component.literal("§e=== 基础命令 ==="), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor toggle §f- 切换全局监控状态"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor help §f- 显示此帮助"), false
        );

        // 区域管理命令
        context.getSource().sendSuccess(
                () -> Component.literal("§e=== 区域管理 ==="), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor area create <名称> §f- 创建新区域"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor area delete <名称> §f- 删除区域"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor area list §f- 列出所有区域（详细信息）"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor area toggle <名称> §f- 启用/禁用指定区域"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor area info <名称> §f- 显示区域详情"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor area setEnterMode <区域> <模式> §f- 设置进入模式"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor area setLeaveMode <区域> <模式> §f- 设置离开模式"), false
        );

        // 可视化工具命令
        context.getSource().sendSuccess(
                () -> Component.literal("§e=== 可视化工具 ==="), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor visual tool §f- 获取区域选择工具"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor visual show <区域> §f- 显示区域边界"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor visual hide §f- 隐藏区域边界"), false
        );

        // 选择工具命令
        context.getSource().sendSuccess(
                () -> Component.literal("§e=== 选择工具 ==="), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor selection create <名称> §f- 从选择创建区域"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor selection cancel §f- 取消当前选择"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor selection info §f- 显示选择信息"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor selection tutorial §f- 显示使用教程"), false
        );

        // 白名单命令
        context.getSource().sendSuccess(
                () -> Component.literal("§e=== 白名单管理 ==="), false
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

        // 其他命令
        context.getSource().sendSuccess(
                () -> Component.literal("§e=== 其他命令 ==="), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor performance §f- 显示性能信息"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor blacklist info §f- 显示当前限制"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor blacklist area <区域> add <物品> §f- 添加物品到区域黑名单"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor blacklist area <区域> remove <物品> §f- 从区域黑名单移除物品"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor blacklist area <区域> list §f- 列出区域黑名单"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor blacklist area <区域> toggle §f- 切换区域黑名单"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor blacklist reload §f- 热加载黑名单配置"), false
        );

        // 配置管理命令
        context.getSource().sendSuccess(
                () -> Component.literal("§e=== 配置管理 ==="), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor config reload §f- 重新加载所有配置文件"), false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§b/areamonitor config generate §f- 生成缺失的配置文件"), false
        );

        return 1;
    }
}