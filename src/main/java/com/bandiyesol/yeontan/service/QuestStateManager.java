package com.bandiyesol.yeontan.service;

import com.bandiyesol.yeontan.util.Helper;
import com.bandiyesol.yeontan.util.QuestHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.*;

/**
 * 퀘스트 상태 관리 클래스
 * 활성 퀘스트 NPC 추적, NPC 검색 캐시, 팀 플레이어 캐시 등을 관리합니다.
 */
public class QuestStateManager {

    // 퀘스트가 있는 NPC ID를 추적하여 불필요한 검사 방지
    private static final Set<Integer> activeQuestNpcIds = new HashSet<>();
    
    // 처리 중인 NPC ID (중복 처리 방지)
    private static final Set<Integer> processingNpcIds = new HashSet<>();
    
    // NPC ID -> WorldServer 매핑 (검색 최적화)
    private static final Map<Integer, WorldServer> npcWorldCache = new HashMap<>();
    
    // 팀별 플레이어 캐싱 (5초마다 갱신)
    private static final Map<String, List<EntityPlayerMP>> teamPlayerCache = new HashMap<>();
    private static long lastCacheUpdate = 0;
    private static final long CACHE_UPDATE_INTERVAL = 5000; // 5초

    // HashSet으로 변경하여 contains() 성능 향상
    private static final Set<String> CLONE_NAMES_SET = new HashSet<>(Arrays.asList(QuestHelper.CLONE_POOL));

    /**
     * 활성 퀘스트 NPC ID 추가
     */
    public static void addActiveQuestNpc(int npcId, WorldServer world) {
        activeQuestNpcIds.add(npcId);
        if (world != null) {
            npcWorldCache.put(npcId, world);
        }
    }
    
    /**
     * 활성 퀘스트 NPC ID 제거
     */
    public static void removeActiveQuestNpc(int npcId) {
        activeQuestNpcIds.remove(npcId);
        npcWorldCache.remove(npcId);
    }
    
    /**
     * 활성 퀘스트 NPC ID 목록 반환 (불변 뷰)
     */
    public static Set<Integer> getActiveQuestNpcIds() {
        return Collections.unmodifiableSet(activeQuestNpcIds);
    }
    
    /**
     * NPC ID로 NPC 찾기 (최적화된 검색: 캐시된 월드부터 검색)
     */
    public static EntityCustomNpc findNpcById(MinecraftServer server, int entityId) {
        // 캐시된 월드부터 검색 (대부분의 경우 여기서 찾을 수 있음)
        WorldServer cachedWorld = npcWorldCache.get(entityId);
        if (cachedWorld != null) {
            EntityCustomNpc npc = (EntityCustomNpc) cachedWorld.getEntityByID(entityId);
            if (npc != null && !npc.isDead && CLONE_NAMES_SET.contains(npc.getName())) {
                return npc;
            } else {
                // 캐시가 무효화되었으면 제거
                npcWorldCache.remove(entityId);
            }
        }
        
        // 캐시에 없으면 모든 월드 검색
        for (WorldServer world : server.worlds) {
            EntityCustomNpc npc = (EntityCustomNpc) world.getEntityByID(entityId);
            if (npc != null && CLONE_NAMES_SET.contains(npc.getName())) {
                // 캐시에 저장
                npcWorldCache.put(entityId, world);
                return npc;
            }
        }
        return null;
    }
    
    /**
     * 처리 중인 NPC인지 확인
     */
    public static boolean isProcessing(int npcId) {
        return processingNpcIds.contains(npcId);
    }
    
    /**
     * 처리 중 플래그 추가
     */
    public static void addProcessing(int npcId) {
        processingNpcIds.add(npcId);
    }
    
    /**
     * 처리 중 플래그 제거
     */
    public static void removeProcessing(int npcId) {
        processingNpcIds.remove(npcId);
    }
    
    /**
     * 팀 플레이어 목록 가져오기 (캐시 사용)
     */
    public static List<EntityPlayerMP> getTeamPlayers(MinecraftServer server, String teamName) {
        long currentTime = System.currentTimeMillis();
        
        // 캐시가 오래되었거나 해당 팀의 캐시가 없으면 갱신
        if (currentTime - lastCacheUpdate > CACHE_UPDATE_INTERVAL || !teamPlayerCache.containsKey(teamName)) {
            updateTeamPlayerCache(server);
            lastCacheUpdate = currentTime;
        }
        
        return teamPlayerCache.getOrDefault(teamName, Collections.emptyList());
    }
    
    /**
     * 팀 플레이어 캐시 업데이트
     */
    private static void updateTeamPlayerCache(MinecraftServer server) {
        teamPlayerCache.clear();
        
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            String teamName = Helper.getPlayerTeamName(player);
            if (teamName != null && !teamName.isEmpty()) {
                teamPlayerCache.computeIfAbsent(teamName, k -> new ArrayList<>()).add(player);
            }
        }
    }
}

