package com.bandiyesol.yeontan.event;

import com.bandiyesol.yeontan.entity.Quest;
import com.bandiyesol.yeontan.entity.QuestManager;
import com.bandiyesol.yeontan.network.QuestMessage;
import com.bandiyesol.yeontan.network.QuestPacketHandler;
import com.bandiyesol.yeontan.service.QuestService;
import com.bandiyesol.yeontan.service.QuestStateManager;
import com.bandiyesol.yeontan.util.Helper;
import com.bandiyesol.yeontan.util.QuestHelper;
import net.minecraft.command.CommandException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.*;

public class QuestEventHandler {

    // HashSet으로 변경하여 contains() 성능 향상 (O(n) -> O(1))
    private static final Set<String> CLONE_NAMES = QuestHelper.getCloneNamesSet();

    private int tickCounter = 0;


    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (++tickCounter >= 20) {
            tickCounter = 0;
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server == null) return;
            
            // 활성 퀘스트 NPC가 없으면 조기 종료
            Set<Integer> activeQuestNpcIds = QuestStateManager.getActiveQuestNpcIds();
            if (activeQuestNpcIds.isEmpty()) return;

            long currentTime = System.currentTimeMillis();
            List<Integer> expiredNpcIds = new ArrayList<>();
            
            // ConcurrentModificationException 방지: Set을 복사해서 순회
            Set<Integer> npcIdsCopy = new HashSet<>(activeQuestNpcIds);
            
            // 추적 중인 NPC만 검사 (모든 NPC 검사 대신)
            for (Integer npcId : npcIdsCopy) {
                EntityCustomNpc npc = QuestStateManager.findNpcById(server, npcId);
                if (npc == null || npc.isDead) {
                    expiredNpcIds.add(npcId);
                    continue;
                }
                
                NBTTagCompound extraData = npc.getEntityData();
                if (extraData.hasKey("YeontanQuest")) {
                    NBTTagCompound questData = extraData.getCompoundTag("YeontanQuest");
                    if (questData.hasKey("ExpireTime")) {
                        long expireTime = questData.getLong("ExpireTime");
                        if (currentTime > expireTime) {
                            QuestService.handleQuestExpiration(npc);
                            expiredNpcIds.add(npcId);
                        }
                    }
                } else {
                    // 퀘스트 데이터가 없으면 추적 목록에서 제거
                    expiredNpcIds.add(npcId);
                }
            }
            
            // 만료되거나 제거된 NPC를 추적 목록에서 제거
            for (Integer npcId : expiredNpcIds) {
                QuestStateManager.removeActiveQuestNpc(npcId);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;

        // 클라이언트 초기화: 모든 퀘스트 정보 제거
        QuestPacketHandler.getInstance().sendTo(
                new QuestMessage(-1, "", "", false),
                player
        );

        // 최적화: 활성 퀘스트 NPC만 검색 (모든 NPC 검색 대신)
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        Set<Integer> activeQuestNpcIds = QuestStateManager.getActiveQuestNpcIds();
        if (activeQuestNpcIds.isEmpty()) return;

        for (Integer npcId : activeQuestNpcIds) {
            EntityCustomNpc npc = QuestStateManager.findNpcById(server, npcId);
            if (npc != null && !npc.isDead) {
                NBTTagCompound extraData = npc.getEntityData();
                if (extraData.hasKey("YeontanQuest")) {
                    NBTTagCompound qData = extraData.getCompoundTag("YeontanQuest");
                    Quest quest = QuestManager.getQuestById(qData.getInteger("QuestID"));

                    if (quest != null) {
                        QuestPacketHandler.getInstance().sendTo(
                                new QuestMessage(npc.getEntityId(), quest.getItemName(), quest.getQuestTitle(), true),
                                player
                        );
                    }
                }
            }
        }
    }


    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) throws CommandException {
        if (event.getWorld().isRemote || !(event.getTarget() instanceof EntityCustomNpc)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        EntityCustomNpc target = (EntityCustomNpc) event.getTarget();
        String teamName = Helper.getPlayerTeamName(player);

        if (!CLONE_NAMES.contains(target.getName())) return;
        
        // 팀 가입 체크
        if (teamName == null || teamName.isEmpty()) {
            player.sendMessage(new TextComponentString("§c[BT2020] §f퀘스트를 받으려면 팀에 가입해야 합니다."));
            return;
        }

        NBTTagCompound extraData = target.getEntityData();

        if (!extraData.hasKey("YeontanQuest")) {
            QuestService.handleQuestAssignment(teamName, target);
        } else {
            NBTTagCompound questData = extraData.getCompoundTag("YeontanQuest");
            
            // 퀘스트 데이터 유효성 검사
            if (questData == null || !questData.hasKey("OwnerTeam")) {
                player.sendMessage(new TextComponentString("§c[BT2020] §f퀘스트 데이터가 손상되었습니다."));
                return;
            }

            String ownerTeam = questData.getString("OwnerTeam");
            
            // 퀘스트 완료 시에도 팀 재확인
            if (ownerTeam == null || ownerTeam.isEmpty()) {
                player.sendMessage(new TextComponentString("§c[BT2020] §f퀘스트 소유 팀 정보가 없습니다."));
                return;
            }
            
            if (ownerTeam.equals(teamName)) {
                try {
                    QuestService.handleQuestCompletion(player, target, teamName, questData);
                } catch (CommandException e) {
                    // CommandException은 QuestService에서 처리됨
                }
            } else {
                player.sendMessage(new TextComponentString("§c[BT2020] §f타 팀이 수행 중입니다."));
            }
        }
    }
    
    /**
     * 월드 언로드 시 캐시 정리
     */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() instanceof WorldServer) {
            WorldServer world = (WorldServer) event.getWorld();
            QuestStateManager.onWorldUnload(world);
        }
    }
}
