package com.bandiyesol.yeontan.network;

import com.bandiyesol.yeontan.Yeontan;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class QuestPacketHandler {

    private static SimpleNetworkWrapper INSTANCE = null;
    private static boolean messageRegistered = false;

    public static void init() { ensureInitialized(); }

    private static void ensureInitialized() {
        if (INSTANCE == null) {
            System.out.println("[Yeontan] Initializing network channel with modid: " + Yeontan.MODID);
            try {
                INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Yeontan.MODID);
                System.out.println("[Yeontan] Network channel created successfully!");
            } catch (Exception e) {
                System.err.println("[Yeontan] Failed to create network channel!");
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize network channel", e);
            }
        }

        if (!messageRegistered && INSTANCE != null) {
            System.out.println("[Yeontan] Registering message handler...");
            INSTANCE.registerMessage(QuestMessage.Handler.class, QuestMessage.class, 0, Side.CLIENT);
            INSTANCE.registerMessage(QuestTimerMessage.Handler.class, QuestTimerMessage.class, 1, Side.CLIENT);
            messageRegistered = true;
            System.out.println("[Yeontan] Message handler registered!");
        }
    }

    public static SimpleNetworkWrapper getInstance() {
        ensureInitialized();
        return INSTANCE;
    }
}