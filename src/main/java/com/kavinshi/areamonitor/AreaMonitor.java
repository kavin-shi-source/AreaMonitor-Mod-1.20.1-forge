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
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Area monitoring core class responsible for monitoring player positions
 * and automatically switching game modes.
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AreaMonitor {
    private static final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();
    private static volatile MinecraftServer minecraftServer;
    private static final AtomicInteger tickCounter = new AtomicInteger(0);
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

    private static final Queue<PendingAction> pendingActions = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        minecraftServer = event.getServer();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        AreaMonitorMod.LOGGER.info("Server stopping, cleaning up resources...");
        
        pendingActions.clear();
        playerStates.clear();
        tickCounter.set(0);
        
        minecraftServer = null;
        
        AreaMonitorMod.LOGGER.info("AreaMonitor cleanup completed");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        processPendingActions();

        PerformanceMonitor.onServerTick(minecraftServer);

        AreaVisualizer.updatePersistentVisualizations();

        int currentTick = tickCounter.incrementAndGet();
        if (currentTick < PerformanceMonitor.getCurrentCheckInterval()) return;
        tickCounter.set(0);

        if (!ConfigManager.CONFIG.isEnabled.get()) {
            AreaMonitorMod.LOGGER.debug("AreaMonitor: Mod is disabled, skipping player checks");
            return;
        }
        if (minecraftServer == null) {
            AreaMonitorMod.LOGGER.debug("AreaMonitor: MinecraftServer is null, skipping player checks");
            return;
        }

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
                    AreaMonitorMod.LOGGER.error("Error executing pending action for player {}", action.playerId, e);
                }
                iterator.remove();
            }
        }
    }

    /**
     * 添加待处理的游戏模式切换（进入区域）
     * 注意：使用 playerId 而非 player 引用，避免玩家下线后访问无效对象
     */
    public static void addPendingGameModeChange(ServerPlayer player, GameType gameMode) {
        UUID playerId = player.getUUID();
        GameType targetMode = gameMode;
        
        pendingActions.add(new PendingAction(playerId, () -> {
            if (minecraftServer == null) return;
            
            ServerPlayer currentPlayer = minecraftServer.getPlayerList().getPlayer(playerId);
            if (currentPlayer == null || !currentPlayer.isAlive()) return;
            
            Set<String> currentAreas = AreaManager.getInstance().getCurrentAreas(currentPlayer);
            
            if (!currentAreas.isEmpty()) {
                if (ConfigManager.CONFIG.debugMode.get()) {
                    AreaMonitorMod.LOGGER.debug("AreaMonitor: Switching game mode for player {} to {}", 
                        currentPlayer.getName().getString(), targetMode);
                }
                currentPlayer.setGameMode(targetMode);

                boolean showMessages = ConfigManager.CONFIG.showMessages.get();
                if (ConfigManager.CONFIG.debugMode.get()) {
                    AreaMonitorMod.LOGGER.debug("AreaMonitor: showMessages config = {}", showMessages);
                }

                if (showMessages) {
                    try {
                        String finalMessage = LocalizationManager.translate("area.gamemode_changed", 
                            LocalizationManager.getGameModeDisplayName(targetMode));

                        if (ConfigManager.CONFIG.debugMode.get()) {
                            AreaMonitorMod.LOGGER.debug("AreaMonitor: Sending message to player {}: {}", 
                                currentPlayer.getName().getString(), finalMessage);
                        }
                        currentPlayer.displayClientMessage(
                            Component.literal(finalMessage),
                            true
                        );
                        if (ConfigManager.CONFIG.debugMode.get()) {
                            AreaMonitorMod.LOGGER.debug("AreaMonitor: Message sent successfully");
                        }
                    } catch (Exception e) {
                        AreaMonitorMod.LOGGER.error("AreaMonitor: Error sending message to player {}", 
                            currentPlayer.getName().getString(), e);
                    }
                }
            }
        }, System.currentTimeMillis() + GAME_MODE_SWITCH_DELAY_MS));
    }

    /**
     * 添加待处理的游戏模式切换（离开区域）
     * 注意：使用 playerId 而非 player 引用，避免玩家下线后访问无效对象
     */
    public static void addPendingGameModeChangeOnLeave(ServerPlayer player, GameType gameMode) {
        UUID playerId = player.getUUID();
        GameType targetMode = gameMode;
        
        pendingActions.add(new PendingAction(playerId, () -> {
            if (minecraftServer == null) return;
            
            ServerPlayer currentPlayer = minecraftServer.getPlayerList().getPlayer(playerId);
            if (currentPlayer == null) return;
            
            currentPlayer.setGameMode(targetMode);
            if (ConfigManager.CONFIG.showMessages.get()) {
                try {
                    String finalMessage = LocalizationManager.translate("area.gamemode_changed", 
                        LocalizationManager.getGameModeDisplayName(targetMode));
                    currentPlayer.displayClientMessage(
                        Component.literal(finalMessage),
                        true
                    );
                } catch (Exception e) {
                    AreaMonitorMod.LOGGER.error("AreaMonitor: Error sending leave message to player {}", 
                        currentPlayer.getName().getString(), e);
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
            AreaMonitorMod.LOGGER.debug("Failed to show title to player {}: {}", 
                player.getName().getString(), e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            playerStates.remove(playerId);
            AreaManager.getInstance().clearPlayerData(playerId);
            
            pendingActions.removeIf(action -> action.playerId.equals(playerId));
            
            // Clean up visualization data
            AreaVisualizer.stopPersistentVisualization(player);
            SelectionTool.cleanupPlayerData(playerId);
        }
    }
}