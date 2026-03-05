package com.kavinshi.areamonitor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

/**
 * 物品黑名单管理器，限制特定区域内使用传送类道具
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemBlacklistManager {
    private static final Set<Item> GLOBAL_BLACKLISTED_ITEMS = new HashSet<>();
    private static final Map<String, Set<Item>> AREA_BLACKLISTS = new ConcurrentHashMap<>();
    private static final Set<String> TELEPORT_COMMANDS = new HashSet<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File blacklistConfigFile;

    // 初始化全局黑名单物品
    static {
        // 传送类物品
        GLOBAL_BLACKLISTED_ITEMS.add(Items.ENDER_PEARL); // 末影珍珠
        GLOBAL_BLACKLISTED_ITEMS.add(Items.CHORUS_FRUIT); // 紫颂果
        GLOBAL_BLACKLISTED_ITEMS.add(Items.RECOVERY_COMPASS); // 定位指南针 (1.19+)

        // 指南针类物品
        GLOBAL_BLACKLISTED_ITEMS.add(Items.COMPASS); // 指南针
        GLOBAL_BLACKLISTED_ITEMS.add(Items.RECOVERY_COMPASS); // 恢复指南针

        // 时钟
        GLOBAL_BLACKLISTED_ITEMS.add(Items.CLOCK); // 时钟


        // 传送命令
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

    /**
     * 为玩家区域添加自定义黑名单
     */
    public static void addAreaBlacklist(String areaName, Set<Item> blacklistedItems) {
        AREA_BLACKLISTS.put(areaName, new HashSet<>(blacklistedItems));
    }

    /**
     * 移除区域黑名单
     */
    public static void removeAreaBlacklist(String areaName) {
        AREA_BLACKLISTS.remove(areaName);
    }

    /**
     * 获取区域的黑名单物品
     */
    public static Set<Item> getAreaBlacklist(String areaName) {
        return AREA_BLACKLISTS.getOrDefault(areaName, Collections.emptySet());
    }

    /**
     * 检查物品是否被禁止
     */
    public static boolean isItemBlacklisted(Item item, ServerPlayer player) {
        Set<String> currentAreas = AreaManager.getInstance().getCurrentAreas(player);

        // 如果没有在任何区域，直接返回false
        if (currentAreas.isEmpty()) {
            return false;
        }

        // 检查全局黑名单 - 如果在任何启用了黑名单的区域中
        if (GLOBAL_BLACKLISTED_ITEMS.contains(item)) {
            for (String areaName : currentAreas) {
                MonitorArea area = AreaManager.getInstance().getArea(areaName);
                if (area != null && area.getRestrictions().isEnableItemBlacklist()) {
                    return true; // 只要在一个启用了黑名单的区域中就阻止
                }
            }
        }

        // 检查区域特定黑名单
        for (String areaName : currentAreas) {
            Set<Item> areaBlacklist = AREA_BLACKLISTS.get(areaName);
            if (areaBlacklist != null && areaBlacklist.contains(item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查玩家是否在被限制的区域
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
     * 检查命令是否被禁止
     */
    public static boolean isCommandBlocked(String command, ServerPlayer player) {
        String baseCommand = command.split(" ")[0].toLowerCase();

        if (!TELEPORT_COMMANDS.contains(baseCommand)) {
            return false;
        }

        Set<String> currentAreas = AreaManager.getInstance().getCurrentAreas(player);
        for (String areaName : currentAreas) {
            MonitorArea area = AreaManager.getInstance().getArea(areaName);
            if (area != null && area.getRestrictions().isBlockTeleportCommands()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理物品使用事件
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

        if (isItemBlacklisted(itemStack.getItem(), player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);

            // 发送拒绝消息
            player.displayClientMessage(
                Component.translatable("command.areamonitor.item.use_denied", BuiltInRegistries.ITEM.getKey(itemStack.getItem()).getPath()).withStyle(ChatFormatting.RED),
                true
            );

            // 播放拒绝音效
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1.0f, 0.5f);

            AreaMonitorMod.LOGGER.debug("阻止玩家 {} 使用黑名单物品: {}",
                player.getName().getString(), itemStack.getItem().toString());
        }
    }

    /**
     * 处理物品投掷事件（如末影珍珠）
     */
    @SubscribeEvent
    public static void onItemThrow(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack itemStack = event.getItemStack();
        if (itemStack.isEmpty()) {
            return;
        }

        // 检查特定的投掷物品
        Item item = itemStack.getItem();
        if ((item == Items.ENDER_PEARL || item == Items.CHORUS_FRUIT) && isItemBlacklisted(item, player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);

            // 发送拒绝消息
            player.displayClientMessage(
                Component.translatable("command.areamonitor.item.use_denied", getItemName(item)).withStyle(ChatFormatting.RED),
                true
            );

            // 播放拒绝音效
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1.0f, 0.5f);

            AreaMonitorMod.LOGGER.debug("阻止玩家 {} 投掷黑名单物品: {}",
                player.getName().getString(), item.toString());
        }
    }

    private static String getItemName(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    /**
     * 处理命令使用事件
     */
    @SubscribeEvent
    public static void onCommandUse(CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String command = event.getParseResults().getReader().getString();
        if (isCommandBlocked(command, player)) {
            event.setCanceled(true);

            // 发送拒绝消息
            player.displayClientMessage(
                Component.translatable("command.areamonitor.teleport.use_denied").withStyle(ChatFormatting.RED),
                true
            );

            // 播放拒绝音效
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1.0f, 0.5f);

            AreaMonitorMod.LOGGER.debug("阻止玩家 {} 使用传送命令: {}",
                player.getName().getString(), command);
        }
    }

    /**
     * 处理方块交互事件（防止使用特定方块）
     */
    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 检查是否在被限制的区域
        if (!isPlayerInRestrictedArea(player)) {
            return;
        }

        // 可以在这里添加对特定方块的限制
        // 例如：末地传送门、下界传送门等
    }

    /**
     * 获取玩家的限制信息
     */
    public static void showPlayerRestrictions(ServerPlayer player) {
        Set<String> currentAreas = AreaManager.getInstance().getCurrentAreas(player);
        if (currentAreas.isEmpty()) {
            player.displayClientMessage(
                Component.translatable("command.areamonitor.blacklist.not_in_restricted_area"),
                true
            );
            return;
        }

        player.displayClientMessage(
            Component.translatable("command.areamonitor.blacklist.restrictions_header"),
            false
        );

        for (String areaName : currentAreas) {
            MonitorArea area = AreaManager.getInstance().getArea(areaName);
            if (area == null) continue;

            player.displayClientMessage(
                Component.translatable("command.areamonitor.blacklist.area_info", area.getDisplayName()),
                false
            );

            RestrictionSettings restrictions = area.getRestrictions();
            if (restrictions.isEnableItemBlacklist()) {
                player.displayClientMessage(
                    Component.translatable("command.areamonitor.blacklist.item_blacklist_enabled"),
                    false
                );
            }
            if (restrictions.isBlockTeleportCommands()) {
                player.displayClientMessage(
                    Component.translatable("command.areamonitor.blacklist.teleport_disabled"),
                    false
                );
            }
        }
    }

    /**
     * 获取全局黑名单物品列表
     */
    public static Set<Item> getGlobalBlacklist() {
        return new HashSet<>(GLOBAL_BLACKLISTED_ITEMS);
    }

    /**
     * 获取所有区域的特定黑名单
     */
    public static Map<String, Set<Item>> getAllAreaBlacklists() {
        return new HashMap<>(AREA_BLACKLISTS);
    }

    /**
     * 添加物品到全局黑名单
     */
    public static void addToGlobalBlacklist(Item item) {
        GLOBAL_BLACKLISTED_ITEMS.add(item);
        AreaMonitorMod.LOGGER.info("添加物品到全局黑名单: {}", item.toString());
    }

    /**
     * 从全局黑名单移除物品
     */
    public static void removeFromGlobalBlacklist(Item item) {
        GLOBAL_BLACKLISTED_ITEMS.remove(item);
        AreaMonitorMod.LOGGER.info("从全局黑名单移除物品: {}", item.toString());
    }

    /**
     * 添加传送命令到黑名单
     */
    public static void addTeleportCommand(String command) {
        TELEPORT_COMMANDS.add(command.toLowerCase());
    }

    /**
     * 从黑名单移除传送命令
     */
    public static void removeTeleportCommand(String command) {
        TELEPORT_COMMANDS.remove(command.toLowerCase());
    }

    /**
     * 获取所有禁止的传送命令
     */
    public static Set<String> getTeleportCommands() {
        return new HashSet<>(TELEPORT_COMMANDS);
    }

    // 黑名单配置文件数据类
    private static class BlacklistConfigData {
        public List<String> global_blacklist = new ArrayList<>();
        public Map<String, List<String>> area_blacklists = new HashMap<>();
    }

    /**
     * 初始化黑名单配置文件
     */
    public static void initBlacklistConfig() {
        // 延迟初始化文件路径，确保服务器目录可用
        if (blacklistConfigFile == null) {
            blacklistConfigFile = new File("config/areamonitor/blacklist.json");
        }
        loadBlacklistConfig();
    }

    /**
     * 加载黑名单配置文件
     */
    public static void loadBlacklistConfig() {
        if (blacklistConfigFile == null || !blacklistConfigFile.exists()) {
            createDefaultBlacklistConfig();
            return;
        }

        try (FileReader reader = new FileReader(blacklistConfigFile)) {
            BlacklistConfigData configData = GSON.fromJson(reader, BlacklistConfigData.class);
            if (configData != null) {
                // 加载全局黑名单
                GLOBAL_BLACKLISTED_ITEMS.clear();
                if (configData.global_blacklist != null) {
                    for (String itemId : configData.global_blacklist) {
                        Item item = parseItemFromId(itemId);
                        if (item != null) {
                            GLOBAL_BLACKLISTED_ITEMS.add(item);
                        }
                    }
                }

                // 加载区域黑名单
                AREA_BLACKLISTS.clear();
                if (configData.area_blacklists != null) {
                    for (Map.Entry<String, List<String>> entry : configData.area_blacklists.entrySet()) {
                        Set<Item> itemSet = new HashSet<>();
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

                AreaMonitorMod.LOGGER.info("黑名单配置已加载");
            }
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("加载黑名单配置文件失败", e);
        }
    }

    /**
     * 保存黑名单配置文件
     */
    public static void saveBlacklistConfig() {
        // 确保文件路径已初始化
        if (blacklistConfigFile == null) {
            blacklistConfigFile = new File("config/areamonitor/blacklist.json");
        }

        if (blacklistConfigFile == null) return;

        try {
            File parentDir = blacklistConfigFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                AreaMonitorMod.LOGGER.error("无法创建黑名单配置目录: {}", parentDir.getAbsolutePath());
                return;
            }

            BlacklistConfigData configData = new BlacklistConfigData();

            // 保存全局黑名单
            for (Item item : GLOBAL_BLACKLISTED_ITEMS) {
                String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
                configData.global_blacklist.add(itemId);
            }

            // 保存区域黑名单
            for (Map.Entry<String, Set<Item>> entry : AREA_BLACKLISTS.entrySet()) {
                List<String> itemIds = new ArrayList<>();
                for (Item item : entry.getValue()) {
                    String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
                    itemIds.add(itemId);
                }
                configData.area_blacklists.put(entry.getKey(), itemIds);
            }

            try (FileWriter writer = new FileWriter(blacklistConfigFile)) {
                GSON.toJson(configData, writer);
            }

            AreaMonitorMod.LOGGER.info("黑名单配置已保存");
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("保存黑名单配置文件失败", e);
        }
    }

    /**
     * 创建默认黑名单配置文件
     */
    public static void createDefaultBlacklistConfig() {
        // 确保文件路径已初始化
        if (blacklistConfigFile == null) {
            blacklistConfigFile = new File("config/areamonitor/blacklist.json");
        }

        if (blacklistConfigFile == null) return;

        // 创建配置目录
        try {
            File parentDir = blacklistConfigFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                AreaMonitorMod.LOGGER.error("无法创建黑名单配置目录: {}", parentDir.getAbsolutePath());
                return;
            }
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("创建配置目录时出错", e);
            return;
        }

        BlacklistConfigData defaultConfig = new BlacklistConfigData();
        defaultConfig.global_blacklist.add("minecraft:ender_pearl");
        defaultConfig.global_blacklist.add("minecraft:chorus_fruit");
        defaultConfig.global_blacklist.add("minecraft:compass");
        defaultConfig.global_blacklist.add("minecraft:clock");

        try (FileWriter writer = new FileWriter(blacklistConfigFile)) {
            GSON.toJson(defaultConfig, writer);
            AreaMonitorMod.LOGGER.info("已创建默认黑名单配置文件: {}", blacklistConfigFile.getAbsolutePath());
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("创建默认黑名单配置文件失败", e);
        }
    }

    /**
     * 从ID解析物品
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
            return null;
        }
    }
}