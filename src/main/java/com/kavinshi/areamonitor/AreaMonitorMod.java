package com.kavinshi.areamonitor;

import com.kavinshi.areamonitor.network.ModNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

        ConfigManager.init();

        ModNetwork.register();

        AreaMonitorMod.LOGGER.info("AreaMonitor mod initialized successfully");
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlacklistManager.initializeDefaultBlacklist();
        });
        AreaMonitorMod.LOGGER.info("AreaMonitor mod setup completed");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LocalizationManager.applyConfigLanguage(ConfigManager.CONFIG.language.get());

        ConfigManager.loadAreasConfig();
        ItemBlacklistManager.loadBlacklistConfig();

        AreaMonitorMod.LOGGER.info("Server started, area monitor ready");
        AreaMonitorMod.LOGGER.info("Loaded {} areas", AreaManager.getInstance().getAllAreas().size());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // P2 #11 fix: wrap each cleanup step in its own try-catch so an exception in one
        // manager does not skip the others. Forge does not guarantee cross-class handler
        // ordering, so each @SubscribeEvent method must be self-sufficient and resilient.
        AreaMonitorMod.LOGGER.info("Server stopping, saving area monitor data...");

        try {
            ConfigManager.saveAreasConfig();
        } catch (Exception ex) {
            AreaMonitorMod.LOGGER.error("Failed to save areas config on shutdown", ex);
        }
        try {
            ItemBlacklistManager.saveBlacklistConfig();
        } catch (Exception ex) {
            AreaMonitorMod.LOGGER.error("Failed to save item blacklist on shutdown", ex);
        }

        try {
            WhitelistManager.saveWhitelist();
        } catch (Exception ex) {
            AreaMonitorMod.LOGGER.error("Failed to save whitelist on shutdown", ex);
        }

        try {
            PerformanceMonitor.clearAllCaches();
        } catch (Exception ex) {
            AreaMonitorMod.LOGGER.error("Failed to clear performance caches on shutdown", ex);
        }
        try {
            SelectionTool.cleanupAllData();
        } catch (Exception ex) {
            AreaMonitorMod.LOGGER.error("Failed to clean up selection tool data on shutdown", ex);
        }
        try {
            AreaVisualizer.cleanupAllData();
        } catch (Exception ex) {
            AreaMonitorMod.LOGGER.error("Failed to clean up area visualizer on shutdown", ex);
        }

        try {
            AreaManager.getInstance().clearAllData();
            AreaManager.getInstance().clearUnusedCaches();
        } catch (Exception ex) {
            AreaMonitorMod.LOGGER.error("Failed to clear area manager data on shutdown", ex);
        }

        try {
            WhitelistManager.clearAllData();
        } catch (Exception ex) {
            AreaMonitorMod.LOGGER.error("Failed to clear whitelist data on shutdown", ex);
        }

        AreaMonitorMod.LOGGER.info("AreaMonitor data saved and resources cleaned up");
    }
}
