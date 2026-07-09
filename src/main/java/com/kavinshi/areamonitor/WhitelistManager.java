package com.kavinshi.areamonitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    private static volatile long lastAutoSave = 0;
    private static volatile int saveFailures = 0;
    private static final long AUTO_SAVE_INTERVAL_MS = 30000; // 30 seconds
    private static final int MAX_SAVE_RETRIES = 5;

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
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!dirty) return;
        if (saveFailures >= MAX_SAVE_RETRIES) return;

        long now = System.currentTimeMillis();
        long backoff = saveFailures > 0 ? AUTO_SAVE_INTERVAL_MS * (1L << Math.min(saveFailures, 4)) : AUTO_SAVE_INTERVAL_MS;
        if (now - lastAutoSave >= backoff) {
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

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(whitelistFile), StandardCharsets.UTF_8)) {
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(oldFile), StandardCharsets.UTF_8))) {
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
                    } else if (minecraftServer != null && !minecraftServer.usesAuthentication()) {
                        // Only use offline-mode UUID on offline-mode servers
                        AreaMonitorMod.LOGGER.warn("Offline-mode server: storing offline UUID for player '{}'", name);
                        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
                        whitelistEntries.put(offlineUuid, name);
                        nameIndex.put(name.toLowerCase(), offlineUuid);
                    } else {
                        AreaMonitorMod.LOGGER.warn("Cannot resolve UUID for offline player '{}' on online-mode server, skipping", name);
                    }
                }
            }
            if (!whitelistEntries.isEmpty()) {
                saveWhitelist();
                AreaMonitorMod.LOGGER.info("Migrated {} entries from old whitelist format", whitelistEntries.size());
                // : delete the old .txt file to avoid future confusion and re-migration
                if (!oldFile.delete()) {
                    AreaMonitorMod.LOGGER.warn("Failed to delete old whitelist file after migration: {}", oldFile.getAbsolutePath());
                }
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

            // Atomic write: write to temp file then move into place
            File tempFile = new File(whitelistFile.getParentFile(), whitelistFile.getName() + ".tmp");
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                GSON.toJson(toSave, writer);
            }
            Files.move(tempFile.toPath(), whitelistFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            dirty = false;
            saveFailures = 0;
            AreaMonitorMod.LOGGER.debug("Whitelist saved ({} entries)", whitelistEntries.size());
        } catch (IOException e) {
            saveFailures++;
            if (saveFailures >= MAX_SAVE_RETRIES) {
                AreaMonitorMod.LOGGER.error("Whitelist save failed {} times, giving up. Changes may be lost on restart.", saveFailures, e);
                dirty = false;
            } else {
                AreaMonitorMod.LOGGER.error("Failed to save whitelist (attempt {}/{}): {}", saveFailures, MAX_SAVE_RETRIES, e.getMessage());
            }
            lastAutoSave = System.currentTimeMillis();
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
        // : prefer UUID match; only fall back to name lookup if the name index
        // points to a UUID that matches this player's UUID (avoids name collision on
        // offline-mode servers where two different players may share a name).
        if (isWhitelisted(player.getUUID())) return true;
        UUID indexed = nameIndex.get(player.getName().getString().toLowerCase());
        return indexed != null && indexed.equals(player.getUUID()) && whitelistEntries.containsKey(indexed);
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
            if (minecraftServer != null && minecraftServer.usesAuthentication()) {
                AreaMonitorMod.LOGGER.warn("Cannot add offline player '{}' on online-mode server: UUID unknown until login", playerName);
                return false;
            }
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
            AreaMonitorMod.LOGGER.info("Player '{}' not online (offline-mode server), stored with offline UUID. Will update on next login.", playerName);
        }

        nameIndex.put(nameLower, uuid);
        whitelistEntries.put(uuid, playerName);
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

    /**
     * : clear all runtime state — called from the central shutdown handler in
     * AreaMonitorMod so cross-class handler ordering is no longer a concern.
     */
    public static void clearAllData() {
        whitelistEntries.clear();
        nameIndex.clear();
        dirty = false;
        saveFailures = 0;
        lastAutoSave = 0;
        minecraftServer = null;
    }
}
