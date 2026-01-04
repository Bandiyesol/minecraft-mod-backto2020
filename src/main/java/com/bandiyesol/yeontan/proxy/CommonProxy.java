package com.bandiyesol.yeontan.proxy;

import com.bandiyesol.yeontan.entity.QuestManager;
import com.bandiyesol.yeontan.event.QuestEventHandler;
import com.bandiyesol.yeontan.network.QuestPacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        System.out.println("[Yeontan] PreInit starting...");

        QuestManager.setupQuests();
        System.out.println("[Yeontan] Quests setup complete");

        QuestPacketHandler.init();
        System.out.println("[Yeontan] Packet handler initialized");

        System.out.println("[Yeontan] PreInit complete");
    }

    public void init(FMLInitializationEvent event) {
        System.out.println("[Yeontan] Init starting...");
        
        MinecraftForge.EVENT_BUS.register(new QuestEventHandler());
        System.out.println("[Yeontan] Event handler registered");
    }

    public void postInit(FMLPostInitializationEvent event) {
        System.out.println("[Yeontan] PostInit complete");
    }
}