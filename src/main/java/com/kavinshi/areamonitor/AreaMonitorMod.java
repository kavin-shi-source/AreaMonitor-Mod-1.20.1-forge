package com.kavinshi.areamonitor;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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
        ConfigManager.ensureConfigFiles();

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
        ConfigManager.loadAreasConfig();
        ItemBlacklistManager.loadBlacklistConfig();
        
        AreaMonitorMod.LOGGER.info("Server started, area monitor ready");
        AreaMonitorMod.LOGGER.info("Loaded {} areas", AreaManager.getInstance().getAllAreas().size());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        AreaMonitorMod.LOGGER.info("Server stopping, saving area monitor data...");
        
        ConfigManager.saveAreasConfig();
        ItemBlacklistManager.saveBlacklistConfig();
        
        PerformanceMonitor.clearAllCaches();
        SelectionTool.cleanupAllData();
        AreaVisualizer.cleanupAllData();
        AreaManager.getInstance().clearUnusedCaches();
        
        AreaMonitorMod.LOGGER.info("AreaMonitor data saved and resources cleaned up");
    }
}
