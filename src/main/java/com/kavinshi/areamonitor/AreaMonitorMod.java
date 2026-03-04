package com.kavinshi.areamonitor;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AreaMonitorMod.MOD_ID)
public class AreaMonitorMod {
    public static final String MOD_ID = "areamonitor";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public AreaMonitorMod() {
        try {
            AreaMonitorMod.LOGGER.info("Initializing AreaMonitor mod...");

            IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

            ConfigManager.init();

            MinecraftForge.EVENT_BUS.register(this);
            modEventBus.addListener(this::setup);

            AreaMonitorMod.LOGGER.info("AreaMonitor mod initialized successfully");
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("Failed to initialize AreaMonitor mod", e);
            throw e;
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
        AreaMonitorMod.LOGGER.info("AreaMonitor mod setup completed");
    }

    @SubscribeEvent
    public static void onModConfigLoaded(final ModConfigEvent.Loading configEvent) {
        if (configEvent.getConfig().getModId().equals(MOD_ID)) {
            AreaMonitorMod.LOGGER.info("Loading config for {}", MOD_ID);
        }
    }

    @SubscribeEvent
    public static void onModConfigReloaded(final ModConfigEvent.Reloading configEvent) {
        if (configEvent.getConfig().getModId().equals(MOD_ID)) {
            AreaMonitorMod.LOGGER.info("Reloading config for {}", MOD_ID);
            ConfigManager.validateConfig();
            ConfigManager.CONFIG.invalidateCache();
        }
    }
}