package com.kavinshi.areamonitor.network;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.AreaMonitorMod;
import com.kavinshi.areamonitor.ConfigManager;
import com.kavinshi.areamonitor.MonitorArea;
import com.kavinshi.areamonitor.TriggerConfig;
import com.kavinshi.areamonitor.model.RestrictionSettings;
import com.kavinshi.areamonitor.util.DimensionUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * C2S: Client sends an area management action to server.
 * Supported actions: TOGGLE, DELETE, CREATE, UPDATE
 */
public class C2SAreaActionPacket {

    private static final Gson GSON = new Gson();

    public enum Action { TOGGLE, DELETE, CREATE, UPDATE }

    private final Action action;
    private final String areaName;
    private final String payload; // JSON for UPDATE action

    public C2SAreaActionPacket(Action action, String areaName) {
        this(action, areaName, null);
    }

    public C2SAreaActionPacket(Action action, String areaName, String payload) {
        this.action = action;
        this.areaName = areaName;
        this.payload = payload;
    }

    public C2SAreaActionPacket(FriendlyByteBuf buf) {
        Action parsed;
        try {
            parsed = Action.valueOf(buf.readUtf(32));
        } catch (IllegalArgumentException e) {
            parsed = null;
            AreaMonitorMod.LOGGER.warn("Received C2SAreaActionPacket with invalid action name");
        }
        this.action = parsed;
        this.areaName = buf.readUtf(8192);
        this.payload = buf.readBoolean() ? buf.readUtf(32760) : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(action.name(), 32);
        buf.writeUtf(areaName, 8192);
        buf.writeBoolean(payload != null);
        if (payload != null) buf.writeUtf(payload, 32760);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) return;
            if (action == null) return;

            switch (action) {
                case TOGGLE: {
                    MonitorArea area = AreaManager.getInstance().getArea(areaName);
                    if (area == null) return;
                    area.setEnabled(!area.isEnabled());
                    ConfigManager.safeSaveConfig();
                    break;
                }
                case DELETE: {
                    MonitorArea area = AreaManager.getInstance().getArea(areaName);
                    if (area == null) return;
                    AreaManager.getInstance().removeArea(areaName);
                    ConfigManager.safeSaveConfig();
                    break;
                }
                case CREATE: {
                    // P2 #14 fix: validate area name before creating — packet comes from client
                    // and could contain empty strings, very long names, or characters that break
                    // JSON serialization / file paths.
                    if (!isValidAreaName(areaName)) {
                        AreaMonitorMod.LOGGER.warn("Rejected CREATE with invalid area name '{}' from player {}",
                            areaName, player.getName().getString());
                        player.displayClientMessage(Component.translatable("gui.error.invalid_area_name"), true);
                        return;
                    }
                    if (AreaManager.getInstance().getArea(areaName) != null) return;
                    MonitorArea newArea = new MonitorArea(areaName);
                    AreaManager.getInstance().addArea(newArea);
                    ConfigManager.safeSaveConfig();
                    break;
                }
                case UPDATE: {
                    MonitorArea area = AreaManager.getInstance().getArea(areaName);
                    if (area == null || payload == null) return;
                    try {
                        applyUpdate(area, payload);
                    } catch (Exception updateEx) {
                        AreaMonitorMod.LOGGER.error("Failed to apply area update for '{}', reloading from disk to roll back partial mutation", areaName, updateEx);
                        try {
                            ConfigManager.loadAreasConfig();
                        } catch (Exception reloadEx) {
                            AreaMonitorMod.LOGGER.error("Critical: failed to rollback area config after update failure", reloadEx);
                            player.displayClientMessage(Component.translatable("gui.error.rollback_failed"), false);
                        }
                        player.displayClientMessage(Component.translatable("gui.error.invalid_config"), true);
                        return;
                    }
                    ConfigManager.AreaConfig cfg = ConfigManager.createConfigFromArea(area);
                    if (!ConfigManager.validateAreaConfig(areaName, cfg)) {
                        AreaMonitorMod.LOGGER.warn("Rejected invalid area update from {} for area '{}', reloading from disk",
                            player.getName().getString(), areaName);
                        player.displayClientMessage(Component.translatable("gui.error.invalid_config"), true);
                        // Rollback: reload all areas from disk to restore the last-saved valid state
                        try {
                            ConfigManager.loadAreasConfig();
                        } catch (Exception reloadEx) {
                            AreaMonitorMod.LOGGER.error("Critical: failed to rollback area config after rejected update", reloadEx);
                            player.displayClientMessage(Component.translatable("gui.error.rollback_failed"), false);
                        }
                        // Push the rolled-back state to the client so GUI does not show stale dirty data
                        S2CAreaListPacket rollbackResponse = S2CAreaListPacket.fromAreas(AreaManager.getInstance().getAllAreas());
                        ModNetwork.sendToPlayer(rollbackResponse, player);
                        return;
                    }
                    ConfigManager.safeSaveConfig();
                    break;
                }
            }

            // Send updated list back to the player
            S2CAreaListPacket response = S2CAreaListPacket.fromAreas(
                AreaManager.getInstance().getAllAreas());
            ModNetwork.sendToPlayer(response, player);
        });
        context.setPacketHandled(true);
    }

    private static void applyUpdate(MonitorArea area, String json) {
        JsonObject obj = GSON.fromJson(json, JsonObject.class);
        if (obj == null) throw new IllegalArgumentException("Invalid JSON payload");
        if (obj.has("displayName") && !obj.get("displayName").isJsonNull()) {
            area.setDisplayName(obj.get("displayName").getAsString());
        }
        if (obj.has("dimension") && !obj.get("dimension").isJsonNull()) {
            String dim = obj.get("dimension").getAsString();
            if (DimensionUtils.isValidDimension(dim)) {
                area.setDimension(dim);
            } else {
                AreaMonitorMod.LOGGER.warn("Rejected invalid dimension '{}' from GUI update", dim);
            }
        }
        if (obj.has("enterMode") && !obj.get("enterMode").isJsonNull()) {
            area.setEnterMode(GameType.byName(obj.get("enterMode").getAsString()));
        }
        if (obj.has("leaveMode") && !obj.get("leaveMode").isJsonNull()) {
            area.setLeaveMode(GameType.byName(obj.get("leaveMode").getAsString()));
        }
        if (obj.has("enabled") && !obj.get("enabled").isJsonNull()) {
            area.setEnabled(obj.get("enabled").getAsBoolean());
        }
        // Protection update
        if (obj.has("protBlockBreak")) area.getProtection().setBlockBreak(obj.get("protBlockBreak").getAsBoolean());
        if (obj.has("protBlockPlace")) area.getProtection().setBlockPlace(obj.get("protBlockPlace").getAsBoolean());
        if (obj.has("protBlockInteract")) area.getProtection().setBlockInteract(obj.get("protBlockInteract").getAsBoolean());
        if (obj.has("protPvp")) area.getProtection().setPvp(obj.get("protPvp").getAsBoolean());
        if (obj.has("protExplosion")) area.getProtection().setExplosion(obj.get("protExplosion").getAsBoolean());
        if (obj.has("protEntityDamage")) area.getProtection().setEntityDamage(obj.get("protEntityDamage").getAsBoolean());
        if (obj.has("protContainerInteract")) area.getProtection().setContainerInteract(obj.get("protContainerInteract").getAsBoolean());
        if (obj.has("protFluidPlace")) area.getProtection().setFluidPlace(obj.get("protFluidPlace").getAsBoolean());
        if (obj.has("protItemDrop")) area.getProtection().setItemDrop(obj.get("protItemDrop").getAsBoolean());
        // Bounds update
        if (obj.has("boundsType") && !obj.get("boundsType").isJsonNull()) {
            String type = obj.get("boundsType").getAsString();
            if ("RECTANGLE".equals(type) &&
                obj.has("minX") && obj.has("minZ") && obj.has("maxX") && obj.has("maxZ")) {
                area.setBounds(new MonitorArea.RectangleBounds(
                    obj.get("minX").getAsInt(), obj.get("minZ").getAsInt(),
                    obj.get("maxX").getAsInt(), obj.get("maxZ").getAsInt()));
            } else if ("CIRCLE".equals(type) &&
                obj.has("centerX") && obj.has("centerZ") && obj.has("radius")) {
                area.setBounds(new MonitorArea.CircleBounds(
                    obj.get("centerX").getAsInt(), obj.get("centerZ").getAsInt(),
                    obj.get("radius").getAsInt()));
            }
        }
        // Trigger update
        if (obj.has("enterTrigger")) {
            if (area.getEnterTrigger() == null) area.setEnterTrigger(new TriggerConfig());
            updateTrigger(area.getEnterTrigger(), obj.getAsJsonObject("enterTrigger"));
        }
        if (obj.has("leaveTrigger")) {
            if (area.getLeaveTrigger() == null) area.setLeaveTrigger(new TriggerConfig());
            updateTrigger(area.getLeaveTrigger(), obj.getAsJsonObject("leaveTrigger"));
        }
        // Whitelist update (collect-then-replace to avoid partial clear on parse failure)
        if (obj.has("whitelist") && obj.get("whitelist").isJsonArray()) {
            List<String> newList = new ArrayList<>();
            for (var e : obj.getAsJsonArray("whitelist")) {
                if (e.isJsonPrimitive()) newList.add(e.getAsString().toLowerCase());
            }
            area.getWhitelist().clear();
            area.getWhitelist().addAll(newList);
        }
        // Protection whitelist update (collect-then-replace)
        if (obj.has("protWhitelist") && obj.get("protWhitelist").isJsonArray()) {
            List<String> newList = new ArrayList<>();
            for (var e : obj.getAsJsonArray("protWhitelist")) {
                if (e.isJsonPrimitive()) newList.add(e.getAsString().toLowerCase());
            }
            area.getProtectionWhitelist().clear();
            area.getProtectionWhitelist().addAll(newList);
        }
        // Schedule update
        if (obj.has("schedule")) applySchedule(area, obj.getAsJsonObject("schedule"));
        // Condition update
        if (obj.has("condition")) applyCondition(area, obj.getAsJsonObject("condition"));
        // Chain update
        if (obj.has("chain")) applyChain(area, obj.getAsJsonObject("chain"));
        // Restrictions update
        if (obj.has("restrictions")) updateRestrictions(area.getRestrictions(), obj.getAsJsonObject("restrictions"));
    }

    private static void updateTrigger(TriggerConfig tc, JsonObject obj) {
        if (tc == null || obj == null) return;
        // P2 #13 fix: collect commands into a temp list before replacing, so a parse failure
        // midway through the array does not leave the commands collection partially cleared.
        if (obj.has("commands") && obj.get("commands").isJsonArray()) {
            List<String> newCommands = new ArrayList<>();
            for (var e : obj.getAsJsonArray("commands")) {
                if (e.isJsonPrimitive()) newCommands.add(e.getAsString());
            }
            tc.getCommands().clear();
            tc.getCommands().addAll(newCommands);
        }
        if (obj.has("soundEvent")) tc.setSoundEvent(obj.get("soundEvent").isJsonNull() ? null : obj.get("soundEvent").getAsString());
        if (obj.has("soundVolume")) tc.setSoundVolume(obj.get("soundVolume").getAsFloat());
        if (obj.has("soundPitch")) tc.setSoundPitch(obj.get("soundPitch").getAsFloat());
        if (obj.has("titleMain")) tc.setTitleMain(obj.get("titleMain").isJsonNull() ? null : obj.get("titleMain").getAsString());
        if (obj.has("titleSub")) tc.setTitleSub(obj.get("titleSub").isJsonNull() ? null : obj.get("titleSub").getAsString());
        if (obj.has("teleportTarget")) tc.setTeleportTarget(obj.get("teleportTarget").isJsonNull() ? null : obj.get("teleportTarget").getAsString());
        if (obj.has("actionBar")) tc.setActionBar(obj.get("actionBar").isJsonNull() ? null : obj.get("actionBar").getAsString());
        if (obj.has("potion")) tc.setPotion(obj.get("potion").isJsonNull() ? null : obj.get("potion").getAsString());
        if (obj.has("potionDuration")) tc.setPotionDuration(obj.get("potionDuration").getAsInt());
        if (obj.has("potionAmplifier")) tc.setPotionAmplifier(obj.get("potionAmplifier").getAsInt());
        if (obj.has("cooldownTicks")) tc.setCooldownTicks(obj.get("cooldownTicks").getAsInt());
        if (obj.has("debounceTicks")) tc.setDebounceTicks(obj.get("debounceTicks").getAsInt());
        // Condition update
        if (obj.has("condition")) {
            TriggerConfig.TriggerCondition cond = GSON.fromJson(obj.get("condition"), TriggerConfig.TriggerCondition.class);
            if (cond != null) tc.setCondition(cond);
        }
    }

    private static void updateRestrictions(RestrictionSettings rs, JsonObject obj) {
        if (rs == null || obj == null) return;
        if (obj.has("enableItemBlacklist")) rs.setEnableItemBlacklist(obj.get("enableItemBlacklist").getAsBoolean());
        if (obj.has("blockTeleportCommands")) rs.setBlockTeleportCommands(obj.get("blockTeleportCommands").getAsBoolean());
        // P2 #13 fix: collect-then-replace so a parse failure does not leave collections cleared
        // P3 #4: use setter (replaces contents atomically) instead of mutating the unmodifiable view
        if (obj.has("blockedItems") && obj.get("blockedItems").isJsonArray()) {
            List<String> newList = new ArrayList<>();
            for (var e : obj.getAsJsonArray("blockedItems")) {
                if (e.isJsonPrimitive()) newList.add(e.getAsString());
            }
            rs.setBlockedItems(newList);
        }
        if (obj.has("blockedCommands") && obj.get("blockedCommands").isJsonArray()) {
            List<String> newList = new ArrayList<>();
            for (var e : obj.getAsJsonArray("blockedCommands")) {
                if (e.isJsonPrimitive()) newList.add(e.getAsString());
            }
            rs.setBlockedCommands(newList);
        }
    }

    private static void applySchedule(MonitorArea area, JsonObject obj) {
        if (obj == null || obj.isJsonNull()) {
            area.setScheduleEnabled(false);
            return;
        }
        area.setScheduleEnabled(obj.has("enabled") && obj.get("enabled").getAsBoolean());
        if (obj.has("timeMin") && !obj.get("timeMin").isJsonNull())
            area.setScheduleTimeMin(obj.get("timeMin").getAsInt());
        else area.setScheduleTimeMin(null);
        if (obj.has("timeMax") && !obj.get("timeMax").isJsonNull())
            area.setScheduleTimeMax(obj.get("timeMax").getAsInt());
        else area.setScheduleTimeMax(null);
    }

    private static void applyCondition(MonitorArea area, JsonObject obj) {
        if (obj == null || obj.isJsonNull()) {
            area.setConditionEnabled(false);
            area.setConditionMinPlayers(null);
            area.setConditionRequirePlayer(null);
            return;
        }
        area.setConditionEnabled(obj.has("enabled") && obj.get("enabled").getAsBoolean());
        if (obj.has("minPlayers") && !obj.get("minPlayers").isJsonNull())
            area.setConditionMinPlayers(obj.get("minPlayers").getAsInt());
        else area.setConditionMinPlayers(null);
        if (obj.has("requirePlayer") && !obj.get("requirePlayer").isJsonNull())
            area.setConditionRequirePlayer(obj.get("requirePlayer").getAsString().toLowerCase());
        else area.setConditionRequirePlayer(null);
    }

    private static void applyChain(MonitorArea area, JsonObject obj) {
        if (obj == null || obj.isJsonNull() || !obj.has("chainNext")) {
            area.setChainNext(null);
            return;
        }
        if (obj.get("chainNext").isJsonNull()) {
            area.setChainNext(null);
        } else {
            area.setChainNext(obj.get("chainNext").getAsString());
        }
    }

    /**
     * P2 #14: Validate area name on CREATE — must be 1-32 chars of [a-zA-Z0-9_-].
     * Rejects empty/oversized names and characters that break JSON / file paths.
     */
    private static boolean isValidAreaName(String name) {
        return AreaManager.isValidAreaName(name);
    }
}
