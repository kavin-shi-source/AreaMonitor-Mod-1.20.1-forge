package com.kavinshi.areamonitor;

import com.kavinshi.areamonitor.model.PlayerPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Area manager responsible for managing all monitored areas.
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class AreaManager {
    private static final AreaManager INSTANCE = new AreaManager();
    
    private static final int MAX_AREA_NAME_LENGTH = 32;
    private static final int MIN_AREA_SIZE = 1;
    private static final int MAX_AREA_SIZE = 1000000;
    private static final int MAX_AREAS_PER_PLAYER = 100;
    
    private static final int TITLE_FADE_IN_TICKS = 5;
    private static final int TITLE_STAY_TICKS = 30;
    private static final int TITLE_FADE_OUT_TICKS = 5;
    
    private static final Set<String> EMPTY_AREA_SET = Collections.emptySet();
    
    private final Map<String, MonitorArea> areas = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerAreas = new ConcurrentHashMap<>();
    private final SpatialPartitionManager spatialPartition = new SpatialPartitionManager();

    public static AreaManager getInstance() {
        return INSTANCE;
    }

    private AreaManager() {}

    public void addArea(MonitorArea area) {
        areas.put(area.getName(), area);
        spatialPartition.addRegion(area);
        AreaMonitorMod.LOGGER.info("添加监控区域: {}", area.getName());
    }

    public void removeArea(String areaName) {
        MonitorArea removed = areas.remove(areaName);
        if (removed != null) {
            spatialPartition.removeRegion(areaName);
            AreaMonitorMod.LOGGER.info("移除监控区域: {}", areaName);
        }
    }

    public MonitorArea getArea(String areaName) {
        return areas.get(areaName);
    }

    public Collection<MonitorArea> getAllAreas() {
        return areas.values();
    }

    public Set<String> getAreaNames() {
        return areas.keySet();
    }

    public void checkPlayer(ServerPlayer player) {
        if (ConfigManager.CONFIG.debugMode.get()) {
            AreaMonitorMod.LOGGER.debug("AreaManager: Checking player {}", player.getName().getString());
        }

        if (WhitelistManager.isWhitelisted(player)) {
            if (ConfigManager.CONFIG.debugMode.get()) {
                AreaMonitorMod.LOGGER.debug("AreaManager: Player {} is whitelisted, skipping", player.getName().getString());
            }
            return;
        }

        PlayerPosition position = new PlayerPosition(
            player.getX(),
            player.getZ(),
            player.level().dimension().location().toString()
        );

        Set<String> currentAreas = getCurrentAreasOptimized(player, position);
        Set<String> previousAreas = playerAreas.getOrDefault(player.getUUID(), EMPTY_AREA_SET);

        // 检测进入的新区域
        for (String areaName : currentAreas) {
            if (!previousAreas.contains(areaName)) {
                handleAreaEnter(player, areaName);
            }
        }

        // 检测离开的区域
        for (String areaName : previousAreas) {
            if (!currentAreas.contains(areaName)) {
                handleAreaLeave(player, areaName);
            }
        }

        // 只有当区域状态发生变化时才更新缓存
        if (!currentAreas.equals(previousAreas)) {
            playerAreas.put(player.getUUID(), new HashSet<>(currentAreas));
        }
    }

    /**
     * 使用空间分区优化的区域检测
     */
    private Set<String> getCurrentAreasOptimized(ServerPlayer player, PlayerPosition position) {
        Set<String> currentAreas = new HashSet<>();

        // 使用空间分区获取可能相关的区域
        Set<MonitorArea> potentialAreas = spatialPartition.getPotentialRegions(
            position.getX(), position.getZ(), position.getDimension()
        );

        // 只检查相关的区域
        for (MonitorArea area : potentialAreas) {
            if (area.isEnabled() && area.isPlayerInArea(position)) {
                currentAreas.add(area.getName());
            }
        }

        return currentAreas;
    }

    private void handleAreaEnter(ServerPlayer player, String areaName) {
        MonitorArea area = areas.get(areaName);
        if (area == null || !area.isEnabled()) return;

        // 检查区域白名单
        if (area.getWhitelist().contains(player.getName().getString().toLowerCase())) {
            return;
        }

        AreaMonitorMod.LOGGER.debug("Player {} entered area {}", player.getName().getString(), areaName);

        // 显示进入消息
        showAreaMessage(player, area.getDisplayName(), true, area.getEnterMode());

        // 延迟切换游戏模式
        AreaMonitor.addPendingGameModeChange(player, area.getEnterMode());

    }

    private void handleAreaLeave(ServerPlayer player, String areaName) {
        MonitorArea area = areas.get(areaName);
        if (area == null || !area.isEnabled()) return;

        AreaMonitorMod.LOGGER.debug("Player {} left area {}", player.getName().getString(), areaName);

        // 显示离开消息
        showAreaMessage(player, area.getDisplayName(), false, area.getLeaveMode());

        // 延迟切换游戏模式（离开区域）
        AreaMonitor.addPendingGameModeChangeOnLeave(player, area.getLeaveMode());

    }

    private void showAreaMessage(ServerPlayer player, String areaName, boolean entering, GameType gameMode) {
        // 只发送屏幕中央Title提示，移除聊天消息
        showAreaTitle(player, areaName, entering, gameMode);
    }

    private void showAreaTitle(ServerPlayer player, String areaName, boolean entering, GameType gameMode) {
        try {
            String action = entering ? LocalizationManager.translate("area.enter") : LocalizationManager.translate("area.leave");
            String modeName = LocalizationManager.getGameModeDisplayName(gameMode);
            // 使用 §7 (灰色) 和较小的格式，去掉粗体
            String message = String.format(LocalizationManager.translate("area.message"), action, areaName, modeName);

            player.connection.send(new ClientboundSetTitlesAnimationPacket(TITLE_FADE_IN_TICKS, TITLE_STAY_TICKS, TITLE_FADE_OUT_TICKS));
            // 只显示一行，作为Title显示
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(message)));
            // 不发送Subtitle，实现单行显示
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("发送Title提示时出错", e);
        }
    }


    private Set<ServerPlayer> getPlayersInArea(MonitorArea area) {
        Set<ServerPlayer> playersInArea = new HashSet<>();

        if (area == null) return playersInArea;

        // 获取当前服务器实例并检查所有在线玩家
        MinecraftServer server = AreaMonitor.getServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerPosition position = new PlayerPosition(
                    player.getX(),
                    player.getZ(),
                    player.level().dimension().location().toString()
                );

                if (area.isPlayerInArea(position)) {
                    playersInArea.add(player);
                }
            }
        }

        return playersInArea;
    }

    public Set<String> getCurrentAreas(ServerPlayer player) {
        return playerAreas.getOrDefault(player.getUUID(), new HashSet<>());
    }

    public void clearPlayerData(UUID playerId) {
        playerAreas.remove(playerId);
    }

    public void clearAllData() {
        playerAreas.clear();
    }

    public void clearUnusedCaches() {
        // 清理长时间未活动的玩家数据
        playerAreas.entrySet().removeIf(entry -> {
            // 这里可以添加更复杂的清理逻辑
            return false; // 暂时不清理
        });
    }

    /**
     * 重新加载所有区域到空间分区（用于区域配置更新后）
     */
    public void rebuildSpatialPartition() {
        spatialPartition.clear();
        for (MonitorArea area : areas.values()) {
            spatialPartition.addRegion(area);
        }
        AreaMonitorMod.LOGGER.info("空间分区已重建，共 {} 个区域", areas.size());
    }

    public enum TriggerType {
        ENTER, LEAVE, PERIODIC, ITEM_HELD, PLAYER_COUNT
    }
}