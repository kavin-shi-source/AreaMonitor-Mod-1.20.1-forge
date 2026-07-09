package com.kavinshi.areamonitor;

import com.kavinshi.areamonitor.network.ModNetwork;
import com.kavinshi.areamonitor.util.AuditLogger;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

@Mod(AreaMonitorMod.MOD_ID)
public class AreaMonitorMod {
    public static final String MOD_ID = "areamonitor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AreaMonitorMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);

        // Allow vanilla or non-modded clients to connect
        ModLoadingContext.get().registerExtensionPoint(
            IExtensionPoint.DisplayTest.class,
            () -> new IExtensionPoint.DisplayTest(
                () -> NetworkConstants.IGNORESERVERONLY,
                (remoteVersion, isFromServer) -> true
            )
        );

        ConfigManager.init();

        ModNetwork.register();

        AreaMonitorMod.LOGGER.info("AreaMonitor mod initialized successfully");
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlacklistManager.initializeDefaultBlacklist();
            AreaMonitorMod.LOGGER.info("AreaMonitor mod setup completed");
        });
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        AreaMonitorMod.LOGGER.info("Server started, area monitor ready");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        AreaMonitorMod.LOGGER.info("Server stopping, saving area monitor data...");

        try {
            ConfigManager.saveAreasConfigSync();
        } catch (Throwable t) {
            AreaMonitorMod.LOGGER.error("Failed to save areas config on shutdown", t);
        }
        try {
            ConfigManager.shutdown();
        } catch (Throwable t) {
            AreaMonitorMod.LOGGER.error("Failed to shutdown config executor", t);
        }
        try {
            ItemBlacklistManager.saveBlacklistConfig();
        } catch (Throwable t) {
            AreaMonitorMod.LOGGER.error("Failed to save item blacklist on shutdown", t);
        }
        try {
            ItemBlacklistManager.clearAllData();
        } catch (Throwable t) {
            AreaMonitorMod.LOGGER.error("Failed to clear item blacklist data on shutdown", t);
        }

        try {
            WhitelistManager.saveWhitelist();
        } catch (Throwable t) {
            AreaMonitorMod.LOGGER.error("Failed to save whitelist on shutdown", t);
        }

        try {
            PerformanceMonitor.clearAllCaches();
        } catch (Throwable t) {
            AreaMonitorMod.LOGGER.error("Failed to clear performance caches on shutdown", t);
        }
        try {
            SelectionTool.cleanupAllData();
        } catch (Throwable t) {
            AreaMonitorMod.LOGGER.error("Failed to clean up selection tool data on shutdown", t);
        }
        try {
            AreaVisualizer.cleanupAllData();
        } catch (Throwable t) {
            AreaMonitorMod.LOGGER.error("Failed to clean up area visualizer on shutdown", t);
        }

        try {
            AreaManager.getInstance().clearAllData();
        } catch (Throwable t) {
            AreaMonitorMod.LOGGER.error("Failed to clear area manager data on shutdown", t);
        }

        try {
            WhitelistManager.clearAllData();
        } catch (Throwable t) {
            AreaMonitorMod.LOGGER.error("Failed to clear whitelist data on shutdown", t);
        }

        try {
            com.kavinshi.areamonitor.util.AuditLogger.shutdown();
        } catch (Throwable t) {
            AreaMonitorMod.LOGGER.error("Failed to shut down audit logger", t);
        }

        try {
            AreaMonitor.cleanupRuntimeState();
        } catch (Throwable t) {
            AreaMonitorMod.LOGGER.error("Failed to clean up AreaMonitor runtime state", t);
        }

        AreaMonitorMod.LOGGER.info("AreaMonitor data saved and resources cleaned up");
    }
}
