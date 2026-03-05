package com.kavinshi.areamonitor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraftforge.items.ItemHandlerHelper;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区域可视化系统，负责显示区域边界和效果
 */
public class AreaVisualizer {
    private static final double PARTICLE_SPACING = 1.0;
    private static final int VISUALIZATION_DURATION = 100; // ticks
    private static final Map<UUID, VisualizationData> activeVisualizations = new ConcurrentHashMap<>();

    /**
     * 显示区域边界
     */
    public static void showAreaBorder(ServerPlayer player, MonitorArea area) {
        if (area.getBounds() instanceof MonitorArea.RectangleBounds) {
            showRectangleBorder(player, (MonitorArea.RectangleBounds) area.getBounds());
        } else if (area.getBounds() instanceof MonitorArea.CircleBounds) {
            showCircleBorder(player, (MonitorArea.CircleBounds) area.getBounds());
        }
    }

    /**
     * 显示矩形区域边界
     */
    private static void showRectangleBorder(ServerPlayer player, MonitorArea.RectangleBounds bounds) {
        Level level = player.level();
        double y = player.getY();

        // 显示四条边的粒子效果
        showHorizontalLine(level, bounds.getMinX(), bounds.getMaxX(), bounds.getMinZ(), y, ParticleTypes.END_ROD);
        showHorizontalLine(level, bounds.getMinX(), bounds.getMaxX(), bounds.getMaxZ(), y, ParticleTypes.END_ROD);
        showHorizontalLine(level, bounds.getMinZ(), bounds.getMaxZ(), bounds.getMinX(), y, ParticleTypes.END_ROD);
        showHorizontalLine(level, bounds.getMinZ(), bounds.getMaxZ(), bounds.getMaxX(), y, ParticleTypes.END_ROD);
    }

    /**
     * 显示圆形区域边界
     */
    private static void showCircleBorder(ServerPlayer player, MonitorArea.CircleBounds bounds) {
        Level level = player.level();
        double y = player.getY();
        double centerX = bounds.getCenterX();
        double centerZ = bounds.getCenterZ();
        double radius = bounds.getRadius();

        // 生成圆形边界点
        int segments = (int) (radius * 2 * Math.PI / PARTICLE_SPACING);
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double x = centerX + radius * Math.cos(angle);
            double z = centerZ + radius * Math.sin(angle);
            spawnParticle(level, x, y, z, ParticleTypes.END_ROD);
        }
    }

    /**
     * 显示水平线段
     */
    private static void showHorizontalLine(Level level, double start, double end, double fixed, double y, ParticleOptions particle) {
        double step = start < end ? PARTICLE_SPACING : -PARTICLE_SPACING;
        for (double pos = start; pos <= end; pos += step) {
            spawnParticle(level, pos, y, fixed, particle);
        }
    }

    /**
     * 生成粒子效果
     */
    public static void spawnParticle(Level level, double x, double y, double z, ParticleOptions particle) {
        if (!level.isClientSide()) {
            // 服务器端发送粒子包给附近玩家
            for (net.minecraft.world.entity.player.Player p : level.players()) {
                if (!(p instanceof ServerPlayer player)) continue;
                if (player.distanceToSqr(x, y, z) <= 1024) { // 32 blocks range
                    player.connection.send(new ClientboundLevelParticlesPacket(
                        particle, false, x, y, z, 0, 0, 0, 0, 1
                    ));
                }
            }
        }
    }

    /**
     * 开始持续显示区域
     */
    public static void startPersistentVisualization(ServerPlayer player, MonitorArea area) {
        UUID playerId = player.getUUID();
        VisualizationData data = new VisualizationData(area, System.currentTimeMillis(), playerId);
        activeVisualizations.put(playerId, data);
    }

    /**
     * 停止持续显示
     */
    public static void stopPersistentVisualization(ServerPlayer player) {
        activeVisualizations.remove(player.getUUID());
    }

    /**
     * 更新持续显示
     */
    public static void updatePersistentVisualizations() {
        long currentTime = System.currentTimeMillis();

        activeVisualizations.entrySet().removeIf(entry -> {
            VisualizationData data = entry.getValue();
            if (currentTime - data.startTime > VISUALIZATION_DURATION * 50) { // 50ms per tick
                return true;
            }

            // 通过AreaMonitor获取服务器实例并查找玩家
            ServerPlayer player = findPlayerByUUID(entry.getKey());
            if (player != null && player.isAlive()) {
                showAreaBorder(player, data.area);
            }
            return false;
        });
    }

    private static ServerPlayer findPlayerByUUID(UUID playerId) {
        try {
            MinecraftServer server = AreaMonitor.getServer();
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (player.getUUID().equals(playerId)) {
                        return player;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    /**
     * 显示选择区域（两个点）
     */
    public static void showSelection(ServerPlayer player, BlockPos pos1, BlockPos pos2) {
        Level level = player.level();
        double y = player.getY();

        // 显示两个选择点
        spawnParticle(level, pos1.getX() + 0.5, y, pos1.getZ() + 0.5, ParticleTypes.ANGRY_VILLAGER);
        spawnParticle(level, pos2.getX() + 0.5, y, pos2.getZ() + 0.5, ParticleTypes.ANGRY_VILLAGER);

        // 显示连接线
        if (pos1 != null && pos2 != null) {
            showLineBetween(level, pos1, pos2, y, ParticleTypes.END_ROD);
        }
    }

    /**
     * 显示两点之间的线段
     */
    private static void showLineBetween(Level level, BlockPos pos1, BlockPos pos2, double y, ParticleOptions particle) {
        double dx = pos2.getX() - pos1.getX();
        double dz = pos2.getZ() - pos1.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        int steps = (int) (distance / PARTICLE_SPACING);

        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            double x = pos1.getX() + dx * ratio;
            double z = pos1.getZ() + dz * ratio;
            spawnParticle(level, x + 0.5, y, z + 0.5, particle);
        }
    }

    /**
     * 可视化数据类
     */
    private static class VisualizationData {
        final MonitorArea area;
        final long startTime;
        final UUID playerId;

        VisualizationData(MonitorArea area, long startTime, UUID playerId) {
            this.area = area;
            this.startTime = startTime;
            this.playerId = playerId;
        }

        ServerPlayer getPlayer() {
            // 通过UUID获取玩家 - 这里需要外部传入服务器实例
            return null; // 暂时返回null，将在updatePersistentVisualizations中处理
        }
    }
}

/**
 * 选择工具系统
 */
class SelectionTool {
    private static final Map<UUID, SelectionPoints> playerSelections = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastHelpTimes = new ConcurrentHashMap<>();
    private static final String SELECTION_TOOL_NAME = "§a区域选择工具";

    /**
     * 检查玩家是否持有选择工具
     */
    public static boolean isHoldingSelectionTool(ServerPlayer player) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return false;

        // 检查显示名称
        net.minecraft.network.chat.Component name = heldItem.getHoverName();
        if (name == null) return false;

        String nameString = name.getString();
        return nameString != null && nameString.contains("区域选择");
    }

    /**
     * 给予玩家选择工具
     */
    public static void giveSelectionTool(ServerPlayer player) {
        ItemStack tool = new ItemStack(Items.WOODEN_AXE); // 使用木斧作为选择工具
        tool.setHoverName(net.minecraft.network.chat.Component.literal(SELECTION_TOOL_NAME));

        // 尝试添加到主手
        if (player.getInventory().getFreeSlot() != -1) {
            if (player.addItem(tool)) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§a已获得区域选择工具"),
                    false
                );
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§e使用方法: 手持工具右键点击方块设置选择点"),
                    false
                );
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§e选择两个点后使用: §b/areamonitor selection create <名称>"),
                    false
                );
            }
        } else {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§c背包已满，无法获得选择工具"),
                true
            );
        }
    }

    /**
     * 处理玩家交互事件
     */
    public static void handlePlayerInteract(ServerPlayer player, BlockPos pos, InteractionHand hand) {
        if (!isHoldingSelectionTool(player) || hand != InteractionHand.MAIN_HAND) {
            return;
        }

        // 检查权限
        if (!player.hasPermissions(2)) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§c你没有权限使用选择工具"),
                true
            );
            return;
        }

        SelectionPoints selection = playerSelections.computeIfAbsent(
            player.getUUID(),
            k -> new SelectionPoints()
        );

        if (!selection.hasFirstPoint()) {
            // 设置第一个点
            selection.setFirstPoint(pos);

            // 显示详细信息
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§6=== 区域选择模式 ===").withStyle(net.minecraft.ChatFormatting.BOLD),
                false
            );
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§a第一个点已设置: [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"),
                false
            );
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§e现在请设置对角点（右键点击另一个方块）"),
                false
            );

            // 显示选择标记
            showSelectionMarker(player, pos);

            // 播放提示音效
            player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),
                                  net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 2.0f);

        } else {
            // 检查距离
            BlockPos firstPos = selection.getFirstPoint();
            double distance = Math.sqrt(
                Math.pow(pos.getX() - firstPos.getX(), 2) +
                Math.pow(pos.getZ() - firstPos.getZ(), 2)
            );

            if (distance > 1000) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§c选择的点距离太远（最大1000格），请重新选择"),
                    true
                );
                return;
            }

            // 检查区域大小
            long area = Math.abs((long)(pos.getX() - firstPos.getX()) * (pos.getZ() - firstPos.getZ()));
            if (area > 1000000) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§c区域过大（" + area + "方块），最大允许1000x1000"),
                    true
                );
                return;
            }

            selection.setSecondPoint(pos);

            // 显示选择区域
            AreaVisualizer.showSelection(player, selection.getFirstPoint(), selection.getSecondPoint());

            // 显示区域信息
            showSelectionInfo(player, selection);

            // 播放成功音效
            player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),
                                  net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    /**
     * 显示选择标记
     */
    private static void showSelectionMarker(ServerPlayer player, BlockPos pos) {
        Level level = player.level();
        // 在选择的方块上方显示粒子柱
        for (int y = pos.getY(); y <= pos.getY() + 3; y++) {
            AreaVisualizer.spawnParticle(level,
                pos.getX() + 0.5, y, pos.getZ() + 0.5,
                ParticleTypes.END_ROD
            );
        }

        // 在方块周围显示边框
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            double x = pos.getX() + 0.5 + Math.cos(angle) * 0.6;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * 0.6;
            AreaVisualizer.spawnParticle(level, x, pos.getY() + 0.5, z, ParticleTypes.END_ROD);
        }
    }

    /**
     * 显示选择信息
     */
    private static void showSelectionInfo(ServerPlayer player, SelectionPoints selection) {
        BlockPos pos1 = selection.getFirstPoint();
        BlockPos pos2 = selection.getSecondPoint();

        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        long area = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        int width = maxX - minX + 1;
        int height = maxZ - minZ + 1;

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§6=== 选择完成！区域信息 ===").withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§e第一个点: [" + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ() + "]"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§e第二个点: [" + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ() + "]"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§e区域范围: X[" + minX + " ~ " + maxX + "], Z[" + minZ + " ~ " + maxZ + "]"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§e尺寸: " + width + " × " + height + " = §b" + area + " 方块"),
            false
        );

        // 根据区域大小给出不同提示
        if (area > 1000000) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§c⚠ 警告: 区域过大，可能影响服务器性能"),
                false
            );
        } else if (area > 100000) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§e⚠ 注意: 区域较大，建议适当减小范围"),
                false
            );
        } else {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§a✓ 区域大小合适"),
                false
            );
        }

        // 显示创建命令提示
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§6使用方法:"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§b/areamonitor selection create <区域名称> §e- 创建区域"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§b/areamonitor selection cancel §e- 取消选择"),
            false
        );

        // 显示当前维度信息
        String currentDimension = player.level().dimension().location().toString();
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§e当前维度: §b" + getDimensionDisplayName(currentDimension)),
            false
        );
    }

    /**
     * 获取维度的显示名称
     */
    private static String getDimensionDisplayName(String dimension) {
        return switch (dimension.toLowerCase()) {
            case "minecraft:overworld" -> "主世界";
            case "minecraft:the_nether" -> "下界";
            case "minecraft:the_end" -> "末地";
            default -> dimension;
        };
    }

    /**
     * 创建区域
     */
    public static void createAreaFromSelection(ServerPlayer player, String areaName) {
        SelectionPoints selection = playerSelections.get(player.getUUID());
        if (selection == null || !selection.isComplete()) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§c请先选择两个点"),
                true
            );
            return;
        }

        BlockPos pos1 = selection.getFirstPoint();
        BlockPos pos2 = selection.getSecondPoint();

        // 自动检测当前维度
        String currentDimension = player.level().dimension().location().toString();

        MonitorArea area = new MonitorArea(areaName);
        area.setDisplayName(areaName); // 默认显示名称
        area.setDimension(currentDimension); // 自动设置当前维度
        area.setBounds(new MonitorArea.RectangleBounds(
            pos1.getX(), pos1.getZ(),
            pos2.getX(), pos2.getZ()
        ));

        AreaManager.getInstance().addArea(area);

        // 显示成功消息
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§a✓ 区域 '" + areaName + "' 创建成功!").withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );

        // 显示区域信息
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§e维度: §b" + getDimensionDisplayName(currentDimension)),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§e范围: §b[" +
                Math.min(pos1.getX(), pos2.getX()) + ", " + Math.max(pos1.getX(), pos2.getX()) + "] × [" +
                Math.min(pos1.getZ(), pos2.getZ()) + ", " + Math.max(pos1.getZ(), pos2.getZ()) + "]"),
            false
        );

        // 引导设置进出模式
        showModeSetupGuide(player, areaName);

        // 保存配置
        ConfigManager.saveAreasConfig();

        // 清理选择
        playerSelections.remove(player.getUUID());

        // 播放成功音效
        player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),
                              net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 2.0f);
    }

    /**
     * 显示模式设置引导
     */
    private static void showModeSetupGuide(ServerPlayer player, String areaName) {
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§6=== 设置区域模式 ===").withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§e现在请设置玩家进入和离开该区域时的游戏模式:"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§b可用模式: §asurvival§b, §acreative§b, §adventure§b, §bspectator"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(""),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§6设置进入模式 (玩家进入区域时的模式):"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§b/areamonitor area setEnterMode " + areaName + " <模式>"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§e例如: §b/areamonitor area setEnterMode " + areaName + " creative"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(" "),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§6设置离开模式 (玩家离开区域时的模式):"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§b/areamonitor area setLeaveMode " + areaName + " <模式>"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§e例如: §b/areamonitor area setLeaveMode " + areaName + " survival"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(" "),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§a💡 小贴士: 创意区域通常设置为进入创造模式，离开生存模式"),
            false
        );
    }

    /**
     * 取消选择
     */
    public static void cancelSelection(ServerPlayer player) {
        SelectionPoints selection = playerSelections.remove(player.getUUID());
        if (selection != null) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§e选择已取消"),
                true
            );
        }
    }

    /**
     * 获取最后帮助时间
     */
    public static Long getLastHelpTime(UUID playerId) {
        return lastHelpTimes.get(playerId);
    }

    /**
     * 设置最后帮助时间
     */
    public static void setLastHelpTime(UUID playerId, long time) {
        lastHelpTimes.put(playerId, time);
    }

    /**
     * 获取当前选择信息
     */
    public static void showCurrentSelection(ServerPlayer player) {
        SelectionPoints selection = playerSelections.get(player.getUUID());
        if (selection == null) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§e当前没有进行中的选择"),
                true
            );
            return;
        }

        if (selection.hasFirstPoint()) {
            BlockPos pos1 = selection.getFirstPoint();
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(String.format("§e第一个点: [%d, %d, %d]",
                    pos1.getX(), pos1.getY(), pos1.getZ())),
                false
            );
        }

        if (selection.hasSecondPoint()) {
            BlockPos pos2 = selection.getSecondPoint();
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(String.format("§e第二个点: [%d, %d, %d]",
                    pos2.getX(), pos2.getY(), pos2.getZ())),
                false
            );
        } else {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§e请设置第二个点"),
                false
            );
        }
    }
}

/**
 * 选择点数据类
 */
class SelectionPoints {
    private BlockPos firstPoint;
    private BlockPos secondPoint;

    public boolean hasFirstPoint() {
        return firstPoint != null;
    }

    public boolean hasSecondPoint() {
        return secondPoint != null;
    }

    public boolean isComplete() {
        return firstPoint != null && secondPoint != null;
    }

    public BlockPos getFirstPoint() {
        return firstPoint;
    }

    public BlockPos getSecondPoint() {
        return secondPoint;
    }

    public void setFirstPoint(BlockPos firstPoint) {
        this.firstPoint = firstPoint;
    }

    public void setSecondPoint(BlockPos secondPoint) {
        this.secondPoint = secondPoint;
    }
}