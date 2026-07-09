package com.kavinshi.areamonitor.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RestrictionSettings {
    private volatile boolean enableItemBlacklist = true;
    private volatile boolean blockTeleportCommands = true;
    private volatile Set<String> blockedItems = ConcurrentHashMap.newKeySet();
    private volatile Set<String> blockedCommands = ConcurrentHashMap.newKeySet();

    public RestrictionSettings() {}

    public RestrictionSettings(RestrictionSettings other) {
        if (other == null) return;
        this.enableItemBlacklist = other.enableItemBlacklist;
        this.blockTeleportCommands = other.blockTeleportCommands;
        Set<String> newItems = ConcurrentHashMap.newKeySet();
        newItems.addAll(other.blockedItems);
        this.blockedItems = newItems;
        Set<String> newCommands = ConcurrentHashMap.newKeySet();
        newCommands.addAll(other.blockedCommands);
        this.blockedCommands = newCommands;
    }

    public boolean isEnableItemBlacklist() {
        return enableItemBlacklist;
    }

    public void setEnableItemBlacklist(boolean enableItemBlacklist) {
        this.enableItemBlacklist = enableItemBlacklist;
    }

    public boolean isBlockTeleportCommands() {
        return blockTeleportCommands;
    }

    public void setBlockTeleportCommands(boolean blockTeleportCommands) {
        this.blockTeleportCommands = blockTeleportCommands;
    }

    /**
     * : return an unmodifiable view of the live set so callers cannot
     * mutate internal state without going through add/remove methods.
     * Iteration is still thread-safe due to ConcurrentHashMap backing.
     */
    public Set<String> getBlockedItems() {
        return Collections.unmodifiableSet(blockedItems);
    }

    public void setBlockedItems(Collection<String> blockedItems) {
        Set<String> newSet = ConcurrentHashMap.newKeySet();
        if (blockedItems != null) newSet.addAll(blockedItems);
        this.blockedItems = newSet;
    }

    public boolean addBlockedItem(String item) {
        return blockedItems.add(item);
    }

    public boolean removeBlockedItem(String item) {
        return blockedItems.remove(item);
    }

    public Set<String> getBlockedCommands() {
        return Collections.unmodifiableSet(blockedCommands);
    }

    public void setBlockedCommands(Collection<String> blockedCommands) {
        Set<String> newSet = ConcurrentHashMap.newKeySet();
        if (blockedCommands != null) newSet.addAll(blockedCommands);
        this.blockedCommands = newSet;
    }

    public boolean addBlockedCommand(String command) {
        return blockedCommands.add(command);
    }

    public boolean removeBlockedCommand(String command) {
        return blockedCommands.remove(command);
    }

    /**
     * Rebuild blocked sets as ConcurrentHashMap-backed sets.
     * Gson deserialization bypasses setters and creates LinkedHashSet,
     * which breaks the thread-safety guarantee of volatile Set fields.
     */
    public void sanitize() {
        Set<String> newItems = ConcurrentHashMap.newKeySet();
        newItems.addAll(blockedItems);
        this.blockedItems = newItems;

        Set<String> newCommands = ConcurrentHashMap.newKeySet();
        newCommands.addAll(blockedCommands);
        this.blockedCommands = newCommands;
    }
}
