package com.kavinshi.areamonitor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Executes area trigger actions when players enter or leave monitored areas.
 * Includes anti-recursion protection: each player can only trigger once per tick.
 */
public class AreaTriggerManager {

    private static final Set<UUID> triggeredThisTick = new HashSet<>();

    private AreaTriggerManager() {}

    /**
     * Execute enter triggers for a player entering an area.
     */
    public static void executeEnterTriggers(ServerPlayer player, MonitorArea area) {
        if (!area.hasEnterTrigger()) return;
        if (!tryAcquireTriggerLock(player.getUUID())) return;
        executeTrigger(player, area.getEnterTrigger());
    }

    /**
     * Execute leave triggers for a player leaving an area.
     */
    public static void executeLeaveTriggers(ServerPlayer player, MonitorArea area) {
        if (!area.hasLeaveTrigger()) return;
        if (!tryAcquireTriggerLock(player.getUUID())) return;
        executeTrigger(player, area.getLeaveTrigger());
    }

    /**
     * Prevent the same player from triggering multiple times within the same tick.
     */
    private static boolean tryAcquireTriggerLock(UUID playerId) {
        return triggeredThisTick.add(playerId);
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

        // 4. Teleport
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
}
