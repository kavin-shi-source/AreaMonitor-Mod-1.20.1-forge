package com.kavinshi.areamonitor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
        // 移除日志
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
            AreaMonitorMod.LOGGER.error("无法加载白名单文件", e);
        }
    }

    public static void saveWhitelist() {
        try {
            if (!whitelistFile.getParentFile().exists()) {
                boolean dirsCreated = whitelistFile.getParentFile().mkdirs();
                if (!dirsCreated) {
                    AreaMonitorMod.LOGGER.error("无法创建配置目录: {}", whitelistFile.getParentFile().getAbsolutePath());
                    return;
                }
            }

            try (FileWriter fileWriter = new FileWriter(whitelistFile);
                 BufferedWriter writer = new BufferedWriter(fileWriter)) {
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