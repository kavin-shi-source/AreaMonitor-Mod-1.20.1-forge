package com.kavinshi.areamonitor;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public TriggerConfig() {}

    public TriggerConfig(TriggerConfig other) {
        if (other == null) return;
        this.commands = new ArrayList<>(other.commands);
        this.soundEvent = other.soundEvent;
        this.soundVolume = other.soundVolume;
        this.soundPitch = other.soundPitch;
        this.titleMain = other.titleMain;
        this.titleSub = other.titleSub;
        this.teleportTarget = other.teleportTarget;
        this.actionBar = other.actionBar;
        this.potion = other.potion;
        this.potionDuration = other.potionDuration;
        this.potionAmplifier = other.potionAmplifier;
        this.cooldownTicks = other.cooldownTicks;
        this.debounceTicks = other.debounceTicks;
        this.condition = new TriggerCondition(other.condition);
    }

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

    public void sanitize() {
        if (soundVolume < 0) soundVolume = 0;
        if (soundVolume > 10.0f) soundVolume = 10.0f;
        if (soundPitch < 0) soundPitch = 0;
        if (soundPitch > 10.0f) soundPitch = 10.0f;
        if (potionDuration < 0) potionDuration = 0;
        if (potionDuration > 72000) potionDuration = 72000;
        if (potionAmplifier < 0 || potionAmplifier > 127)
            potionAmplifier = Math.max(0, Math.min(127, potionAmplifier));
        if (cooldownTicks < 0) cooldownTicks = 0;
        if (cooldownTicks > 72000) cooldownTicks = 72000;
        if (debounceTicks < 0) debounceTicks = 0;
        if (debounceTicks > 6000) debounceTicks = 6000;
        if (condition != null) condition.sanitize();
    }

    // : equals/hashCode so TriggerConfig can be used in sets/maps and compared in tests
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TriggerConfig that)) return false;
        return Float.compare(that.soundVolume, soundVolume) == 0 &&
               Float.compare(that.soundPitch, soundPitch) == 0 &&
               potionDuration == that.potionDuration &&
               potionAmplifier == that.potionAmplifier &&
               cooldownTicks == that.cooldownTicks &&
               debounceTicks == that.debounceTicks &&
               Objects.equals(commands, that.commands) &&
               Objects.equals(soundEvent, that.soundEvent) &&
               Objects.equals(titleMain, that.titleMain) &&
               Objects.equals(titleSub, that.titleSub) &&
               Objects.equals(teleportTarget, that.teleportTarget) &&
               Objects.equals(actionBar, that.actionBar) &&
               Objects.equals(potion, that.potion) &&
               Objects.equals(condition, that.condition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commands, soundEvent, soundVolume, soundPitch, titleMain, titleSub,
            teleportTarget, actionBar, potion, potionDuration, potionAmplifier,
            cooldownTicks, debounceTicks, condition);
    }

    /**
     * Condition that must be met for the trigger to fire.
     * All non-null fields are AND-ed together.
     */
    public static class TriggerCondition {
        /** Player must have this item (resource location). */
        private String playerHasItem = null;
        /** Min game time (0-24000 ticks). */
        private Integer timeMin = null;
        /** Max game time (0-24000 ticks). */
        private Integer timeMax = null;
        /** Required weather: "clear", "rain", "thunder". */
        private String weather = null;
        /** Minimum online players on the server. */
        private Integer minPlayers = null;

        public TriggerCondition() {}

        public TriggerCondition(TriggerCondition other) {
            if (other == null) return;
            this.playerHasItem = other.playerHasItem;
            this.timeMin = other.timeMin;
            this.timeMax = other.timeMax;
            this.weather = other.weather;
            this.minPlayers = other.minPlayers;
        }

        public String getPlayerHasItem() { return playerHasItem; }
        public void setPlayerHasItem(String v) { this.playerHasItem = v; }

        public Integer getTimeMin() { return timeMin; }
        public void setTimeMin(Integer v) {
            // : validate game time range (0..24000)
            if (v != null && (v < 0 || v > 24000)) {
                throw new IllegalArgumentException("timeMin must be 0..24000, got " + v);
            }
            this.timeMin = v;
        }

        public Integer getTimeMax() { return timeMax; }
        public void setTimeMax(Integer v) {
            if (v != null && (v < 0 || v > 24000)) {
                throw new IllegalArgumentException("timeMax must be 0..24000, got " + v);
            }
            this.timeMax = v;
        }

        public String getWeather() { return weather; }
        public void setWeather(String v) {
            if (v != null && !v.equals("clear") && !v.equals("rain") && !v.equals("thunder")) {
                throw new IllegalArgumentException("weather must be clear/rain/thunder, got " + v);
            }
            this.weather = v;
        }

        public Integer getMinPlayers() { return minPlayers; }
        public void setMinPlayers(Integer v) {
            if (v != null && v < 0) {
                throw new IllegalArgumentException("minPlayers must be >= 0, got " + v);
            }
            this.minPlayers = v;
        }

        public boolean isActive() {
            return playerHasItem != null || timeMin != null || timeMax != null
                || weather != null || minPlayers != null;
        }

        /** Sanitize invalid values that may have bypassed setters during Gson deserialization. */
        public void sanitize() {
            if (timeMin != null && (timeMin < 0 || timeMin > 24000)) {
                AreaMonitorMod.LOGGER.warn("TriggerCondition: invalid timeMin {}, resetting to null", timeMin);
                timeMin = null;
            }
            if (timeMax != null && (timeMax < 0 || timeMax > 24000)) {
                AreaMonitorMod.LOGGER.warn("TriggerCondition: invalid timeMax {}, resetting to null", timeMax);
                timeMax = null;
            }
            if (weather != null && !"clear".equals(weather) && !"rain".equals(weather) && !"thunder".equals(weather)) {
                AreaMonitorMod.LOGGER.warn("TriggerCondition: invalid weather '{}', resetting to null", weather);
                weather = null;
            }
            if (minPlayers != null && minPlayers < 0) {
                AreaMonitorMod.LOGGER.warn("TriggerCondition: invalid minPlayers {}, resetting to null", minPlayers);
                minPlayers = null;
            }
        }
    }
}
