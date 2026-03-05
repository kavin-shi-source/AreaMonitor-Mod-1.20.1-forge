package com.kavinshi.areamonitor;

/*
 * Area Monitor Mod - Minecraft mod for monitoring and managing protected areas
 * Copyright (C) 2024 AreaMonitor Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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

            // 初始化多语言系统
            LocalizationManager.getInstance();

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
            // 在配置加载时验证并初始化配置文件
            ConfigManager.ensureConfigFiles();
            ConfigManager.loadAreasConfig();
            ItemBlacklistManager.loadBlacklistConfig();
        }
    }

    @SubscribeEvent
    public static void onModConfigReloaded(final ModConfigEvent.Reloading configEvent) {
        if (configEvent.getConfig().getModId().equals(MOD_ID)) {
            AreaMonitorMod.LOGGER.info("Reloading config for {}", MOD_ID);
            ConfigManager.validateConfig();
        }
    }
}