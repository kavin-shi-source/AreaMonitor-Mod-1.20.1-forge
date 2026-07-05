package com.kavinshi.areamonitor;

import com.kavinshi.areamonitor.util.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selection tool system for creating areas.
 * Provides functionality for selecting two corner points and creating monitored areas.
 */
public class SelectionTool {
    /**
     * NBT tag key for identifying selection tools.
     */
    private static final String NBT_TAG_SELECTION_TOOL = "areamonitor_selection_tool";

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

    private SelectionTool() {
    }

    /**
     * Get the configured selection tool item from config.
     */
    private static net.minecraft.world.item.Item getSelectionToolItem() {
        String itemId = ConfigManager.CONFIG.selectionToolItemId.get();
        try {
            ResourceLocation location = ResourceLocation.tryParse(itemId);
            if (location != null) {
                net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(location);
                if (item != Items.AIR) {
                    return item;
                }
            }
            AreaMonitorMod.LOGGER.warn("Invalid selection tool item ID in config: {}, falling back to wooden_axe", itemId);
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.warn("Invalid selection tool item ID in config: {}, falling back to wooden_axe", itemId, e);
        }
        return Items.WOODEN_AXE;
    }

    /**
     * Check if player is holding the selection tool.
     * Uses NBT tag for identification, independent of item name or language.
     */
    public static boolean isHoldingSelectionTool(ServerPlayer player) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return false;

        CompoundTag tag = heldItem.getTag();
        return tag != null && tag.getBoolean(NBT_TAG_SELECTION_TOOL);
    }

    /**
     * Give selection tool to player.
     */
    public static void giveSelectionTool(ServerPlayer player) {
        ItemStack tool = new ItemStack(getSelectionToolItem());

        CompoundTag tag = tool.getOrCreateTag();
        tag.putBoolean(NBT_TAG_SELECTION_TOOL, true);

        tool.setHoverName(net.minecraft.network.chat.Component.translatable("area.selection.tool_name"));

        if (player.getInventory().getFreeSlot() != -1) {
            if (player.addItem(tool)) {
                player.displayClientMessage(
                    MessageUtils.smartComponent(player, "selection.tool.obtained"),
                    false
                );
                player.displayClientMessage(
                    MessageUtils.smartComponent(player, "selection.tool.instructions"),
                    false
                );
            }
        } else {
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.tool.inventory_full"),
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
                MessageUtils.smartComponent(player, "selection.tool.no_permission"),
                true
            );
            return;
        }

        SelectionPoints selection = playerSelections.computeIfAbsent(
            player.getUUID(),
            k -> new SelectionPoints()
        );

        // Check for polygon mode
        if (selection.isMultiPointMode()) {
            handlePolygonClick(player, pos, selection);
            return;
        }

        if (!selection.hasFirstPoint()) {
            selection.setFirstPoint(pos, player.level().dimension().location().toString());

            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.mode.header").withStyle(net.minecraft.ChatFormatting.BOLD),
                false
            );
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.first_point_set", pos.getX(), pos.getY(), pos.getZ()),
                false
            );
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.set_second_point"),
                false
            );

            showSelectionMarker(player, pos);

            player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),
                                  net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 2.0f);

        } else {
            String currentDim = player.level().dimension().location().toString();
            if (!currentDim.equals(selection.getFirstPointDimension())) {
                player.displayClientMessage(
                    MessageUtils.smartComponent(player, "selection.cross_dimension_denied"),
                    true
                );
                return;
            }

            BlockPos firstPos = selection.getFirstPoint();
            double distance = Math.sqrt(
                Math.pow(pos.getX() - firstPos.getX(), 2) +
                Math.pow(pos.getZ() - firstPos.getZ(), 2)
            );

            if (distance > MAX_SELECTION_DISTANCE) {
                player.displayClientMessage(
                    MessageUtils.smartComponent(player, "selection.distance_too_far"),
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

            selection.setSecondPoint(pos, currentDim);

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
            MessageUtils.smartComponent(player, "selection.complete.header").withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.point.first", pos1.getX(), pos1.getY(), pos1.getZ()),
            false
        );
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.point.second", pos2.getX(), pos2.getY(), pos2.getZ()),
            false
        );
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.area.bounds", minX, maxX, minZ, maxZ),
            false
        );
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.area.size", width, height, area),
            false
        );

        if (area > LARGE_AREA_WARNING_THRESHOLD) {
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.note.area_large"),
                false
            );
        } else {
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.area.size_ok"),
                false
            );
        }

        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.instructions.create_or_cancel"),
            false
        );

        String currentDimension = player.level().dimension().location().toString();
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.dimension.current", getDimensionDisplayName(currentDimension)),
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
        if (!AreaManager.isValidAreaName(areaName)) {
            player.displayClientMessage(Component.translatable("gui.error.invalid_area_name"), true);
            return;
        }
        SelectionPoints selection = playerSelections.get(player.getUUID());
        if (selection == null || !selection.isComplete()) {
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.error.no_points"),
                true
            );
            return;
        }
        if (selection.isMultiPointMode() && selection.hasEnoughVerticesForPolygon()) {
            createPolygonAreaFromSelection(player, areaName, selection);
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
            MessageUtils.smartComponent(player, "selection.area.created", areaName).withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );

        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.area.dimension", getDimensionDisplayName(currentDimension)),
            false
        );
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.area.range",
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

    private static void createPolygonAreaFromSelection(ServerPlayer player, String areaName, SelectionPoints selection) {
        List<BlockPos> vertices = selection.getVertexPoints();
        List<MonitorArea.Vec2i> vecList = new ArrayList<>();
        for (BlockPos v : vertices) {
            vecList.add(new MonitorArea.Vec2i(v.getX(), v.getZ()));
        }

        String currentDimension = player.level().dimension().location().toString();

        MonitorArea area = new MonitorArea(areaName);
        area.setDisplayName(areaName);
        area.setDimension(currentDimension);
        area.setBounds(new MonitorArea.PolygonBounds(vecList));

        AreaManager.getInstance().addArea(area);

        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.area.created", areaName).withStyle(net.minecraft.ChatFormatting.BOLD), false);
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.area.dimension", getDimensionDisplayName(currentDimension)), false);
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.polygon.vertex_count", vertices.size()), false);

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
            MessageUtils.smartComponent(player, "selection.mode.setup_header").withStyle(net.minecraft.ChatFormatting.BOLD),
            false
        );
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.mode.setup_instructions"),
            false
        );
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.mode.available_modes"),
            false
        );
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(""),
            false
        );
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.mode.setup_complete"),
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
                MessageUtils.smartComponent(player, "selection.cancelled"),
                true
            );
        }
    }

    /**
     * Start polygon selection mode.
     */
    public static void startPolygonMode(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.tool.no_permission"), true);
            return;
        }
        SelectionPoints selection = playerSelections.computeIfAbsent(
            player.getUUID(), k -> new SelectionPoints());
        selection.setMultiPointMode(true);
        selection.clearVertices();
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.polygon.start").withStyle(net.minecraft.ChatFormatting.BOLD), false);
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.polygon.instructions"), false);
    }

    private static void handlePolygonClick(ServerPlayer player, BlockPos pos, SelectionPoints selection) {
        selection.addVertexPoint(pos);
        int count = selection.getVertexPoints().size();

        showSelectionMarker(player, pos);

        player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),
                              net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f + count * 0.1f);

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(
                String.format(LocalizationManager.translate("selection.polygon.vertex_added"), count)),
            false);

        // Show line to previous vertex
        if (count >= 2) {
            BlockPos prev = selection.getVertexPoints().get(count - 2);
            AreaVisualizer.drawLineBetween(player, prev, pos);
        }

        if (count >= 3) {
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.polygon.can_finish"), false);
        }
    }

    /**
     * Get player's current selection (for external access).
     */
    public static SelectionPoints getPlayerSelection(UUID playerId) {
        return playerSelections.get(playerId);
    }

    /**
     * Finish polygon selection and show summary.
     */
    public static void finishPolygon(ServerPlayer player) {
        SelectionPoints selection = playerSelections.get(player.getUUID());
        if (selection == null || !selection.isMultiPointMode() || !selection.hasEnoughVerticesForPolygon()) {
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.polygon.not_enough_vertices"), true);
            return;
        }

        List<BlockPos> vertices = selection.getVertexPoints();

        String currentDimension = player.level().dimension().location().toString();

        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.polygon.complete").withStyle(net.minecraft.ChatFormatting.BOLD), false);
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.polygon.vertex_count", vertices.size()), false);
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.instructions.create_or_cancel"), false);
        player.displayClientMessage(
            MessageUtils.smartComponent(player, "selection.dimension.current",
                getDimensionDisplayName(currentDimension)), false);

        player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),
                              net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 2.0f);
    }

    /**
     * Show current selection info to player.
     */
    public static void showCurrentSelection(ServerPlayer player) {
        SelectionPoints selection = playerSelections.get(player.getUUID());
        if (selection == null) {
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.no_active_selection"),
                true
            );
            return;
        }

        if (selection.hasFirstPoint()) {
            BlockPos pos1 = selection.getFirstPoint();
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.current.first_point",
                    pos1.getX(), pos1.getY(), pos1.getZ()),
                false
            );
        }

        if (selection.hasSecondPoint()) {
            BlockPos pos2 = selection.getSecondPoint();
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.current.second_point",
                    pos2.getX(), pos2.getY(), pos2.getZ()),
                false
            );
        } else {
            player.displayClientMessage(
                MessageUtils.smartComponent(player, "selection.need_second_point"),
                false
            );
        }
    }

    /**
     * Clean up player data when they disconnect.
     */
    public static void cleanupPlayerData(UUID playerId) {
        playerSelections.remove(playerId);
    }

    /**
     * Clean up all data when server stops.
     */
    public static void cleanupAllData() {
        playerSelections.clear();
    }
}
