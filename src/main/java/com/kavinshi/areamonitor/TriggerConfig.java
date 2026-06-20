package com.kavinshi.areamonitor;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for area triggers — actions executed when a player enters or leaves an area.
 * Supports commands, sound, title, actionbar, potion, teleport, cooldown, and debounce.
 */
public class TriggerConfig {
    private List<String> commands = new ArrayList<>();
    @SerializedName("sound")
    private String soundEvent = null;
    private float soundVolume = 1.0f;
    private float soundPitch = 1.0f;
    private String titleMain = null;
    private String titleSub = null;
    @SerializedName("teleport")
    private String teleportTarget = null;
    private String actionBar = null;
    private String potion = null;
    private int potionDuration = 200;
    private int potionAmplifier = 0;
    @SerializedName("cooldown")
    private int cooldownTicks = 0;
    @SerializedName("debounce")
    private int debounceTicks = 0;
    private TriggerCondition condition = new TriggerCondition();

    // Getters and Setters
    public List<String> getCommands() { return commands; }
    public void setCommands(List<String> v) { this.commands = v != null ? v : new ArrayList<>(); }

    public String getSoundEvent() { return soundEvent; }
    public void setSoundEvent(String v) { this.soundEvent = v; }

    public float getSoundVolume() { return soundVolume; }
    public void setSoundVolume(float v) { this.soundVolume = v; }

    public float getSoundPitch() { return soundPitch; }
    public void setSoundPitch(float v) { this.soundPitch = v; }

    public String getTitleMain() { return titleMain; }
    public void setTitleMain(String v) { this.titleMain = v; }

    public String getTitleSub() { return titleSub; }
    public void setTitleSub(String v) { this.titleSub = v; }

    public String getTeleportTarget() { return teleportTarget; }
    public void setTeleportTarget(String v) { this.teleportTarget = v; }

    public String getActionBar() { return actionBar; }
    public void setActionBar(String v) { this.actionBar = v; }

    public String getPotion() { return potion; }
    public void setPotion(String v) { this.potion = v; }
    public int getPotionDuration() { return potionDuration; }
    public void setPotionDuration(int v) { this.potionDuration = v; }
    public int getPotionAmplifier() { return potionAmplifier; }
    public void setPotionAmplifier(int v) { this.potionAmplifier = v; }

    public int getCooldownTicks() { return cooldownTicks; }
    public void setCooldownTicks(int v) { this.cooldownTicks = v; }
    public int getDebounceTicks() { return debounceTicks; }
    public void setDebounceTicks(int v) { this.debounceTicks = v; }

    public TriggerCondition getCondition() { return condition; }
    public void setCondition(TriggerCondition v) { this.condition = v != null ? v : new TriggerCondition(); }

    /**
     * Check if this trigger has any actionable configuration.
     */
    public boolean hasAnyAction() {
        return !commands.isEmpty() || soundEvent != null || titleMain != null
            || teleportTarget != null || actionBar != null || potion != null;
    }

    /**
     * Condition that must be met for the trigger to fire.
     * All non-null fields are AND-ed together.
     */
    public static class TriggerCondition {
        /** Player must have this item (resource location). */
        public String playerHasItem = null;
        /** Min game time (0-24000 ticks). */
        public Integer timeMin = null;
        /** Max game time (0-24000 ticks). */
        public Integer timeMax = null;
        /** Required weather: "clear", "rain", "thunder". */
        public String weather = null;
        /** Minimum online players on the server. */
        public Integer minPlayers = null;

        public boolean isActive() {
            return playerHasItem != null || timeMin != null || timeMax != null
                || weather != null || minPlayers != null;
        }
    }
}
