package com.kavinshi.areamonitor.commands;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.AreaMonitorMod;
import com.kavinshi.areamonitor.ConfigManager;
import com.kavinshi.areamonitor.ItemBlacklistManager;
import com.kavinshi.areamonitor.MonitorArea;
import com.kavinshi.areamonitor.util.MessageUtils;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blacklist management commands and item suggestion infrastructure.
 */
public class BlacklistCommands {

    private BlacklistCommands() {}

    // ---- Item suggestion index ----

    private static final Map<Character, List<String>> ITEM_SUGGESTION_INDEX = new ConcurrentHashMap<>();
    private static volatile boolean suggestionIndexBuilt = false;
    private static final int MAX_SUGGESTIONS = 100;

    private static void buildSuggestionIndex() {
        if (suggestionIndexBuilt) return;
        synchronized (ITEM_SUGGESTION_INDEX) {
            if (suggestionIndexBuilt) return;
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                if (entry.getValue() == Items.AIR) continue;
                String itemId = entry.getKey().location().toString();
                String itemName = entry.getKey().location().getPath();
                ITEM_SUGGESTION_INDEX.computeIfAbsent(itemId.charAt(0), k -> new ArrayList<>()).add(itemId);
                if (!itemId.startsWith("minecraft:")) {
                    ITEM_SUGGESTION_INDEX.computeIfAbsent(itemName.charAt(0), k -> new ArrayList<>()).add(itemId);
                }
            }
            suggestionIndexBuilt = true;
            AreaMonitorMod.LOGGER.debug("Item suggestion index built with {} entries",
                ITEM_SUGGESTION_INDEX.values().stream().mapToInt(List::size).sum());
        }
    }

    // ---- Item suggestions ----

    public static CompletableFuture<Suggestions> suggestItems(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        buildSuggestionIndex();
        String remaining = builder.getRemaining().toLowerCase();

        if (remaining.isEmpty()) {
            return builder.buildFuture();
        }

        int suggestionCount = 0;
        List<String> candidates = ITEM_SUGGESTION_INDEX.getOrDefault(remaining.charAt(0), Collections.emptyList());
        for (String itemId : candidates) {
            if (suggestionCount >= MAX_SUGGESTIONS) break;
            String itemName = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
            if (itemId.startsWith(remaining) || itemName.startsWith(remaining)) {
                builder.suggest(itemId);
                suggestionCount++;
            }
        }

        return builder.buildFuture();
    }

    // ---- Item parsing ----

    private static Item parseItem(String itemName) {
        try {
            if (!itemName.contains(":")) {
                itemName = "minecraft:" + itemName;
            }
            ResourceLocation location = new ResourceLocation(itemName);
            Item item = BuiltInRegistries.ITEM.get(location);
            if (item == Items.AIR && !itemName.equals("minecraft:air")) {
                return null;
            }
            return item;
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.debug("Failed to parse item name (invalid format): {}", itemName);
            return null;
        }
    }

    private static String getItemDisplayName(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    // ---- Blacklist info ----

    public static int showBlacklistInfo(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            MessageUtils.sendFailure(context.getSource(), "player.only_command");
            return 0;
        }

        ItemBlacklistManager.showPlayerRestrictions(player);
        return 1;
    }

    // ---- Add item to area blacklist ----

    public static int addItemToAreaBlacklist(String areaName, String itemName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName);
        if (area == null) return 0;

        Item item = parseItem(itemName);
        if (item == null) {
            MessageUtils.sendFailure(context.getSource(), "blacklist.invalid_item", itemName);
            return 0;
        }

        Set<Item> areaBlacklist = new HashSet<>(ItemBlacklistManager.getAreaBlacklist(areaName));

        if (areaBlacklist.contains(item)) {
            MessageUtils.sendSuccess(context.getSource(), "blacklist.item_already_blacklisted", true, getItemDisplayName(item));
        } else {
            areaBlacklist.add(item);
            ItemBlacklistManager.addAreaBlacklist(areaName, areaBlacklist);
            ItemBlacklistManager.saveBlacklistConfig();

            MessageUtils.sendSuccess(context.getSource(), "blacklist.item_added", true, getItemDisplayName(item), areaName);
        }
        return 1;
    }

    // ---- Remove item from area blacklist ----

    public static int removeItemFromAreaBlacklist(String areaName, String itemName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName);
        if (area == null) return 0;

        Item item = parseItem(itemName);
        if (item == null) {
            MessageUtils.sendFailure(context.getSource(), "blacklist.invalid_item", itemName);
            return 0;
        }

        Set<Item> areaBlacklist = new HashSet<>(ItemBlacklistManager.getAreaBlacklist(areaName));
        if (areaBlacklist.remove(item)) {
            if (areaBlacklist.isEmpty()) {
                ItemBlacklistManager.removeAreaBlacklist(areaName);
            } else {
                ItemBlacklistManager.addAreaBlacklist(areaName, areaBlacklist);
            }
            ItemBlacklistManager.saveBlacklistConfig();

            MessageUtils.sendSuccess(context.getSource(), "blacklist.item_removed", true, getItemDisplayName(item), areaName);
        } else {
            MessageUtils.sendSuccess(context.getSource(), "blacklist.item_not_found", true, getItemDisplayName(item));
        }
        return 1;
    }

    // ---- List area blacklist ----

    public static int listAreaBlacklist(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName);
        if (area == null) return 0;

        Set<Item> areaBlacklist = ItemBlacklistManager.getAreaBlacklist(areaName);

        MessageUtils.sendSuccess(context.getSource(), "blacklist.area_header", false, areaName);

        if (areaBlacklist.isEmpty()) {
            MessageUtils.sendSuccess(context.getSource(), "blacklist.area_empty", false);
        } else {
            for (Item item : areaBlacklist) {
                context.getSource().sendSuccess(
                    () -> Component.translatable("blacklist.item.entry_active", getItemDisplayName(item)),
                    false
                );
            }
        }

        Set<Item> globalBlacklist = ItemBlacklistManager.getGlobalBlacklist();
        if (!globalBlacklist.isEmpty()) {
            MessageUtils.sendSuccess(context.getSource(), "blacklist.global_items", false);
            for (Item item : globalBlacklist) {
                context.getSource().sendSuccess(
                    () -> Component.translatable("blacklist.item.entry_inactive", getItemDisplayName(item)),
                    false
                );
            }
        }

        return 1;
    }

    // ---- Toggle area blacklist ----

    public static int toggleAreaBlacklist(String areaName, CommandContext<CommandSourceStack> context) {
        MonitorArea area = AreaCommandHelper.requireArea(context, areaName);
        if (area == null) return 0;

        boolean currentState = area.getRestrictions().isEnableItemBlacklist();
        area.getRestrictions().setEnableItemBlacklist(!currentState);
        ConfigManager.safeSaveConfig();

        String newState = !currentState ? "area.enabled" : "area.disabled";
        MessageUtils.sendSuccess(context.getSource(), "blacklist.area_toggle", true, areaName, newState);

        return 1;
    }

    // ---- Reload blacklist config ----

    public static int reloadBlacklistConfig(CommandContext<CommandSourceStack> context) {
        try {
            ItemBlacklistManager.loadBlacklistConfig();
            MessageUtils.sendSuccess(context.getSource(), "blacklist.reloaded", true);
        } catch (Exception e) {
            MessageUtils.sendFailure(context.getSource(), "blacklist.reload_failed", e.getMessage());
            return 0;
        }
        return 1;
    }
}
