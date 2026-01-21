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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = AreaMonitorMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AreaMonitor {
    private static final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();
    private static MinecraftServer minecraftServer;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 5;

    private static class PlayerState {
        boolean wasInArea = false;
        int lastX = 0;
        int lastZ = 0;
        long pendingActionTime = 0;
        GameType pendingGameMode = null;
    }

    private static class PendingAction {
        final UUID playerId;
        final Runnable action;
        final long executeTime;

        PendingAction(UUID playerId, Runnable action, long delayMs) {
            this.playerId = playerId;
            this.action = action;
            this.executeTime = System.currentTimeMillis() + delayMs;
        }
    }

    private static final List<PendingAction> pendingActions = Collections.synchronizedList(new ArrayList<>());

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        minecraftServer = event.getServer();
        AreaMonitorMod.LOGGER.info("区域监控模组已连接到服务器");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        processPendingActions();

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        if (!ConfigManager.CONFIG.isEnabled.get()) return;
        if (minecraftServer == null) return;

        for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
            checkPlayer(player);
        }
    }

    private static void checkPlayer(ServerPlayer player) {
        if (WhitelistManager.isWhitelisted(player)) {
            return;
        }

        UUID playerId = player.getUUID();
        String playerDim = player.level().dimension().location().toString();
        String targetDim = ConfigManager.CONFIG.targetDimension.get();

        if (!playerDim.equals(targetDim)) {
            playerStates.remove(playerId);
            return;
        }

        int x = (int) player.getX();
        int z = (int) player.getZ();

        PlayerState state = playerStates.computeIfAbsent(playerId, k -> new PlayerState());

        int dx = Math.abs(x - state.lastX);
        int dz = Math.abs(z - state.lastZ);

        if (dx > 2 || dz > 2) {
            state.lastX = x;
            state.lastZ = z;

            boolean inArea = ConfigManager.CONFIG.isInArea(x, z);

            if (inArea && !state.wasInArea) {
                handleAreaEnter(player, state);
            } else if (!inArea && state.wasInArea) {
                handleAreaLeave(player, state);
            }
        }
    }

    private static void handleAreaEnter(ServerPlayer player, PlayerState state) {
        state.wasInArea = true;

        showTitle(player,
                Component.literal("进入活动区域").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN),
                Component.literal("1.5秒后切换为冒险模式").withStyle(ChatFormatting.GRAY),
                10, 40, 10
        );

        pendingActions.add(new PendingAction(player.getUUID(), () -> {
            if (player.isAlive() && state.wasInArea) {
                player.setGameMode(GameType.ADVENTURE);
                if (ConfigManager.CONFIG.showMessages.get()) {
                    player.displayClientMessage(Component.literal("§a已切换为冒险模式"), true);
                }
            }
        }, 1500));
    }

    private static void handleAreaLeave(ServerPlayer player, PlayerState state) {
        state.wasInArea = false;

        showTitle(player,
                Component.literal("离开活动区域").withStyle(ChatFormatting.BOLD, ChatFormatting.RED),
                Component.literal("1.5秒后切换为生存模式").withStyle(ChatFormatting.GRAY),
                10, 40, 10
        );

        pendingActions.add(new PendingAction(player.getUUID(), () -> {
            if (player.isAlive() && !state.wasInArea) {
                player.setGameMode(GameType.SURVIVAL);
                if (ConfigManager.CONFIG.showMessages.get()) {
                    player.displayClientMessage(Component.literal("§c已恢复为生存模式"), true);
                }
            }
        }, 1500));
    }

    private static void showTitle(ServerPlayer player, Component title, Component subtitle,
                                  int fadeIn, int stay, int fadeOut) {
        if (player.connection == null) return;

        try {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));

            if (title != null) {
                player.connection.send(new ClientboundSetTitleTextPacket(title));
            }

            if (subtitle != null) {
                player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            }
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.warn("发送标题消息失败", e);
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
                    AreaMonitorMod.LOGGER.error("执行延迟动作时出错", e);
                }
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            playerStates.remove(playerId);

            synchronized (pendingActions) {
                pendingActions.removeIf(action -> action.playerId.equals(playerId));
            }

            AreaMonitorMod.LOGGER.debug("玩家退出，已清理状态: {}", player.getName().getString());
        }
    }

    public static MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }
}