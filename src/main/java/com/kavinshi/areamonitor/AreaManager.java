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
    
    static final int TITLE_FADE_IN_TICKS = 5;
    static final int TITLE_STAY_TICKS = 30;
    static final int TITLE_FADE_OUT_TICKS = 5;
    private static final int MAX_CHAIN_HOPS = 16;
    
    private static final Set<String> EMPTY_AREA_SET = Collections.emptySet();
    
    private final Map<String, MonitorArea> areas = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerAreas = new ConcurrentHashMap<>();
    private final SpatialPartitionManager spatialPartition = new SpatialPartitionManager();

    // Reusable Set pool to reduce allocations in checkPlayer
    private final Map<UUID, Set<String>> currentAreasCache = new ConcurrentHashMap<>();

    /**
     * Lock protecting the areas collection against concurrent mutation during config reload.
     * ConfigManager.loadAreasConfig and saveAreasConfig acquire this lock so that addArea/removeArea
     * cannot interleave with a clear+rebuild cycle. Reentrant, so loadAreasConfig can call addArea
     * while already holding the lock.
     */
    private static final Object AREAS_LOCK = new Object();

    /**
     * Returns the lock protecting the areas collection. ConfigManager uses this to synchronize
     * config load/save with addArea/removeArea mutations.
     */
    public Object getAreasLock() {
        return AREAS_LOCK;
    }

    public static AreaManager getInstance() {
        return INSTANCE;
    }

    private AreaManager() {}

    public void addArea(MonitorArea area) {
        synchronized (AREAS_LOCK) {
            areas.put(area.getName(), area);
            spatialPartition.addRegion(area);
        }
        AreaMonitorMod.LOGGER.info("Added monitoring area: {}", area.getName());
    }

    public void removeArea(String areaName) {
        synchronized (AREAS_LOCK) {
            MonitorArea removed = areas.remove(areaName);
            if (removed != null) {
                spatialPartition.removeRegion(areaName);
                ItemBlacklistManager.removeAreaBlacklist(areaName);
                AreaMonitorMod.LOGGER.info("Removed monitoring area: {}", areaName);
            }
        }
    }

    public MonitorArea getArea(String areaName) {
        return areas.get(areaName);
    }

    /**
     * Validates area name: 1-32 chars, only letters, digits, underscore, hyphen.
     * Shared validation used by commands, packets, and selection tool.
     */
    public static boolean isValidAreaName(String name) {
        if (name == null || name.isEmpty() || name.length() > 32) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-')) return false;
        }
        return true;
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

        // previously whitelisted players returned early here, which kept their
        // playerAreas cache frozen at whatever state it was in when they were whitelisted.
        // When the whitelist entry was later removed, the next checkPlayer fired spurious
        // enter/leave events for areas the player was no longer in (or had entered while
        // whitelisted). Now we always compute currentAreas and refresh the cache; the
        // whitelist gate only suppresses trigger/game-mode side effects below.
        boolean isWhitelisted = WhitelistManager.isWhitelisted(player);
        if (isWhitelisted && ConfigManager.CONFIG.debugMode.get()) {
            AreaMonitorMod.LOGGER.debug("AreaManager: Player {} is whitelisted, refreshing cache only",
                player.getName().getString());
        }

        double playerX = player.getX();
        double playerZ = player.getZ();
        String dimension = player.level().dimension().location().toString();

        Set<String> currentAreas = getCurrentAreasOptimized(player, playerX, playerZ, dimension);
        Set<String> previousAreas = playerAreas.getOrDefault(player.getUUID(), EMPTY_AREA_SET);

        // Whitelisted players: only refresh the cache so future un-whitelisting produces correct
        // diff events. Skip enter/leave triggers and game-mode changes entirely.
        if (!isWhitelisted) {
            // Snapshot both sets before iteration — handleAreaEnter may trigger chain teleport
            // which mutates currentAreas (clear+add in teleportToAreaCenter), causing CME.
            List<String> currentSnapshot = new ArrayList<>(currentAreas);
            List<String> previousSnapshot = new ArrayList<>(previousAreas);

            // capture player position before enter handling so we can detect chain teleport
            // and abort the stale-snapshot iteration (avoids firing enter/leave events for areas the
            // player is no longer in after being teleported away).
            final double posXBefore = player.getX();
            final double posZBefore = player.getZ();

            // Detect newly entered areas
            for (String areaName : currentSnapshot) {
                if (!previousAreas.contains(areaName)) {
                    handleAreaEnter(player, areaName);
                    // If handleAreaEnter triggered a chain teleport, currentSnapshot is stale — defer to next tick
                    if (Math.abs(player.getX() - posXBefore) > 0.5 || Math.abs(player.getZ() - posZBefore) > 0.5) {
                        return;
                    }
                }
            }

            // Detect left areas (compare against live currentAreas in case chain teleport updated it)
            for (String areaName : previousSnapshot) {
                if (!currentAreas.contains(areaName)) {
                    handleAreaLeave(player, areaName);
                }
            }
        }

        // Always update cache when area state changes (including for whitelisted players)
        if (!currentAreas.equals(previousAreas)) {
            // Create defensive copy for storage
            playerAreas.put(player.getUUID(), new HashSet<>(currentAreas));
        }
    }

    /**
     * Optimized area detection using spatial partitioning.
     * Reuses Set objects to reduce GC pressure.
     */
    private Set<String> getCurrentAreasOptimized(ServerPlayer player, double x, double z, String dimension) {
        UUID playerId = player.getUUID();

        // Reuse existing Set or create new one
        Set<String> currentAreas = currentAreasCache.computeIfAbsent(playerId, k -> new HashSet<>());
        currentAreas.clear(); // Clear previous contents

        // Use spatial partitioning to get potentially relevant areas
        Set<MonitorArea> potentialAreas = spatialPartition.getPotentialRegions(x, z, dimension);

        // Only check relevant areas
        for (MonitorArea area : potentialAreas) {
            if (area.isEnabled() && area.isPlayerInArea(x, z, dimension)) {
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
        showAreaTitle(player, area.getDisplayName(), true, area.getEnterMode());

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

        Set<String> visited = new HashSet<>();
        visited.add(area.getName());
        MonitorArea current = area;
        int maxHops = MAX_CHAIN_HOPS;
        while (current.hasChainTarget() && maxHops-- > 0) {
            String nextName = current.getChainNext();
            if (!visited.add(nextName)) {
                AreaMonitorMod.LOGGER.warn("Chain teleport cycle detected starting from area '{}'", area.getName());
                break;
            }
            MonitorArea next = areas.get(nextName);
            if (next == null || !next.isEnabled()) {
                AreaMonitorMod.LOGGER.debug("Chain target '{}' not found or disabled for area '{}'", nextName, current.getName());
                break;
            }
            current = next;
        }

        if (current != area) {
            teleportToAreaCenter(player, current);
        }
    }

    private void teleportToAreaCenter(ServerPlayer player, MonitorArea target) {
        double[] center = target.getBounds().getCenter();
        double tpX = center[0];
        double tpZ = center[1];
        var targetDim = target.getDimension();
        MinecraftServer server = AreaMonitor.getServer();
        if (server == null) return;
        var rl = net.minecraft.resources.ResourceLocation.tryParse(targetDim);
        if (rl == null) return;
        var targetLevel = server.getLevel(net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, rl));
        if (targetLevel == null) return;
        double tpY = findSafeY(targetLevel, (int)tpX, (int)tpZ);
        player.teleportTo(targetLevel, tpX + 0.5, tpY, tpZ + 0.5,
            player.getYRot(), player.getXRot());
        player.displayClientMessage(
            Component.literal("§7→ " + LocalizationManager.translate("gui.chain_teleported").replace("%s", target.getDisplayName())), true);

        // Sync playerAreas cache so next tick doesn't double-trigger enter/leave
        UUID playerId = player.getUUID();
        Set<String> currentAreas = currentAreasCache.get(playerId);
        if (currentAreas != null) {
            currentAreas.clear();
            if (target.isEnabled() && target.isPlayerInArea(
                    player.getX(), player.getZ(),
                    player.level().dimension().location().toString())) {
                currentAreas.add(target.getName());
            }
            playerAreas.put(playerId, new HashSet<>(currentAreas));
        }

        // Fire enter effects (title, game mode, triggers, stats) for the chain destination.
        // Guard: only when target is a true final destination (no further chain target) to prevent
        // infinite recursion through processChainTeleport on cyclic or over-long chains.
        if (!target.hasChainTarget()) {
            handleAreaEnter(player, target.getName());
        }
    }

    private static double findSafeY(net.minecraft.world.level.Level level, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        // Synchronously load the chunk if not yet loaded so we can scan real terrain.
        // getChunk on ServerLevel will load/generate the chunk synchronously; this is acceptable
        // because chain teleport is a one-shot per enter event, not per-tick.
        try {
            level.getChunk(chunkX, chunkZ);
        } catch (Exception ex) {
            AreaMonitorMod.LOGGER.warn("findSafeY: failed to load chunk at {},{} - falling back to sea level ({})",
                x, z, level.getSeaLevel(), ex);
            return level.getSeaLevel();
        }
        int y = level.getMaxBuildHeight() - 1;
        while (y > level.getMinBuildHeight()) {
            var pos = new net.minecraft.core.BlockPos(x, y, z);
            var headPos = new net.minecraft.core.BlockPos(x, y + 1, z);
            var belowPos = new net.minecraft.core.BlockPos(x, y - 1, z);
            var state = level.getBlockState(pos);
            var head = level.getBlockState(headPos);
            var below = level.getBlockState(belowPos);
            if (state.isAir() && head.isAir() && below.canOcclude() && below.isFaceSturdy(level, belowPos, net.minecraft.core.Direction.UP)) {
                return y;
            }
            y--;
        }
        // No safe spot found — fall back to sea level rather than a hard-coded 64
        return level.getSeaLevel();
    }

    private void handleAreaLeave(ServerPlayer player, String areaName) {
        MonitorArea area = areas.get(areaName);
        if (area == null || !area.isEnabled()) return;

        // Check area whitelist — skip leave effects for whitelisted players (consistent with handleAreaEnter)
        String playerNameLower = player.getName().getString().toLowerCase();
        if (area.getWhitelist().contains(playerNameLower)) {
            return;
        }

        AreaMonitorMod.LOGGER.debug("Player {} left area {}", player.getName().getString(), areaName);

        // Show leave message
        showAreaTitle(player, area.getDisplayName(), false, area.getLeaveMode());

        // Delayed game mode switch (leaving area)
        AreaMonitor.addPendingGameModeChangeOnLeave(player, area.getLeaveMode());

        // Execute leave triggers
        AreaTriggerManager.executeLeaveTriggers(player, area);

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

    /**
     * Location-based area lookup via spatial partitioning.
     * Used by explosion/entity-damage protection where no player context is available.
     * Returns candidates — callers must still verify with area.getBounds().contains().
     */
    public Set<MonitorArea> getPotentialAreasAt(double x, double z, String dimension) {
        return spatialPartition.getPotentialRegions(x, z, dimension);
    }

    /**
     * AABB-based area lookup for events that span multiple grid cells (explosions, etc.).
     * returns the union of all regions registered in any grid cell covered by the box.
     */
    public Set<MonitorArea> getPotentialAreasInBox(double minX, double minZ, double maxX, double maxZ, String dimension) {
        return spatialPartition.getPotentialRegionsInBox(minX, minZ, maxX, maxZ, dimension);
    }

    public void clearPlayerData(UUID playerId) {
        playerAreas.remove(playerId);
        currentAreasCache.remove(playerId);
    }

    public void clearAllData() {
        synchronized (AREAS_LOCK) {
            areas.clear();
            spatialPartition.clear();
        }
        playerAreas.clear();
        currentAreasCache.clear();
    }

    /**
     * Clear area definitions and spatial index only, preserving per-player area caches.
     * Used during config reload so online players don't get false enter triggers on the next tick.
     * Stale entries pointing to removed areas are filtered lazily during diff computation.
     */
    public void clearAreasOnly() {
        synchronized (AREAS_LOCK) {
            areas.clear();
            spatialPartition.clear();
        }
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
        synchronized (AREAS_LOCK) {
            spatialPartition.clear();
            for (MonitorArea area : areas.values()) {
                spatialPartition.addRegion(area);
            }
        }
        AreaMonitorMod.LOGGER.info("Spatial partition rebuilt, total {} areas", areas.size());
    }
}