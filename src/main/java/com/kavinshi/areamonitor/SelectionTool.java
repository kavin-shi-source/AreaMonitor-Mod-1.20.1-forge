package com.kavinshi.areamonitor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selection tool system for creating areas.
 * Provides functionality for selecting two corner points and creating monitored areas.
 */
public class SelectionTool {
    /**
     * Maximum distance between two selection points (in blocks).
     * Prevents creating extremely large areas that could impact performance.
     */
    private static final double MAX_SELECTION_DISTANCE = 1000.0;
    
    /**
     * Maximum area size in square blocks.
     * Limits the total area that can be monitored to prevent performance issues.
     */
    private static final long MAX_SELECTION_AREA = 1_000_000L;
    
    /**
     * Threshold for showing a warning about large area selection.
     * Areas larger than this will show a warning but still allow creation.
     */
    public static final long LARGE_AREA_WARNING_THRESHOLD = 100_000L;
    
    /**
     * Cooldown period for help messages in milliseconds.
     * Prevents chat spam when player repeatedly right-clicks without selection.
     */
    public static final int HELP_MESSAGE_COOLDOWN_MS = 5000;
    
    private static final Map<UUID, SelectionPoints> playerSelections = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastHelpTimes = new ConcurrentHashMap<>();

    private SelectionTool() {
    }

    private static String getSelectionToolName() {
        return "§a" + LocalizationManager.translate("area.selection.tool_name") + " Tool";
    }

    /**
     * Check if player is holding the selection tool.
     */
    public static boolean isHoldingSelectionTool(ServerPlayer player) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return false;

        net.minecraft.network.chat.Component name = heldItem.getHoverName();
        if (name == null) return false;

        String nameString = name.getString();
        if (nameString == null) return false;

        return nameString.contains("Area Selection") ||
               nameString.contains("区域选择") ||
               nameString.contains(LocalizationManager.translate("area.selection.tool_name"));
    }

    /**
     * Give selection tool to player.
     */
    public static void giveSelectionTool(ServerPlayer player) {
        ItemStack tool = new ItemStack(Items.WOODEN_AXE);
        tool.setHoverName(net.minecraft.network.chat.Component.literal(getSelectionToolName()));

        if (player.getInventory().getFreeSlot() != -1) {
            if (player.addItem(tool)) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("selection.tool.obtained"),
                    false
                );
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("selection.tool.instructions"),
                    false
                );
            }
        } else {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.tool.inventory_full"),
                true
            );
        }
    }

    /**
     * Handle player interaction event.
     */
    public static void handlePlayerInteract(ServerPlayer player, BlockPos pos, InteractionHand hand) {
        if (!isHoldingSelectionTool(player) || hand != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!player.hasPermissions(2)) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.tool.no_permission"),
                true
            );
            return;
        }

        SelectionPoints selection = playerSelections.computeIfAbsent(
            player.getUUID(),
            k -> new SelectionPoints()
        );

        if (!selection.hasFirstPoint()) {
            selection.setFirstPoint(pos);

            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.mode.header").withStyle(net.minecraft.ChatFormatting.BOLD),
                false
            );
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.first_point_set", pos.getX(), pos.getY(), pos.getZ()),
                false
            );
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.set_second_point"),
                false
            );

            showSelectionMarker(player, pos);

            player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),
                                  net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 2.0f);

        } else {
            BlockPos firstPos = selection.getFirstPoint();
            double distance = Math.sqrt(
                Math.pow(pos.getX() - firstPos.getX(), 2) +
                Math.pow(pos.getZ() - firstPos.getZ(), 2)
            );

            if (distance > MAX_SELECTION_DISTANCE) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("selection.distance_too_far"),
                    true
                );
                return;
            }

            long area = Math.abs((long)(pos.getX() - firstPos.getX()) * (pos.getZ() - firstPos.getZ()));
            if (area > MAX_SELECTION_AREA) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(LocalizationManager.translate("area.error.too_large", area)),
                    true
                );
                return;
            }

            selection.setSecondPoint(pos);

            AreaVisualizer.showSelection(player, selection.getFirstPoint(), selection.getSecondPoint());

            showSelectionInfo(player, selection);

            player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),
                                  net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    /**
     * Show selection marker at position.
     */
    private static void showSelectionMarker(ServerPlayer player, BlockPos pos) {
        Level level = player.level();
        for (int y = pos.getY(); y <= pos.getY() + 3; y++) {
            AreaVisualizer.spawnParticle(level,
                pos.getX() + 0.5, y, pos.getZ() + 0.5,
                ParticleTypes.END_ROD
            );
        }

        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            double x = pos.getX() + 0.5 + Math.cos(angle) * 0.6;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * 0.6;
            AreaVisualizer.spawnParticle(level, x, pos.getY() + 0.5, z, ParticleTypes.END_ROD);
        }
    }

    /**
     * Show selection info to player.
     */
    private static void showSelectionInfo(ServerPlayer player, SelectionPoints selection) {
        BlockPos pos1 = selection.getFirstPoint();
        BlockPos pos2 = selection.getSecondPoint();

        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        long area = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        int width = maxX - minX + 1;
        int height = maxZ - minZ + 1;

        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.complete.header").withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.point.first", pos1.getX(), pos1.getY(), pos1.getZ()),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.point.second", pos2.getX(), pos2.getY(), pos2.getZ()),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.area.bounds", minX, maxX, minZ, maxZ),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.area.size", width, height, area),
            false
        );

        if (area > MAX_SELECTION_AREA) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.warning.area_too_large"),
                false
            );
        } else if (area > LARGE_AREA_WARNING_THRESHOLD) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.note.area_large"),
                false
            );
        } else {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.area.size_ok"),
                false
            );
        }

        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.instructions.create_or_cancel"),
            false
        );

        String currentDimension = player.level().dimension().location().toString();
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.dimension.current", getDimensionDisplayName(currentDimension)),
            false
        );
    }

    /**
     * Get display name for dimension.
     */
    private static String getDimensionDisplayName(String dimension) {
        return switch (dimension.toLowerCase()) {
            case "minecraft:overworld" -> LocalizationManager.translate("dimension.minecraft.overworld");
            case "minecraft:the_nether" -> LocalizationManager.translate("dimension.minecraft.the_nether");
            case "minecraft:the_end" -> LocalizationManager.translate("dimension.minecraft.the_end");
            default -> dimension;
        };
    }

    /**
     * Create area from current selection.
     */
    public static void createAreaFromSelection(ServerPlayer player, String areaName) {
        SelectionPoints selection = playerSelections.get(player.getUUID());
        if (selection == null || !selection.isComplete()) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.error.no_points"),
                true
            );
            return;
        }

        BlockPos pos1 = selection.getFirstPoint();
        BlockPos pos2 = selection.getSecondPoint();

        String currentDimension = player.level().dimension().location().toString();

        MonitorArea area = new MonitorArea(areaName);
        area.setDisplayName(areaName);
        area.setDimension(currentDimension);
        area.setBounds(new MonitorArea.RectangleBounds(
            pos1.getX(), pos1.getZ(),
            pos2.getX(), pos2.getZ()
        ));

        AreaManager.getInstance().addArea(area);

        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.area.created", areaName).withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );

        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.area.dimension", getDimensionDisplayName(currentDimension)),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.area.range",
                Math.min(pos1.getX(), pos2.getX()) + ", " + Math.max(pos1.getX(), pos2.getX()) + "] × [" +
                Math.min(pos1.getZ(), pos2.getZ()) + ", " + Math.max(pos1.getZ(), pos2.getZ()) + "]"),
            false
        );

        showModeSetupGuide(player);

        ConfigManager.saveAreasConfig();

        playerSelections.remove(player.getUUID());

        player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),
                              net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 2.0f);
    }

    /**
     * Show mode setup guide to player.
     */
    private static void showModeSetupGuide(ServerPlayer player) {
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.mode.setup_header").withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.mode.setup_instructions"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.mode.available_modes"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(""),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("selection.mode.setup_complete"),
            false
        );
    }

    /**
     * Cancel current selection.
     */
    public static void cancelSelection(ServerPlayer player) {
        SelectionPoints selection = playerSelections.remove(player.getUUID());
        if (selection != null) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.cancelled"),
                true
            );
        }
    }

    /**
     * Get last help time for player.
     */
    public static Long getLastHelpTime(UUID playerId) {
        return lastHelpTimes.get(playerId);
    }

    /**
     * Set last help time for player.
     */
    public static void setLastHelpTime(UUID playerId, long time) {
        lastHelpTimes.put(playerId, time);
    }

    /**
     * Show current selection info to player.
     */
    public static void showCurrentSelection(ServerPlayer player) {
        SelectionPoints selection = playerSelections.get(player.getUUID());
        if (selection == null) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.no_active_selection"),
                true
            );
            return;
        }

        if (selection.hasFirstPoint()) {
            BlockPos pos1 = selection.getFirstPoint();
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.current.first_point",
                    pos1.getX(), pos1.getY(), pos1.getZ()),
                false
            );
        }

        if (selection.hasSecondPoint()) {
            BlockPos pos2 = selection.getSecondPoint();
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.current.second_point",
                    pos2.getX(), pos2.getY(), pos2.getZ()),
                false
            );
        } else {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("selection.need_second_point"),
                false
            );
        }
    }

    /**
     * Clean up player data when they disconnect.
     */
    public static void cleanupPlayerData(UUID playerId) {
        playerSelections.remove(playerId);
        lastHelpTimes.remove(playerId);
    }

    /**
     * Clean up all data when server stops.
     */
    public static void cleanupAllData() {
        playerSelections.clear();
        lastHelpTimes.clear();
    }
}
