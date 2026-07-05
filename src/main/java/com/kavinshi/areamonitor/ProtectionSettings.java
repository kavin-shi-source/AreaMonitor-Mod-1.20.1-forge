package com.kavinshi.areamonitor;

/**
 * Area protection settings for controlling block/PVP/explosion/container/fluid/item behavior.
 * Each boolean field represents a protection type, default false (no protection).
 */
public class ProtectionSettings {
    // P2 #46: volatile — these fields are mutated by command threads and read by
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
}
