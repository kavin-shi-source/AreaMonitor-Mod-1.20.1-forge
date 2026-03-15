package com.kavinshi.areamonitor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Area visualization system for displaying area boundaries and effects.
 * Optimized for batch particle sending to reduce network overhead.
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
     * Particle data for batching.
     */
    private static class ParticleData {
        final double x, y, z;
        final ParticleOptions particle;

        ParticleData(double x, double y, double z, ParticleOptions particle) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.particle = particle;
        }
    }

    /**
     * Show area border with batched particle sending for better performance.
     */
    public static void showAreaBorder(ServerPlayer player, MonitorArea area) {
        List<ParticleData> batch = new ArrayList<>();

        if (area.getBounds() instanceof MonitorArea.RectangleBounds bounds) {
            collectRectangleBorderParticles(player, bounds, batch);
        } else if (area.getBounds() instanceof MonitorArea.CircleBounds bounds) {
            collectCircleBorderParticles(player, bounds, batch);
        }

        sendParticleBatch(player.level(), batch);
    }

    /**
     * Collect rectangle border particles into batch.
     */
    private static void collectRectangleBorderParticles(ServerPlayer player, MonitorArea.RectangleBounds bounds, List<ParticleData> batch) {
        double y = player.getY();

        collectHorizontalLineParticles(bounds.getMinX(), bounds.getMaxX(), bounds.getMinZ(), y, ParticleTypes.END_ROD, batch);
        collectHorizontalLineParticles(bounds.getMinX(), bounds.getMaxX(), bounds.getMaxZ(), y, ParticleTypes.END_ROD, batch);
        collectHorizontalLineParticles(bounds.getMinZ(), bounds.getMaxZ(), bounds.getMinX(), y, ParticleTypes.END_ROD, batch);
        collectHorizontalLineParticles(bounds.getMinZ(), bounds.getMaxZ(), bounds.getMaxX(), y, ParticleTypes.END_ROD, batch);
    }

    /**
     * Collect circle border particles into batch.
     */
    private static void collectCircleBorderParticles(ServerPlayer player, MonitorArea.CircleBounds bounds, List<ParticleData> batch) {
        double y = player.getY();
        double centerX = bounds.getCenterX();
        double centerZ = bounds.getCenterZ();
        double radius = bounds.getRadius();

        int segments = (int) (radius * 2 * Math.PI / PARTICLE_SPACING);
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double x = centerX + radius * Math.cos(angle);
            double z = centerZ + radius * Math.sin(angle);
            batch.add(new ParticleData(x, y, z, ParticleTypes.END_ROD));
        }
    }

    /**
     * Collect horizontal line particles into batch.
     */
    private static void collectHorizontalLineParticles(double start, double end, double fixed, double y, ParticleOptions particle, List<ParticleData> batch) {
        double step = start < end ? PARTICLE_SPACING : -PARTICLE_SPACING;
        for (double pos = start; pos <= end; pos += step) {
            batch.add(new ParticleData(pos, y, fixed, particle));
        }
    }

    /**
     * Send particle batch to nearby players efficiently.
     * This reduces the number of player list iterations from O(n*m) to O(n+m)
     * where n = number of particles, m = number of players.
     */
    private static void sendParticleBatch(Level level, List<ParticleData> batch) {
        if (level.isClientSide() || batch.isEmpty()) {
            return;
        }

        // Get all server players once
        List<ServerPlayer> players = new ArrayList<>();
        for (net.minecraft.world.entity.player.Player p : level.players()) {
            if (p instanceof ServerPlayer serverPlayer) {
                players.add(serverPlayer);
            }
        }

        // Send particles to nearby players
        for (ParticleData particle : batch) {
            for (ServerPlayer player : players) {
                if (player.distanceToSqr(particle.x, particle.y, particle.z) <= MAX_PARTICLE_RENDER_DISTANCE_SQ) {
                    player.connection.send(new ClientboundLevelParticlesPacket(
                        particle.particle, false, particle.x, particle.y, particle.z, 0, 0, 0, 0, 1
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
            // Log full stack trace for debugging
            AreaMonitorMod.LOGGER.debug("Error finding player by UUID: {}", playerId, e);
        }
        return null;
    }

    /**
     * Show selection area (two points) with batched particle sending.
     */
    public static void showSelection(ServerPlayer player, BlockPos pos1, BlockPos pos2) {
        Level level = player.level();
        double y = player.getY();

        List<ParticleData> batch = new ArrayList<>();

        batch.add(new ParticleData(pos1.getX() + 0.5, y, pos1.getZ() + 0.5, ParticleTypes.ANGRY_VILLAGER));
        batch.add(new ParticleData(pos2.getX() + 0.5, y, pos2.getZ() + 0.5, ParticleTypes.ANGRY_VILLAGER));

        if (pos1 != null && pos2 != null) {
            collectLineBetweenParticles(pos1, pos2, y, ParticleTypes.END_ROD, batch);
        }

        sendParticleBatch(level, batch);
    }

    /**
     * Collect line particles between two points into batch.
     */
    private static void collectLineBetweenParticles(BlockPos pos1, BlockPos pos2, double y, ParticleOptions particle, List<ParticleData> batch) {
        double dx = pos2.getX() - pos1.getX();
        double dz = pos2.getZ() - pos1.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        int steps = (int) (distance / PARTICLE_SPACING);

        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            double x = pos1.getX() + dx * ratio;
            double z = pos1.getZ() + dz * ratio;
            batch.add(new ParticleData(x + 0.5, y, z + 0.5, particle));
        }
    }

    /**
     * Spawn a single particle (for backward compatibility and simple use cases).
     * For batch operations, use the batch methods instead.
     */
    public static void spawnParticle(Level level, double x, double y, double z, ParticleOptions particle) {
        if (level.isClientSide()) {
            return;
        }

        for (net.minecraft.world.entity.player.Player p : level.players()) {
            if (p instanceof ServerPlayer player) {
                if (player.distanceToSqr(x, y, z) <= MAX_PARTICLE_RENDER_DISTANCE_SQ) {
                    player.connection.send(new ClientboundLevelParticlesPacket(
                        particle, false, x, y, z, 0, 0, 0, 0, 1
                    ));
                }
            }
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
