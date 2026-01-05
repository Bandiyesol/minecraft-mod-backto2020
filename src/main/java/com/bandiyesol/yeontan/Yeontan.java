package com.bandiyesol.yeontan;

import com.bandiyesol.yeontan.command.CommandQuest;
import com.bandiyesol.yeontan.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(
        modid = Yeontan.MODID,
        name = Yeontan.NAME,
        version = Yeontan.VERSION,
        dependencies = "required-after:forge@[14.23.5.2847,);after:customnpcs;required-after:iadd"
)
public class Yeontan {

    public static final String MODID = "yeontan";
    public static final String NAME = "Back To 2020";
    public static final String VERSION = "1.1";

    @Mod.Instance
    public static Yeontan instance;

    @SidedProxy(
            clientSide = "com.bandiyesol.yeontan.proxy.ClientProxy",
            serverSide = "com.bandiyesol.yeontan.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandQuest());
    }
}