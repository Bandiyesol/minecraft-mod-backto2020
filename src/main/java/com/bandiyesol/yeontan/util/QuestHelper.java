package com.bandiyesol.yeontan.util;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Objects;

/**
 * 퀘스트 NPC 스폰/제거 유틸리티 클래스
 * NPC 스폰, 제거, 위치 추적 등의 기능을 제공합니다.
 */
public class QuestHelper {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    public static final String[] CLONE_POOL = {"아경", "재경", "연경", "보경", "경빈", "예빈", "채빈"};
    // HashSet으로 변경하여 contains() 성능 향상
    private static final Set<String> CLONE_NAMES_SET = new HashSet<>(Arrays.asList(CLONE_POOL));
    private static final Random RANDOM = new Random();
    
    /**
     * CLONE_NAMES_SET을 반환하는 공통 메서드 (중복 정의 방지)
     */
    public static Set<String> getCloneNamesSet() {
        return CLONE_NAMES_SET;
    }

    // 스폰 중인 위치 추적 (중복 스폰 방지) - 동시성 안전
    private static final Set<String> spawningLocations = Collections.synchronizedSet(new HashSet<>());
    
    // 스폰 예정인 위치와 Future 추적 (취소용) - 동시성 안전
    private static final Map<String, ScheduledFuture<?>> scheduledSpawns = Collections.synchronizedMap(new HashMap<>());
    
    // stop 후 모니터링할 위치 목록 (5초 동안) - 동시성 안전
    private static final Set<String> monitoringLocations = Collections.synchronizedSet(new HashSet<>());
    
    // 모니터링 작업 추적 (취소용)
    private static ScheduledFuture<?> monitoringTask = null;
    
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
            return;
        }
        
        spawningLocations.add(locationKey);

        // 3. 명령어 구성: /noppes clone spawn <이름> <탭> <좌표>
        scheduler.schedule(() -> {
            server.addScheduledTask(() -> {
                try {
                    String command = String.format("noppes clone spawn %s 0 %s %.2f %.2f %.2f", selectedClone, selectedClone, x, y, z);
                    server.getCommandManager().executeCommand(Objects.requireNonNull(entity), Objects.requireNonNull(command));
                } catch (Exception e) {
                    System.err.println("[Yeontan] Failed to spawn quest NPC: " + e.getMessage());
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

        // 중복 스폰 방지
        String locationKey = String.format("%d:%.1f,%.1f,%.1f", location.dimension, location.x, location.y, location.z);
        if (spawningLocations.contains(locationKey)) {
            return;
        }
        
        spawningLocations.add(locationKey);
        String selectedClone = CLONE_POOL[RANDOM.nextInt(CLONE_POOL.length)];
        
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            if (monitoringLocations.contains(locationKey)) {
                spawningLocations.remove(locationKey);
                scheduledSpawns.remove(locationKey);
                return;
            }
            
            server.addScheduledTask(() -> {
                // 해당 위치에 있는 엔티티 찾기 (명령어 실행자로 사용)
                Entity executorEntity = null;
                for (Entity entity : world.loadedEntityList) {
                    if (entity != null && !entity.isDead) {
                        executorEntity = entity;
                        break;
                    }
                }
                
                if (executorEntity == null) {
                    spawningLocations.remove(locationKey);
                    scheduledSpawns.remove(locationKey);
                    return;
                }
                
                // 안전하게 엔티티 위치를 임시 변경하여 명령어 실행
                // 동시성 문제 방지를 위해 엔티티를 동기화하고 try-finally로 위치 복원 보장
                synchronized (executorEntity) {
                    // 원래 위치 저장
                    double oldX = executorEntity.posX;
                    double oldY = executorEntity.posY;
                    double oldZ = executorEntity.posZ;
                    
                    try {
                        // 목표 위치로 임시 변경
                        executorEntity.posX = location.x;
                        executorEntity.posY = location.y;
                        executorEntity.posZ = location.z;
                        
                        // 명령어 실행
                        String command = String.format("noppes clone spawn %s 0 %s %.2f %.2f %.2f",
                            selectedClone, selectedClone, location.x, location.y, location.z);
                        
                        int result = server.getCommandManager().executeCommand(executorEntity, Objects.requireNonNull(command));
                        
                        if (result > 0) {
                            scheduler.schedule(() -> {
                                server.addScheduledTask(() -> {
                                    if (monitoringLocations.contains(locationKey)) {
                                        findAndRemoveNpcAtLocation(world, location);
                                    } else {
                                        findAndTrackNpcAtLocation(world, location);
                                    }
                                    spawningLocations.remove(locationKey);
                                    scheduledSpawns.remove(locationKey);
                                });
                            }, 2, TimeUnit.SECONDS);
                        } else {
                            spawningLocations.remove(locationKey);
                            scheduledSpawns.remove(locationKey);
                        }
                    } catch (Exception e) {
                        System.err.println("[Yeontan] Failed to spawn NPC at location: " + e.getMessage());
                        spawningLocations.remove(locationKey);
                        scheduledSpawns.remove(locationKey);
                    } finally {
                        // 반드시 위치 복원 (예외 발생 시에도 보장)
                        if (!executorEntity.isDead) {
                            executorEntity.posX = oldX;
                            executorEntity.posY = oldY;
                            executorEntity.posZ = oldZ;
                        }
                    }
                }
            });
        }, 1, TimeUnit.SECONDS);
        
        scheduledSpawns.put(locationKey, future);
    }


    // 스폰 위치 근처에서 NPC를 찾아서 추적 목록에 추가
    private static void findAndTrackNpcAtLocation(WorldServer world, QuestNpcLocationManager.SpawnLocation location) {
        // 최적화: 위치 기반 AABB 검색 사용 (모든 엔티티 검사 대신)
        double searchRadius = 5.0;
        double searchRadiusSq = searchRadius * searchRadius; // 제곱 거리로 비교 (sqrt 제거)
        
        net.minecraft.util.math.AxisAlignedBB searchBox = new net.minecraft.util.math.AxisAlignedBB(
            location.x - searchRadius, location.y - searchRadius, location.z - searchRadius,
            location.x + searchRadius, location.y + searchRadius, location.z + searchRadius
        );
        
        for (EntityCustomNpc npc : world.getEntitiesWithinAABB(EntityCustomNpc.class, searchBox)) {
            if (npc == null || npc.isDead || !getCloneNamesSet().contains(npc.getName())) {
                continue;
            }
            
            // 제곱 거리로 비교 (sqrt 연산 제거)
            double dx = npc.posX - location.x;
            double dy = npc.posY - location.y;
            double dz = npc.posZ - location.z;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            
            // 위치가 가까우면 추적 목록에 추가
            if (distanceSq <= searchRadiusSq) {
                QuestNpcLocationManager.addActiveNpc(npc.getEntityId());
                return; // 첫 번째 매칭되는 NPC만 추가
            }
        }
    }
    
    // --- [고정 위치에 스폰된 NPC만 제거] ---
    public static void removeAllActiveNpcs() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        
        // 1. 스폰 예정인 작업 취소
        for (ScheduledFuture<?> future : scheduledSpawns.values()) {
            if (future != null && !future.isDone()) {
                future.cancel(false);
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
            
            // 최적화: 월드별로 그룹화하여 검색 횟수 감소
            Map<WorldServer, List<Integer>> npcsByWorld = new HashMap<>();
            for (Integer npcId : activeNpcIds) {
                for (WorldServer world : server.worlds) {
                    EntityCustomNpc npc = (EntityCustomNpc) world.getEntityByID(npcId);
                    if (npc != null && !npc.isDead) {
                        npcsByWorld.computeIfAbsent(world, k -> new ArrayList<>()).add(npcId);
                        break;
                    }
                }
            }
            
            // 월드별로 일괄 처리
            for (Map.Entry<WorldServer, List<Integer>> entry : npcsByWorld.entrySet()) {
                WorldServer world = entry.getKey();
                for (Integer npcId : entry.getValue()) {
                    EntityCustomNpc npc = (EntityCustomNpc) world.getEntityByID(npcId);
                    if (npc != null && !npc.isDead) {
                        npc.isDead = true;
                        npc.world.removeEntity(npc);
                        QuestNpcLocationManager.removeActiveNpc(npcId);
                    }
                }
            }
            
            QuestNpcLocationManager.clearActiveNpcs();
            
            // 3. 5초 동안 모니터링하여 새로 생성되는 NPC 제거
            startLocationMonitoring(server, locations, 5);
        });
    }
    
    // 위치 모니터링 시작 (지연 스폰된 NPC 제거)
    private static void startLocationMonitoring(MinecraftServer server, 
            List<QuestNpcLocationManager.SpawnLocation> locations, int durationSeconds) {
        if (locations.isEmpty()) return;
        
        // 기존 모니터링 작업이 있으면 취소
        if (monitoringTask != null && !monitoringTask.isDone()) {
            monitoringTask.cancel(false);
        }
        
        // 최적화: 단일 스케줄 작업으로 변경 (반복 스케줄링 대신)
        monitoringTask = scheduler.scheduleAtFixedRate(() -> {
            server.addScheduledTask(() -> {
                // 모니터링이 비활성화되었으면 종료
                if (monitoringLocations.isEmpty()) {
                    return;
                }
                
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
            });
        }, 1, 1, TimeUnit.SECONDS);
        
        // 모니터링 종료 스케줄
        scheduler.schedule(() -> {
            monitoringLocations.clear();
            if (monitoringTask != null && !monitoringTask.isDone()) {
                monitoringTask.cancel(false);
                monitoringTask = null;
            }
        }, durationSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * 스케줄러 정리 (모드 언로드 시 호출)
     */
    public static void shutdown() {
        // 모든 예정된 작업 취소
        if (monitoringTask != null && !monitoringTask.isDone()) {
            monitoringTask.cancel(false);
            monitoringTask = null;
        }
        
        for (ScheduledFuture<?> future : scheduledSpawns.values()) {
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
        }
        scheduledSpawns.clear();
        
        // 스케줄러 종료
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 컬렉션 정리
        spawningLocations.clear();
        monitoringLocations.clear();
    }
    
    // 위치에서 NPC를 찾아서 제거
    private static void findAndRemoveNpcAtLocation(WorldServer world, QuestNpcLocationManager.SpawnLocation location) {
        // 최적화: 위치 기반 AABB 검색 사용
        double searchRadius = 5.0;
        double searchRadiusSq = searchRadius * searchRadius; // 제곱 거리로 비교 (sqrt 제거)
        
        net.minecraft.util.math.AxisAlignedBB searchBox = new net.minecraft.util.math.AxisAlignedBB(
            location.x - searchRadius, location.y - searchRadius, location.z - searchRadius,
            location.x + searchRadius, location.y + searchRadius, location.z + searchRadius
        );
        
        for (EntityCustomNpc npc : world.getEntitiesWithinAABB(EntityCustomNpc.class, searchBox)) {
            if (npc == null || npc.isDead || !getCloneNamesSet().contains(npc.getName())) {
                continue;
            }
            
            // 제곱 거리로 비교 (sqrt 연산 제거)
            double dx = npc.posX - location.x;
            double dy = npc.posY - location.y;
            double dz = npc.posZ - location.z;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            
            // 위치가 가까우면 제거
            if (distanceSq <= searchRadiusSq) {
                npc.isDead = true;
                npc.world.removeEntity(npc);
                return; // 첫 번째 매칭되는 NPC만 제거
            }
        }
    }
}
