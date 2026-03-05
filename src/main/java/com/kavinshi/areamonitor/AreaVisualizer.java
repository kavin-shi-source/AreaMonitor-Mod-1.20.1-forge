package com.kavinshi.areamonitor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    }
}

/**
 * 选择工具系统
 */
class SelectionTool {
    private static final Map<UUID, SelectionPoints> playerSelections = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastHelpTimes = new ConcurrentHashMap<>();
    private static String getSelectionToolName() {
        return "§a" + LocalizationManager.translate("area.selection.tool_name") + " Tool";
    }

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
        if (nameString == null) return false;

        // 支持多语言的工具名称检查
        return nameString.contains("Area Selection") ||
               nameString.contains("区域选择") ||
               nameString.contains(LocalizationManager.translate("area.selection.tool_name"));
    }

    /**
     * 给予玩家选择工具
     */
    public static void giveSelectionTool(ServerPlayer player) {
        ItemStack tool = new ItemStack(Items.WOODEN_AXE); // 使用木斧作为选择工具
        tool.setHoverName(net.minecraft.network.chat.Component.literal(getSelectionToolName()));

        // 尝试添加到主手
        if (player.getInventory().getFreeSlot() != -1) {
            if (player.addItem(tool)) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("selection.tool.obtained"),
                    false
                );
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("selection.tool.instructions"),
                    false
                );
            }
        } else {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.tool.inventory_full"),
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
                net.minecraft.network.chat.Component.translatable("selection.tool.no_permission"),
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
                net.minecraft.network.chat.Component.translatable("selection.mode.header").withStyle(net.minecraft.ChatFormatting.BOLD),
                false
            );
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.first_point_set", pos.getX(), pos.getY(), pos.getZ()),
                false
            );
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.set_second_point"),
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
                    net.minecraft.network.chat.Component.translatable("selection.distance_too_far"),
                    true
                );
                return;
            }

            // 检查区域大小
            long area = Math.abs((long)(pos.getX() - firstPos.getX()) * (pos.getZ() - firstPos.getZ()));
            if (area > 1000000) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(LocalizationManager.translate("area.error.too_large", area)),
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
            net.minecraft.network.chat.Component.translatable("selection.complete.header").withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.point.first", pos1.getX(), pos1.getY(), pos1.getZ()),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.point.second", pos2.getX(), pos2.getY(), pos2.getZ()),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.area.bounds", minX, maxX, minZ, maxZ),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.area.size", width, height, area),
            false
        );

        // 根据区域大小给出不同提示
        if (area > 1000000) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.warning.area_too_large"),
                false
            );
        } else if (area > 100000) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.note.area_large"),
                false
            );
        } else {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.area.size_ok"),
                false
            );
        }

        // 显示创建命令提示
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.instructions.create_or_cancel"),
            false
        );

        // 显示当前维度信息
        String currentDimension = player.level().dimension().location().toString();
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.dimension.current", getDimensionDisplayName(currentDimension)),
            false
        );
    }

    /**
     * 获取维度的显示名称
     */
    private static String getDimensionDisplayName(String dimension) {
        return switch (dimension.toLowerCase()) {
            case "minecraft:overworld" -> LocalizationManager.translate("dimension.minecraft.overworld");
            case "minecraft:the_nether" -> LocalizationManager.translate("dimension.minecraft.the_nether");
            case "minecraft:the_end" -> LocalizationManager.translate("dimension.minecraft.the_end");
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
                net.minecraft.network.chat.Component.translatable("selection.error.no_points"),
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
            net.minecraft.network.chat.Component.translatable("selection.area.created", areaName).withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );

        // 显示区域信息
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.area.dimension", getDimensionDisplayName(currentDimension)),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.area.range",
                Math.min(pos1.getX(), pos2.getX()) + ", " + Math.max(pos1.getX(), pos2.getX()) + "] × [" +
                Math.min(pos1.getZ(), pos2.getZ()) + ", " + Math.max(pos1.getZ(), pos2.getZ()) + "]"),
            false
        );

        // 引导设置进出模式
        showModeSetupGuide(player);

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
    private static void showModeSetupGuide(ServerPlayer player) {
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.mode.setup_header").withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.mode.setup_instructions"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.mode.available_modes"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(""),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.mode.setup_complete"),
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
                net.minecraft.network.chat.Component.translatable("selection.cancelled"),
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
                net.minecraft.network.chat.Component.translatable("selection.no_active_selection"),
                true
            );
            return;
        }

        if (selection.hasFirstPoint()) {
            BlockPos pos1 = selection.getFirstPoint();
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.current.first_point",
                    pos1.getX(), pos1.getY(), pos1.getZ()),
                false
            );
        }

        if (selection.hasSecondPoint()) {
            BlockPos pos2 = selection.getSecondPoint();
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.current.second_point",
                    pos2.getX(), pos2.getY(), pos2.getZ()),
                false
            );
        } else {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.need_second_point"),
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