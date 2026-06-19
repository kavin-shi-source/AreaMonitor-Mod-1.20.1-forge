package com.kavinshi.areamonitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whitelist manager for controlling which players are exempt from area monitoring.
 *
 * <p>Stores whitelist as JSON with UUID as primary key, enabling reliable
 * identification even after player name changes. Uses delayed write strategy
 * to avoid excessive disk I/O during batch operations.</p>
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WhitelistManager {
    /** UUID → username mapping (primary storage) */
    private static final Map<UUID, String> whitelistEntries = new ConcurrentHashMap<>();
    /** Reverse lookup: lowercase username → UUID */
    private static final Map<String, UUID> nameIndex = new ConcurrentHashMap<>();
    private static File whitelistFile;
    private static MinecraftServer minecraftServer;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Dirty flag for delayed write strategy */
    private static volatile boolean dirty = false;
    private static long lastAutoSave = 0;
    private static final long AUTO_SAVE_INTERVAL_MS = 30000; // 30 seconds

    private static File getWhitelistFile() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("areamonitor");
        if (!configDir.toFile().exists()) {
            configDir.toFile().mkdirs();
        }
        return configDir.resolve("whitelist.json").toFile();
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
        whitelistEntries.clear();
        nameIndex.clear();
        minecraftServer = null;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!dirty) return;

        long now = System.currentTimeMillis();
        if (now - lastAutoSave >= AUTO_SAVE_INTERVAL_MS) {
            saveWhitelist();
            lastAutoSave = now;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            updatePlayerNameCache(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // UUID-based entries persist across sessions, no removal needed
    }

    /**
     * Update the name cache for a player. When a player logs in with a UUID
     * that is already in the whitelist, update their stored name.
     */
    private static void updatePlayerNameCache(ServerPlayer player) {
        UUID uuid = player.getUUID();
        String currentName = player.getName().getString();
        String storedName = whitelistEntries.get(uuid);
        if (storedName != null && !storedName.equals(currentName)) {
            whitelistEntries.put(uuid, currentName);
            nameIndex.remove(storedName.toLowerCase());
            nameIndex.put(currentName.toLowerCase(), uuid);
            markDirty();
            AreaMonitorMod.LOGGER.info("Updated whitelist player name: {} → {}", storedName, currentName);
        }
    }

    public static void loadWhitelist() {
        whitelistEntries.clear();
        nameIndex.clear();

        if (whitelistFile == null) {
            whitelistFile = getWhitelistFile();
        }

        if (!whitelistFile.exists()) {
            // Try to migrate from old txt format
            File oldFile = getOldWhitelistFile();
            if (oldFile.exists()) {
                migrateFromOldFormat(oldFile);
            }
            return;
        }

        try (FileReader reader = new FileReader(whitelistFile)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                for (Map.Entry<String, String> entry : loaded.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        String name = entry.getValue();
                        whitelistEntries.put(uuid, name);
                        nameIndex.put(name.toLowerCase(), uuid);
                    } catch (IllegalArgumentException e) {
                        AreaMonitorMod.LOGGER.warn("Invalid UUID in whitelist: {}", entry.getKey());
                    }
                }
            }
            AreaMonitorMod.LOGGER.info("Loaded {} whitelist entries", whitelistEntries.size());
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.error("Failed to load whitelist file", e);
        }
    }

    /**
     * Migrate from old whitelist.txt format to new whitelist.json format.
     * Old format: one player name per line.
     * New format: JSON Map<UUID, String>.
     */
    private static void migrateFromOldFormat(File oldFile) {
        AreaMonitorMod.LOGGER.info("Migrating whitelist from old format: {}", oldFile.getAbsolutePath());
        try (BufferedReader reader = new BufferedReader(new FileReader(oldFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    // For old entries without UUID, resolve from online players
                    String name = line;
                    UUID uuid = null;
                    if (minecraftServer != null) {
                        ServerPlayer player = minecraftServer.getPlayerList().getPlayerByName(name);
                        if (player != null) {
                            uuid = player.getUUID();
                        }
                    }
                    if (uuid != null) {
                        whitelistEntries.put(uuid, name);
                        nameIndex.put(name.toLowerCase(), uuid);
                    } else {
                        // Store with offline-mode UUID derived from name for now
                        // This will be corrected when the player next logs in
                        AreaMonitorMod.LOGGER.warn("Cannot resolve UUID for offline player '{}', storing as pending", name);
                        // Use a name-based UUID as placeholder
                        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
                        whitelistEntries.put(offlineUuid, name);
                        nameIndex.put(name.toLowerCase(), offlineUuid);
                    }
                }
            }
            if (!whitelistEntries.isEmpty()) {
                saveWhitelist();
                AreaMonitorMod.LOGGER.info("Migrated {} entries from old whitelist format", whitelistEntries.size());
            }
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.error("Failed to migrate whitelist from old format", e);
        }
    }

    private static File getOldWhitelistFile() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("areamonitor");
        return configDir.resolve("whitelist.txt").toFile();
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

            // Convert to JSON-safe format: Map<String, String>
            Map<String, String> toSave = new LinkedHashMap<>();
            for (Map.Entry<UUID, String> entry : whitelistEntries.entrySet()) {
                toSave.put(entry.getKey().toString(), entry.getValue());
            }

            try (FileWriter writer = new FileWriter(whitelistFile)) {
                GSON.toJson(toSave, writer);
            }

            dirty = false;
            AreaMonitorMod.LOGGER.debug("Whitelist saved ({} entries)", whitelistEntries.size());
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.error("Failed to save whitelist file: {}", whitelistFile.getAbsolutePath(), e);
        }
    }

    /**
     * Mark whitelist as dirty for delayed write.
     */
    private static void markDirty() {
        dirty = true;
    }

    public static boolean isWhitelisted(String playerName) {
        UUID uuid = nameIndex.get(playerName.toLowerCase());
        return uuid != null && whitelistEntries.containsKey(uuid);
    }

    public static boolean isWhitelisted(UUID playerUUID) {
        return whitelistEntries.containsKey(playerUUID);
    }

    public static boolean isWhitelisted(ServerPlayer player) {
        return isWhitelisted(player.getUUID()) || isWhitelisted(player.getName().getString());
    }

    /**
     * Add a player to the whitelist by name. Resolves UUID from online players
     * if possible, otherwise uses an offline-mode UUID as placeholder.
     */
    public static boolean addToWhitelist(String playerName) {
        String nameLower = playerName.toLowerCase();

        // Check if already whitelisted by name
        if (nameIndex.containsKey(nameLower)) {
            return false;
        }

        UUID uuid = null;
        if (minecraftServer != null) {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayerByName(playerName);
            if (player != null) {
                uuid = player.getUUID();
            }
        }

        if (uuid == null) {
            // Player not online, use offline UUID as placeholder
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
            AreaMonitorMod.LOGGER.info("Player '{}' not online, stored with offline UUID. Will update on next login.", playerName);
        }

        whitelistEntries.put(uuid, playerName);
        nameIndex.put(nameLower, uuid);
        markDirty();
        return true;
    }

    /**
     * Remove a player from the whitelist by name.
     */
    public static boolean removeFromWhitelist(String playerName) {
        UUID uuid = nameIndex.remove(playerName.toLowerCase());
        if (uuid != null && whitelistEntries.remove(uuid) != null) {
            markDirty();
            return true;
        }
        return false;
    }

    public static Set<String> getWhitelist() {
        return new HashSet<>(whitelistEntries.values());
    }

    public static void clearWhitelist() {
        whitelistEntries.clear();
        nameIndex.clear();
        markDirty();
    }
}
