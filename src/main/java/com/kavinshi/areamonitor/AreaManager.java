package com.kavinshi.areamonitor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区域管理器，负责管理所有监控区域
 */
public class AreaManager {
    private static AreaManager instance;
    private final Map<String, MonitorArea> areas = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerAreas = new ConcurrentHashMap<>();

    public static AreaManager getInstance() {
        if (instance == null) {
            instance = new AreaManager();
        }
        return instance;
    }

    private AreaManager() {}

    public void addArea(MonitorArea area) {
        areas.put(area.getName(), area);
        AreaMonitorMod.LOGGER.info("添加监控区域: {}", area.getName());
    }

    public void removeArea(String areaName) {
        areas.remove(areaName);
        AreaMonitorMod.LOGGER.info("移除监控区域: {}", areaName);
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
        if (WhitelistManager.isWhitelisted(player)) {
            return;
        }

        PlayerPosition position = new PlayerPosition(
            player.getX(),
            player.getZ(),
            player.level().dimension().location().toString()
        );

        Set<String> currentAreas = getCurrentAreas(player, position);
        Set<String> previousAreas = playerAreas.getOrDefault(player.getUUID(), new HashSet<>());

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

        playerAreas.put(player.getUUID(), currentAreas);
    }

    private Set<String> getCurrentAreas(ServerPlayer player, PlayerPosition position) {
        Set<String> currentAreas = new HashSet<>();

        for (MonitorArea area : areas.values()) {
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

        AreaMonitorMod.LOGGER.debug("玩家 {} 进入区域 {}", player.getName().getString(), areaName);

        // 显示进入消息
        showAreaMessage(player, area.getDisplayName(), true, area.getEnterMode());

        // 延迟切换游戏模式
        AreaMonitor.addPendingGameModeChange(player, area.getEnterMode());

        // 检查触发器
        checkTriggers(player, area, TriggerType.ENTER);
    }

    private void handleAreaLeave(ServerPlayer player, String areaName) {
        MonitorArea area = areas.get(areaName);
        if (area == null || !area.isEnabled()) return;

        AreaMonitorMod.LOGGER.debug("玩家 {} 离开区域 {}", player.getName().getString(), areaName);

        // 显示离开消息
        showAreaMessage(player, area.getDisplayName(), false, area.getLeaveMode());

        // 延迟切换游戏模式
        AreaMonitor.addPendingGameModeChange(player, area.getLeaveMode());

        // 检查触发器
        checkTriggers(player, area, TriggerType.LEAVE);
    }

    private void showAreaMessage(ServerPlayer player, String areaName, boolean entering, GameType gameMode) {
        // 只发送屏幕中央Title提示，移除聊天消息
        showAreaTitle(player, areaName, entering, gameMode);
    }

    private void showAreaTitle(ServerPlayer player, String areaName, boolean entering, GameType gameMode) {
        try {
            String action = entering ? "§a进入" : "§c离开";
            String modeName = getGameModeDisplayName(gameMode);
            // 使用 §7 (灰色) 和较小的格式，去掉粗体
            String message = "§7" + action + "区域: " + areaName + " | " + modeName;

            // 设置Title动画时间 (淡入, 停留, 淡出)
            int fadeIn = 5;   // 0.25秒淡入 (减小)
            int stay = 30;    // 1.5秒停留 (减小)
            int fadeOut = 5;  // 0.25秒淡出 (减小)

            player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
            // 只显示一行，作为Title显示
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(message)));
            // 不发送Subtitle，实现单行显示
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("发送Title提示时出错", e);
        }
    }

    private String getGameModeDisplayName(GameType gameMode) {
        return switch (gameMode) {
            case CREATIVE -> "创造模式";
            case ADVENTURE -> "冒险模式";
            case SPECTATOR -> "旁观模式";
            default -> "生存模式";
        };
    }

    private void checkTriggers(ServerPlayer player, MonitorArea area, TriggerType triggerType) {
        // 检查物品触发器
        if (area.getTriggers().isEnableItemTriggers()) {
            for (ItemTrigger trigger : area.getTriggers().getItemTriggers()) {
                if (trigger.getType() == triggerType && trigger.check(player)) {
                    trigger.execute(player, area);
                }
            }
        }

        // 检查玩家数量触发器
        if (area.getTriggers().isEnablePlayerCountTriggers()) {
            int playerCount = getPlayersInArea(area).size();
            for (PlayerCountTrigger trigger : area.getTriggers().getPlayerCountTriggers()) {
                if (trigger.getType() == triggerType && trigger.check(playerCount)) {
                    trigger.execute(player, area);
                }
            }
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
        long currentTime = System.currentTimeMillis();
        playerAreas.entrySet().removeIf(entry -> {
            // 这里可以添加更复杂的清理逻辑
            return false; // 暂时不清理
        });
    }

    public enum TriggerType {
        ENTER, LEAVE, PERIODIC, ITEM_HELD, PLAYER_COUNT
    }
}

// 基础触发器接口
interface Trigger {
    boolean check(ServerPlayer player);
    void execute(ServerPlayer player, MonitorArea area);
    AreaManager.TriggerType getType();
}

// 物品触发器实现
class ItemTrigger implements Trigger {
    private AreaManager.TriggerType type;
    private List<String> requiredItems;
    private TriggerCondition condition;
    private String actionCommand;

    @Override
    public boolean check(ServerPlayer player) {
        // 检查玩家是否持有指定物品
        return true; // 简化实现
    }

    @Override
    public void execute(ServerPlayer player, MonitorArea area) {
        // 执行触发动作
    }

    @Override
    public AreaManager.TriggerType getType() {
        return type;
    }

    public enum TriggerCondition {
        AND, OR
    }
}

// 玩家数量触发器实现
class PlayerCountTrigger implements Trigger {
    private AreaManager.TriggerType type;
    private int minPlayers;
    private int maxPlayers;
    private String actionCommand;

    public boolean check(int playerCount) {
        return playerCount >= minPlayers && playerCount <= maxPlayers;
    }

    @Override
    public boolean check(ServerPlayer player) {
        return false; // 这个触发器不检查单个玩家
    }

    @Override
    public void execute(ServerPlayer player, MonitorArea area) {
        // 执行触发动作
    }

    @Override
    public AreaManager.TriggerType getType() {
        return type;
    }
}