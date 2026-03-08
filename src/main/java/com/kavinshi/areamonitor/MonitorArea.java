package com.kavinshi.areamonitor;

import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 监控区域类，表示一个独立的监控区域
 */
public class MonitorArea {
    private final String name;
    private String displayName;
    private String dimension;
    private AreaBounds bounds;
    private GameType enterMode;
    private GameType leaveMode;
    private boolean enabled;
    private List<String> whitelist;
    private RestrictionSettings restrictions;

    public MonitorArea(String name) {
        this.name = name;
        this.displayName = name;
        this.dimension = "minecraft:overworld";
        this.bounds = new RectangleBounds(0, 0, 0, 0);
        this.enterMode = GameType.ADVENTURE;
        this.leaveMode = GameType.SURVIVAL;
        this.enabled = true;
        this.whitelist = new ArrayList<>();
        this.restrictions = new RestrictionSettings();
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public AreaBounds getBounds() { return bounds; }
    public void setBounds(AreaBounds bounds) { this.bounds = bounds; }

    public GameType getEnterMode() { return enterMode; }
    public void setEnterMode(GameType enterMode) { this.enterMode = enterMode; }

    public GameType getLeaveMode() { return leaveMode; }
    public void setLeaveMode(GameType leaveMode) { this.leaveMode = leaveMode; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<String> getWhitelist() { return whitelist; }
    public void setWhitelist(List<String> whitelist) { this.whitelist = whitelist; }

    public RestrictionSettings getRestrictions() { return restrictions; }
    public void setRestrictions(RestrictionSettings restrictions) { this.restrictions = restrictions; }

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
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        @Override
        public AABB getBoundingBox() {
            return new AABB(minX, 0, minZ, maxX + 1, 256, maxZ + 1);
        }

        @Override
        public BoundsType getType() {
            return BoundsType.RECTANGLE;
        }

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
            return new AABB(centerX - radius, 0, centerZ - radius,
                          centerX + radius + 1, 256, centerZ + radius + 1);
        }

        @Override
        public BoundsType getType() {
            return BoundsType.CIRCLE;
        }

        public int getCenterX() { return centerX; }
        public int getCenterZ() { return centerZ; }
        public int getRadius() { return radius; }
    }

    public enum BoundsType {
        RECTANGLE, CIRCLE, POLYGON
    }
}

class PlayerPosition {
    private final double x, z;
    private final String dimension;

    public PlayerPosition(double x, double z, String dimension) {
        this.x = x;
        this.z = z;
        this.dimension = dimension;
    }

    public double getX() { return x; }
    public double getZ() { return z; }
    public String getDimension() { return dimension; }
}


class RestrictionSettings {
    private boolean enableItemBlacklist = true;  // 默认启用物品黑名单
    private boolean blockTeleportCommands = true;  // 默认阻止传送命令
    private Set<String> blockedItems = new HashSet<>();
    private Set<String> blockedCommands = new HashSet<>();

    public boolean isEnableItemBlacklist() { return enableItemBlacklist; }
    public void setEnableItemBlacklist(boolean enableItemBlacklist) { this.enableItemBlacklist = enableItemBlacklist; }

    public boolean isBlockTeleportCommands() { return blockTeleportCommands; }
    public void setBlockTeleportCommands(boolean blockTeleportCommands) { this.blockTeleportCommands = blockTeleportCommands; }

    public Set<String> getBlockedItems() { return blockedItems; }
    public void setBlockedItems(Set<String> blockedItems) { this.blockedItems = blockedItems; }

    public Set<String> getBlockedCommands() { return blockedCommands; }
    public void setBlockedCommands(Set<String> blockedCommands) { this.blockedCommands = blockedCommands; }
}