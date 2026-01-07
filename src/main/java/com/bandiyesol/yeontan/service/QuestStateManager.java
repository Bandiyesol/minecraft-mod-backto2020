package com.bandiyesol.yeontan.service;

import com.bandiyesol.yeontan.util.Helper;
import com.bandiyesol.yeontan.util.QuestHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.*;


public class QuestStateManager {

    // 퀘스트가 있는 NPC ID를 추적하여 불필요한 검사 방지 - 동시성 안전
    private static final Set<Integer> activeQuestNpcIds = Collections.synchronizedSet(new HashSet<>());
    
    // 처리 중인 NPC ID (중복 처리 방지) - 동시성 안전
    private static final Set<Integer> processingNpcIds = Collections.synchronizedSet(new HashSet<>());
    
    // NPC ID -> WorldServer 매핑 (검색 최적화) - 동시성 안전
    private static final Map<Integer, WorldServer> npcWorldCache = Collections.synchronizedMap(new HashMap<>());
    
    // 팀별 플레이어 캐싱 (5초마다 갱신) - 동시성 안전
    private static final Map<String, List<EntityPlayerMP>> teamPlayerCache = Collections.synchronizedMap(new HashMap<>());
    private static long lastCacheUpdate = 0;
    private static final long CACHE_UPDATE_INTERVAL = 5000; // 5초

    // HashSet으로 변경하여 contains() 성능 향상
    private static final Set<String> CLONE_NAMES_SET = QuestHelper.getCloneNamesSet();


    public static boolean isProcessing(int npcId) {
        return processingNpcIds.contains(npcId);
    }

    public static void addProcessing(int npcId) {
        processingNpcIds.add(npcId);
    }

    public static void removeProcessing(int npcId) {
        processingNpcIds.remove(npcId);
    }

    public static Set<Integer> getActiveQuestNpcIds() { return Collections.unmodifiableSet(activeQuestNpcIds); }

    public static void addActiveQuestNpc(int npcId, WorldServer world) {
        activeQuestNpcIds.add(npcId);
        if (world != null) {
            npcWorldCache.put(npcId, world);
        }
    }

    public static void removeActiveQuestNpc(int npcId) {
        activeQuestNpcIds.remove(npcId);
        npcWorldCache.remove(npcId);
    }

    public static EntityCustomNpc findNpcById(MinecraftServer server, int entityId) {
        WorldServer cachedWorld = npcWorldCache.get(entityId);
        if (cachedWorld != null) {
            EntityCustomNpc npc = (EntityCustomNpc) cachedWorld.getEntityByID(entityId);
            if (npc != null && !npc.isDead && CLONE_NAMES_SET.contains(npc.getName())) return npc;
            else { npcWorldCache.remove(entityId); }
        }
        
        for (WorldServer world : server.worlds) {
            EntityCustomNpc npc = (EntityCustomNpc) world.getEntityByID(entityId);
            if (npc != null && CLONE_NAMES_SET.contains(npc.getName())) {
                npcWorldCache.put(entityId, world);
                return npc;
            }
        } return null;
    }

    public static List<EntityPlayerMP> getTeamPlayers(MinecraftServer server, String teamName) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastCacheUpdate > CACHE_UPDATE_INTERVAL || !teamPlayerCache.containsKey(teamName)) {
            updateTeamPlayerCache(server);
            lastCacheUpdate = currentTime;
        }

        return teamPlayerCache.getOrDefault(teamName, Collections.emptyList());
    }

    private static void updateTeamPlayerCache(MinecraftServer server) {
        synchronized (teamPlayerCache) {
            teamPlayerCache.clear();
            
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                String teamName = Helper.getPlayerTeamName(player);
                if (teamName != null && !teamName.isEmpty()) {
                    teamPlayerCache.computeIfAbsent(teamName, k -> new ArrayList<>()).add(player);
                }
            }
        }
    }

    public static void onWorldUnload(WorldServer world) {
        synchronized (npcWorldCache) { npcWorldCache.entrySet().removeIf(entry -> entry.getValue() == world); }
        synchronized (activeQuestNpcIds) {
            List<Integer> toRemove = new ArrayList<>();
            for (Integer npcId : activeQuestNpcIds) {
                WorldServer cachedWorld = npcWorldCache.get(npcId);
                if (cachedWorld == world) toRemove.add(npcId);
            } toRemove.forEach(activeQuestNpcIds::remove);
        }
    }
}

