package com.kavinshi.areamonitor.util;

import com.kavinshi.areamonitor.AreaMonitorMod;
import com.kavinshi.areamonitor.LocalizationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Smart message utility for handling client mod detection and message localization.
 * 
 * <p>This utility provides intelligent message sending that adapts to whether
 * the client has the mod installed or not:</p>
 * <ul>
 *   <li>Clients with the mod: Receive translatable components (client-side translation)</li>
 *   <li>Clients without the mod: Receive literal components (server-side translation)</li>
 * </ul>
 * 
 * <p>The server language is controlled by the {@code /areamonitor language en/zh} command,
 * defaulting to English.</p>
 * 
 * @since 2.0.3
 */
public final class MessageUtils {
    
    private static final String MOD_ID = AreaMonitorMod.MOD_ID;
    
    private MessageUtils() {
    }
    
    /**
     * Check if a player's client has the mod installed.
     * 
     * <p>This uses Forge's network connection data to determine if the client
     * has the areamonitor mod loaded. If detection fails, returns false.</p>
     * 
     * @param player The server player to check
     * @return true if the client has the mod installed, false otherwise or if detection fails
     */
    public static boolean clientHasMod(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return false;
        }
        
        try {
            net.minecraft.network.Connection connection = player.connection.connection;
            if (connection != null && connection.channel() != null) {
                io.netty.channel.Channel channel = connection.channel();
                io.netty.util.AttributeKey<?> fmlConnectionDataKey = 
                    io.netty.util.AttributeKey.valueOf("fml:connectionData");
                
                Object connectionData = channel.attr(fmlConnectionDataKey).get();
                
                if (connectionData instanceof net.minecraftforge.network.ConnectionData data) {
                    com.google.common.collect.ImmutableList<String> modList = data.getModList();
                    if (modList != null) {
                        return modList.contains(MOD_ID);
                    }
                }
            }
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.debug("Could not determine client mod status for player {}: {}", 
                player.getName().getString(), e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Create a smart component that adapts to client mod status.
     * 
     * <p>If the client has the mod, returns a translatable component (client translates).
     * Otherwise, returns a literal component with server-side translation using
     * the server's configured language.</p>
     * 
     * @param player The target player (can be null for console)
     * @param translationKey The translation key
     * @param args Format arguments
     * @return Appropriate component based on client status
     */
    public static MutableComponent smartComponent(ServerPlayer player, String translationKey, Object... args) {
        if (player != null && clientHasMod(player)) {
            return Component.translatable(translationKey, args);
        } else {
            return Component.literal(LocalizationManager.translate(translationKey, args));
        }
    }
    
    /**
     * Create a smart component for CommandSourceStack context.
     * 
     * @param source The command source
     * @param translationKey The translation key
     * @param args Format arguments
     * @return Appropriate component based on client status
     */
    public static MutableComponent smartComponent(
            net.minecraft.commands.CommandSourceStack source, 
            String translationKey, 
            Object... args) {
        ServerPlayer player = null;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
        }
        return smartComponent(player, translationKey, args);
    }
    
    /**
     * Send a success message with smart localization.
     * 
     * @param source The command source
     * @param translationKey The translation key
     * @param logToConsole Whether to log to console
     * @param args Format arguments
     */
    public static void sendSuccess(
            net.minecraft.commands.CommandSourceStack source,
            String translationKey,
            boolean logToConsole,
            Object... args) {
        source.sendSuccess(
            () -> smartComponent(source, translationKey, args),
            logToConsole
        );
    }
    
    /**
     * Send a failure message with smart localization.
     * 
     * @param source The command source
     * @param translationKey The translation key
     * @param args Format arguments
     */
    public static void sendFailure(
            net.minecraft.commands.CommandSourceStack source,
            String translationKey,
            Object... args) {
        source.sendFailure(smartComponent(source, translationKey, args));
    }
    
    /**
     * Send a message directly to a player with smart localization.
     * 
     * @param player The target player
     * @param translationKey The translation key
     * @param args Format arguments
     */
    public static void sendMessage(ServerPlayer player, String translationKey, Object... args) {
        if (player != null) {
            player.sendSystemMessage(smartComponent(player, translationKey, args));
        }
    }
}
