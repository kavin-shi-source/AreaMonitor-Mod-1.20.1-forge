package com.kavinshi.areamonitor;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AreaMonitorMod.MOD_ID)
public class AreaMonitorMod {
    public static final String MOD_ID = "areamonitor";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public AreaMonitorMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ConfigManager.init();

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::setup);

    }

    private void setup(final FMLCommonSetupEvent event) {
    }
}