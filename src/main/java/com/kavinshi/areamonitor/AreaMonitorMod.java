package com.kavinshi.areamonitor;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AreaMonitorMod.MODID)
public class AreaMonitorMod {
    public static final String MODID = "areamonitor";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public AreaMonitorMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 初始化配置
        ConfigManager.init();

        // 注册事件监听器
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // 模组初始化完成
    }
}