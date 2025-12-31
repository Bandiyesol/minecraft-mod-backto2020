package com.bandiyesol.yeontan.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class QuestPacketHandler {

    public static SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("backto2020_quest");

    public static void init() {
        INSTANCE.registerMessage(QuestMessage.Handler.class, QuestMessage.class, 0, Side.CLIENT);
    }
}
