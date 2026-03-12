package com.kavinshi.areamonitor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Area visualization system for displaying area boundaries and effects.
 */
public class AreaVisualizer {
    private static final double PARTICLE_SPACING = 1.0;
    private static final int VISUALIZATION_DURATION = 100;
    /**
     * Maximum squared distance for particle visibility (32 blocks).
     * Players beyond this distance will not receive particle packets.
     */
    private static final double MAX_PARTICLE_RENDER_DISTANCE_SQ = 1024.0;
    private static final Map<UUID, VisualizationData> activeVisualizations = new ConcurrentHashMap<>();

    private AreaVisualizer() {
    }

    /**
     * Show area border.
     */
    public static void showAreaBorder(ServerPlayer player, MonitorArea area) {
        if (area.getBounds() instanceof MonitorArea.RectangleBounds bounds) {
            showRectangleBorder(player, bounds);
        } else if (area.getBounds() instanceof MonitorArea.CircleBounds bounds) {
            showCircleBorder(player, bounds);
        }
    }

    /**
     * Show rectangle area border.
     */
    private static void showRectangleBorder(ServerPlayer player, MonitorArea.RectangleBounds bounds) {
        Level level = player.level();
        double y = player.getY();

        showHorizontalLine(level, bounds.getMinX(), bounds.getMaxX(), bounds.getMinZ(), y, ParticleTypes.END_ROD);
        showHorizontalLine(level, bounds.getMinX(), bounds.getMaxX(), bounds.getMaxZ(), y, ParticleTypes.END_ROD);
        showHorizontalLine(level, bounds.getMinZ(), bounds.getMaxZ(), bounds.getMinX(), y, ParticleTypes.END_ROD);
        showHorizontalLine(level, bounds.getMinZ(), bounds.getMaxZ(), bounds.getMaxX(), y, ParticleTypes.END_ROD);
    }

    /**
     * Show circle area border.
     */
    private static void showCircleBorder(ServerPlayer player, MonitorArea.CircleBounds bounds) {
        Level level = player.level();
        double y = player.getY();
        double centerX = bounds.getCenterX();
        double centerZ = bounds.getCenterZ();
        double radius = bounds.getRadius();

        int segments = (int) (radius * 2 * Math.PI / PARTICLE_SPACING);
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double x = centerX + radius * Math.cos(angle);
            double z = centerZ + radius * Math.sin(angle);
            spawnParticle(level, x, y, z, ParticleTypes.END_ROD);
        }
    }

    /**
     * Show horizontal line segment.
     */
    private static void showHorizontalLine(Level level, double start, double end, double fixed, double y, ParticleOptions particle) {
        double step = start < end ? PARTICLE_SPACING : -PARTICLE_SPACING;
        for (double pos = start; pos <= end; pos += step) {
            spawnParticle(level, pos, y, fixed, particle);
        }
    }

    /**
     * Spawn particle effect.
     */
    public static void spawnParticle(Level level, double x, double y, double z, ParticleOptions particle) {
        if (!level.isClientSide()) {
            for (net.minecraft.world.entity.player.Player p : level.players()) {
                if (!(p instanceof ServerPlayer player)) continue;
                if (player.distanceToSqr(x, y, z) <= MAX_PARTICLE_RENDER_DISTANCE_SQ) {
                    player.connection.send(new ClientboundLevelParticlesPacket(
                        particle, false, x, y, z, 0, 0, 0, 0, 1
                    ));
                }
            }
        }
    }

    /**
     * Start persistent visualization for an area.
     */
    public static void startPersistentVisualization(ServerPlayer player, MonitorArea area) {
        UUID playerId = player.getUUID();
        VisualizationData data = new VisualizationData(area, System.currentTimeMillis(), playerId);
        activeVisualizations.put(playerId, data);
    }

    /**
     * Stop persistent visualization.
     */
    public static void stopPersistentVisualization(ServerPlayer player) {
        activeVisualizations.remove(player.getUUID());
    }

    /**
     * Update persistent visualizations.
     */
    public static void updatePersistentVisualizations() {
        long currentTime = System.currentTimeMillis();

        activeVisualizations.entrySet().removeIf(entry -> {
            VisualizationData data = entry.getValue();
            if (currentTime - data.startTime > VISUALIZATION_DURATION * 50) {
                return true;
            }

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
            AreaMonitorMod.LOGGER.debug("Error finding player by UUID: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Show selection area (two points).
     */
    public static void showSelection(ServerPlayer player, BlockPos pos1, BlockPos pos2) {
        Level level = player.level();
        double y = player.getY();

        spawnParticle(level, pos1.getX() + 0.5, y, pos1.getZ() + 0.5, ParticleTypes.ANGRY_VILLAGER);
        spawnParticle(level, pos2.getX() + 0.5, y, pos2.getZ() + 0.5, ParticleTypes.ANGRY_VILLAGER);

        if (pos1 != null && pos2 != null) {
            showLineBetween(level, pos1, pos2, y, ParticleTypes.END_ROD);
        }
    }

    /**
     * Show line between two points.
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
     * Clean up visualization data for a player.
     */
    public static void cleanupPlayerData(UUID playerId) {
        activeVisualizations.remove(playerId);
    }

    /**
     * Clean up all visualization data when server stops.
     */
    public static void cleanupAllData() {
        activeVisualizations.clear();
    }

    /**
     * Visualization data class.
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
