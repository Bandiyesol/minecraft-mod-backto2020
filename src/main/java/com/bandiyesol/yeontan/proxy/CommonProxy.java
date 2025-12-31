package com.bandiyesol.yeontan.proxy;

import com.bandiyesol.yeontan.event.QuestEventHandler;
import com.bandiyesol.yeontan.network.QuestPacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        QuestPacketHandler.init();
        MinecraftForge.EVENT_BUS.register(new QuestEventHandler());
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}
}
