package com.kavinshi.areamonitor;

import java.util.Objects;

/**
 * Area protection settings for controlling block/PVP/explosion/container/fluid/item behavior.
 * Each boolean field represents a protection type, default false (no protection).
 */
public class ProtectionSettings {

    public enum ProtectionType {
        BLOCK_BREAK,
        BLOCK_PLACE,
        BLOCK_INTERACT,
        PVP,
        EXPLOSION,
        ENTITY_DAMAGE,
        CONTAINER_INTERACT,
        FLUID_PLACE,
        ITEM_DROP
    }

    // : volatile — these fields are mutated by command threads and read by
    // event handlers (world tick / Forge event bus) on potentially different threads.
    private volatile boolean blockBreak = false;
    private volatile boolean blockPlace = false;
    private volatile boolean blockInteract = false;
    private volatile boolean pvp = false;
    private volatile boolean explosion = false;
    private volatile boolean entityDamage = false;
    private volatile boolean containerInteract = false;
    private volatile boolean fluidPlace = false;
    private volatile boolean itemDrop = false;

    public ProtectionSettings() {}

    public ProtectionSettings(ProtectionSettings other) {
        if (other == null) return;
        this.blockBreak = other.blockBreak;
        this.blockPlace = other.blockPlace;
        this.blockInteract = other.blockInteract;
        this.pvp = other.pvp;
        this.explosion = other.explosion;
        this.entityDamage = other.entityDamage;
        this.containerInteract = other.containerInteract;
        this.fluidPlace = other.fluidPlace;
        this.itemDrop = other.itemDrop;
    }

    // Getters
    public boolean isBlockBreak() { return blockBreak; }
    public boolean isBlockPlace() { return blockPlace; }
    public boolean isBlockInteract() { return blockInteract; }
    public boolean isPvp() { return pvp; }
    public boolean isExplosion() { return explosion; }
    public boolean isEntityDamage() { return entityDamage; }
    public boolean isContainerInteract() { return containerInteract; }
    public boolean isFluidPlace() { return fluidPlace; }
    public boolean isItemDrop() { return itemDrop; }

    // Setters
    public void setBlockBreak(boolean v) { this.blockBreak = v; }
    public void setBlockPlace(boolean v) { this.blockPlace = v; }
    public void setBlockInteract(boolean v) { this.blockInteract = v; }
    public void setPvp(boolean v) { this.pvp = v; }
    public void setExplosion(boolean v) { this.explosion = v; }
    public void setEntityDamage(boolean v) { this.entityDamage = v; }
    public void setContainerInteract(boolean v) { this.containerInteract = v; }
    public void setFluidPlace(boolean v) { this.fluidPlace = v; }
    public void setItemDrop(boolean v) { this.itemDrop = v; }

    /**
     * Check if any protection type is enabled.
     */
    public boolean isAnyProtectionEnabled() {
        return blockBreak || blockPlace || blockInteract || pvp || explosion
            || entityDamage || containerInteract || fluidPlace || itemDrop;
    }

    public boolean matches(ProtectionType type) {
        return switch (type) {
            case BLOCK_BREAK -> blockBreak;
            case BLOCK_PLACE -> blockPlace;
            case BLOCK_INTERACT -> blockInteract;
            case PVP -> pvp;
            case EXPLOSION -> explosion;
            case ENTITY_DAMAGE -> entityDamage;
            case CONTAINER_INTERACT -> containerInteract;
            case FLUID_PLACE -> fluidPlace;
            case ITEM_DROP -> itemDrop;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProtectionSettings p)) return false;
        return blockBreak == p.blockBreak && blockPlace == p.blockPlace
            && blockInteract == p.blockInteract && pvp == p.pvp
            && explosion == p.explosion && entityDamage == p.entityDamage
            && containerInteract == p.containerInteract && fluidPlace == p.fluidPlace
            && itemDrop == p.itemDrop;
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockBreak, blockPlace, blockInteract, pvp, explosion,
            entityDamage, containerInteract, fluidPlace, itemDrop);
    }
}
