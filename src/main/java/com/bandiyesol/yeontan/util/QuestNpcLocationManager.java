package com.bandiyesol.yeontan.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class QuestNpcLocationManager {
    
    // 게임 플레이 중 스폰된 NPC 엔티티 ID를 추적
    private static final Set<Integer> activeNpcIds = new HashSet<>();
    
    // NPC 스폰 위치 목록 (하드코딩 기본값 + 명령어로 추가 가능)
    private static final List<SpawnLocation> spawnLocations = new ArrayList<>();
    
    static {
        // 기본 하드코딩 위치들 (예시 - 필요에 따라 수정)
        // spawnLocations.add(new SpawnLocation(0, 100, 64, 200)); // dimension, x, y, z
        // spawnLocations.add(new SpawnLocation(0, 200, 64, 200));
        // spawnLocations.add(new SpawnLocation(0, 300, 64, 200));
    }
    
    public static class SpawnLocation {
        public final int dimension;
        public final double x;
        public final double y;
        public final double z;
        
        public SpawnLocation(int dimension, double x, double y, double z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }
    
    /**
     * 현재 플레이어 위치를 기반으로 스폰 위치 추가
     */
    public static void addLocation(World world, BlockPos pos) {
        spawnLocations.add(new SpawnLocation(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ()));
    }
    
    /**
     * 인덱스로 위치 제거
     */
    public static boolean removeLocation(int index) {
        if (index >= 0 && index < spawnLocations.size()) {
            spawnLocations.remove(index);
            return true;
        }
        return false;
    }
    
    /**
     * 모든 위치 목록 반환
     */
    public static List<SpawnLocation> getLocations() {
        return new ArrayList<>(spawnLocations);
    }
    
    /**
     * 위치 목록 초기화
     */
    public static void clearLocations() {
        spawnLocations.clear();
    }
    
    /**
     * 활성 NPC ID 추가
     */
    public static void addActiveNpc(int entityId) {
        activeNpcIds.add(entityId);
    }
    
    /**
     * 활성 NPC ID 제거
     */
    public static void removeActiveNpc(int entityId) {
        activeNpcIds.remove(entityId);
    }
    
    /**
     * 모든 활성 NPC ID 반환
     */
    public static Set<Integer> getActiveNpcIds() {
        return new HashSet<>(activeNpcIds);
    }
    
    /**
     * 모든 활성 NPC ID 초기화
     */
    public static void clearActiveNpcs() {
        activeNpcIds.clear();
    }
}

