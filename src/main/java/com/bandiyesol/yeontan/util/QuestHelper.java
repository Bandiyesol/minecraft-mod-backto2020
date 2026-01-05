package com.bandiyesol.yeontan.util;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class QuestHelper {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    public static final String[] CLONE_POOL = {"아경", "재경", "연경", "보경", "경빈", "예빈", "채빈"};
    private static final Random RANDOM = new Random();


    // 스폰 중인 위치 추적 (중복 스폰 방지)
    private static final Set<String> spawningLocations = new HashSet<>();
    
    // 스폰 예정인 위치와 Future 추적 (취소용)
    private static final Map<String, ScheduledFuture<?>> scheduledSpawns = new HashMap<>();
    
    // stop 후 모니터링할 위치 목록 (5초 동안)
    private static final Set<String> monitoringLocations = new HashSet<>();
    
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
        
        // 중복 스폰 방지: 같은 위치에 이미 스폰 중이면 스킵
        String locationKey = String.format("%.1f,%.1f,%.1f", x, y, z);
        if (spawningLocations.contains(locationKey)) {
            System.out.println("[QuestLog] NPC already spawning at location: " + locationKey);
            return;
        }
        
        spawningLocations.add(locationKey);

        // 3. 명령어 구성: /noppes clone spawn <이름> <탭> <좌표>
        // 좌표 형식을 명확히 하기 위해 String.format 사용
        scheduler.schedule(() -> {
            server.addScheduledTask(() -> {
                String command = String.format("noppes clone spawn %s 0 %s %.2f %.2f %.2f", selectedClone, selectedClone, x, y, z);
                System.out.println("[QuestLog] Executing Command: " + command);
                try {
                    server.getCommandManager().executeCommand(entity, command);
                } finally {
                    // 스폰 완료 후 위치 추적에서 제거 (5초 후)
                    scheduler.schedule(() -> spawningLocations.remove(locationKey), 5, TimeUnit.SECONDS);
                }
            });
        },3, TimeUnit.SECONDS);
    }
    
    // --- [고정 위치에 NPC 스폰] ---
    public static void spawnQuestNpcAtLocation(QuestNpcLocationManager.SpawnLocation location, ICommandSender commandSender) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        
        WorldServer world = server.getWorld(location.dimension);
        if (world == null) return;

        // 중복 스폰 방지: 같은 위치에 이미 스폰 중이면 스킵
        String locationKey = String.format("%d:%.1f,%.1f,%.1f", location.dimension, location.x, location.y, location.z);
        if (spawningLocations.contains(locationKey)) {
            System.out.println("[QuestLog] NPC already spawning at location: " + locationKey);
            return;
        }
        
        spawningLocations.add(locationKey);
        
        // 랜덤하게 클론 선택
        String selectedClone = CLONE_POOL[RANDOM.nextInt(CLONE_POOL.length)];
        
        // 스폰 작업을 Future로 추적 (취소 가능하도록)
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            // 모니터링 중인 위치면 스폰 취소
            if (monitoringLocations.contains(locationKey)) {
                System.out.println("[QuestLog] Spawn cancelled for location (stop command active): " + locationKey);
                spawningLocations.remove(locationKey);
                scheduledSpawns.remove(locationKey);
                return;
            }
            
            server.addScheduledTask(() -> {
                String command = String.format("noppes clone spawn %s 0 %s %.2f %.2f %.2f", 
                    selectedClone, selectedClone, location.x, location.y, location.z);
                System.out.println("[QuestLog] Spawning NPC at location: " + command);
                
                try {
                    // 명령어를 실행한 플레이어 또는 서버의 첫 번째 플레이어 사용
                    ICommandSender executor;
                    if (commandSender instanceof EntityPlayerMP) {
                        executor = commandSender;
                    } else {
                        // 플레이어가 없으면 서버의 첫 번째 플레이어 사용
                        if (!server.getPlayerList().getPlayers().isEmpty()) {
                            executor = server.getPlayerList().getPlayers().get(0);
                        } else {
                            // 플레이어가 없으면 서버 사용 (콘솔)
                            executor = server;
                        }
                    }
                    
                    int result = server.getCommandManager().executeCommand(executor, command);
                    if (result > 0) {
                        System.out.println("[QuestLog] NPC spawned successfully");
                        
                        // 스폰 후 NPC를 찾아서 추적 목록에 추가 (2초 후)
                        scheduler.schedule(() -> {
                            server.addScheduledTask(() -> {
                                // 모니터링 중이면 즉시 제거
                                if (monitoringLocations.contains(locationKey)) {
                                    findAndRemoveNpcAtLocation(world, location);
                                } else {
                                    findAndTrackNpcAtLocation(world, location, selectedClone);
                                }
                                spawningLocations.remove(locationKey);
                                scheduledSpawns.remove(locationKey);
                            });
                        }, 2, TimeUnit.SECONDS);
                    } else {
                        System.err.println("[QuestLog] Command execution returned 0");
                        spawningLocations.remove(locationKey);
                        scheduledSpawns.remove(locationKey);
                    }
                } catch (Exception e) {
                    System.err.println("[QuestLog] Failed to spawn NPC: " + e.getMessage());
                    e.printStackTrace();
                    spawningLocations.remove(locationKey);
                    scheduledSpawns.remove(locationKey);
                }
            });
        }, 1, TimeUnit.SECONDS);
        
        scheduledSpawns.put(locationKey, future);
    }
    
    // 스폰 위치 근처에서 NPC를 찾아서 추적 목록에 추가
    private static void findAndTrackNpcAtLocation(WorldServer world, QuestNpcLocationManager.SpawnLocation location, String cloneName) {
        // 스폰 위치 근처(5블록 반경)에서 NPC 찾기
        double searchRadius = 5.0;
        List<String> cloneNames = Arrays.asList(CLONE_POOL);
        
        for (EntityCustomNpc npc : world.getEntities(EntityCustomNpc.class, 
                n -> n != null && cloneNames.contains(n.getName()))) {
            double distance = Math.sqrt(
                Math.pow(npc.posX - location.x, 2) + 
                Math.pow(npc.posY - location.y, 2) + 
                Math.pow(npc.posZ - location.z, 2)
            );
            
            // 위치가 가까우면 추적 목록에 추가
            if (distance <= searchRadius) {
                QuestNpcLocationManager.addActiveNpc(npc.getEntityId());
                System.out.println("[QuestLog] Tracked NPC " + npc.getEntityId() + " at location (" + 
                    location.x + ", " + location.y + ", " + location.z + ")");
                return; // 첫 번째 매칭되는 NPC만 추가
            }
        }
        
        System.out.println("[QuestLog] Warning: Could not find NPC at location (" + 
            location.x + ", " + location.y + ", " + location.z + ")");
    }
    
    // --- [고정 위치에 스폰된 NPC만 제거] ---
    public static void removeAllActiveNpcs() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        
        // 1. 스폰 예정인 작업 취소
        for (Map.Entry<String, ScheduledFuture<?>> entry : scheduledSpawns.entrySet()) {
            String locationKey = entry.getKey();
            ScheduledFuture<?> future = entry.getValue();
            if (future != null && !future.isDone()) {
                future.cancel(false);
                System.out.println("[QuestLog] Cancelled scheduled spawn for location: " + locationKey);
            }
        }
        scheduledSpawns.clear();
        
        // 2. 모든 스폰 위치를 모니터링 목록에 추가
        List<QuestNpcLocationManager.SpawnLocation> locations = QuestNpcLocationManager.getLocations();
        monitoringLocations.clear();
        for (QuestNpcLocationManager.SpawnLocation location : locations) {
            String locationKey = String.format("%d:%.1f,%.1f,%.1f", location.dimension, location.x, location.y, location.z);
            monitoringLocations.add(locationKey);
        }
        
        server.addScheduledTask(() -> {
            Set<Integer> activeNpcIds = QuestNpcLocationManager.getActiveNpcIds();
            int removedCount = 0;
            
            // 추적된 NPC ID만 제거
            for (Integer npcId : activeNpcIds) {
                for (WorldServer world : server.worlds) {
                    EntityCustomNpc npc = (EntityCustomNpc) world.getEntityByID(npcId);
                    if (npc != null && !npc.isDead) {
                        npc.isDead = true;
                        npc.world.removeEntity(npc);
                        QuestNpcLocationManager.removeActiveNpc(npcId);
                        removedCount++;
                        System.out.println("[QuestLog] Removed tracked NPC " + npcId);
                        break; // 찾았으면 다음 NPC로
                    }
                }
            }
            
            QuestNpcLocationManager.clearActiveNpcs();
            System.out.println("[QuestLog] Removed " + removedCount + " tracked quest NPCs");
            
            // 3. 5초 동안 모니터링하여 새로 생성되는 NPC 제거
            startLocationMonitoring(server, locations, 5);
        });
    }
    
    // 위치 모니터링 시작 (지연 스폰된 NPC 제거)
    private static void startLocationMonitoring(MinecraftServer server, 
            List<QuestNpcLocationManager.SpawnLocation> locations, int durationSeconds) {
        if (locations.isEmpty()) return;
        
        System.out.println("[QuestLog] Starting location monitoring for " + durationSeconds + " seconds");
        
        // 1초마다 체크
        for (int i = 1; i <= durationSeconds; i++) {
            final int checkTime = i;
            scheduler.schedule(() -> {
                server.addScheduledTask(() -> {
                    for (QuestNpcLocationManager.SpawnLocation location : locations) {
                        String locationKey = String.format("%d:%.1f,%.1f,%.1f", 
                            location.dimension, location.x, location.y, location.z);
                        
                        // 모니터링 중인 위치면 체크
                        if (monitoringLocations.contains(locationKey)) {
                            WorldServer world = server.getWorld(location.dimension);
                            if (world != null) {
                                findAndRemoveNpcAtLocation(world, location);
                            }
                        }
                    }
                    
                    // 모니터링 시간 종료 시 목록 정리
                    if (checkTime == durationSeconds) {
                        monitoringLocations.clear();
                        System.out.println("[QuestLog] Location monitoring completed");
                    }
                });
            }, i, TimeUnit.SECONDS);
        }
    }
    
    // 위치에서 NPC를 찾아서 제거
    private static void findAndRemoveNpcAtLocation(WorldServer world, QuestNpcLocationManager.SpawnLocation location) {
        double searchRadius = 5.0;
        List<String> cloneNames = Arrays.asList(CLONE_POOL);
        
        for (EntityCustomNpc npc : world.getEntities(EntityCustomNpc.class, 
                n -> n != null && !n.isDead && cloneNames.contains(n.getName()))) {
            double distance = Math.sqrt(
                Math.pow(npc.posX - location.x, 2) + 
                Math.pow(npc.posY - location.y, 2) + 
                Math.pow(npc.posZ - location.z, 2)
            );
            
            // 위치가 가까우면 제거
            if (distance <= searchRadius) {
                npc.isDead = true;
                npc.world.removeEntity(npc);
                System.out.println("[QuestLog] Removed late-spawned NPC " + npc.getEntityId() + 
                    " at location (" + location.x + ", " + location.y + ", " + location.z + ")");
                return; // 첫 번째 매칭되는 NPC만 제거
            }
        }
    }
}
