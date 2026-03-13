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
        AreaMonitorMod.LOGGER.info("Added monitoring area: {}", area.getName());
    }

    public void removeArea(String areaName) {
        MonitorArea removed = areas.remove(areaName);
        if (removed != null) {
            spatialPartition.removeRegion(areaName);
            AreaMonitorMod.LOGGER.info("Removed monitoring area: {}", areaName);
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

        // Detect newly entered areas
        for (String areaName : currentAreas) {
            if (!previousAreas.contains(areaName)) {
                handleAreaEnter(player, areaName);
            }
        }

        // Detect left areas
        for (String areaName : previousAreas) {
            if (!currentAreas.contains(areaName)) {
                handleAreaLeave(player, areaName);
            }
        }

        // Only update cache when area state changes
        if (!currentAreas.equals(previousAreas)) {
            playerAreas.put(player.getUUID(), new HashSet<>(currentAreas));
        }
    }

    /**
     * Optimized area detection using spatial partitioning.
     */
    private Set<String> getCurrentAreasOptimized(ServerPlayer player, PlayerPosition position) {
        Set<String> currentAreas = new HashSet<>();

        // Use spatial partitioning to get potentially relevant areas
        Set<MonitorArea> potentialAreas = spatialPartition.getPotentialRegions(
            position.getX(), position.getZ(), position.getDimension()
        );

        // Only check relevant areas
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

        // Check area whitelist
        if (area.getWhitelist().contains(player.getName().getString().toLowerCase())) {
            return;
        }

        AreaMonitorMod.LOGGER.debug("Player {} entered area {}", player.getName().getString(), areaName);

        // Show enter message
        showAreaMessage(player, area.getDisplayName(), true, area.getEnterMode());

        // Delayed game mode switch
        AreaMonitor.addPendingGameModeChange(player, area.getEnterMode());

    }

    private void handleAreaLeave(ServerPlayer player, String areaName) {
        MonitorArea area = areas.get(areaName);
        if (area == null || !area.isEnabled()) return;

        AreaMonitorMod.LOGGER.debug("Player {} left area {}", player.getName().getString(), areaName);

        // Show leave message
        showAreaMessage(player, area.getDisplayName(), false, area.getLeaveMode());

        // Delayed game mode switch (leaving area)
        AreaMonitor.addPendingGameModeChangeOnLeave(player, area.getLeaveMode());

    }

    private void showAreaMessage(ServerPlayer player, String areaName, boolean entering, GameType gameMode) {
        // Only send screen center Title notification, remove chat messages
        showAreaTitle(player, areaName, entering, gameMode);
    }

    private void showAreaTitle(ServerPlayer player, String areaName, boolean entering, GameType gameMode) {
        try {
            String action = entering ? LocalizationManager.translate("area.enter") : LocalizationManager.translate("area.leave");
            String modeName = LocalizationManager.getGameModeDisplayName(gameMode);
            // Use §7 (gray) and smaller format, remove bold
            String message = String.format(LocalizationManager.translate("area.message"), action, areaName, modeName);

            player.connection.send(new ClientboundSetTitlesAnimationPacket(TITLE_FADE_IN_TICKS, TITLE_STAY_TICKS, TITLE_FADE_OUT_TICKS));
            // Only display one line as Title
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(message)));
            // Do not send Subtitle, achieve single line display
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("Error sending title notification", e);
        }
    }


    private Set<ServerPlayer> getPlayersInArea(MonitorArea area) {
        Set<ServerPlayer> playersInArea = new HashSet<>();

        if (area == null) return playersInArea;

        // Get current server instance and check all online players
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
        // Clean up player data that has been inactive for a long time
        playerAreas.entrySet().removeIf(entry -> {
            // More complex cleanup logic can be added here
            return false; // Do not clean up for now
        });
    }

    /**
     * Rebuild all areas into spatial partition (used after area config updates).
     */
    public void rebuildSpatialPartition() {
        spatialPartition.clear();
        for (MonitorArea area : areas.values()) {
            spatialPartition.addRegion(area);
        }
        AreaMonitorMod.LOGGER.info("Spatial partition rebuilt, total {} areas", areas.size());
    }

    public enum TriggerType {
        ENTER, LEAVE, PERIODIC, ITEM_HELD, PLAYER_COUNT
    }
}