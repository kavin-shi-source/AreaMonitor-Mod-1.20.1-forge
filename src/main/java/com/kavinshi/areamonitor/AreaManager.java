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
    
    private static final int TITLE_FADE_IN_TICKS = 5;
    private static final int TITLE_STAY_TICKS = 30;
    private static final int TITLE_FADE_OUT_TICKS = 5;
    
    private static final Set<String> EMPTY_AREA_SET = Collections.emptySet();
    
    private final Map<String, MonitorArea> areas = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerAreas = new ConcurrentHashMap<>();
    private final SpatialPartitionManager spatialPartition = new SpatialPartitionManager();

    // Reusable Set pool to reduce allocations in checkPlayer
    private final Map<UUID, Set<String>> currentAreasCache = new ConcurrentHashMap<>();

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
            // Create defensive copy for storage
            playerAreas.put(player.getUUID(), new HashSet<>(currentAreas));
        }
    }

    /**
     * Optimized area detection using spatial partitioning.
     * Reuses Set objects to reduce GC pressure.
     */
    private Set<String> getCurrentAreasOptimized(ServerPlayer player, PlayerPosition position) {
        UUID playerId = player.getUUID();

        // Reuse existing Set or create new one
        Set<String> currentAreas = currentAreasCache.computeIfAbsent(playerId, k -> new HashSet<>());
        currentAreas.clear(); // Clear previous contents

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

        // Check area whitelist (whitelist is already lowercase)
        String playerNameLower = player.getName().getString().toLowerCase();
        if (area.getWhitelist().contains(playerNameLower)) {
            return;
        }

        AreaMonitorMod.LOGGER.debug("Player {} entered area {}", player.getName().getString(), areaName);

        // Show enter message
        showAreaMessage(player, area.getDisplayName(), true, area.getEnterMode());

        // Delayed game mode switch
        AreaMonitor.addPendingGameModeChange(player, area.getEnterMode());

        // Execute enter triggers
        AreaTriggerManager.executeEnterTriggers(player, area);

        // Record stats
        area.recordEntry(player.getGameProfile().getName());

        // Area chaining: auto-teleport to next area in chain
        processChainTeleport(player, area);

    }

    private void processChainTeleport(ServerPlayer player, MonitorArea area) {
        if (!area.hasChainTarget()) return;
        String nextName = area.getChainNext();
        MonitorArea next = areas.get(nextName);
        if (next == null || !next.isEnabled()) {
            AreaMonitorMod.LOGGER.debug("Chain target '{}' not found or disabled for area '{}'", nextName, area.getName());
            return;
        }
        // Calculate teleport position: center of the next area
        double[] center = next.getBounds().getCenter();
        double tpX = center[0];
        double tpZ = center[1];
        var targetDim = next.getDimension();
        MinecraftServer server = AreaMonitor.getServer();
        if (server == null) return;
        var targetLevel = server.getLevel(net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            new net.minecraft.resources.ResourceLocation(targetDim)));
        if (targetLevel == null) return;
        // Find safe Y near center
        double tpY = findSafeY(targetLevel, (int)tpX, (int)tpZ);
        player.teleportTo(targetLevel, tpX + 0.5, tpY, tpZ + 0.5,
            player.getYRot(), player.getXRot());
        player.displayClientMessage(
            Component.literal("§7→ " + LocalizationManager.translate("gui.chain_teleported").replace("%s", next.getDisplayName())), true);
    }

    private static double findSafeY(net.minecraft.world.level.Level level, int x, int z) {
        int y = level.getMaxBuildHeight() - 1;
        while (y > level.getMinBuildHeight()) {
            var pos = new net.minecraft.core.BlockPos(x, y, z);
            var belowPos = new net.minecraft.core.BlockPos(x, y - 1, z);
            var state = level.getBlockState(pos);
            var below = level.getBlockState(belowPos);
            if (state.isAir() && below.canOcclude() && below.isFaceSturdy(level, belowPos, net.minecraft.core.Direction.UP)) {
                return y;
            }
            y--;
        }
        return 64;
    }

    private void handleAreaLeave(ServerPlayer player, String areaName) {
        MonitorArea area = areas.get(areaName);
        if (area == null || !area.isEnabled()) return;

        AreaMonitorMod.LOGGER.debug("Player {} left area {}", player.getName().getString(), areaName);

        // Show leave message
        showAreaMessage(player, area.getDisplayName(), false, area.getLeaveMode());

        // Delayed game mode switch (leaving area)
        AreaMonitor.addPendingGameModeChangeOnLeave(player, area.getLeaveMode());

        // Execute leave triggers
        AreaTriggerManager.executeLeaveTriggers(player, area);

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


    public Set<String> getCurrentAreas(ServerPlayer player) {
        return playerAreas.getOrDefault(player.getUUID(), EMPTY_AREA_SET);
    }

    public void clearPlayerData(UUID playerId) {
        playerAreas.remove(playerId);
        currentAreasCache.remove(playerId);
    }

    public void clearAllData() {
        playerAreas.clear();
        currentAreasCache.clear();
    }

    /**
     * Clear unused player area caches for players who have been offline for a long time.
     * This helps prevent memory leaks from accumulating player data.
     */
    public void clearUnusedCaches() {
        // Remove player data for players who are no longer online
        MinecraftServer server = AreaMonitor.getServer();
        if (server != null) {
            Set<UUID> onlinePlayerIds = new HashSet<>();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                onlinePlayerIds.add(player.getUUID());
            }

            // Remove offline player data
            playerAreas.keySet().removeIf(playerId -> !onlinePlayerIds.contains(playerId));
            currentAreasCache.keySet().removeIf(playerId -> !onlinePlayerIds.contains(playerId));
        }
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
}