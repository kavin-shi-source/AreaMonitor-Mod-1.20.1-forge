package com.kavinshi.areamonitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.kavinshi.areamonitor.model.RestrictionSettings;
import com.kavinshi.areamonitor.util.MessageUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Item blacklist manager that restricts the use of teleportation items in specific areas.
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemBlacklistManager {
    private static final Set<Item> GLOBAL_BLACKLISTED_ITEMS = ConcurrentHashMap.newKeySet();
    private static final Map<String, Set<Item>> AREA_BLACKLISTS = new ConcurrentHashMap<>();
    private static final Set<String> TELEPORT_COMMANDS = ConcurrentHashMap.newKeySet();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File blacklistConfigFile;

    private static volatile boolean initialized = false;

    static {
        TELEPORT_COMMANDS.add("/tp");
        TELEPORT_COMMANDS.add("/teleport");
        TELEPORT_COMMANDS.add("/home");
        TELEPORT_COMMANDS.add("/spawn");
        TELEPORT_COMMANDS.add("/warp");
        TELEPORT_COMMANDS.add("/back");
        TELEPORT_COMMANDS.add("/tpa");
        TELEPORT_COMMANDS.add("/tpaccept");
        TELEPORT_COMMANDS.add("/tpdeny");
    }

    public static void initializeDefaultBlacklist() {
        if (initialized) return;
        initialized = true;

        // P2 #41: keep default set consistent with createDefaultBlacklistConfig()
        // so subsequent loadBlacklistConfig() doesn't wipe items the user expected
        GLOBAL_BLACKLISTED_ITEMS.add(Items.ENDER_PEARL);
        GLOBAL_BLACKLISTED_ITEMS.add(Items.CHORUS_FRUIT);
        GLOBAL_BLACKLISTED_ITEMS.add(Items.COMPASS);
        GLOBAL_BLACKLISTED_ITEMS.add(Items.CLOCK);

        AreaMonitorMod.LOGGER.info("Default item blacklist initialized");
    }

    /**
     * Add custom blacklist for a player area.
     */
    public static void addAreaBlacklist(String areaName, Set<Item> blacklistedItems) {
        // P2 #42: build the set fully before publishing — avoid put-then-get race
        Set<Item> set = ConcurrentHashMap.newKeySet();
        set.addAll(blacklistedItems);
        AREA_BLACKLISTS.put(areaName, set);
    }

    /**
     * Remove area blacklist.
     */
    public static void removeAreaBlacklist(String areaName) {
        AREA_BLACKLISTS.remove(areaName);
    }

    /**
     * Get blacklist items for an area.
     */
    public static Set<Item> getAreaBlacklist(String areaName) {
        var set = AREA_BLACKLISTS.get(areaName);
        return set != null ? Collections.unmodifiableSet(set) : Collections.emptySet();
    }

    /**
     * Check if an item is blacklisted.
     */
    public static boolean isItemBlacklisted(Item item, ServerPlayer player) {
        Set<String> currentAreas = AreaManager.getInstance().getCurrentAreas(player);

        // If not in any area, return false
        if (currentAreas.isEmpty()) {
            return false;
        }

        // Check global blacklist - if in any area with blacklist enabled
        if (GLOBAL_BLACKLISTED_ITEMS.contains(item)) {
            for (String areaName : currentAreas) {
                MonitorArea area = AreaManager.getInstance().getArea(areaName);
                if (area != null && area.getRestrictions().isEnableItemBlacklist()) {
                    return true;
                }
            }
        }

        // Check area-specific blacklist
        // P2 #40: respect enableItemBlacklist toggle for area-specific blacklists too
        for (String areaName : currentAreas) {
            MonitorArea area = AreaManager.getInstance().getArea(areaName);
            if (area == null || !area.getRestrictions().isEnableItemBlacklist()) continue;
            Set<Item> areaBlacklist = AREA_BLACKLISTS.get(areaName);
            if (areaBlacklist != null && areaBlacklist.contains(item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if player is in a restricted area.
     */
    private static boolean isPlayerInRestrictedArea(ServerPlayer player) {
        Set<String> currentAreas = AreaManager.getInstance().getCurrentAreas(player);
        for (String areaName : currentAreas) {
            MonitorArea area = AreaManager.getInstance().getArea(areaName);
            if (area != null && area.getRestrictions().isEnableItemBlacklist()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a command is blocked.
     */
    public static boolean isCommandBlocked(String command, ServerPlayer player) {
        if (command == null || command.isBlank()) return false;
        String baseCommand = command.split(" ")[0].toLowerCase();
        while (baseCommand.startsWith("/")) baseCommand = baseCommand.substring(1);
        String normalized = stripNamespace(baseCommand);

        Set<String> currentAreas = AreaManager.getInstance().getCurrentAreas(player);
        for (String areaName : currentAreas) {
            MonitorArea area = AreaManager.getInstance().getArea(areaName);
            if (area == null) continue;
            RestrictionSettings rs = area.getRestrictions();
            if (rs.isBlockTeleportCommands() && isTeleportCommand(baseCommand)) {
                return true;
            }
            for (String blocked : rs.getBlockedCommands()) {
                String b = blocked.toLowerCase();
                while (b.startsWith("/")) b = b.substring(1);
                if (stripNamespace(b).equals(normalized)) return true;
            }
        }
        return false;
    }

    private static String stripNamespace(String command) {
        int idx = command.indexOf(':');
        if (idx > 0) {
            return command.substring(idx + 1);
        }
        return command;
    }

    private static boolean isTeleportCommand(String command) {
        // P3 #1: strip namespace once and compare against the (already-stripped) command
        String normalized = stripNamespace(command);
        for (String tc : TELEPORT_COMMANDS) {
            String stripped = tc;
            while (stripped.startsWith("/")) stripped = stripped.substring(1);
            stripped = stripNamespace(stripped);
            if (stripped.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handle item use event (includes all blacklisted items like ender pearls, chorus fruit, etc.)
     */
    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack itemStack = event.getItemStack();
        if (itemStack.isEmpty()) {
            return;
        }

        Item item = itemStack.getItem();
        if (isItemBlacklisted(item, player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);

            player.displayClientMessage(
                MessageUtils.smartComponent(player, "command.areamonitor.item.use_denied", 
                    BuiltInRegistries.ITEM.getKey(item).getPath()).withStyle(ChatFormatting.RED),
                true
            );

            player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1.0f, 0.5f);

            AreaMonitorMod.LOGGER.debug("Blocked player {} from using blacklisted item: {}",
                player.getName().getString(), item.toString());
        }
    }

    /**
     * Handle command use event.
     */
    @SubscribeEvent
    public static void onCommandUse(CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String command = event.getParseResults().getReader().getString();
        if (isCommandBlocked(command, player)) {
            event.setCanceled(true);

            player.displayClientMessage(
                MessageUtils.smartComponent(player, "command.areamonitor.teleport.use_denied").withStyle(ChatFormatting.RED),
                true
            );

            player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1.0f, 0.5f);

            AreaMonitorMod.LOGGER.debug("Blocked player {} from using teleport command: {}",
                player.getName().getString(), command);
        }
    }

    /**
     * Get player restriction info.
     */
    public static void showPlayerRestrictions(ServerPlayer player) {
        Set<String> currentAreas = AreaManager.getInstance().getCurrentAreas(player);
        if (currentAreas.isEmpty()) {
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "command.areamonitor.blacklist.not_in_restricted_area"),
                true
            );
            return;
        }

        player.displayClientMessage(
            MessageUtils.smartComponent(player, "command.areamonitor.blacklist.restrictions_header"),
            false
        );

        for (String areaName : currentAreas) {
            MonitorArea area = AreaManager.getInstance().getArea(areaName);
            if (area == null) continue;

            player.displayClientMessage(
                MessageUtils.smartComponent(player, "command.areamonitor.blacklist.area_info", area.getDisplayName()),
                false
            );

            RestrictionSettings restrictions = area.getRestrictions();
            if (restrictions.isEnableItemBlacklist()) {
                player.displayClientMessage(
                    MessageUtils.smartComponent(player, "command.areamonitor.blacklist.item_blacklist_enabled"),
                    false
                );
            }
            if (restrictions.isBlockTeleportCommands()) {
                player.displayClientMessage(
                    MessageUtils.smartComponent(player, "command.areamonitor.blacklist.teleport_disabled"),
                    false
                );
            }
        }
    }

    /**
     * Get global blacklist items.
     */
    public static Set<Item> getGlobalBlacklist() {
        return new HashSet<>(GLOBAL_BLACKLISTED_ITEMS);
    }

    /**
     * Get all area-specific blacklists.
     */
    public static Map<String, Set<Item>> getAllAreaBlacklists() {
        return new HashMap<>(AREA_BLACKLISTS);
    }

    /**
     * Add item to global blacklist.
     */
    public static void addToGlobalBlacklist(Item item) {
        GLOBAL_BLACKLISTED_ITEMS.add(item);
        AreaMonitorMod.LOGGER.info("Added item to global blacklist: {}", item.toString());
    }

    /**
     * Remove item from global blacklist.
     */
    public static void removeFromGlobalBlacklist(Item item) {
        GLOBAL_BLACKLISTED_ITEMS.remove(item);
        AreaMonitorMod.LOGGER.info("Removed item from global blacklist: {}", item.toString());
    }

    // Blacklist config data class
    private static class BlacklistConfigData {
        public List<String> global_blacklist = new ArrayList<>();
        public Map<String, List<String>> area_blacklists = new HashMap<>();
    }

    /**
     * Get blacklist config file path using FMLPaths
     */
    private static File getBlacklistConfigFile() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("areamonitor");
        return configDir.resolve("blacklist.json").toFile();
    }

    /**
     * Initialize blacklist config file.
     */
    public static void initBlacklistConfig() {
        if (blacklistConfigFile == null) {
            blacklistConfigFile = getBlacklistConfigFile();
        }
        loadBlacklistConfig();
    }

    /**
     * Load blacklist config file.
     */
    public static void loadBlacklistConfig() {
        if (blacklistConfigFile == null || !blacklistConfigFile.exists()) {
            createDefaultBlacklistConfig();
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(blacklistConfigFile), StandardCharsets.UTF_8)) {
            BlacklistConfigData configData = GSON.fromJson(reader, BlacklistConfigData.class);
            if (configData != null) {
                // Load global blacklist
                GLOBAL_BLACKLISTED_ITEMS.clear();
                if (configData.global_blacklist != null) {
                    for (String itemId : configData.global_blacklist) {
                        Item item = parseItemFromId(itemId);
                        if (item != null) {
                            GLOBAL_BLACKLISTED_ITEMS.add(item);
                        }
                    }
                }

                // Load area blacklists
                AREA_BLACKLISTS.clear();
                if (configData.area_blacklists != null) {
                    for (Map.Entry<String, List<String>> entry : configData.area_blacklists.entrySet()) {
                        // Use concurrent set — isItemBlacklisted reads these from event-handler threads
                        Set<Item> itemSet = ConcurrentHashMap.newKeySet();
                        for (String itemId : entry.getValue()) {
                            Item item = parseItemFromId(itemId);
                            if (item != null) {
                                itemSet.add(item);
                            }
                        }
                        if (!itemSet.isEmpty()) {
                            AREA_BLACKLISTS.put(entry.getKey(), itemSet);
                        }
                    }
                }

                AreaMonitorMod.LOGGER.info("Blacklist config loaded");
            }
        } catch (FileNotFoundException e) {
            AreaMonitorMod.LOGGER.warn("Blacklist config file not found: {}", blacklistConfigFile.getAbsolutePath());
            createDefaultBlacklistConfig();
        } catch (JsonSyntaxException e) {
            AreaMonitorMod.LOGGER.error("Blacklist config JSON syntax error: {}", blacklistConfigFile.getAbsolutePath(), e);
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.error("Failed to read blacklist config: {}", blacklistConfigFile.getAbsolutePath(), e);
        }
    }

    /**
     * Save blacklist config file.
     */
    public static void saveBlacklistConfig() {
        // Ensure file path is initialized
        if (blacklistConfigFile == null) {
            blacklistConfigFile = getBlacklistConfigFile();
        }

        if (blacklistConfigFile == null) return;

        try {
            File parentDir = blacklistConfigFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                AreaMonitorMod.LOGGER.error("Failed to create blacklist config directory: {}", parentDir.getAbsolutePath());
                return;
            }

            BlacklistConfigData configData = new BlacklistConfigData();

            // Save global blacklist
            for (Item item : GLOBAL_BLACKLISTED_ITEMS) {
                String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
                configData.global_blacklist.add(itemId);
            }

            // Save area blacklists
            for (Map.Entry<String, Set<Item>> entry : AREA_BLACKLISTS.entrySet()) {
                List<String> itemIds = new ArrayList<>();
                for (Item item : entry.getValue()) {
                    String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
                    itemIds.add(itemId);
                }
                configData.area_blacklists.put(entry.getKey(), itemIds);
            }

            // Atomic write: write to temp file then move into place
            File tempFile = new File(blacklistConfigFile.getParentFile(), blacklistConfigFile.getName() + ".tmp");
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                GSON.toJson(configData, writer);
            }
            Files.move(tempFile.toPath(), blacklistConfigFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            AreaMonitorMod.LOGGER.info("Blacklist config saved");
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.error("Failed to save blacklist config: {}", blacklistConfigFile.getAbsolutePath(), e);
        }
    }

    /**
     * Create default blacklist config file.
     */
    public static void createDefaultBlacklistConfig() {
        // Ensure file path is initialized
        if (blacklistConfigFile == null) {
            blacklistConfigFile = getBlacklistConfigFile();
        }

        if (blacklistConfigFile == null) return;

        // Create config directory
        File parentDir = blacklistConfigFile.getParentFile();
        try {
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                AreaMonitorMod.LOGGER.error("Failed to create blacklist config directory: {}", parentDir.getAbsolutePath());
                return;
            }
        } catch (SecurityException e) {
            AreaMonitorMod.LOGGER.error("Failed to create config directory (permission issue): {}", parentDir != null ? parentDir.getAbsolutePath() : "null", e);
            return;
        }

        BlacklistConfigData defaultConfig = new BlacklistConfigData();
        defaultConfig.global_blacklist.add("minecraft:ender_pearl");
        defaultConfig.global_blacklist.add("minecraft:chorus_fruit");
        defaultConfig.global_blacklist.add("minecraft:compass");
        defaultConfig.global_blacklist.add("minecraft:clock");

        // Atomic write: write to temp file then move into place
        try {
            File tempFile = new File(blacklistConfigFile.getParentFile(), blacklistConfigFile.getName() + ".tmp");
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                GSON.toJson(defaultConfig, writer);
            }
            Files.move(tempFile.toPath(), blacklistConfigFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            AreaMonitorMod.LOGGER.info("Created default blacklist config: {}", blacklistConfigFile.getAbsolutePath());
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.error("Failed to create default blacklist config: {}", blacklistConfigFile.getAbsolutePath(), e);
        }
    }

    /**
     * Parse item from ID string.
     */
    private static Item parseItemFromId(String itemId) {
        try {
            ResourceLocation location = new ResourceLocation(itemId);
            Item item = BuiltInRegistries.ITEM.get(location);
            if (item == Items.AIR && !itemId.equals("minecraft:air")) {
                return null;
            }
            return item;
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.warn("Invalid item ID format: {}", itemId);
            return null;
        }
    }
}
