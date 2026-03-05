package com.kavinshi.areamonitor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import java.io.File;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区域监控核心类，负责监控玩家位置并自动切换游戏模式
 *
 * 主要功能：
 * - 定期检查玩家在目标维度中的位置
 * - 检测玩家进入/离开监控区域
 * - 延迟切换游戏模式并显示提示
 * - 管理白名单玩家
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AreaMonitor {
    private static final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();
    private static MinecraftServer minecraftServer;
    private static int tickCounter = 0;
    private static final int TITLE_FADE_IN_TICKS = 10;
    private static final int TITLE_STAY_TICKS = 30;
    private static final int TITLE_FADE_OUT_TICKS = 10;
    private static final long GAME_MODE_SWITCH_DELAY_MS = 1000L;

    private static class PlayerState {
        boolean wasInArea = false;
        int lastX = Integer.MAX_VALUE;
        int lastZ = Integer.MAX_VALUE;

        public boolean isInitialized() {
            return lastX != Integer.MAX_VALUE && lastZ != Integer.MAX_VALUE;
        }
    }

    private record PendingAction(UUID playerId, Runnable action, long executeTime) {
        // record类自动生成构造函数
        // executeTime应该是绝对时间戳（当前时间 + 延迟）
    }

    /**
     * 获取当前的Minecraft服务器实例
     */
    public static MinecraftServer getServer() {
        return minecraftServer;
    }

    private static final List<PendingAction> pendingActions = Collections.synchronizedList(new ArrayList<>());

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        minecraftServer = event.getServer();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // 服务器启动完成后，确保配置文件正确初始化
        AreaMonitorMod.LOGGER.info("服务器启动完成，初始化配置文件...");

        // 确保配置目录存在
        try {
            File configDir = new File("config/areamonitor");
            if (!configDir.exists()) {
                configDir.mkdirs();
                AreaMonitorMod.LOGGER.info("已创建配置目录: {}", configDir.getAbsolutePath());
            }
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("创建配置目录时出错", e);
        }

        // 加载或创建配置文件
        ConfigManager.loadAreasConfig();
        ItemBlacklistManager.loadBlacklistConfig();

        AreaMonitorMod.LOGGER.info("配置文件初始化完成");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        processPendingActions();

        // 性能监控
        PerformanceMonitor.onServerTick(minecraftServer);

        // 更新区域可视化
        AreaVisualizer.updatePersistentVisualizations();

        tickCounter++;
        if (tickCounter < PerformanceMonitor.getCurrentCheckInterval()) return;
        tickCounter = 0;

        if (!ConfigManager.CONFIG.isEnabled.get()) return;
        if (minecraftServer == null) return;

        // 使用新的多区域系统检查玩家
        for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
            AreaManager.getInstance().checkPlayer(player);
        }
    }

    private static void processPendingActions() {
        long currentTime = System.currentTimeMillis();
        Iterator<PendingAction> iterator = pendingActions.iterator();

        while (iterator.hasNext()) {
            PendingAction action = iterator.next();
            if (currentTime >= action.executeTime) {
                try {
                    action.action.run();
                } catch (Exception e) {
                    // 保留错误日志
                    AreaMonitorMod.LOGGER.error("执行延迟动作时出错", e);
                }
                iterator.remove();
            }
        }
    }

    /**
     * 添加待处理的游戏模式切换
     */
    public static void addPendingGameModeChange(ServerPlayer player, GameType gameMode) {
        pendingActions.add(new PendingAction(player.getUUID(), () -> {
            if (player.isAlive()) {
                player.setGameMode(gameMode);
                if (ConfigManager.CONFIG.showMessages.get()) {
                    player.displayClientMessage(
                        Component.literal("§a已切换为" + getModeDisplayName(gameMode)),
                        true
                    );
                }
            }
        }, System.currentTimeMillis() + GAME_MODE_SWITCH_DELAY_MS));
    }




    private static String getModeDisplayName(GameType gameMode) {
        return switch (gameMode) {
            case CREATIVE -> "创造模式";
            case ADVENTURE -> "冒险模式";
            case SPECTATOR -> "旁观模式";
            default -> "生存模式";
        };
    }

    private static void showTitle(ServerPlayer player, Component title, Component subtitle) {
        try {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(TITLE_FADE_IN_TICKS, TITLE_STAY_TICKS, TITLE_FADE_OUT_TICKS));

            if (title != null) {
                player.connection.send(new ClientboundSetTitleTextPacket(title));
            }

            if (subtitle != null) {
                player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            }
        } catch (Exception e) {
            // 静默处理标题发送错误，避免影响游戏体验
            // 常见原因：玩家断开连接、网络问题等
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            playerStates.remove(playerId);
            AreaManager.getInstance().clearPlayerData(playerId);

            synchronized (pendingActions) {
                pendingActions.removeIf(action -> action.playerId.equals(playerId));
            }
        }
    }
}