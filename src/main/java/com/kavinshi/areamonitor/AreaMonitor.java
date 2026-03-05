package com.kavinshi.areamonitor;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import java.io.File;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区域监控核心类，负责监控玩家位置并自动切换游戏模式
 * 主要功能：
 * - 定期检查玩家在目标维度中的位置
 * - 检测玩家进入/离开监控区域
 * - 延迟切换游戏模式并显示提示
 * - 管理白名单玩家
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AreaMonitor {
    private static final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();
    private static MinecraftServer minecraftServer;
    private static int tickCounter = 0;
    private static final int TITLE_FADE_IN_TICKS = 10;
    private static final int TITLE_STAY_TICKS = 30;
    private static final int TITLE_FADE_OUT_TICKS = 10;
    private static final long GAME_MODE_SWITCH_DELAY_MS = 1000L;

    private static class PlayerState {
        boolean wasInArea = false;
        int lastX = Integer.MAX_VALUE;
        int lastZ = Integer.MAX_VALUE;

        public boolean isInitialized() {
            return lastX != Integer.MAX_VALUE && lastZ != Integer.MAX_VALUE;
        }
    }

    private record PendingAction(UUID playerId, Runnable action, long executeTime) {
        // record类自动生成构造函数
        // executeTime应该是绝对时间戳（当前时间 + 延迟）
    }

    /**
     * 获取当前的Minecraft服务器实例
     */
    public static MinecraftServer getServer() {
        return minecraftServer;
    }

    private static final List<PendingAction> pendingActions = Collections.synchronizedList(new ArrayList<>());

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        minecraftServer = event.getServer();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // 服务器启动完成后，确保配置文件正确初始化
        AreaMonitorMod.LOGGER.info("Server started, initializing configuration files...");

        // 确保配置目录存在
        try {
            File configDir = new File("config/areamonitor");
            if (!configDir.exists()) {
                configDir.mkdirs();
                AreaMonitorMod.LOGGER.info("Created configuration directory: {}", configDir.getAbsolutePath());
            }
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("Error creating configuration directory", e);
        }

        // 加载或创建配置文件
        ConfigManager.loadAreasConfig();
        ItemBlacklistManager.loadBlacklistConfig();

        // 调试信息：显示加载的区域
        var areas = AreaManager.getInstance().getAllAreas();
        AreaMonitorMod.LOGGER.info("Loaded {} areas:", areas.size());
        for (var area : areas) {
            AreaMonitorMod.LOGGER.info("  - {} ({}): {}", area.getName(), area.getDimension(), area.getDisplayName());
        }

        AreaMonitorMod.LOGGER.info("Configuration files initialization completed");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        processPendingActions();

        // 性能监控
        PerformanceMonitor.onServerTick(minecraftServer);

        // 更新区域可视化
        AreaVisualizer.updatePersistentVisualizations();

        tickCounter++;
        if (tickCounter < PerformanceMonitor.getCurrentCheckInterval()) return;
        tickCounter = 0;

        if (!ConfigManager.CONFIG.isEnabled.get()) {
            AreaMonitorMod.LOGGER.debug("AreaMonitor: Mod is disabled, skipping player checks");
            return;
        }
        if (minecraftServer == null) {
            AreaMonitorMod.LOGGER.debug("AreaMonitor: MinecraftServer is null, skipping player checks");
            return;
        }

        // 使用新的多区域系统检查玩家
        var players = minecraftServer.getPlayerList().getPlayers();
        if (ConfigManager.CONFIG.debugMode.get()) {
            AreaMonitorMod.LOGGER.debug("AreaMonitor: Checking {} players", players.size());
        }
        for (ServerPlayer player : players) {
            AreaManager.getInstance().checkPlayer(player);
        }
    }

    private static void processPendingActions() {
        long currentTime = System.currentTimeMillis();
        Iterator<PendingAction> iterator = pendingActions.iterator();

        while (iterator.hasNext()) {
            PendingAction action = iterator.next();
            if (currentTime >= action.executeTime) {
                try {
                    action.action.run();
                } catch (Exception e) {
                    AreaMonitorMod.LOGGER.error("执行延迟动作时出错", e);
                }
                iterator.remove();
            }
        }
    }

    /**
     * 添加待处理的游戏模式切换（进入区域）
     */
    public static void addPendingGameModeChange(ServerPlayer player, GameType gameMode) {
        UUID playerId = player.getUUID();
        pendingActions.add(new PendingAction(playerId, () -> {
            // 检查玩家是否仍然存活且在线
            if (player.isAlive() && minecraftServer != null) {
                // 获取玩家当前位置并检查区域
                Set<String> currentAreas = AreaManager.getInstance().getCurrentAreas(player);

                // 只有在玩家仍在区域内时才切换模式
                if (!currentAreas.isEmpty()) {
                    if (ConfigManager.CONFIG.debugMode.get()) {
                        AreaMonitorMod.LOGGER.debug("AreaMonitor: Switching game mode for player {} to {}", player.getName().getString(), gameMode);
                    }
                    player.setGameMode(gameMode);

                    // 调试消息发送逻辑
                    boolean showMessages = ConfigManager.CONFIG.showMessages.get();
                    if (ConfigManager.CONFIG.debugMode.get()) {
                        AreaMonitorMod.LOGGER.debug("AreaMonitor: showMessages config = {}", showMessages);
                    }

                    if (showMessages) {
                        try {
                            String finalMessage = LocalizationManager.translate("area.gamemode_changed", LocalizationManager.getGameModeDisplayName(gameMode));

                            if (ConfigManager.CONFIG.debugMode.get()) {
                                AreaMonitorMod.LOGGER.debug("AreaMonitor: Sending message to player {}: {}", player.getName().getString(), finalMessage);
                            }
                            player.displayClientMessage(
                                Component.literal(finalMessage),
                                true
                            );
                            if (ConfigManager.CONFIG.debugMode.get()) {
                                AreaMonitorMod.LOGGER.debug("AreaMonitor: Message sent successfully");
                            }
                        } catch (Exception e) {
                            AreaMonitorMod.LOGGER.error("AreaMonitor: Error sending message to player {}", player.getName().getString(), e);
                        }
                    }
                }
            }
        }, System.currentTimeMillis() + GAME_MODE_SWITCH_DELAY_MS));
    }

    /**
     * 添加待处理的游戏模式切换（离开区域）
     */
    public static void addPendingGameModeChangeOnLeave(ServerPlayer player, GameType gameMode) {
        UUID playerId = player.getUUID();
        pendingActions.add(new PendingAction(playerId, () -> {
            // 检查玩家是否仍然存活且在线
            if (player.isAlive() && minecraftServer != null) {
                // 离开区域的模式切换不需要验证位置
                player.setGameMode(gameMode);
                if (ConfigManager.CONFIG.showMessages.get()) {
                    try {
                        String finalMessage = LocalizationManager.translate("area.gamemode_changed", LocalizationManager.getGameModeDisplayName(gameMode));
                        player.displayClientMessage(
                            Component.literal(finalMessage),
                            true
                        );
                    } catch (Exception e) {
                        AreaMonitorMod.LOGGER.error("AreaMonitor: Error sending leave message to player {}", player.getName().getString(), e);
                    }
                }
            }
        }, System.currentTimeMillis() + GAME_MODE_SWITCH_DELAY_MS));
    }

    private static void showTitle(ServerPlayer player, Component title, Component subtitle) {
        try {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(TITLE_FADE_IN_TICKS, TITLE_STAY_TICKS, TITLE_FADE_OUT_TICKS));

            if (title != null) {
                player.connection.send(new ClientboundSetTitleTextPacket(title));
            }

            if (subtitle != null) {
                player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            }
        } catch (Exception e) {
            // 静默处理标题发送错误，避免影响游戏体验
            // 常见原因：玩家断开连接、网络问题等
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            playerStates.remove(playerId);
            AreaManager.getInstance().clearPlayerData(playerId);

            synchronized (pendingActions) {
                pendingActions.removeIf(action -> action.playerId.equals(playerId));
            }
        }
    }
}