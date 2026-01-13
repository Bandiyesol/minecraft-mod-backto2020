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


public class QuestHelper {

    public static final String[] CLONE_POOL = {"아경", "재경", "연경", "보경", "경빈", "예빈", "채빈"};
    private static final Set<String> CLONE_NAMES_SET = new HashSet<>(Arrays.asList(CLONE_POOL));
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Random RANDOM = new Random();

    // 스폰 중인 위치 추적 (중복 스폰 방지) - 동시성 안전
    private static final Set<String> spawningLocations = Collections.synchronizedSet(new HashSet<>());

    // 스폰 예정인 위치와 Future 추적 (취소용) - 동시성 안전
    private static final Map<String, ScheduledFuture<?>> scheduledSpawns = Collections.synchronizedMap(new HashMap<>());

    // stop 후 모니터링할 위치 목록 (5초 동안) - 동시성 안전
    private static final Set<String> monitoringLocations = Collections.synchronizedSet(new HashSet<>());

    // 모니터링 작업 추적 (취소용)
    private static ScheduledFuture<?> monitoringTask = null;


    public static Set<String> getCloneNamesSet() {
        return CLONE_NAMES_SET;
    }


    // --- [퀘스트 엔티티 스폰 로직] ---
    public static void spawnQuestNpc(Entity entity) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || entity == null) return;

        String selectedClone = CLONE_POOL[RANDOM.nextInt(CLONE_POOL.length)];

        // entity가 제거되기 전에 위치와 world 정보를 저장
        double x = entity.posX;
        double y = entity.posY;
        double z = entity.posZ;
        WorldServer world = entity.world instanceof WorldServer ? (WorldServer) entity.world : null;
        if (world == null) return;
        
        String locationKey = String.format("%.1f,%.1f,%.1f", x, y, z);
        if (spawningLocations.contains(locationKey)) return;
        
        spawningLocations.add(locationKey);

        scheduler.schedule(() -> {
            server.addScheduledTask(() -> {
                try {
                    // world에서 사용 가능한 entity를 찾아서 command executor로 사용
                    Entity executorEntity = null;
                    for (Entity e : world.loadedEntityList) {
                        if (e != null && !e.isDead) {
                            executorEntity = e;
                            break;
                        }
                    }
                    
                    if (executorEntity == null) {
                        System.err.println("[Yeontan] No valid entity found for command execution");
                        spawningLocations.remove(locationKey);
                        return;
                    }
                    
                    String command = String.format("noppes clone spawn %s 0 %s %.2f %.2f %.2f", selectedClone, selectedClone, x, y, z);
                    server.getCommandManager().executeCommand(executorEntity, Objects.requireNonNull(command));
                } catch (Exception e) { 
                    System.err.println("[Yeontan] Failed to spawn quest NPC: " + e.getMessage());
                    e.printStackTrace();
                } finally { 
                    scheduler.schedule(() -> spawningLocations.remove(locationKey), 5, TimeUnit.SECONDS); 
                }
            });
        }, 3, TimeUnit.SECONDS);
    }

    
    // --- [고정 위치에 NPC 스폰] ---
    public static void spawnQuestNpcAtLocation(QuestNpcLocationManager.SpawnLocation location, ICommandSender commandSender) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        
        WorldServer world = server.getWorld(location.dimension);
        String locationKey = String.format("%d:%.1f,%.1f,%.1f", location.dimension, location.x, location.y, location.z);

        if (spawningLocations.contains(locationKey)) return;
        spawningLocations.add(locationKey);
        String selectedClone = CLONE_POOL[RANDOM.nextInt(CLONE_POOL.length)];
        
        ScheduledFuture<?> future =
                scheduler.schedule(() -> {
                    if (monitoringLocations.contains(locationKey)) {
                        spawningLocations.remove(locationKey);
                        scheduledSpawns.remove(locationKey);
                        return;
                    }

                    server.addScheduledTask(() -> {
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

                        synchronized (executorEntity) {
                            double oldX = executorEntity.posX;
                            double oldY = executorEntity.posY;
                            double oldZ = executorEntity.posZ;

                            try {
                                executorEntity.posX = location.x;
                                executorEntity.posY = location.y;
                                executorEntity.posZ = location.z;

                                String command = String.format("noppes clone spawn %s 0 %s %.2f %.2f %.2f", selectedClone, selectedClone, location.x, location.y, location.z);
                                int result = server.getCommandManager().executeCommand(executorEntity, Objects.requireNonNull(command));

                                if (result > 0) {
                                    scheduler.schedule(() -> {
                                        server.addScheduledTask(() -> {
                                            if (monitoringLocations.contains(locationKey)) removeNpcAtLocation(world, location);
                                            else trackNpcAtLocation(world, location);
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


    // --- [고정 위치의 NPC 제거] ---
    public static void removeAllActiveNpcs() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        for (ScheduledFuture<?> future : scheduledSpawns.values()) {
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
        }

        scheduledSpawns.clear();
        List<QuestNpcLocationManager.SpawnLocation> locations = QuestNpcLocationManager.getLocations();
        monitoringLocations.clear();

        for (QuestNpcLocationManager.SpawnLocation location : locations) {
            String locationKey = String.format("%d:%.1f,%.1f,%.1f", location.dimension, location.x, location.y, location.z);
            monitoringLocations.add(locationKey);
        }

        server.addScheduledTask(() -> {
            Set<Integer> activeNpcIds = QuestNpcLocationManager.getActiveNpcIds();
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
            startLocationMonitoring(server, locations);
        });
    }

    // --- [위치 모니터링 시작] ---
    private static void startLocationMonitoring(MinecraftServer server, List<QuestNpcLocationManager.SpawnLocation> locations) {
        if (locations.isEmpty()) return;
        if (monitoringTask != null && !monitoringTask.isDone()) monitoringTask.cancel(false);

        monitoringTask = scheduler.scheduleAtFixedRate(() -> {
            server.addScheduledTask(() -> {
                if (monitoringLocations.isEmpty()) return;

                for (QuestNpcLocationManager.SpawnLocation location : locations) {
                    String locationKey = String.format("%d:%.1f,%.1f,%.1f", location.dimension, location.x, location.y, location.z);
                    if (monitoringLocations.contains(locationKey)) {
                        WorldServer world = server.getWorld(location.dimension);
                        removeNpcAtLocation(world, location);
                    }
                }
            });
        }, 1, 1, TimeUnit.SECONDS);

        scheduler.schedule(() -> {
            monitoringLocations.clear();
            if (monitoringTask != null && !monitoringTask.isDone()) {
                monitoringTask.cancel(false);
                monitoringTask = null;
            }
        }, 5, TimeUnit.SECONDS);
    }


    // --- [npc 위치 찾기] ---
    private static EntityCustomNpc findNpcAtLocation(WorldServer world, QuestNpcLocationManager.SpawnLocation location) {
        double searchRadius = 5.0;
        double searchRadiusSq = searchRadius * searchRadius;

        net.minecraft.util.math.AxisAlignedBB searchBox = new net.minecraft.util.math.AxisAlignedBB(
                location.x - searchRadius, location.y - searchRadius, location.z - searchRadius,
                location.x + searchRadius, location.y + searchRadius, location.z + searchRadius
        );

        for (EntityCustomNpc npc : world.getEntitiesWithinAABB(EntityCustomNpc.class, searchBox)) {
            if (npc == null || npc.isDead || !getCloneNamesSet().contains(npc.getName())) continue;

            double dx = npc.posX - location.x;
            double dy = npc.posY - location.y;
            double dz = npc.posZ - location.z;
            double distanceSq = dx * dx + dy * dy + dz * dz;

            if (distanceSq <= searchRadiusSq) return npc;
        } return null;
    }


    // --- [추적 목록에 추가] ---
    private static void trackNpcAtLocation(WorldServer world, QuestNpcLocationManager.SpawnLocation location) {
        EntityCustomNpc npc = findNpcAtLocation(world, location);
        if (npc == null) return;

        QuestNpcLocationManager.addActiveNpc(npc.getEntityId());
    }


    // --- [추적 목록에서 제거] ---
    private static void removeNpcAtLocation(WorldServer world, QuestNpcLocationManager.SpawnLocation location) {
        EntityCustomNpc npc = findNpcAtLocation(world, location);
        if (npc == null) return;

        npc.isDead = true;
        npc.world.removeEntity(npc);
    }


    // --- [스케줄러 정리] ---
    public static void shutdown() {
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
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        spawningLocations.clear();
        monitoringLocations.clear();
    }
}