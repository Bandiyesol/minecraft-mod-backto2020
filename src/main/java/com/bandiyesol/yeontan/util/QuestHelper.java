package com.bandiyesol.yeontan.util;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
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
    // 템플릿 NPC 이름 목록 - 실제 월드에 있는 NPC 이름으로 변경 필요
    // 서버 로그의 "[QuestLog] Available NPC names in the world:" 메시지를 확인하여 정확한 이름으로 변경하세요
    public static final String[] CLONE_POOL = {"Bora", "Meirios", "Agamesius"};
    private static final Random RANDOM = new Random();


    // --- [퀘스트 엔티티 스폰 로직] ---
    public static void spawnQuestNpc(Entity entity) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        // 1. 랜덤하게 클론 선택
        String selectedClone = CLONE_POOL[RANDOM.nextInt(CLONE_POOL.length)];

        // 2. 소환 위치 및 차원 설정
        double x = entity.posX;
        double y = entity.posY;
        double z = entity.posZ;
        int dimension = entity.dimension;
        WorldServer world = server.getWorld(dimension);

        if (world == null) {
            System.out.println("[QuestLog] ERROR: World not found for dimension " + dimension);
            return;
        }

        // 3. 기존 NPC를 찾아서 복제하는 방식으로 변경
        scheduler.schedule(() -> {
            server.addScheduledTask(() -> {
                System.out.println("[QuestLog] Attempting to spawn NPC: " + selectedClone);
                System.out.println("[QuestLog] Location: x=" + x + ", y=" + y + ", z=" + z + ", dim=" + dimension);
                
                try {
                    // 먼저 같은 이름의 NPC를 월드에서 찾기
                    EntityCustomNpc templateNpc = null;
                    System.out.println("[QuestLog] Searching for template NPC: " + selectedClone);
                    
                    // 모든 월드에서 검색
                    java.util.List<EntityCustomNpc> allAvailableNpcs = new java.util.ArrayList<>();
                    for (WorldServer w : server.worlds) {
                        System.out.println("[QuestLog] Checking dimension " + w.provider.getDimension());
                        try {
                            // 모든 EntityCustomNpc 가져오기
                            java.util.List<EntityCustomNpc> allNpcs = w.getEntities(EntityCustomNpc.class, n -> true);
                            System.out.println("[QuestLog] Found " + allNpcs.size() + " NPCs in dimension " + w.provider.getDimension());
                            
                            // 이름으로 필터링 및 목록 수집
                            for (EntityCustomNpc npc : allNpcs) {
                                if (npc == null) continue;
                                try {
                                    String npcName = npc.getName();
                                    System.out.println("[QuestLog] Checking NPC: " + npcName);
                                    
                                    // YeontanQuest가 없는 NPC만 템플릿 후보로 추가
                                    NBTTagCompound extraData = npc.getEntityData();
                                    if (!extraData.hasKey("YeontanQuest")) {
                                        allAvailableNpcs.add(npc);
                                    }
                                    
                                    if (npcName != null && selectedClone.equals(npcName)) {
                                        templateNpc = npc;
                                        System.out.println("[QuestLog] Found template NPC: " + selectedClone + " in dimension " + w.provider.getDimension());
                                        break;
                                    }
                                } catch (Exception e) {
                                    System.out.println("[QuestLog] Error getting NPC name: " + e.getMessage());
                                }
                            }
                            if (templateNpc != null) break;
                        } catch (Exception e) {
                            System.out.println("[QuestLog] Error searching in dimension " + w.provider.getDimension() + ": " + e.getMessage());
                        }
                    }
                    
                    if (templateNpc != null) {
                        // 템플릿 NPC를 찾았으므로 NBT를 복사하여 새 NPC 생성
                        spawnNpcFromTemplate(world, templateNpc, x, y, z);
                    } else {
                        // 템플릿 NPC를 찾지 못한 경우, 사용 가능한 NPC 중에서 랜덤 선택
                        if (!allAvailableNpcs.isEmpty()) {
                            templateNpc = allAvailableNpcs.get(RANDOM.nextInt(allAvailableNpcs.size()));
                            try {
                                String fallbackName = templateNpc.getName();
                                System.out.println("[QuestLog] Template NPC '" + selectedClone + "' not found, using fallback NPC: " + fallbackName);
                                spawnNpcFromTemplate(world, templateNpc, x, y, z);
                            } catch (Exception e) {
                                System.out.println("[QuestLog] ERROR using fallback NPC: " + e.getMessage());
                            }
                        } else {
                            System.out.println("[QuestLog] ERROR: Template NPC '" + selectedClone + "' not found and no available NPCs in the world.");
                            System.out.println("[QuestLog] Please make sure an NPC with this name exists in the world.");
                            System.out.println("[QuestLog] Available NPC names in the world:");
                            // 사용 가능한 NPC 이름 목록 출력
                            for (WorldServer w : server.worlds) {
                                try {
                                    for (EntityCustomNpc npc : w.getEntities(EntityCustomNpc.class, n -> true)) {
                                        if (npc != null) {
                                            try {
                                                String name = npc.getName();
                                                if (name != null) {
                                                    System.out.println("[QuestLog]   - " + name + " (dim " + w.provider.getDimension() + ")");
                                                }
                                            } catch (Exception e) {
                                                // 이름 가져오기 실패 시 무시
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // 월드 검색 실패 시 무시
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[QuestLog] ERROR executing spawn: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }, 3, TimeUnit.SECONDS);
    }
    
    // --- [템플릿 NPC로부터 새 NPC 생성] ---
    private static void spawnNpcFromTemplate(WorldServer world, EntityCustomNpc template, double x, double y, double z) {
        try {
            System.out.println("[QuestLog] Starting NPC clone process...");
            
            // 1단계: 템플릿 NPC의 NBT를 완전히 복사
            System.out.println("[QuestLog] Step 1: Copying NBT from template NPC...");
            NBTTagCompound nbt = new NBTTagCompound();
            template.writeToNBT(nbt);
            System.out.println("[QuestLog] NBT copied, size: " + nbt.getSize() + " tags");
            
            // 2단계: NBT 수정 (위치 및 YeontanQuest 데이터)
            System.out.println("[QuestLog] Step 2: Modifying NBT (position and quest data)...");
            nbt.setDouble("PosX", x);
            nbt.setDouble("PosY", y);
            nbt.setDouble("PosZ", z);
            
            // YeontanQuest 데이터 제거
            if (nbt.hasKey("YeontanQuest")) {
                nbt.removeTag("YeontanQuest");
                System.out.println("[QuestLog] Removed YeontanQuest from main NBT");
            }
            
            // EntityData에서도 제거
            if (nbt.hasKey("EntityData")) {
                NBTTagCompound entityData = nbt.getCompoundTag("EntityData");
                if (entityData.hasKey("YeontanQuest")) {
                    entityData.removeTag("YeontanQuest");
                    System.out.println("[QuestLog] Removed YeontanQuest from EntityData");
                }
            }
            
            // 3단계: 새 NPC 생성
            System.out.println("[QuestLog] Step 3: Creating new NPC entity...");
            EntityCustomNpc newNpc = new EntityCustomNpc(world);
            System.out.println("[QuestLog] New NPC entity created");
            
            // 4단계: NBT를 새 NPC에 로드
            System.out.println("[QuestLog] Step 4: Loading NBT into new NPC...");
            newNpc.readFromNBT(nbt);
            System.out.println("[QuestLog] NBT loaded into new NPC");
            
            // 5단계: 위치 및 회전 설정
            System.out.println("[QuestLog] Step 5: Setting position and rotation...");
            newNpc.setPosition(x, y, z);
            newNpc.prevPosX = x;
            newNpc.prevPosY = y;
            newNpc.prevPosZ = z;
            newNpc.lastTickPosX = x;
            newNpc.lastTickPosY = y;
            newNpc.lastTickPosZ = z;
            newNpc.rotationYaw = 0.0F;
            newNpc.rotationPitch = 0.0F;
            newNpc.prevRotationYaw = 0.0F;
            newNpc.prevRotationPitch = 0.0F;
            System.out.println("[QuestLog] Position set to: " + x + ", " + y + ", " + z);
            
            // 6단계: 월드에 스폰
            System.out.println("[QuestLog] Step 6: Spawning NPC in world...");
            
            // spawnEntity 시도
            boolean spawnResult = world.spawnEntity(newNpc);
            System.out.println("[QuestLog] spawnEntity() returned: " + spawnResult);
            
            if (spawnResult) {
                // CustomNPCs 초기화를 위한 추가 호출
                try {
                    world.updateEntityWithOptionalForce(newNpc, false);
                    System.out.println("[QuestLog] updateEntityWithOptionalForce() called");
                    
                    // onUpdate 호출로 초기화 시도 (다음 틱에서 실행되도록)
                    world.getMinecraftServer().addScheduledTask(() -> {
                        try {
                            newNpc.onUpdate();
                            System.out.println("[QuestLog] onUpdate() called for initialization");
                        } catch (Exception e) {
                            System.out.println("[QuestLog] Warning during onUpdate: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    System.out.println("[QuestLog] Warning during initialization: " + e.getMessage());
                }
            } else {
                System.out.println("[QuestLog] ERROR: spawnEntity() returned false. CustomNPCs may require special initialization.");
                System.out.println("[QuestLog] This NPC will not be spawned. Please check CustomNPCs documentation or use in-game commands.");
            }
            
            // 7단계: 스폰 확인 (약간의 지연 후)
            System.out.println("[QuestLog] Step 7: Verifying spawn...");
            try {
                Thread.sleep(100); // NPC가 스폰될 시간을 기다림
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            boolean spawned = false;
            int foundCount = 0;
            for (EntityCustomNpc npc : world.getEntities(EntityCustomNpc.class, n -> {
                if (n == null) return false;
                double dist = Math.sqrt(Math.pow(n.posX - x, 2) + Math.pow(n.posY - y, 2) + Math.pow(n.posZ - z, 2));
                return dist < 1.0; // 1 블록 이내
            })) {
                foundCount++;
                if (npc.getName() != null && npc.getName().equals(template.getName())) {
                    spawned = true;
                    System.out.println("[QuestLog] SUCCESS: Found spawned NPC '" + npc.getName() + "' at distance " + 
                        Math.sqrt(Math.pow(npc.posX - x, 2) + Math.pow(npc.posY - y, 2) + Math.pow(npc.posZ - z, 2)));
                    break;
                }
            }
            
            if (!spawned) {
                System.out.println("[QuestLog] WARNING: NPC not found after spawn attempt");
                System.out.println("[QuestLog] Found " + foundCount + " NPCs near the target location");
                System.out.println("[QuestLog] spawnEntity() returned: " + spawnResult);
                System.out.println("[QuestLog] This may indicate CustomNPCs requires special initialization");
            }
            
        } catch (Exception e) {
            System.out.println("[QuestLog] ERROR spawning NPC from template: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
