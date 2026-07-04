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

    private static final Set<UUID> triggeredThisTick = new HashSet<>();
    /** Player+Area → last trigger time (for cooldown) */
    private static final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();
    /** Player+Area → queued debounce task timestamp */
    private static final Map<String, Long> debounceMap = new ConcurrentHashMap<>();

    private AreaTriggerManager() {}

    /**
     * Execute enter triggers for a player entering an area.
     */
    public static void executeEnterTriggers(ServerPlayer player, MonitorArea area) {
        if (!area.hasEnterTrigger()) return;
        if (!tryAcquireTriggerLock(player.getUUID())) return;
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
        if (!tryAcquireTriggerLock(player.getUUID())) return;
        TriggerConfig config = area.getLeaveTrigger();
        if (!checkCooldown(player.getUUID(), area.getName(), config, false)) return;
        if (config.getDebounceTicks() > 0) {
            scheduleDebounce(player, area, config, false);
        } else {
            executeTrigger(player, config);
        }
    }

    /**
     * Prevent the same player from triggering multiple times within the same tick.
     */
    private static boolean tryAcquireTriggerLock(UUID playerId) {
        return triggeredThisTick.add(playerId);
    }

    /**
     * Check cooldown. Returns false if still cooling down.
     */
    private static boolean checkCooldown(UUID playerId, String areaName, TriggerConfig config, boolean isEnter) {
        if (config.getCooldownTicks() <= 0) return true;
        String key = playerId + ":" + areaName + ":" + (isEnter ? "enter" : "leave");
        long now = System.currentTimeMillis();
        Long last = cooldownMap.get(key);
        if (last != null && (now - last) < config.getCooldownTicks() * 50L) {
            return false;
        }
        cooldownMap.put(key, now);
        return true;
    }

    /**
     * Schedule a debounced trigger execution.
     */
    private static void scheduleDebounce(ServerPlayer player, MonitorArea area, TriggerConfig config, boolean isEnter) {
        String key = player.getUUID() + ":" + area.getName() + ":" + (isEnter ? "enter" : "leave");
        long execTime = System.currentTimeMillis() + config.getDebounceTicks() * 50L;
        // Simple: overwrite previous pending task for same key
        debounceMap.put(key, execTime);
    }

    /**
     * Process pending debounced triggers. Called each server tick.
     */
    public static void processDebouncedTriggers(net.minecraft.server.MinecraftServer server) {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();
        for (var entry : debounceMap.entrySet()) {
            if (now >= entry.getValue()) {
                toRemove.add(entry.getKey());
                // Parse key: uuid:areaName:direction
                String[] parts = entry.getKey().split(":", 3);
                if (parts.length == 3) {
                    ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(parts[0]));
                    if (player != null) {
                        MonitorArea area = AreaManager.getInstance().getArea(parts[1]);
                        if (area != null) {
                            TriggerConfig config = "enter".equals(parts[2])
                                ? area.getEnterTrigger() : area.getLeaveTrigger();
                            if (config != null) {
                                executeTrigger(player, config);
                            }
                        }
                    }
                }
            }
        }
        for (String key : toRemove) debounceMap.remove(key);
    }

    /**
     * Clear all trigger locks. Should be called at the start of each server tick.
     */
    public static void clearTickLocks() {
        triggeredThisTick.clear();
    }

    /**
     * Execute all configured trigger actions for a player.
     */
    private static void executeTrigger(ServerPlayer player, TriggerConfig config) {
        // 0. Check conditions
        if (!checkConditions(player, config.getCondition())) return;

        // 1. Execute commands
        MinecraftServer server = player.getServer();
        if (server != null && !config.getCommands().isEmpty()) {
            CommandSourceStack source = server.createCommandSourceStack()
                .withPosition(new Vec3(player.getX(), player.getY(), player.getZ()))
                .withRotation(new Vec2(player.getXRot(), player.getYRot()))
                .withLevel((ServerLevel) player.level());
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
                ResourceLocation soundId = ResourceLocation.tryParse(config.getSoundEvent());
                if (soundId != null) {
                    SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundId);
                    if (sound != null) {
                        player.playNotifySound(sound, SoundSource.MASTER,
                            config.getSoundVolume(), config.getSoundPitch());
                    }
                }
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.error("Error playing trigger sound", e);
            }
        }

        // 3. Show title
        if (config.getTitleMain() != null) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(config.getTitleMain())));
            if (config.getTitleSub() != null) {
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(config.getTitleSub())));
            }
        }

        // 4. ActionBar message
        if (config.getActionBar() != null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(
                Component.literal(config.getActionBar())));
        }

        // 5. Potion effect
        if (config.getPotion() != null) {
            try {
                ResourceLocation potionId = ResourceLocation.tryParse(config.getPotion());
                if (potionId != null) {
                    MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(potionId);
                    if (effect != null) {
                        player.addEffect(new MobEffectInstance(effect,
                            config.getPotionDuration(), config.getPotionAmplifier(),
                            false, true));
                    }
                }
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.error("Error applying potion effect via trigger", e);
            }
        }

        // 6. Teleport
        if (config.getTeleportTarget() != null && server != null) {
            try {
                String[] parts = config.getTeleportTarget().split(",", 4);
                if (parts.length == 4) {
                    String dim = parts[0];
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    ResourceLocation dimKey = ResourceLocation.tryParse(dim);
                    if (dimKey != null) {
                        ServerLevel targetLevel = server.getLevel(
                            net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION, dimKey));
                        if (targetLevel != null) {
                            player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
                        }
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
        ResourceLocation resourceId = ResourceLocation.tryParse(itemIdStr);
        if (resourceId == null) return false;
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
    }

    /**
     * Evaluate trigger conditions. Returns true if all conditions pass.
     */
    private static boolean checkConditions(ServerPlayer player, TriggerConfig.TriggerCondition c) {
        if (c == null || !c.isActive()) return true;

        // playerHasItem: check inventory
        if (c.playerHasItem != null && !c.playerHasItem.isEmpty()) {
            if (!playerHasItem(player, c.playerHasItem)) {
                return false;
            }
        }

        // timeMin/timeMax: check game time (supports cross-midnight ranges)
        long time = player.level().getDayTime() % 24000;
        if (c.timeMin != null && c.timeMax != null) {
            if (c.timeMin <= c.timeMax && (time < c.timeMin || time > c.timeMax)) return false;
            if (c.timeMin > c.timeMax && !(time >= c.timeMin || time <= c.timeMax)) return false;
        } else {
            if (c.timeMin != null && time < c.timeMin) return false;
            if (c.timeMax != null && time > c.timeMax) return false;
        }

        // weather
        if (c.weather != null) {
            String actual = player.level().isThundering() ? "thunder" :
                player.level().isRaining() ? "rain" : "clear";
            if (!actual.equals(c.weather)) return false;
        }

        // minPlayers
        if (c.minPlayers != null) {
            var server = player.getServer();
            if (server != null && server.getPlayerCount() < c.minPlayers) return false;
        }

        return true;
    }
}
