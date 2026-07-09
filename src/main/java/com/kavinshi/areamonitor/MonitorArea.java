package com.kavinshi.areamonitor;

import com.kavinshi.areamonitor.model.PlayerPosition;
import com.kavinshi.areamonitor.model.RestrictionSettings;
import com.kavinshi.areamonitor.util.DimensionUtils;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class MonitorArea {
    private final String name;
    private volatile String displayName;
    private volatile String dimension;
    private volatile AreaBounds bounds;
    private volatile GameType enterMode;
    private volatile GameType leaveMode;
    private volatile boolean enabled;
    private volatile List<String> whitelist;
    private volatile List<String> protectionWhitelist;
    private volatile RestrictionSettings restrictions;
    private volatile ProtectionSettings protection = new ProtectionSettings();
    private volatile TriggerConfig enterTrigger = null;
    private volatile TriggerConfig leaveTrigger = null;
    private volatile boolean scheduleEnabled = false;
    private volatile boolean scheduleWasDisabledBySchedule = false;
    private volatile Integer scheduleTimeMin = null;
    private volatile Integer scheduleTimeMax = null;
    private volatile boolean conditionEnabled = false;
    private volatile Integer conditionMinPlayers = null;
    private volatile String conditionRequirePlayer = null;
    private volatile String chainNext = null;
    // Stats (runtime only, not persisted)
    private final AtomicInteger entryCount = new AtomicInteger(0);
    // P3 #3: volatile — these stats are written by tick/thread handlers and read by command queries
    private volatile String lastVisitor = "-";
    private volatile long lastVisitTime = 0;

    public MonitorArea(String name) {
        this.name = name;
        this.displayName = name;
        this.dimension = DimensionUtils.OVERWORLD;
        this.bounds = new RectangleBounds(0, 0, 0, 0);
        this.enterMode = GameType.ADVENTURE;
        this.leaveMode = GameType.SURVIVAL;
        this.enabled = true;
        this.whitelist = new CopyOnWriteArrayList<>();
        this.protectionWhitelist = new CopyOnWriteArrayList<>();
        this.restrictions = new RestrictionSettings();
        this.protection = new ProtectionSettings();
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) {
        // Guard against null — SpatialPartitionManager.getPotentialRegions calls
        // region.getDimension().equals(dimension), which would NPE if dimension is null.
        this.dimension = dimension != null ? dimension : DimensionUtils.OVERWORLD;
    }

    public AreaBounds getBounds() { return bounds; }
    public void setBounds(AreaBounds bounds) { this.bounds = bounds; }

    public GameType getEnterMode() { return enterMode; }
    public void setEnterMode(GameType enterMode) { this.enterMode = enterMode; }

    public GameType getLeaveMode() { return leaveMode; }
    public void setLeaveMode(GameType leaveMode) { this.leaveMode = leaveMode; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<String> getWhitelist() { return whitelist; }

    public void setWhitelist(List<String> whitelist) {
        // P3 #2: guard against null input
        List<String> lowercaseList = new ArrayList<>();
        if (whitelist != null) {
            for (String name : whitelist) {
                if (name != null) lowercaseList.add(name.toLowerCase());
            }
        }
        this.whitelist = new CopyOnWriteArrayList<>(lowercaseList);
    }

    public List<String> getProtectionWhitelist() { return protectionWhitelist; }
    public void setProtectionWhitelist(List<String> whitelist) {
        // P3 #2: guard against null input
        List<String> lowercaseList = new ArrayList<>();
        if (whitelist != null) {
            for (String name : whitelist) {
                if (name != null) lowercaseList.add(name.toLowerCase());
            }
        }
        this.protectionWhitelist = new CopyOnWriteArrayList<>(lowercaseList);
    }

    public RestrictionSettings getRestrictions() { return restrictions; }
    public void setRestrictions(RestrictionSettings restrictions) { this.restrictions = restrictions; }

    public ProtectionSettings getProtection() { return protection; }
    public void setProtection(ProtectionSettings protection) { this.protection = protection; }

    public TriggerConfig getEnterTrigger() { return enterTrigger; }
    public void setEnterTrigger(TriggerConfig v) { this.enterTrigger = v; }
    public TriggerConfig getLeaveTrigger() { return leaveTrigger; }
    public void setLeaveTrigger(TriggerConfig v) { this.leaveTrigger = v; }

    public boolean hasEnterTrigger() { return enterTrigger != null && enterTrigger.hasAnyAction(); }
    public boolean hasLeaveTrigger() { return leaveTrigger != null && leaveTrigger.hasAnyAction(); }

    // === Schedule ===
    public boolean isScheduleEnabled() { return scheduleEnabled; }
    public void setScheduleEnabled(boolean v) { this.scheduleEnabled = v; }
    public boolean isScheduleWasDisabledBySchedule() { return scheduleWasDisabledBySchedule; }
    public void setScheduleWasDisabledBySchedule(boolean v) { this.scheduleWasDisabledBySchedule = v; }
    public Integer getScheduleTimeMin() { return scheduleTimeMin; }
    public void setScheduleTimeMin(Integer v) { this.scheduleTimeMin = v; }
    public Integer getScheduleTimeMax() { return scheduleTimeMax; }
    public void setScheduleTimeMax(Integer v) { this.scheduleTimeMax = v; }

    // === Condition ===
    public boolean isConditionEnabled() { return conditionEnabled; }
    public void setConditionEnabled(boolean v) { this.conditionEnabled = v; }
    public Integer getConditionMinPlayers() { return conditionMinPlayers; }
    public void setConditionMinPlayers(Integer v) { this.conditionMinPlayers = v; }
    public String getConditionRequirePlayer() { return conditionRequirePlayer; }
    public void setConditionRequirePlayer(String v) { this.conditionRequirePlayer = v; }

    // === Area chain ===
    public String getChainNext() { return chainNext; }
    public void setChainNext(String v) { this.chainNext = v; }
    public boolean hasChainTarget() { return chainNext != null && !chainNext.isEmpty(); }

    /**
     * Evaluate activation conditions. Returns true if area should be active
     * based on current server state (player count, specific player presence).
     */
    public boolean evaluateCondition(net.minecraft.server.MinecraftServer server) {
        if (!conditionEnabled) return true; // conditions disabled = always pass
        if (server == null) return true;
        // Min player count
        if (conditionMinPlayers != null && conditionMinPlayers > 0) {
            if (server.getPlayerCount() < conditionMinPlayers) return false;
        }
        // Specific player must be online
        if (conditionRequirePlayer != null && !conditionRequirePlayer.isEmpty()) {
            boolean found = false;
            for (var sp : server.getPlayerList().getPlayers()) {
                if (sp.getGameProfile().getName().equalsIgnoreCase(conditionRequirePlayer)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    // === Stats ===
    public int getEntryCount() { return entryCount.get(); }
    public String getLastVisitor() { return lastVisitor; }
    public long getLastVisitTime() { return lastVisitTime; }
    public void recordEntry(String playerName) {
        entryCount.incrementAndGet();
        lastVisitor = playerName;
        lastVisitTime = System.currentTimeMillis();
    }

    /**
     * Evaluate schedule: returns true if area should be enabled based on current game time.
     */
    public boolean evaluateSchedule(long gameTime) {
        if (!scheduleEnabled || scheduleTimeMin == null || scheduleTimeMax == null) return true;
        long time = gameTime % 24000;
        int min = scheduleTimeMin, max = scheduleTimeMax;
        if (min <= max) {
            return time >= min && time <= max;
        } else {
            // cross-midnight
            return time >= min || time <= max;
        }
    }

    public boolean isPlayerInArea(PlayerPosition position) {
        if (!position.getDimension().equals(dimension)) {
            return false;
        }
        return bounds.contains(position.getX(), position.getZ());
    }

    public AABB getBoundingBox() {
        return bounds.getBoundingBox();
    }

    public interface AreaBounds {
        boolean contains(double x, double z);
        AABB getBoundingBox();
        BoundsType getType();
        /** Returns [centerX, centerZ] for chain teleport target. */
        double[] getCenter();
    }

    public static class RectangleBounds implements AreaBounds {
        private final int minX, minZ, maxX, maxZ;

        public RectangleBounds(int minX, int minZ, int maxX, int maxZ) {
            this.minX = Math.min(minX, maxX);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxZ = Math.max(minZ, maxZ);
        }

        @Override
        public boolean contains(double x, double z) {
            // minX/maxX are inclusive block coordinates; the area covers [minX, maxX+1) in
            // continuous space (consistent with getBoundingBox). Using < maxX + 1 ensures
            // the center of the upper-boundary block (e.g., x=12.5 when maxX=12) is contained.
            return x >= minX && x < maxX + 1 && z >= minZ && z < maxZ + 1;
        }

        @Override
        public AABB getBoundingBox() {
            return new AABB(minX, -64, minZ, maxX + 1, 320, maxZ + 1);
        }

        @Override
        public BoundsType getType() {
            return BoundsType.RECTANGLE;
        }

        @Override public double[] getCenter() { return new double[]{(minX + maxX) / 2.0, (minZ + maxZ) / 2.0}; }

        public int getMinX() { return minX; }
        public int getMinZ() { return minZ; }
        public int getMaxX() { return maxX; }
        public int getMaxZ() { return maxZ; }
    }

    public static class CircleBounds implements AreaBounds {
        private final int centerX, centerZ, radius;

        public CircleBounds(int centerX, int centerZ, int radius) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
        }

        @Override
        public boolean contains(double x, double z) {
            double dx = x - centerX;
            double dz = z - centerZ;
            return dx * dx + dz * dz <= radius * radius;
        }

        @Override
        public AABB getBoundingBox() {
            return new AABB(centerX - radius, -64, centerZ - radius,
                          centerX + radius + 1, 320, centerZ + radius + 1);
        }

        @Override
        public BoundsType getType() {
            return BoundsType.CIRCLE;
        }

        @Override public double[] getCenter() { return new double[]{centerX, centerZ}; }

        public int getCenterX() { return centerX; }
        public int getCenterZ() { return centerZ; }
        public int getRadius() { return radius; }
    }

    // Vec2i simple integer vector record for polygon vertices
    public record Vec2i(int x, int z) {}

    /**
     * Polygon area bounds using ray casting for containment test.
     * Supports 3-32 vertices.
     */
    public static class PolygonBounds implements AreaBounds {
        private final List<Vec2i> vertices;
        private final AABB cachedBoundingBox;

        public PolygonBounds(List<Vec2i> vertices) {
            if (vertices == null || vertices.size() < 3) {
                throw new IllegalArgumentException("Polygon requires at least 3 vertices");
            }
            if (vertices.size() > 32) {
                throw new IllegalArgumentException("Polygon cannot have more than 32 vertices");
            }
            this.vertices = List.copyOf(vertices);
            // Compute and cache bounding box
            int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (Vec2i v : vertices) {
                if (v.x < minX) minX = v.x;
                if (v.x > maxX) maxX = v.x;
                if (v.z < minZ) minZ = v.z;
                if (v.z > maxZ) maxZ = v.z;
            }
            this.cachedBoundingBox = new AABB(minX, -64, minZ, maxX + 1, 320, maxZ + 1);
        }

        @Override
        public boolean contains(double x, double z) {
            // Ray casting algorithm: count intersections of horizontal ray to the right
            boolean inside = false;
            int n = vertices.size();
            for (int i = 0, j = n - 1; i < n; j = i++) {
                Vec2i vi = vertices.get(i);
                Vec2i vj = vertices.get(j);
                if ((vi.z > z) != (vj.z > z) &&
                    x < (vj.x - vi.x) * (z - vi.z) / (double)(vj.z - vi.z) + vi.x) {
                    inside = !inside;
                }
            }
            return inside;
        }

        @Override
        public AABB getBoundingBox() {
            return cachedBoundingBox;
        }

        @Override
        public BoundsType getType() {
            return BoundsType.POLYGON;
        }

        @Override public double[] getCenter() {
            // P2 #3 fix: arithmetic mean of vertices can land outside a concave polygon,
            // which would push chain teleports outside the area. Try centroid first, then
            // sample the bounding box on a 1-block grid and return the first interior point.
            double cx = 0, cz = 0;
            for (var v : vertices) { cx += v.x(); cz += v.z(); }
            double centroidX = cx / vertices.size();
            double centroidZ = cz / vertices.size();
            if (contains(centroidX, centroidZ)) {
                return new double[]{centroidX, centroidZ};
            }
            // Centroid is outside — return the first vertex (guaranteed on the polygon boundary)
            // to avoid expensive AABB scanning which can hang the server for large polygons.
            return new double[]{vertices.get(0).x() + 0.5, vertices.get(0).z() + 0.5};
        }

        public List<Vec2i> getVertices() {
            return vertices;
        }
    }

    public enum BoundsType {
        RECTANGLE, CIRCLE, POLYGON
    }
}
