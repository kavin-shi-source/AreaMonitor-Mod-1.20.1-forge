package com.kavinshi.areamonitor.network;

import com.kavinshi.areamonitor.AreaManager;
import com.kavinshi.areamonitor.AreaMonitorMod;
import com.kavinshi.areamonitor.ConfigManager;
import com.kavinshi.areamonitor.MonitorArea;
import com.kavinshi.areamonitor.ProtectionSettings;
import com.kavinshi.areamonitor.TriggerConfig;
import com.kavinshi.areamonitor.commands.TriggerCommands;
import com.kavinshi.areamonitor.model.RestrictionSettings;
import com.kavinshi.areamonitor.util.DimensionUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
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
            if (player == null || !player.hasPermissions(4)) return;
            if (action == null) return;

            // Handle actions that modify area data...
            boolean stateChanged = false;
            switch (action) {
                case TOGGLE: {
                    MonitorArea area = AreaManager.getInstance().getArea(areaName);
                    if (area == null) return;
                    area.setEnabled(!area.isEnabled());
                    stateChanged = true;
                    break;
                }
                case DELETE: {
                    MonitorArea area = AreaManager.getInstance().getArea(areaName);
                    if (area == null) return;
                    AreaManager.getInstance().removeArea(areaName);
                    stateChanged = true;
                    break;
                }
                case CREATE: {
                    if (!isValidAreaName(areaName)) {
                        AreaMonitorMod.LOGGER.warn("Rejected CREATE with invalid area name '{}' from player {}",
                            areaName, player.getName().getString());
                        player.displayClientMessage(Component.translatable("gui.error.invalid_area_name"), true);
                        return;
                    }
                    if (AreaManager.getInstance().getArea(areaName) != null) return;
                    MonitorArea newArea = new MonitorArea(areaName);
                    AreaManager.getInstance().addArea(newArea);
                    stateChanged = true;
                    break;
                }
                case UPDATE: {
                    MonitorArea area = AreaManager.getInstance().getArea(areaName);
                    if (area == null || payload == null) return;
                    
                    // 1. Create draft from live area
                    ConfigManager.AreaConfig draft = ConfigManager.createConfigFromArea(area);
                    
                    // 2. Apply patch to draft
                    try {
                        applyUpdateToDraft(draft, payload);
                    } catch (Exception updateEx) {
                        AreaMonitorMod.LOGGER.error("Failed to parse area update payload for '{}'", areaName, updateEx);
                        player.displayClientMessage(Component.translatable("gui.error.invalid_config"), true);
                        return;
                    }
                    
                    // 3. Validate draft
                    if (!ConfigManager.validateAreaConfig(areaName, draft)) {
                        AreaMonitorMod.LOGGER.warn("Rejected invalid area update from {} for area '{}'",
                            player.getName().getString(), areaName);
                        player.displayClientMessage(Component.translatable("gui.error.invalid_config"), true);
                        // Push current live state to the client so GUI does not show stale dirty data
                        S2CAreaListPacket rollbackResponse = S2CAreaListPacket.fromAreas(AreaManager.getInstance().getAllAreas());
                        ModNetwork.sendToPlayer(rollbackResponse, player);
                        return;
                    }
                    
                    // 4. Apply validated draft back to live area
                    applyDraftToArea(area, draft);
                    stateChanged = true;
                    break;
                }
            }

            // Only trigger disk I/O and sync if state actually changed
            if (stateChanged) {
                ConfigManager.safeSaveConfig();
                
                // Broadcast update to all connected players who have the mod installed
                S2CAreaListPacket response = S2CAreaListPacket.fromAreas(AreaManager.getInstance().getAllAreas());
                MinecraftServer server = player.getServer();
                if (server != null) {
                    for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                        ModNetwork.sendToPlayer(response, sp);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }

    private static void applyUpdateToDraft(ConfigManager.AreaConfig draft, String json) {
        JsonObject obj = GSON.fromJson(json, JsonObject.class);
        if (obj == null) throw new IllegalArgumentException("Invalid JSON payload");
        if (obj.has("displayName") && !obj.get("displayName").isJsonNull()) {
            draft.setDisplayName(obj.get("displayName").getAsString());
        }
        if (obj.has("dimension") && !obj.get("dimension").isJsonNull()) {
            String dim = obj.get("dimension").getAsString();
            if (DimensionUtils.isValidDimension(dim)) {
                draft.setDimension(dim);
            } else {
                AreaMonitorMod.LOGGER.warn("Rejected invalid dimension '{}' from GUI update", dim);
            }
        }
        if (obj.has("enterMode") && !obj.get("enterMode").isJsonNull()) {
            draft.setEnterMode(obj.get("enterMode").getAsString());
        }
        if (obj.has("leaveMode") && !obj.get("leaveMode").isJsonNull()) {
            draft.setLeaveMode(obj.get("leaveMode").getAsString());
        }
        if (obj.has("enabled") && !obj.get("enabled").isJsonNull()) {
            draft.setEnabled(obj.get("enabled").getAsBoolean());
        }
        
        // Protection update
        if (draft.getProtection() == null) draft.setProtection(new ProtectionSettings());
        ProtectionSettings prot = draft.getProtection();
        if (obj.has("protBlockBreak") && !obj.get("protBlockBreak").isJsonNull()) prot.setBlockBreak(obj.get("protBlockBreak").getAsBoolean());
        if (obj.has("protBlockPlace") && !obj.get("protBlockPlace").isJsonNull()) prot.setBlockPlace(obj.get("protBlockPlace").getAsBoolean());
        if (obj.has("protBlockInteract") && !obj.get("protBlockInteract").isJsonNull()) prot.setBlockInteract(obj.get("protBlockInteract").getAsBoolean());
        if (obj.has("protPvp") && !obj.get("protPvp").isJsonNull()) prot.setPvp(obj.get("protPvp").getAsBoolean());
        if (obj.has("protExplosion") && !obj.get("protExplosion").isJsonNull()) prot.setExplosion(obj.get("protExplosion").getAsBoolean());
        if (obj.has("protEntityDamage") && !obj.get("protEntityDamage").isJsonNull()) prot.setEntityDamage(obj.get("protEntityDamage").getAsBoolean());
        if (obj.has("protContainerInteract") && !obj.get("protContainerInteract").isJsonNull()) prot.setContainerInteract(obj.get("protContainerInteract").getAsBoolean());
        if (obj.has("protFluidPlace") && !obj.get("protFluidPlace").isJsonNull()) prot.setFluidPlace(obj.get("protFluidPlace").getAsBoolean());
        if (obj.has("protItemDrop") && !obj.get("protItemDrop").isJsonNull()) prot.setItemDrop(obj.get("protItemDrop").getAsBoolean());
        
        // Bounds update
        if (obj.has("boundsType") && !obj.get("boundsType").isJsonNull()) {
            String type = obj.get("boundsType").getAsString();
            draft.setBoundsType(type);
            if ("RECTANGLE".equals(type) &&
                obj.has("minX") && obj.has("minZ") && obj.has("maxX") && obj.has("maxZ")) {
                draft.setMinX(obj.get("minX").getAsInt());
                draft.setMinZ(obj.get("minZ").getAsInt());
                draft.setMaxX(obj.get("maxX").getAsInt());
                draft.setMaxZ(obj.get("maxZ").getAsInt());
            } else if ("CIRCLE".equals(type) &&
                obj.has("centerX") && obj.has("centerZ") && obj.has("radius")) {
                draft.setCenterX(obj.get("centerX").getAsInt());
                draft.setCenterZ(obj.get("centerZ").getAsInt());
                draft.setRadius(obj.get("radius").getAsInt());
            } else if ("POLYGON".equals(type) &&
                obj.has("vertices") && obj.get("vertices").isJsonArray()) {
                var jsonArray = obj.getAsJsonArray("vertices");
                List<int[]> vertices = new ArrayList<>();
                for (var v : jsonArray) {
                    if (v.isJsonArray()) {
                        var arr = v.getAsJsonArray();
                        if (arr.size() >= 2) {
                            vertices.add(new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt()});
                        }
                    }
                }
                if (vertices.size() >= 3 && vertices.size() <= 32) {
                    int[][] vertArray = new int[vertices.size()][2];
                    for (int i = 0; i < vertices.size(); i++) {
                        vertArray[i] = vertices.get(i);
                    }
                    draft.setVertices(vertArray);
                }
            }
        }
        
        // Trigger update
        if (obj.has("enterTrigger")) {
            if (draft.getEnterTrigger() == null) draft.setEnterTrigger(new TriggerConfig());
            updateTrigger(draft.getEnterTrigger(), obj.getAsJsonObject("enterTrigger"));
        }
        if (obj.has("leaveTrigger")) {
            if (draft.getLeaveTrigger() == null) draft.setLeaveTrigger(new TriggerConfig());
            updateTrigger(draft.getLeaveTrigger(), obj.getAsJsonObject("leaveTrigger"));
        }
        
        // Whitelist update (collect-then-replace to avoid partial clear on parse failure)
        if (obj.has("whitelist") && obj.get("whitelist").isJsonArray()) {
            List<String> newList = new ArrayList<>();
            for (var e : obj.getAsJsonArray("whitelist")) {
                if (e.isJsonPrimitive()) newList.add(e.getAsString().toLowerCase());
            }
            draft.setWhitelist(newList);
        }
        
        // Protection whitelist update (collect-then-replace)
        if (obj.has("protWhitelist") && obj.get("protWhitelist").isJsonArray()) {
            List<String> newList = new ArrayList<>();
            for (var e : obj.getAsJsonArray("protWhitelist")) {
                if (e.isJsonPrimitive()) newList.add(e.getAsString().toLowerCase());
            }
            draft.setProtectionWhitelist(newList);
        }
        
        // Schedule update
        if (obj.has("schedule")) applyScheduleToDraft(draft, obj.getAsJsonObject("schedule"));
        // Condition update
        if (obj.has("condition")) applyConditionToDraft(draft, obj.getAsJsonObject("condition"));
        // Chain update
        if (obj.has("chain")) applyChainToDraft(draft, obj.getAsJsonObject("chain"));
        
        // Restrictions update
        if (obj.has("restrictions")) {
            if (draft.getRestrictions() == null) draft.setRestrictions(new RestrictionSettings());
            updateRestrictions(draft.getRestrictions(), obj.getAsJsonObject("restrictions"));
        }
    }

    private static void updateTrigger(TriggerConfig tc, JsonObject obj) {
        if (tc == null || obj == null) return;
        if (obj.has("commands") && obj.get("commands").isJsonArray()) {
            List<String> newCommands = new ArrayList<>();
            for (var e : obj.getAsJsonArray("commands")) {
                if (e.isJsonPrimitive()) {
                    String cmd = e.getAsString();
                    if (TriggerCommands.isValidCommand(cmd)) {
                        newCommands.add(cmd);
                    } else {
                        AreaMonitorMod.LOGGER.warn("Rejected dangerous/invalid command in trigger update: {}", cmd);
                    }
                }
            }
            tc.getCommands().clear();
            tc.getCommands().addAll(newCommands);
        }
        if (obj.has("soundEvent")) tc.setSoundEvent(obj.get("soundEvent").isJsonNull() ? null : obj.get("soundEvent").getAsString());
        if (obj.has("soundVolume") && !obj.get("soundVolume").isJsonNull()) tc.setSoundVolume(obj.get("soundVolume").getAsFloat());
        if (obj.has("soundPitch") && !obj.get("soundPitch").isJsonNull()) tc.setSoundPitch(obj.get("soundPitch").getAsFloat());
        if (obj.has("titleMain")) tc.setTitleMain(obj.get("titleMain").isJsonNull() ? null : obj.get("titleMain").getAsString());
        if (obj.has("titleSub")) tc.setTitleSub(obj.get("titleSub").isJsonNull() ? null : obj.get("titleSub").getAsString());
        if (obj.has("teleportTarget")) tc.setTeleportTarget(obj.get("teleportTarget").isJsonNull() ? null : obj.get("teleportTarget").getAsString());
        if (obj.has("actionBar")) tc.setActionBar(obj.get("actionBar").isJsonNull() ? null : obj.get("actionBar").getAsString());
        if (obj.has("potion")) tc.setPotion(obj.get("potion").isJsonNull() ? null : obj.get("potion").getAsString());
        if (obj.has("potionDuration") && !obj.get("potionDuration").isJsonNull()) tc.setPotionDuration(obj.get("potionDuration").getAsInt());
        if (obj.has("potionAmplifier") && !obj.get("potionAmplifier").isJsonNull()) tc.setPotionAmplifier(obj.get("potionAmplifier").getAsInt());
        if (obj.has("cooldownTicks") && !obj.get("cooldownTicks").isJsonNull()) tc.setCooldownTicks(obj.get("cooldownTicks").getAsInt());
        if (obj.has("debounceTicks") && !obj.get("debounceTicks").isJsonNull()) tc.setDebounceTicks(obj.get("debounceTicks").getAsInt());
        // Condition update
        if (obj.has("condition")) {
            TriggerConfig.TriggerCondition cond = GSON.fromJson(obj.get("condition"), TriggerConfig.TriggerCondition.class);
            if (cond != null) tc.setCondition(cond);
        }
    }

    private static void updateRestrictions(RestrictionSettings rs, JsonObject obj) {
        if (rs == null || obj == null) return;
        if (obj.has("enableItemBlacklist") && !obj.get("enableItemBlacklist").isJsonNull()) rs.setEnableItemBlacklist(obj.get("enableItemBlacklist").getAsBoolean());
        if (obj.has("blockTeleportCommands") && !obj.get("blockTeleportCommands").isJsonNull()) rs.setBlockTeleportCommands(obj.get("blockTeleportCommands").getAsBoolean());
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

    private static void applyScheduleToDraft(ConfigManager.AreaConfig draft, JsonObject obj) {
        if (obj == null || obj.isJsonNull()) {
            draft.setScheduleEnabled(false);
            return;
        }
        draft.setScheduleEnabled(obj.has("enabled") && obj.get("enabled").getAsBoolean());
        if (obj.has("timeMin") && !obj.get("timeMin").isJsonNull())
            draft.setScheduleTimeMin(obj.get("timeMin").getAsInt());
        else draft.setScheduleTimeMin(null);
        if (obj.has("timeMax") && !obj.get("timeMax").isJsonNull())
            draft.setScheduleTimeMax(obj.get("timeMax").getAsInt());
        else draft.setScheduleTimeMax(null);
    }

    private static void applyConditionToDraft(ConfigManager.AreaConfig draft, JsonObject obj) {
        if (obj == null || obj.isJsonNull()) {
            draft.setConditionEnabled(false);
            draft.setConditionMinPlayers(null);
            draft.setConditionRequirePlayer(null);
            return;
        }
        draft.setConditionEnabled(obj.has("enabled") && obj.get("enabled").getAsBoolean());
        if (obj.has("minPlayers") && !obj.get("minPlayers").isJsonNull())
            draft.setConditionMinPlayers(obj.get("minPlayers").getAsInt());
        else draft.setConditionMinPlayers(null);
        if (obj.has("requirePlayer") && !obj.get("requirePlayer").isJsonNull())
            draft.setConditionRequirePlayer(obj.get("requirePlayer").getAsString().toLowerCase());
        else draft.setConditionRequirePlayer(null);
    }

    private static void applyChainToDraft(ConfigManager.AreaConfig draft, JsonObject obj) {
        if (obj == null || obj.isJsonNull() || !obj.has("chainNext")) {
            draft.setChainNext(null);
            return;
        }
        if (obj.get("chainNext").isJsonNull()) {
            draft.setChainNext(null);
        } else {
            draft.setChainNext(obj.get("chainNext").getAsString());
        }
    }

    private static void applyDraftToArea(MonitorArea area, ConfigManager.AreaConfig draft) {
        area.setDisplayName(draft.getDisplayName() != null ? draft.getDisplayName() : area.getName());
        area.setDimension(draft.getDimension() != null ? draft.getDimension() : "minecraft:overworld");
        area.setEnterMode(GameType.byName(draft.getEnterMode()));
        area.setLeaveMode(GameType.byName(draft.getLeaveMode()));
        area.setEnabled(draft.isEnabled());

        // Bounds
        String type = draft.getBoundsType() != null ? draft.getBoundsType() : "RECTANGLE";
        switch (type) {
            case "CIRCLE":
                if (draft.getCenterX() != null && draft.getCenterZ() != null && draft.getRadius() != null) {
                    area.setBounds(new MonitorArea.CircleBounds(
                        draft.getCenterX(), draft.getCenterZ(), draft.getRadius()));
                }
                break;
            case "POLYGON":
                if (draft.getVertices() != null && draft.getVertices().length >= 3) {
                    List<MonitorArea.Vec2i> vertexList = new ArrayList<>();
                    for (int[] v : draft.getVertices()) {
                        if (v.length >= 2) vertexList.add(new MonitorArea.Vec2i(v[0], v[1]));
                    }
                    if (vertexList.size() >= 3) {
                        area.setBounds(new MonitorArea.PolygonBounds(vertexList));
                    }
                }
                break;
            default: // RECTANGLE
                if (draft.getMinX() != null && draft.getMaxX() != null && draft.getMinZ() != null && draft.getMaxZ() != null) {
                    area.setBounds(new MonitorArea.RectangleBounds(
                        draft.getMinX(), draft.getMinZ(), draft.getMaxX(), draft.getMaxZ()));
                }
                break;
        }

        // Whitelists
        if (draft.getWhitelist() != null) area.setWhitelist(new ArrayList<>(draft.getWhitelist()));
        if (draft.getProtectionWhitelist() != null) area.setProtectionWhitelist(new ArrayList<>(draft.getProtectionWhitelist()));

        // Protection
        if (draft.getProtection() != null) area.setProtection(draft.getProtection());

        // Restrictions
        if (draft.getRestrictions() != null) area.setRestrictions(draft.getRestrictions());

        // Triggers
        if (draft.getEnterTrigger() != null) {
            area.setEnterTrigger(draft.getEnterTrigger());
            if (draft.getEnterTrigger().getCondition() != null)
                draft.getEnterTrigger().getCondition().sanitize();
        }
        if (draft.getLeaveTrigger() != null) {
            area.setLeaveTrigger(draft.getLeaveTrigger());
            if (draft.getLeaveTrigger().getCondition() != null)
                draft.getLeaveTrigger().getCondition().sanitize();
        }

        // Schedule / Condition / Chain
        if (draft.getScheduleEnabled() != null) area.setScheduleEnabled(draft.getScheduleEnabled());
        if (draft.getScheduleTimeMin() != null) area.setScheduleTimeMin(draft.getScheduleTimeMin());
        if (draft.getScheduleTimeMax() != null) area.setScheduleTimeMax(draft.getScheduleTimeMax());
        if (draft.getConditionEnabled() != null) area.setConditionEnabled(draft.getConditionEnabled());
        if (draft.getConditionMinPlayers() != null) area.setConditionMinPlayers(draft.getConditionMinPlayers());
        if (draft.getConditionRequirePlayer() != null) area.setConditionRequirePlayer(draft.getConditionRequirePlayer());
        if (draft.getChainNext() != null) area.setChainNext(draft.getChainNext());
    }

    /**
     * P2 #14: Validate area name on CREATE — must be 1-32 chars of [a-zA-Z0-9_-].
     * Rejects empty/oversized names and characters that break JSON / file paths.
     */
    private static boolean isValidAreaName(String name) {
        return AreaManager.isValidAreaName(name);
    }
}
