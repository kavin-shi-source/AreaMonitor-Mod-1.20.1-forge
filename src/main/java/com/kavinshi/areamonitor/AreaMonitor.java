package com.kavinshi.areamonitor;

import net.minecraft.network.chat.Component;
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Area monitoring core class responsible for monitoring player positions
 * and automatically switching game modes.
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AreaMonitor {
    private static volatile MinecraftServer minecraftServer;
    private static final AtomicInteger tickCounter = new AtomicInteger(0);
    private static final AtomicInteger scheduleTickCounter = new AtomicInteger(0);
    private static final AtomicInteger visualizationTickCounter = new AtomicInteger(0);
    private static final AtomicInteger cacheCleanupTickCounter = new AtomicInteger(0);
    private static final AtomicLong monotonicTickCounter = new AtomicLong(0);
    private static final int VISUALIZATION_INTERVAL_TICKS = 5;
    private static final int CACHE_CLEANUP_INTERVAL_TICKS = 6000; // 5 min
    // P2 #10 fix: pending actions are now timed in ticks rather than wall-clock milliseconds,
    // so they survive server pauses (single-player pause, integrated-server tick halt) without
    // being expired or executed prematurely. 10s @ 20 TPS = 200 ticks.
    private static final long PENDING_ACTION_TIMEOUT_TICKS = 200L;

    private record PendingAction(UUID playerId, Runnable action, long executeTick) {
        // executeTick is an absolute tick index (monotonicTickCounter value at scheduled time)
    }

    /**
     * Get the current Minecraft server instance.
     */
    public static MinecraftServer getServer() {
        return minecraftServer;
    }

    private static final Queue<PendingAction> pendingActions = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        minecraftServer = event.getServer();
        ConfigManager.ensureConfigFiles();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // P2 #11 fix: defensive — wrap each step in try-catch so a failure does not skip
        // subsequent cleanup or propagate to other Forge handlers.
        AreaMonitorMod.LOGGER.info("Server stopping, cleaning up runtime state...");

        try {
            pendingActions.clear();
        } catch (Exception ex) {
            AreaMonitorMod.LOGGER.error("AreaMonitor: failed to clear pending actions on shutdown", ex);
        }
        try {
            tickCounter.set(0);
            monotonicTickCounter.set(0);
        } catch (Exception ex) {
            AreaMonitorMod.LOGGER.error("AreaMonitor: failed to reset tick counters on shutdown", ex);
        }
        try {
            AreaTriggerManager.clearAll();
        } catch (Exception ex) {
            AreaMonitorMod.LOGGER.error("AreaMonitor: failed to clear trigger manager on shutdown", ex);
        }

        minecraftServer = null;

        AreaMonitorMod.LOGGER.info("AreaMonitor runtime cleanup completed");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        long monotonicTick = monotonicTickCounter.incrementAndGet();
        AreaTriggerManager.setCurrentTick(monotonicTick);

        AreaTriggerManager.clearTickLocks();

        AreaTriggerManager.processDebouncedTriggers(minecraftServer);

        processPendingActions();

        PerformanceMonitor.onServerTick(minecraftServer);

        if (visualizationTickCounter.incrementAndGet() >= VISUALIZATION_INTERVAL_TICKS) {
            visualizationTickCounter.set(0);
            AreaVisualizer.updatePersistentVisualizations();
        }

        if (scheduleTickCounter.incrementAndGet() >= 20) {
            scheduleTickCounter.set(0);
            processSchedules();
        }

        if (cacheCleanupTickCounter.incrementAndGet() >= CACHE_CLEANUP_INTERVAL_TICKS) {
            cacheCleanupTickCounter.set(0);
            AreaManager.getInstance().clearUnusedCaches();
        }

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

        List<ServerPlayer> players = minecraftServer.getPlayerList().getPlayers();
        if (ConfigManager.CONFIG.debugMode.get()) {
            AreaMonitorMod.LOGGER.debug("AreaMonitor: Checking {} players", players.size());
        }
        for (ServerPlayer player : players) {
            AreaManager.getInstance().checkPlayer(player);
        }
    }

    /**
     * Process pending actions and clean up expired ones.
     * This prevents memory leaks from actions that never execute.
     */
    private static void processPendingActions() {
        long currentTick = monotonicTickCounter.get();
        Iterator<PendingAction> iterator = pendingActions.iterator();

        while (iterator.hasNext()) {
            PendingAction action = iterator.next();

            // Remove expired actions (timeout after PENDING_ACTION_TIMEOUT_TICKS)
            if (currentTick - action.executeTick > PENDING_ACTION_TIMEOUT_TICKS) {
                AreaMonitorMod.LOGGER.debug("Removing expired pending action for player {}", action.playerId);
                iterator.remove();
                continue;
            }

            // Execute actions that are ready
            if (currentTick >= action.executeTick) {
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
     * Enqueue a game mode change with configurable delay.
     */
    private static void enqueueGameModeChange(UUID playerId, GameType gameMode, boolean requireAreaCheck) {
        // P2 #10 fix: schedule based on tick count, not wall-clock time, so a server pause
        // (single-player menu, integrated-server tick halt) doesn't fire the action prematurely
        // or expire it during the pause window.
        long delayTicks = Math.max(1L, ConfigManager.CONFIG.gameModeSwitchDelayMs.get() / 50L);
        long executeTick = monotonicTickCounter.get() + delayTicks;
        pendingActions.add(new PendingAction(playerId, () -> {
            if (minecraftServer == null) return;

            ServerPlayer currentPlayer = minecraftServer.getPlayerList().getPlayer(playerId);
            if (currentPlayer == null || !currentPlayer.isAlive()) return;

            // For enter: only apply if player is still in at least one monitored area
            if (requireAreaCheck && AreaManager.getInstance().getCurrentAreas(currentPlayer).isEmpty()) {
                return;
            }

            if (ConfigManager.CONFIG.debugMode.get()) {
                AreaMonitorMod.LOGGER.debug("AreaMonitor: Switching game mode for player {} to {}",
                    currentPlayer.getName().getString(), gameMode);
            }
            currentPlayer.setGameMode(gameMode);

            if (ConfigManager.CONFIG.showMessages.get()) {
                try {
                    String finalMessage = LocalizationManager.translate("area.gamemode_changed",
                        LocalizationManager.getGameModeDisplayName(gameMode));
                    currentPlayer.displayClientMessage(Component.literal(finalMessage), true);
                } catch (Exception e) {
                    AreaMonitorMod.LOGGER.error("AreaMonitor: Error sending message to player {}",
                        currentPlayer.getName().getString(), e);
                }
            }
        }, executeTick));
    }

    /**
     * Add pending game mode change (entering area).
     */
    public static void addPendingGameModeChange(ServerPlayer player, GameType gameMode) {
        enqueueGameModeChange(player.getUUID(), gameMode, true);
    }

    /**
     * Add pending game mode change (leaving area).
     */
    public static void addPendingGameModeChangeOnLeave(ServerPlayer player, GameType gameMode) {
        enqueueGameModeChange(player.getUUID(), gameMode, false);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            AreaManager.getInstance().clearPlayerData(playerId);
            AreaTriggerManager.clearPlayer(playerId);

            pendingActions.removeIf(action -> action.playerId.equals(playerId));

            AreaVisualizer.stopPersistentVisualization(player);
            SelectionTool.cleanupPlayerData(playerId);
        }
    }

    /**
     * Process area time-based schedules: auto-enable/disable areas based on game time.
     */
    private static void processSchedules() {
        if (minecraftServer == null) return;
        var overworld = minecraftServer.overworld();
        if (overworld == null) return;
        long gameTime = overworld.getDayTime();
        AreaManager am = AreaManager.getInstance();
        for (MonitorArea area : am.getAllAreas()) {
            if (!area.isScheduleEnabled()) continue;
            boolean shouldEnable = area.evaluateSchedule(gameTime) && area.evaluateCondition(minecraftServer);
            if (shouldEnable && !area.isEnabled()) {
                // Schedule says enable, but area is disabled — and wasn't disabled by schedule
                if (!area.isScheduleWasDisabledBySchedule()) {
                    area.setEnabled(true);
                    AreaMonitorMod.LOGGER.debug("Schedule: enabled area '{}'", area.getName());
                }
                area.setScheduleWasDisabledBySchedule(false);
            } else if (!shouldEnable && area.isEnabled()) {
                // Schedule says disable, but area is enabled
                area.setEnabled(false);
                area.setScheduleWasDisabledBySchedule(true);
                AreaMonitorMod.LOGGER.debug("Schedule: disabled area '{}'", area.getName());
            }
        }
    }
}