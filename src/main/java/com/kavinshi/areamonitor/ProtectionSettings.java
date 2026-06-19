package com.kavinshi.areamonitor;

/**
 * Area protection settings for controlling block/PVP/explosion behavior.
 * Each boolean field represents a protection type, default false (no protection).
 */
public class ProtectionSettings {
    private boolean blockBreak = false;
    private boolean blockPlace = false;
    private boolean blockInteract = false;
    private boolean pvp = false;
    private boolean explosion = false;
    private boolean entityDamage = false;

    // Getters
    public boolean isBlockBreak() { return blockBreak; }
    public boolean isBlockPlace() { return blockPlace; }
    public boolean isBlockInteract() { return blockInteract; }
    public boolean isPvp() { return pvp; }
    public boolean isExplosion() { return explosion; }
    public boolean isEntityDamage() { return entityDamage; }

    // Setters
    public void setBlockBreak(boolean v) { this.blockBreak = v; }
    public void setBlockPlace(boolean v) { this.blockPlace = v; }
    public void setBlockInteract(boolean v) { this.blockInteract = v; }
    public void setPvp(boolean v) { this.pvp = v; }
    public void setExplosion(boolean v) { this.explosion = v; }
    public void setEntityDamage(boolean v) { this.entityDamage = v; }

    /**
     * Check if any protection type is enabled.
     */
    public boolean isAnyProtectionEnabled() {
        return blockBreak || blockPlace || blockInteract || pvp || explosion || entityDamage;
    }
}
