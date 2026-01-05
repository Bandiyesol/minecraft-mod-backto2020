package com.bandiyesol.yeontan.util;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QuestHelper {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    public static final String[] CLONE_POOL = {"아경", "재경", "연경", "보경", "경빈", "예빈", "채빈"};
    private static final Random RANDOM = new Random();


    // --- [퀘스트 엔티티 스폰 로직] ---
    public static void spawnQuestNpc(Entity entity) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        // 1. 랜덤하게 클론 선택
        String selectedClone = CLONE_POOL[RANDOM.nextInt(CLONE_POOL.length)];

        // 2. 소환 위치 설정
        double x = entity.posX;
        double y = entity.posY;
        double z = entity.posZ;

        // 3. 명령어 구성: /noppes clone spawn <이름> <탭> <좌표>
        // 좌표 형식을 명확히 하기 위해 String.format 사용
        scheduler.schedule(() -> {
            server.addScheduledTask(() -> {
                String command = String.format("noppes clone spawn %s 0 %s %.2f %.2f %.2f", selectedClone, selectedClone, x, y, z);
                System.out.println("[QuestLog] Executing Command: " + command);
                server.getCommandManager().executeCommand(entity, command);
            });
        },3, TimeUnit.SECONDS);
    }
    
    // --- [고정 위치에 NPC 스폰] ---
    public static void spawnQuestNpcAtLocation(QuestNpcLocationManager.SpawnLocation location) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        
        WorldServer world = server.getWorld(location.dimension);
        if (world == null) return;
        
        // 랜덤하게 클론 선택
        String selectedClone = CLONE_POOL[RANDOM.nextInt(CLONE_POOL.length)];
        
        scheduler.schedule(() -> {
            server.addScheduledTask(() -> {
                String command = String.format("noppes clone spawn %s 0 %s %.2f %.2f %.2f", 
                    selectedClone, selectedClone, location.x, location.y, location.z);
                System.out.println("[QuestLog] Spawning NPC at location: " + command);
                
                try {
                    // 서버를 ICommandSender로 사용하여 명령어 실행
                    int result = server.getCommandManager().executeCommand(server, command);
                    if (result > 0) {
                        System.out.println("[QuestLog] NPC spawned successfully");
                    } else {
                        System.err.println("[QuestLog] Command execution returned 0");
                    }
                } catch (Exception e) {
                    System.err.println("[QuestLog] Failed to spawn NPC: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }, 1, TimeUnit.SECONDS);
    }
    
    // --- [모든 활성 NPC 제거] ---
    public static void removeAllActiveNpcs() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        
        server.addScheduledTask(() -> {
            List<String> cloneNames = Arrays.asList(CLONE_POOL);
            int removedCount = 0;
            
            for (WorldServer world : server.worlds) {
                for (EntityCustomNpc npc : world.getEntities(EntityCustomNpc.class, 
                        n -> n != null && cloneNames.contains(n.getName()))) {
                    // 모든 퀘스트 관련 NPC 제거 (YeontanQuest 태그가 있거나 없는 모든 클론 NPC)
                    npc.setDead();
                    world.removeEntity(npc);
                    QuestNpcLocationManager.removeActiveNpc(npc.getEntityId());
                    removedCount++;
                }
            }
            
            QuestNpcLocationManager.clearActiveNpcs();
            System.out.println("[QuestLog] Removed " + removedCount + " quest NPCs");
        });
    }
}
