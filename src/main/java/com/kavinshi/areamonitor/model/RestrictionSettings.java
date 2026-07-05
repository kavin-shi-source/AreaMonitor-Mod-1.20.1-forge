package com.kavinshi.areamonitor.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RestrictionSettings {
    private volatile boolean enableItemBlacklist = true;
    private volatile boolean blockTeleportCommands = true;
    private Set<String> blockedItems = ConcurrentHashMap.newKeySet();
    private Set<String> blockedCommands = ConcurrentHashMap.newKeySet();

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
     * P3 #4: return an unmodifiable view of the live set so callers cannot
     * mutate internal state without going through add/remove methods.
     * Iteration is still thread-safe due to ConcurrentHashMap backing.
     */
    public Set<String> getBlockedItems() {
        return Collections.unmodifiableSet(blockedItems);
    }

    public void setBlockedItems(Collection<String> blockedItems) {
        this.blockedItems.clear();
        if (blockedItems != null) this.blockedItems.addAll(blockedItems);
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
        this.blockedCommands.clear();
        if (blockedCommands != null) this.blockedCommands.addAll(blockedCommands);
    }

    public boolean addBlockedCommand(String command) {
        return blockedCommands.add(command);
    }

    public boolean removeBlockedCommand(String command) {
        return blockedCommands.remove(command);
    }
}
