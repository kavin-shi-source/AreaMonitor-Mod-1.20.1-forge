package com.kavinshi.areamonitor;

import com.kavinshi.areamonitor.util.MessageUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * Handles area protection events — blocks breaking/placing/interacting,
 * PVP, explosion, and entity damage within protected areas.
 */
@Mod.EventBusSubscriber(modid = AreaMonitorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AreaProtectionManager {

    private AreaProtectionManager() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer sp)) return;
        if (!isProtected(sp, "blockBreak")) return;
        event.setCanceled(true);
        sp.displayClientMessage(MessageUtils.smartComponent(sp, "protection.block_break_denied"), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!isProtected(sp, "blockPlace")) return;
        event.setCanceled(true);
        sp.displayClientMessage(MessageUtils.smartComponent(sp, "protection.block_place_denied"), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!isProtected(sp, "blockInteract")) return;
        event.setCanceled(true);
        sp.displayClientMessage(MessageUtils.smartComponent(sp, "protection.block_interact_denied"), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // PVP: Player attacking another player
        if (event.getSource().getEntity() instanceof ServerPlayer attacker &&
            event.getEntity() instanceof ServerPlayer victim) {
            if (isProtected(attacker, "pvp") || isProtected(victim, "pvp")) {
                event.setCanceled(true);
                attacker.displayClientMessage(MessageUtils.smartComponent(attacker, "protection.pvp_denied"), true);
                return;
            }
        }
        // Entity damage to player (non-player entity attacking)
        if (event.getEntity() instanceof ServerPlayer victim &&
            !(event.getSource().getEntity() instanceof Player)) {
            if (isProtected(victim, "entityDamage")) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        // Remove blocks within explosion-protected areas from affected list
        AreaManager am = AreaManager.getInstance();
        event.getAffectedBlocks().removeIf(pos -> {
            for (MonitorArea area : am.getAllAreas()) {
                if (area.isEnabled() && area.getProtection().isExplosion() &&
                    area.getBounds().contains(pos.getX(), pos.getZ())) {
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
        // Global whitelist bypass
        if (WhitelistManager.isWhitelisted(player)) return false;

        Set<String> currentAreas = AreaManager.getInstance().getCurrentAreas(player);
        if (currentAreas.isEmpty()) return false;

        AreaManager am = AreaManager.getInstance();
        for (String areaName : currentAreas) {
            MonitorArea area = am.getArea(areaName);
            if (area == null || !area.isEnabled()) continue;
            ProtectionSettings p = area.getProtection();
            switch (protectionType) {
                case "blockBreak": if (p.isBlockBreak()) return true; break;
                case "blockPlace": if (p.isBlockPlace()) return true; break;
                case "blockInteract": if (p.isBlockInteract()) return true; break;
                case "pvp": if (p.isPvp()) return true; break;
                case "entityDamage": if (p.isEntityDamage()) return true; break;
            }
        }
        return false;
    }
}
