package com.kavinshi.areamonitor.network;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.ConfigManager;
import com.kavinshi.areamonitor.MonitorArea;
import com.kavinshi.areamonitor.TriggerConfig;
import com.kavinshi.areamonitor.model.RestrictionSettings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.NetworkEvent;

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
        this.action = Action.valueOf(buf.readUtf());
        this.areaName = buf.readUtf();
        this.payload = buf.readBoolean() ? buf.readUtf() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(action.name());
        buf.writeUtf(areaName);
        buf.writeBoolean(payload != null);
        if (payload != null) buf.writeUtf(payload);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) return;

            switch (action) {
                case TOGGLE: {
                    MonitorArea area = AreaManager.getInstance().getArea(areaName);
                    if (area == null) return;
                    area.setEnabled(!area.isEnabled());
                    ConfigManager.saveAreasConfig();
                    break;
                }
                case DELETE: {
                    MonitorArea area = AreaManager.getInstance().getArea(areaName);
                    if (area == null) return;
                    AreaManager.getInstance().removeArea(areaName);
                    ConfigManager.saveAreasConfig();
                    break;
                }
                case CREATE: {
                    if (AreaManager.getInstance().getArea(areaName) != null) return;
                    MonitorArea newArea = new MonitorArea(areaName);
                    AreaManager.getInstance().addArea(newArea);
                    ConfigManager.saveAreasConfig();
                    break;
                }
                case UPDATE: {
                    MonitorArea area = AreaManager.getInstance().getArea(areaName);
                    if (area == null || payload == null) return;
                    applyUpdate(area, payload);
                    ConfigManager.saveAreasConfig();
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
        try {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj.has("displayName") && !obj.get("displayName").isJsonNull()) {
                area.setDisplayName(obj.get("displayName").getAsString());
            }
            if (obj.has("dimension") && !obj.get("dimension").isJsonNull()) {
                area.setDimension(obj.get("dimension").getAsString());
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
            if (obj.has("enterTrigger")) updateTrigger(area.getEnterTrigger(), obj.getAsJsonObject("enterTrigger"));
            if (obj.has("leaveTrigger")) updateTrigger(area.getLeaveTrigger(), obj.getAsJsonObject("leaveTrigger"));
            // Whitelist update
            if (obj.has("whitelist") && obj.get("whitelist").isJsonArray()) {
                area.getWhitelist().clear();
                for (var e : obj.getAsJsonArray("whitelist"))
                    area.getWhitelist().add(e.getAsString().toLowerCase());
            }
            // Protection whitelist update
            if (obj.has("protWhitelist") && obj.get("protWhitelist").isJsonArray()) {
                area.getProtectionWhitelist().clear();
                for (var e : obj.getAsJsonArray("protWhitelist"))
                    area.getProtectionWhitelist().add(e.getAsString().toLowerCase());
            }
            // Schedule update
            if (obj.has("schedule")) applySchedule(area, obj.getAsJsonObject("schedule"));
            // Condition update
            if (obj.has("condition")) applyCondition(area, obj.getAsJsonObject("condition"));
            // Chain update
            if (obj.has("chain")) applyChain(area, obj.getAsJsonObject("chain"));
            // Restrictions update
            if (obj.has("restrictions")) updateRestrictions(area.getRestrictions(), obj.getAsJsonObject("restrictions"));
        } catch (Exception e) {
            com.kavinshi.areamonitor.AreaMonitorMod.LOGGER.error("Failed to apply area update", e);
        }
    }

    private static void updateTrigger(TriggerConfig tc, JsonObject obj) {
        if (tc == null || obj == null) return;
        if (obj.has("commands")) tc.getCommands().clear();
        if (obj.has("commands") && obj.get("commands").isJsonArray()) {
            for (var e : obj.getAsJsonArray("commands"))
                tc.getCommands().add(e.getAsString());
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
        if (obj.has("blockedItems") && obj.get("blockedItems").isJsonArray()) {
            rs.getBlockedItems().clear();
            for (var e : obj.getAsJsonArray("blockedItems"))
                rs.getBlockedItems().add(e.getAsString());
        }
        if (obj.has("blockedCommands") && obj.get("blockedCommands").isJsonArray()) {
            rs.getBlockedCommands().clear();
            for (var e : obj.getAsJsonArray("blockedCommands"))
                rs.getBlockedCommands().add(e.getAsString());
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
            area.setChainDelayTicks(0);
            return;
        }
        if (obj.get("chainNext").isJsonNull()) {
            area.setChainNext(null);
        } else {
            area.setChainNext(obj.get("chainNext").getAsString());
        }
        if (obj.has("chainDelayTicks") && !obj.get("chainDelayTicks").isJsonNull())
            area.setChainDelayTicks(obj.get("chainDelayTicks").getAsInt());
        else area.setChainDelayTicks(0);
    }
}
