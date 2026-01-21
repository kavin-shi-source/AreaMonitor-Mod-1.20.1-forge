package com.kavinshi.areamonitor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AreaMonitorMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WhitelistManager {
    private static final Set<String> playerWhitelist = new HashSet<>();
    private static final Set<UUID> whitelistUUIDs = new HashSet<>();
    private static File whitelistFile;
    private static MinecraftServer minecraftServer;

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        minecraftServer = event.getServer();
        whitelistFile = new File(minecraftServer.getServerDirectory(), "config/areamonitor-whitelist.txt");
        loadWhitelist();
        AreaMonitorMod.LOGGER.info("白名单管理器初始化完成，已加载 {} 个玩家", playerWhitelist.size());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        saveWhitelist();
        minecraftServer = null;
        AreaMonitorMod.LOGGER.info("白名单已保存");
    }

    public static void loadWhitelist() {
        playerWhitelist.clear();
        whitelistUUIDs.clear();

        if (!whitelistFile.exists()) {
            AreaMonitorMod.LOGGER.info("白名单文件不存在，将创建新文件: {}", whitelistFile.getAbsolutePath());
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(whitelistFile))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    playerWhitelist.add(line.toLowerCase());
                    count++;
                }
            }
            AreaMonitorMod.LOGGER.info("成功加载白名单，共 {} 个玩家", count);
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.error("无法加载白名单文件", e);
        }
    }

    public static void saveWhitelist() {
        try {
            whitelistFile.getParentFile().mkdirs();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(whitelistFile))) {
                writer.write("# 区域监控模组白名单");
                writer.newLine();
                writer.write("# 每行一个玩家名（不区分大小写）");
                writer.newLine();
                writer.newLine();

                for (String name : playerWhitelist) {
                    writer.write(name);
                    writer.newLine();
                }
            }
            AreaMonitorMod.LOGGER.info("白名单已保存到: {}", whitelistFile.getAbsolutePath());
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.error("无法保存白名单文件", e);
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
            AreaMonitorMod.LOGGER.info("玩家 {} 已添加到白名单", playerName);
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
            AreaMonitorMod.LOGGER.info("玩家 {} 已从白名单移除", playerName);
            return true;
        }
        return false;
    }

    public static Set<String> getWhitelist() {
        return new HashSet<>(playerWhitelist);
    }

    public static void clearWhitelist() {
        int count = playerWhitelist.size();
        playerWhitelist.clear();
        whitelistUUIDs.clear();
        saveWhitelist();
        AreaMonitorMod.LOGGER.info("已清空白名单，共移除 {} 个玩家", count);
    }

    public static void onPlayerLogin(ServerPlayer player) {
        if (isWhitelisted(player.getName().getString())) {
            whitelistUUIDs.add(player.getUUID());
        }
    }

    public static void onPlayerLogout(ServerPlayer player) {
        whitelistUUIDs.remove(player.getUUID());
    }
}