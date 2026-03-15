package com.kavinshi.areamonitor.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RestrictionSettings {
    private boolean enableItemBlacklist = true;
    private boolean blockTeleportCommands = true;
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
