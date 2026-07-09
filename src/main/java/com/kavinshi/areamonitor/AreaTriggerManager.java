package com.kavinshi.areamonitor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes area trigger actions when players enter or leave monitored areas.
 * Includes anti-recursion protection: each player can only trigger once per tick.
 * Supports cooldown (minimum interval between triggers) and debounce (delay before execution).
 */
public class AreaTriggerManager {

    private static final Set<TriggerLockKey> triggeredThisTick = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();
    private static final Map<DebounceKey, Long> debounceMap = new ConcurrentHashMap<>();
    private static volatile long currentTick = 0;
    private static final long COOLDOWN_EXPIRY_TICKS = 72000L;
    private static long lastCooldownCleanup = 0;

    private record DebounceKey(UUID playerId, String areaName, boolean isEnter) {}
    // P2 #8 fix: per-(player, area, direction) lock so a player entering one area and leaving
    // another in the same tick no longer has the second trigger silently dropped.
    private record TriggerLockKey(UUID playerId, String areaName, boolean isEnter) {}

    private AreaTriggerManager() {}

    /**
     * Execute enter triggers for a player entering an area.
     */
    public static void executeEnterTriggers(ServerPlayer player, MonitorArea area) {
        if (!area.hasEnterTrigger()) return;
        if (!tryAcquireTriggerLock(player.getUUID(), area.getName(), true)) return;
        TriggerConfig config = area.getEnterTrigger();
        if (!checkCooldown(player.getUUID(), area.getName(), config, true)) return;
        if (config.getDebounceTicks() > 0) {
            scheduleDebounce(player, area, config, true);
        } else {
            executeTrigger(player, config);
        }
    }

    /**
     * Execute leave triggers for a player leaving an area.
     */
    public static void executeLeaveTriggers(ServerPlayer player, MonitorArea area) {
        if (!area.hasLeaveTrigger()) return;
        if (!tryAcquireTriggerLock(player.getUUID(), area.getName(), false)) return;
        TriggerConfig config = area.getLeaveTrigger();
        if (!checkCooldown(player.getUUID(), area.getName(), config, false)) return;
        if (config.getDebounceTicks() > 0) {
            scheduleDebounce(player, area, config, false);
        } else {
            executeTrigger(player, config);
        }
    }

    /**
     * Prevent the same (player, area, direction) tuple from triggering multiple times within the same tick.
     */
    private static boolean tryAcquireTriggerLock(UUID playerId, String areaName, boolean isEnter) {
        return triggeredThisTick.add(new TriggerLockKey(playerId, areaName, isEnter));
    }

    /**
     * Update current tick count. Called once per server tick by AreaMonitor.
     */
    public static void setCurrentTick(long tick) {
        currentTick = tick;
    }

    /**
     * Check cooldown. Returns false if still cooling down.
     */
    private static boolean checkCooldown(UUID playerId, String areaName, TriggerConfig config, boolean isEnter) {
        if (config.getCooldownTicks() <= 0) return true;
        String key = playerId + ":" + areaName + ":" + (isEnter ? "enter" : "leave");
        long now = currentTick;
        Long last = cooldownMap.get(key);
        if (last != null && (now - last) < config.getCooldownTicks()) {
            return false;
        }
        cooldownMap.put(key, now);
        return true;
    }

    /**
     * Schedule a debounced trigger execution.
     */
    private static void scheduleDebounce(ServerPlayer player, MonitorArea area, TriggerConfig config, boolean isEnter) {
        DebounceKey key = new DebounceKey(player.getUUID(), area.getName(), isEnter);
        long execTick = currentTick + config.getDebounceTicks();
        // Simple: overwrite previous pending task for same key
        debounceMap.put(key, execTick);
    }

    /**
     * Process pending debounced triggers. Called each server tick.
     */
    public static void processDebouncedTriggers(net.minecraft.server.MinecraftServer server) {
        long now = currentTick;
        List<DebounceKey> toRemove = new ArrayList<>();
        for (var entry : debounceMap.entrySet()) {
            if (now >= entry.getValue()) {
                toRemove.add(entry.getKey());
                DebounceKey key = entry.getKey();
                ServerPlayer player = server.getPlayerList().getPlayer(key.playerId());
                if (player != null) {
                    MonitorArea area = AreaManager.getInstance().getArea(key.areaName());
                    if (area != null) {
                        TriggerConfig config = key.isEnter()
                            ? area.getEnterTrigger() : area.getLeaveTrigger();
                        if (config != null) {
                            // P2 #9 fix: skip if player's state no longer matches the trigger direction.
                            // Enter triggers require the player to still be inside the area;
                            // leave triggers require them to still be outside. This prevents stale
                            // debounces from firing after the player has already moved on.
                            boolean stillInArea = AreaManager.getInstance().getCurrentAreas(player).contains(key.areaName());
                            if (key.isEnter() && !stillInArea) {
                                AreaMonitorMod.LOGGER.debug("Skipping debounced enter trigger for area '{}' — player {} is no longer in the area",
                                    area.getName(), player.getName().getString());
                                continue;
                            }
                            if (!key.isEnter() && stillInArea) {
                                AreaMonitorMod.LOGGER.debug("Skipping debounced leave trigger for area '{}' — player {} re-entered the area",
                                    area.getName(), player.getName().getString());
                                continue;
                            }
                            // P1-9 fix: wrap executeTrigger so a single failure does not skip toRemove cleanup / cooldown cleanup
                            try {
                                executeTrigger(player, config);
                            } catch (Exception ex) {
                                AreaMonitorMod.LOGGER.error("Failed to execute {} trigger for area '{}' on player '{}'",
                                    key.isEnter() ? "enter" : "leave", area.getName(), player.getName().getString(), ex);
                            }
                        }
                    }
                }
            }
        }
        for (DebounceKey key : toRemove) debounceMap.remove(key);

        if (now - lastCooldownCleanup >= 6000L) {
            cooldownMap.entrySet().removeIf(e -> now - e.getValue() > COOLDOWN_EXPIRY_TICKS);
            lastCooldownCleanup = now;
        }
    }

    /**
     * Clear all trigger locks. Should be called at the start of each server tick.
     */
    public static void clearTickLocks() {
        triggeredThisTick.clear();
    }

    /**
     * Clear all trigger state. Should be called on server stopping to prevent
     * cross-world data leakage in integrated server scenarios.
     */
    public static void clearAll() {
        triggeredThisTick.clear();
        cooldownMap.clear();
        debounceMap.clear();
        lastCooldownCleanup = 0;
    }

    /**
     * Clear trigger state for a specific player. Should be called on player logout
     * to prevent memory leaks from accumulating per-player entries.
     */
    public static void clearPlayer(UUID playerId) {
        String prefix = playerId.toString() + ":";
        cooldownMap.keySet().removeIf(k -> k.startsWith(prefix));
        debounceMap.keySet().removeIf(k -> k.playerId().equals(playerId));
    }

    /**
     * Execute all configured trigger actions for a player.
     */
    private static void executeTrigger(ServerPlayer player, TriggerConfig config) {
        // 0. Check conditions
        if (!checkConditions(player, config.getCondition())) return;

        // 1. Execute commands — use the player's own command source so permissions
        // and selector context (@p) are correct. Elevate permission to 2 to allow command execution.
        MinecraftServer server = player.getServer();
        if (server != null && !config.getCommands().isEmpty()) {
            CommandSourceStack source = player.createCommandSourceStack().withPermission(2);
            for (String cmd : config.getCommands()) {
                try {
                    server.getCommands().performPrefixedCommand(source, cmd);
                } catch (Exception e) {
                    AreaMonitorMod.LOGGER.error("Error executing trigger command: {}", cmd, e);
                }
            }
        }

        // 2. Play sound
        if (config.getSoundEvent() != null) {
            try {
                ResourceLocation soundId = new ResourceLocation(config.getSoundEvent());
                SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundId);
                if (sound != null) {
                    player.playNotifySound(sound, SoundSource.MASTER,
                        config.getSoundVolume(), config.getSoundPitch());
                }
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.error("Error playing trigger sound", e);
            }
        }

        // 3. Show title
        if (config.getTitleMain() != null) {
            try {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 10));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(config.getTitleMain())));
                if (config.getTitleSub() != null) {
                    player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(config.getTitleSub())));
                }
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.error("Error showing trigger title", e);
            }
        }

        // 4. ActionBar message
        if (config.getActionBar() != null) {
            try {
                player.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal(config.getActionBar())));
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.error("Error showing trigger action bar", e);
            }
        }

        // 5. Potion effect
        if (config.getPotion() != null) {
            try {
                ResourceLocation potionId = new ResourceLocation(config.getPotion());
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(potionId);
                if (effect != null) {
                    player.addEffect(new MobEffectInstance(effect,
                        config.getPotionDuration(), config.getPotionAmplifier(),
                        false, true));
                }
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.error("Error applying potion effect via trigger", e);
            }
        }

        // 6. Teleport
        if (config.getTeleportTarget() != null && server != null) {
            try {
                String tp = config.getTeleportTarget().trim();
                // P2 #20 fix: support both space-separated (current, from GUI/commands) and
                // comma-separated (legacy configs) formats.
                String[] parts = tp.contains(" ") ? tp.split("\\s+", 4) : tp.split(",", 4);
                if (parts.length == 4) {
                    String dim = parts[0].trim();
                    double x = Double.parseDouble(parts[1].trim());
                    double y = Double.parseDouble(parts[2].trim());
                    double z = Double.parseDouble(parts[3].trim());
                    ResourceLocation dimKey = new ResourceLocation(dim);
                    ServerLevel targetLevel = server.getLevel(
                        net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION, dimKey));
                    if (targetLevel != null) {
                        player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
                    }
                }
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.error("Error teleporting player via trigger", e);
            }
        }
    }

    /**
     * Check if player has a specific item in any inventory slot.
     * Searches main inventory, armor slots, and offhand.
     */
    private static boolean playerHasItem(ServerPlayer player, String itemIdStr) {
        try {
            ResourceLocation resourceId = new ResourceLocation(itemIdStr);
            var item = BuiltInRegistries.ITEM.get(resourceId);
            if (item == null) return false;
            for (var stack : player.getInventory().items) {
                if (stack.getItem() == item) return true;
            }
            for (var stack : player.getInventory().armor) {
                if (stack.getItem() == item) return true;
            }
            for (var stack : player.getInventory().offhand) {
                if (stack.getItem() == item) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Evaluate trigger conditions. Returns true if all conditions pass.
     */
    private static boolean checkConditions(ServerPlayer player, TriggerConfig.TriggerCondition c) {
        if (c == null || !c.isActive()) return true;

        // playerHasItem: check inventory
        // P2 #47: use getters instead of direct field access
        String hasItem = c.getPlayerHasItem();
        if (hasItem != null && !hasItem.isEmpty()) {
            if (!playerHasItem(player, hasItem)) {
                return false;
            }
        }

        // timeMin/timeMax: check game time (supports cross-midnight ranges)
        long time = player.level().getDayTime() % 24000;
        Integer tmin = c.getTimeMin();
        Integer tmax = c.getTimeMax();
        if (tmin != null && tmax != null) {
            if (tmin <= tmax && (time < tmin || time > tmax)) return false;
            if (tmin > tmax && !(time >= tmin || time <= tmax)) return false;
        } else {
            if (tmin != null && time < tmin) return false;
            if (tmax != null && time > tmax) return false;
        }

        // weather
        String w = c.getWeather();
        if (w != null) {
            String actual = player.level().isThundering() ? "thunder" :
                player.level().isRaining() ? "rain" : "clear";
            if (!actual.equals(w)) return false;
        }

        // minPlayers
        Integer mp = c.getMinPlayers();
        if (mp != null) {
            var server = player.getServer();
            if (server != null && server.getPlayerCount() < mp) return false;
        }

        return true;
    }
}
