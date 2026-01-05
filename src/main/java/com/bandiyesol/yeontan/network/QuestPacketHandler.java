package com.bandiyesol.yeontan.network;

import com.bandiyesol.yeontan.Yeontan;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class QuestPacketHandler {

    private static SimpleNetworkWrapper INSTANCE = null;
    private static boolean messageRegistered = false;

    public static void init() {
        // Initialize network channel immediately
        ensureInitialized();
    }

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

        // Register message handler only once
        if (!messageRegistered && INSTANCE != null) {
            System.out.println("[Yeontan] Registering message handler...");
            INSTANCE.registerMessage(QuestMessage.Handler.class, QuestMessage.class, 0, Side.CLIENT);
            messageRegistered = true;
            System.out.println("[Yeontan] Message handler registered!");
        }
    }

    public static SimpleNetworkWrapper getInstance() {
        ensureInitialized();
        return INSTANCE;
    }
    
    /**
     * 안전하게 패킷 전송 (null 체크 포함)
     */
    public static void sendToSafely(IMessage message, net.minecraft.entity.player.EntityPlayerMP player) {
        if (INSTANCE == null || message == null || player == null) return;
        try {
            INSTANCE.sendTo(message, player);
        } catch (Exception e) {
            System.err.println("[Yeontan] Failed to send packet to player: " + e.getMessage());
        }
    }
    
    /**
     * 안전하게 모든 클라이언트에 패킷 전송 (null 체크 포함)
     */
    public static void sendToAllSafely(IMessage message) {
        if (INSTANCE == null || message == null) return;
        try {
            INSTANCE.sendToAll(message);
        } catch (Exception e) {
            System.err.println("[Yeontan] Failed to send packet to all: " + e.getMessage());
        }
    }
}