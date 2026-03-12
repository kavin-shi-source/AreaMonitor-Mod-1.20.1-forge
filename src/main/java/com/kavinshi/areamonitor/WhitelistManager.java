package com.kavinshi.areamonitor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whitelist manager for controlling which players are exempt from area monitoring.
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WhitelistManager {
    private static final Set<String> playerWhitelist = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> whitelistUUIDs = ConcurrentHashMap.newKeySet();
    private static File whitelistFile;
    private static MinecraftServer minecraftServer;

    private static File getWhitelistFile() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("areamonitor");
        if (!configDir.toFile().exists()) {
            configDir.toFile().mkdirs();
        }
        return configDir.resolve("whitelist.txt").toFile();
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        minecraftServer = event.getServer();
        whitelistFile = getWhitelistFile();
        loadWhitelist();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        saveWhitelist();
        minecraftServer = null;
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (isWhitelisted(player.getName().getString())) {
                whitelistUUIDs.add(player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            whitelistUUIDs.remove(player.getUUID());
        }
    }

    public static void loadWhitelist() {
        playerWhitelist.clear();
        whitelistUUIDs.clear();

        if (whitelistFile == null) {
            whitelistFile = getWhitelistFile();
        }

        if (!whitelistFile.exists()) {
            return;
        }

        try (FileReader fileReader = new FileReader(whitelistFile);
             BufferedReader reader = new BufferedReader(fileReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    playerWhitelist.add(line.toLowerCase());
                }
            }

            if (minecraftServer != null) {
                for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
                    if (isWhitelisted(player.getName().getString())) {
                        whitelistUUIDs.add(player.getUUID());
                    }
                }
            }
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.error("Failed to load whitelist file", e);
        }
    }

    public static void saveWhitelist() {
        if (whitelistFile == null) {
            whitelistFile = getWhitelistFile();
        }

        try {
            File parentDir = whitelistFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                AreaMonitorMod.LOGGER.error("Failed to create config directory: {}", parentDir.getAbsolutePath());
                return;
            }

            try (FileWriter fileWriter = new FileWriter(whitelistFile);
                 BufferedWriter writer = new BufferedWriter(fileWriter)) {
                writer.write("# Area Monitor Mod Whitelist");
                writer.newLine();
                writer.write("# One player name per line (case insensitive)");
                writer.newLine();
                writer.newLine();

                for (String name : playerWhitelist) {
                    writer.write(name);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.error("Failed to save whitelist file: {}", whitelistFile.getAbsolutePath(), e);
        }
    }

    public static boolean isWhitelisted(String playerName) {
        return playerWhitelist.contains(playerName.toLowerCase());
    }

    public static boolean isWhitelisted(UUID playerUUID) {
        return whitelistUUIDs.contains(playerUUID);
    }

    public static boolean isWhitelisted(ServerPlayer player) {
        return isWhitelisted(player.getUUID()) || isWhitelisted(player.getName().getString());
    }

    public static boolean addToWhitelist(String playerName) {
        if (playerWhitelist.add(playerName.toLowerCase())) {
            if (minecraftServer != null) {
                ServerPlayer player = minecraftServer.getPlayerList().getPlayerByName(playerName);
                if (player != null) {
                    whitelistUUIDs.add(player.getUUID());
                }
            }
            saveWhitelist();
            return true;
        }
        return false;
    }

    public static boolean removeFromWhitelist(String playerName) {
        if (playerWhitelist.remove(playerName.toLowerCase())) {
            if (minecraftServer != null) {
                ServerPlayer player = minecraftServer.getPlayerList().getPlayerByName(playerName);
                if (player != null) {
                    whitelistUUIDs.remove(player.getUUID());
                }
            }
            saveWhitelist();
            return true;
        }
        return false;
    }

    public static Set<String> getWhitelist() {
        return new HashSet<>(playerWhitelist);
    }

    public static void clearWhitelist() {
        playerWhitelist.clear();
        whitelistUUIDs.clear();
        saveWhitelist();
    }
}
