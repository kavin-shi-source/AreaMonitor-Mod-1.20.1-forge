package com.kavinshi.areamonitor;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for area triggers — actions executed when a player enters or leaves an area.
 * Supports commands, sound, title, and teleport.
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

    /**
     * Check if this trigger has any actionable configuration.
     */
    public boolean hasAnyAction() {
        return !commands.isEmpty() || soundEvent != null || titleMain != null || teleportTarget != null;
    }
}
