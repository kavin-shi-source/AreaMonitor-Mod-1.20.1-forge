package com.kavinshi.areamonitor.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Area restriction settings class.
 * 
 * <p>Defines restriction rules within an area, such as item blacklist and command restrictions.</p>
 * <p>Thread-safe implementation using ConcurrentHashMap.newKeySet().</p>
 * 
 * @since 1.0.0
 */
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

    public Set<String> getBlockedItems() { 
        return blockedItems; 
    }
    
    public void setBlockedItems(Set<String> blockedItems) { 
        this.blockedItems = blockedItems; 
    }

    public Set<String> getBlockedCommands() { 
        return blockedCommands; 
    }
    
    public void setBlockedCommands(Set<String> blockedCommands) { 
        this.blockedCommands = blockedCommands; 
    }
}
