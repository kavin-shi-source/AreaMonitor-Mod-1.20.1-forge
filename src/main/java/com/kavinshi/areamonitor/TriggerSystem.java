package com.kavinshi.areamonitor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 高级触发系统，处理各种触发条件和动作
 */
public class TriggerSystem {

    /**
     * 基础触发器接口
     */
    public interface Trigger {
        boolean check(ServerPlayer player);
        void execute(ServerPlayer player, MonitorArea area);
        AreaManager.TriggerType getType();
    }

    /**
     * 物品持有触发器
     */
    public static class ItemHoldTrigger implements Trigger {
        private final AreaManager.TriggerType type;
        private final List<Item> requiredItems;
        private final TriggerCondition condition;
        private final List<TriggerAction> actions;
        private final boolean checkInventory;
        private final boolean checkHotbar;
        private final boolean checkArmor;

        public ItemHoldTrigger(AreaManager.TriggerType type, List<Item> requiredItems,
                             TriggerCondition condition, List<TriggerAction> actions,
                             boolean checkInventory, boolean checkHotbar, boolean checkArmor) {
            this.type = type;
            this.requiredItems = requiredItems;
            this.condition = condition;
            this.actions = actions;
            this.checkInventory = checkInventory;
            this.checkHotbar = checkHotbar;
            this.checkArmor = checkArmor;
        }

        @Override
        public boolean check(ServerPlayer player) {
            if (requiredItems.isEmpty()) return false;

            List<ItemStack> checkItems = new ArrayList<>();

            if (checkInventory) {
                checkItems.addAll(player.getInventory().items);
            }
            if (checkHotbar) {
                checkItems.addAll(player.getInventory().items.subList(0, 9));
            }
            if (checkArmor) {
                checkItems.addAll(player.getInventory().armor);
            }

            if (condition == TriggerCondition.AND) {
                return requiredItems.stream()
                    .allMatch(reqItem -> checkItems.stream()
                        .anyMatch(itemStack -> !itemStack.isEmpty() && itemStack.getItem() == reqItem));
            } else {
                return requiredItems.stream()
                    .anyMatch(reqItem -> checkItems.stream()
                        .anyMatch(itemStack -> !itemStack.isEmpty() && itemStack.getItem() == reqItem));
            }
        }

        @Override
        public void execute(ServerPlayer player, MonitorArea area) {
            for (TriggerAction action : actions) {
                try {
                    action.execute(player, area);
                } catch (Exception e) {
                    AreaMonitorMod.LOGGER.error("执行触发动作时出错", e);
                }
            }
        }

        @Override
        public AreaManager.TriggerType getType() {
            return type;
        }
    }

    /**
     * 玩家数量触发器
     */
    public static class PlayerCountTrigger implements Trigger {
        private final AreaManager.TriggerType type;
        private final int minPlayers;
        private final int maxPlayers;
        private final List<TriggerAction> actions;
        private final boolean countByDimension;

        public PlayerCountTrigger(AreaManager.TriggerType type, int minPlayers, int maxPlayers,
                                 List<TriggerAction> actions, boolean countByDimension) {
            this.type = type;
            this.minPlayers = minPlayers;
            this.maxPlayers = maxPlayers;
            this.actions = actions;
            this.countByDimension = countByDimension;
        }

        @Override
        public boolean check(ServerPlayer player) {
            // 对于玩家数量触发器，这个方法不会被直接调用
            return false;
        }

        public boolean check(int playerCount) {
            return playerCount >= minPlayers && playerCount <= maxPlayers;
        }

        public boolean check(MinecraftServer server, MonitorArea area) {
            if (server == null) return false;

            long playerCount;
            if (countByDimension) {
                String targetDim = area.getDimension();
                playerCount = server.getPlayerList().getPlayers().stream()
                    .filter(p -> p.level().dimension().location().toString().equals(targetDim))
                    .count();
            } else {
                playerCount = server.getPlayerList().getPlayers().stream()
                    .filter(p -> area.isPlayerInArea(new PlayerPosition(
                        p.getX(), p.getZ(),
                        p.level().dimension().location().toString())))
                    .count();
            }

            return check((int) playerCount);
        }

        @Override
        public void execute(ServerPlayer player, MonitorArea area) {
            for (TriggerAction action : actions) {
                try {
                    action.execute(player, area);
                } catch (Exception e) {
                    AreaMonitorMod.LOGGER.error("执行玩家数量触发动作时出错", e);
                }
            }
        }

        @Override
        public AreaManager.TriggerType getType() {
            return type;
        }
    }

    /**
     * 周期性触发器
     */
    public static class PeriodicTrigger implements Trigger {
        private final AreaManager.TriggerType type;
        private final int interval; // tick间隔
        private final List<TriggerAction> actions;
        private long lastTriggerTime = 0;

        public PeriodicTrigger(AreaManager.TriggerType type, int interval, List<TriggerAction> actions) {
            this.type = type;
            this.interval = interval;
            this.actions = actions;
        }

        @Override
        public boolean check(ServerPlayer player) {
            long currentTime = player.level().getGameTime();
            if (currentTime - lastTriggerTime >= interval) {
                lastTriggerTime = currentTime;
                return true;
            }
            return false;
        }

        @Override
        public void execute(ServerPlayer player, MonitorArea area) {
            for (TriggerAction action : actions) {
                try {
                    action.execute(player, area);
                } catch (Exception e) {
                    AreaMonitorMod.LOGGER.error("执行周期性触发动作时出错", e);
                }
            }
        }

        @Override
        public AreaManager.TriggerType getType() {
            return type;
        }
    }

    /**
     * 触发条件枚举
     */
    public enum TriggerCondition {
        AND, OR
    }

    /**
     * 触发动作接口
     */
    public interface TriggerAction {
        void execute(ServerPlayer player, MonitorArea area);
    }

    /**
     * 发送消息动作
     */
    public static class SendMessageAction implements TriggerAction {
        private final String message;
        private final boolean actionBar;
        private final int color;

        public SendMessageAction(String message, boolean actionBar, int color) {
            this.message = message;
            this.actionBar = actionBar;
            this.color = color;
        }

        @Override
        public void execute(ServerPlayer player, MonitorArea area) {
            String processedMessage = processPlaceholders(message, player, area);
            String coloredMessage = "§" + Integer.toHexString(color) + processedMessage;

            player.displayClientMessage(
                Component.literal(coloredMessage),
                actionBar
            );
        }

        private String processPlaceholders(String message, ServerPlayer player, MonitorArea area) {
            return message
                .replace("%player%", player.getName().getString())
                .replace("%area%", area.getDisplayName())
                .replace("%area_name%", area.getName())
                .replace("%dimension%", area.getDimension());
        }
    }

    /**
     * 执行命令动作
     */
    public static class ExecuteCommandAction implements TriggerAction {
        private final String command;
        private final boolean asConsole;

        public ExecuteCommandAction(String command, boolean asConsole) {
            this.command = command;
            this.asConsole = asConsole;
        }

        @Override
        public void execute(ServerPlayer player, MonitorArea area) {
            String processedCommand = processPlaceholders(command, player, area);

            if (asConsole) {
                MinecraftServer server = player.getServer();
                if (server != null) {
                    CommandSourceStack sourceStack = server.createCommandSourceStack()
                        .withSuppressedOutput()
                        .withPermission(4);
                    server.getCommands().performPrefixedCommand(sourceStack, processedCommand);
                }
            } else {
                player.sendSystemMessage(Component.translatable("trigger.command.executing", processedCommand));
                player.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack(),
                    processedCommand
                );
            }
        }

        private String processPlaceholders(String command, ServerPlayer player, MonitorArea area) {
            return command
                .replace("%player%", player.getName().getString())
                .replace("%uuid%", player.getUUID().toString())
                .replace("%area%", area.getDisplayName())
                .replace("%x%", String.valueOf((int) player.getX()))
                .replace("%y%", String.valueOf((int) player.getY()))
                .replace("%z%", String.valueOf((int) player.getZ()));
        }
    }

    /**
     * 播放音效动作
     */
    public static class PlaySoundAction implements TriggerAction {
        private final String soundName;
        private final float volume;
        private final float pitch;

        public PlaySoundAction(String soundName, float volume, float pitch) {
            this.soundName = soundName;
            this.volume = volume;
            this.pitch = pitch;
        }

        @Override
        public void execute(ServerPlayer player, MonitorArea area) {
            try {
                player.playNotifySound(
                    SoundEvents.NOTE_BLOCK_PLING.get(),
                    SoundSource.PLAYERS,
                    volume,
                    pitch
                );
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.error("播放音效失败", e);
            }
        }
    }

    /**
     * 粒子效果动作
     */
    public static class ParticleEffectAction implements TriggerAction {
        private final String particleType;
        private final int count;
        private final double offsetX, offsetY, offsetZ;
        private final double speed;

        public ParticleEffectAction(String particleType, int count,
                                  double offsetX, double offsetY, double offsetZ, double speed) {
            this.particleType = particleType;
            this.count = count;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.speed = speed;
        }

        @Override
        public void execute(ServerPlayer player, MonitorArea area) {
            try {
                net.minecraft.core.particles.ParticleOptions particle = ParticleTypes.HAPPY_VILLAGER;

                player.level().addParticle(
                    particle,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    offsetX, offsetY, offsetZ
                );
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.error("显示粒子效果失败", e);
            }
        }
    }

    /**
     * 传送动作
     */
    public static class TeleportAction implements TriggerAction {
        private final double x, y, z;
        private final String dimension;
        private final float yaw, pitch;

        public TeleportAction(double x, double y, double z, String dimension, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        @Override
        public void execute(ServerPlayer player, MonitorArea area) {
            try {
                // 简单的传送实现
                player.teleportTo(x, y, z);
                player.setYRot(yaw);
                player.setXRot(pitch);
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.error("传送玩家失败", e);
            }
        }
    }

    /**
     * 切换游戏模式动作
     */
    public static class GameModeAction implements TriggerAction {
        private final GameType gameMode;

        public GameModeAction(GameType gameMode) {
            this.gameMode = gameMode;
        }

        @Override
        public void execute(ServerPlayer player, MonitorArea area) {
            player.setGameMode(gameMode);
        }
    }
}