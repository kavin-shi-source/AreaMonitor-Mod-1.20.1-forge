package com.kavinshi.areamonitor;

import com.kavinshi.areamonitor.util.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles area protection events — blocks breaking/placing/interacting,
 * PVP, explosion, entity damage, container access, fluid placement, item drop/pickup.
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AreaProtectionManager {

    private AreaProtectionManager() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        if (!isProtected(sp, "itemDrop")) return;
        event.setCanceled(true);
        sp.displayClientMessage(MessageUtils.smartComponent(sp, "protection.item_drop_denied"), true);
        AreaVisualizer.spawnDeniedBurst(sp, sp.getX(), sp.getY(), sp.getZ());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer sp)) return;
        if (!isProtected(sp, "blockBreak")) return;
        event.setCanceled(true);
        sp.displayClientMessage(MessageUtils.smartComponent(sp, "protection.block_break_denied"), true);
        BlockPos pos = event.getPos();
        AreaVisualizer.spawnDeniedBurst(sp, pos.getX(), pos.getY(), pos.getZ());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        // Check block-place protection or fluid-place protection
        if (isProtected(sp, "blockPlace")) {
            event.setCanceled(true);
            sp.displayClientMessage(MessageUtils.smartComponent(sp, "protection.block_place_denied"), true);
            BlockPos pos = event.getPos();
            AreaVisualizer.spawnDeniedBurst(sp, pos.getX(), pos.getY(), pos.getZ());
            return;
        }
        if (isFluidPlaceBlocked(sp, event)) {
            event.setCanceled(true);
            sp.displayClientMessage(MessageUtils.smartComponent(sp, "protection.fluid_place_denied"), true);
            BlockPos pos = event.getPos();
            AreaVisualizer.spawnDeniedBurst(sp, pos.getX(), pos.getY(), pos.getZ());
        }
    }

    private static boolean isFluidPlaceBlocked(ServerPlayer sp, BlockEvent.EntityPlaceEvent event) {
        if (!isProtected(sp, "fluidPlace")) return false;
        var state = event.getPlacedBlock();
        if (state == null) return false;
        return state.is(Blocks.WATER) || state.is(Blocks.LAVA)
            || state.getFluidState().is(Fluids.WATER)
            || state.getFluidState().is(Fluids.LAVA);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        BlockPos pos = event.getPos();
        // Container interaction protection
        if (isProtected(sp, "containerInteract")) {
            var level = event.getLevel();
            var be = level.getBlockEntity(pos);
            if (be instanceof BaseContainerBlockEntity || level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.ANVIL) || level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.CHIPPED_ANVIL) || level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.DAMAGED_ANVIL)) {
                event.setCanceled(true);
                sp.displayClientMessage(MessageUtils.smartComponent(sp, "protection.container_interact_denied"), true);
                AreaVisualizer.spawnDeniedBurst(sp, pos.getX(), pos.getY(), pos.getZ());
                return;
            }
        }
        // Block interaction protection
        if (isProtected(sp, "blockInteract")) {
            event.setCanceled(true);
            sp.displayClientMessage(MessageUtils.smartComponent(sp, "protection.block_interact_denied"), true);
            AreaVisualizer.spawnDeniedBurst(sp, pos.getX(), pos.getY(), pos.getZ());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        net.minecraft.world.entity.Entity target = event.getTarget();
        if (target == null) return;

        // Container interaction protection for entity-based containers (e.g., chest minecarts)
        if (isProtected(sp, "containerInteract") && target instanceof net.minecraft.world.Container) {
            event.setCanceled(true);
            sp.displayClientMessage(MessageUtils.smartComponent(sp, "protection.container_interact_denied"), true);
            AreaVisualizer.spawnDeniedBurst(sp, target.getX(), target.getY(), target.getZ());
            return;
        }

        // Entity interaction protection maps to blockInteract (e.g., item frames, armor stands)
        if (isProtected(sp, "blockInteract")) {
            event.setCanceled(true);
            sp.displayClientMessage(MessageUtils.smartComponent(sp, "protection.block_interact_denied"), true);
            AreaVisualizer.spawnDeniedBurst(sp, target.getX(), target.getY(), target.getZ());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker)) return;
        net.minecraft.world.entity.Entity victim = event.getTarget();
        if (victim == null) return;
        
        // Skip players as they are handled by LivingHurtEvent for PVP
        if (victim instanceof Player) return;

        // Short-circuit when no areas exist, matching onLivingHurt
        AreaManager am = AreaManager.getInstance();
        if (am.getAllAreas().isEmpty()) return;

        // Check if victim is in a protected area
        String dimension = victim.level().dimension().location().toString();
        boolean anyProtection = false;
        boolean allWhitelisted = true;

        if (WhitelistManager.isWhitelisted(attacker)) {
            return;
        }

        String attackerName = attacker.getGameProfile().getName().toLowerCase();
        for (MonitorArea area : am.getPotentialAreasAt(victim.getX(), victim.getZ(), dimension)) {
            if (area == null || !area.isEnabled()) continue;
            if (!area.getDimension().equals(dimension)) continue;
            if (!area.getProtection().isEntityDamage()) continue;
            if (!area.getBounds().contains(victim.getX(), victim.getZ())) continue;
            
            anyProtection = true;
            if (!area.getProtectionWhitelist().contains(attackerName)) {
                allWhitelisted = false;
                break;
            }
        }
        
        if (anyProtection && !allWhitelisted) {
            event.setCanceled(true);
            // Re-use block_interact_denied or a similar message as feedback
            attacker.displayClientMessage(MessageUtils.smartComponent(attacker, "protection.block_interact_denied"), true);
            AreaVisualizer.spawnDeniedBurst(attacker, victim.getX(), victim.getY(), victim.getZ());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (AreaManager.getInstance().getAllAreas().isEmpty()) return;
        var source = event.getSource();
        if (source == null) return;
        // PVP: Player attacking another player
        if (source.getEntity() instanceof ServerPlayer attacker &&
            event.getEntity() instanceof ServerPlayer victim) {
            if (isProtected(attacker, "pvp") || isProtected(victim, "pvp")) {
                event.setCanceled(true);
                attacker.displayClientMessage(MessageUtils.smartComponent(attacker, "protection.pvp_denied"), true);
                AreaVisualizer.spawnDeniedBurst(attacker, victim.getX(), victim.getY(), victim.getZ());
                return;
            }
        }
        // Entity damage to player (non-player source: mobs, fall, fire, etc.)
        if (event.getEntity() instanceof ServerPlayer victim &&
            !(source.getEntity() instanceof Player)) {
            if (isProtected(victim, "entityDamage")) {
                event.setCanceled(true);
                AreaVisualizer.spawnDeniedBurst(victim, victim.getX(), victim.getY(), victim.getZ());
                return;
            }
        }
        // Entity damage to non-player living entities (villagers, animals, armor stands, etc.)
        // Protects all living entities inside entityDamage-protected areas
        if (!(event.getEntity() instanceof Player)) {
            net.minecraft.world.entity.LivingEntity victim = event.getEntity();
            String dimension = victim.level().dimension().location().toString();
            // P2 #6 fix: previously the first area whose protection whitelist contained the
            // attacker caused a `return`, bypassing every other overlapping protected area.
            // Now we require the attacker to be whitelisted in ALL overlapping protected areas
            // (or globally whitelisted) before allowing the damage.
            AreaManager am = AreaManager.getInstance();
            boolean anyProtection = false;
            boolean allWhitelisted = true;
            ServerPlayer attacker = source.getEntity() instanceof ServerPlayer sp ? sp : null;
            if (attacker != null && WhitelistManager.isWhitelisted(attacker)) {
                return; // global whitelist bypasses everything
            }
            String attackerName = attacker != null ? attacker.getGameProfile().getName().toLowerCase() : null;
            for (MonitorArea area : am.getPotentialAreasAt(victim.getX(), victim.getZ(), dimension)) {
                if (area == null || !area.isEnabled()) continue;
                if (!area.getDimension().equals(dimension)) continue;
                if (!area.getProtection().isEntityDamage()) continue;
                if (!area.getBounds().contains(victim.getX(), victim.getZ())) continue;
                anyProtection = true;
                if (attacker == null || attackerName == null ||
                    !area.getProtectionWhitelist().contains(attackerName)) {
                    allWhitelisted = false;
                    break;
                }
            }
            if (anyProtection && !allWhitelisted) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        AreaManager am = AreaManager.getInstance();
        String dimension = event.getLevel().dimension().location().toString();
        double centerX = event.getExplosion().getPosition().x;
        double centerZ = event.getExplosion().getPosition().z;

        // P2 #5 fix: collect candidate areas from every grid cell covered by the explosion's
        // affected blocks/entities, not just the center cell. An explosion near a grid boundary
        // can damage blocks in a neighbouring cell that contains a protected area which the
        // center-only lookup would miss.
        double minX = centerX, maxX = centerX;
        double minZ = centerZ, maxZ = centerZ;
        for (var pos : event.getAffectedBlocks()) {
            if (pos.getX() < minX) minX = pos.getX();
            if (pos.getX() > maxX) maxX = pos.getX();
            if (pos.getZ() < minZ) minZ = pos.getZ();
            if (pos.getZ() > maxZ) maxZ = pos.getZ();
        }
        for (var ent : event.getAffectedEntities()) {
            if (ent.getX() < minX) minX = ent.getX();
            if (ent.getX() > maxX) maxX = ent.getX();
            if (ent.getZ() < minZ) minZ = ent.getZ();
            if (ent.getZ() > maxZ) maxZ = ent.getZ();
        }

        Set<MonitorArea> candidateAreas = am.getPotentialAreasInBox(minX, minZ, maxX, maxZ, dimension);

        if (candidateAreas.isEmpty()) {
            return;
        }

        List<MonitorArea> explosionProtected = new ArrayList<>();
        for (MonitorArea area : candidateAreas) {
            if (area.isEnabled() && area.getProtection().isExplosion() &&
                area.getDimension().equals(dimension)) {
                explosionProtected.add(area);
            }
        }
        if (explosionProtected.isEmpty()) {
            return;
        }

        event.getAffectedBlocks().removeIf(pos -> {
            for (MonitorArea area : explosionProtected) {
                if (area.getBounds().contains(pos.getX(), pos.getZ())) {
                    return true;
                }
            }
            return false;
        });
        event.getAffectedEntities().removeIf(ent -> {
            for (MonitorArea area : explosionProtected) {
                if (area.getBounds().contains(ent.getX(), ent.getZ())) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Check if the given protection type is active for the player in any of their current areas.
     */
    private static boolean isProtected(ServerPlayer player, String protectionType) {
        // Guard against null player or player without a level (edge case during login/logout transitions)
        if (player == null || player.level() == null) return false;
        // Global whitelist bypass
        if (WhitelistManager.isWhitelisted(player)) return false;

        AreaManager am = AreaManager.getInstance();
        String playerName = player.getGameProfile().getName().toLowerCase();
        String dimension = player.level().dimension().location().toString();

        Set<String> currentAreas = am.getCurrentAreas(player);
        // P2 #7 fix: when the playerAreas cache is empty (player just entered and checkPlayer
        // hasn't ticked yet — up to 5 ticks of window), fall back to a live spatial lookup so
        // protection is enforced from the first interaction rather than after the cache warms.
        if (currentAreas.isEmpty()) {
            for (MonitorArea area : am.getPotentialAreasAt(player.getX(), player.getZ(), dimension)) {
                if (area == null || !area.isEnabled()) continue;
                if (!area.getDimension().equals(dimension)) continue;
                if (!area.getBounds().contains(player.getX(), player.getZ())) continue;
                if (area.getProtectionWhitelist().contains(playerName)) continue;
                ProtectionSettings p = area.getProtection();
                if (matchesProtection(p, protectionType)) return true;
            }
            return false;
        }

        for (String areaName : currentAreas) {
            MonitorArea area = am.getArea(areaName);
            if (area == null || !area.isEnabled()) continue;
            // Per-area protection whitelist: player bypasses protection but still gets game mode changes
            if (area.getProtectionWhitelist().contains(playerName)) continue;
            ProtectionSettings p = area.getProtection();
            if (matchesProtection(p, protectionType)) return true;
        }
        return false;
    }

    private static boolean matchesProtection(ProtectionSettings p, String protectionType) {
        switch (protectionType) {
            case "blockBreak": return p.isBlockBreak();
            case "blockPlace": return p.isBlockPlace();
            case "blockInteract": return p.isBlockInteract();
            case "pvp": return p.isPvp();
            case "entityDamage": return p.isEntityDamage();
            case "containerInteract": return p.isContainerInteract();
            case "fluidPlace": return p.isFluidPlace();
            case "itemDrop": return p.isItemDrop();
            default: return false;
        }
    }

    /**
     * Location-based protection check for non-player entities (villagers, animals, etc.)
     * that have no whitelist concept. Used by entityDamage and explosion protection.
     * Uses spatial partitioning for O(k) lookup instead of O(n).
     */
    private static boolean isProtectedAtLocation(double x, double z, String dimension, String protectionType) {
        AreaManager am = AreaManager.getInstance();
        for (MonitorArea area : am.getPotentialAreasAt(x, z, dimension)) {
            if (area == null || !area.isEnabled()) continue;
            if (!area.getDimension().equals(dimension)) continue;
            if (!area.getBounds().contains(x, z)) continue;
            ProtectionSettings p = area.getProtection();
            switch (protectionType) {
                case "entityDamage": if (p.isEntityDamage()) return true; break;
                case "explosion": if (p.isExplosion()) return true; break;
            }
        }
        return false;
    }
}
