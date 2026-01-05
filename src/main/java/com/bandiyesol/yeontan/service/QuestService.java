package com.bandiyesol.yeontan.service;

import com.bandiyesol.yeontan.entity.Quest;
import com.bandiyesol.yeontan.entity.QuestManager;
import com.bandiyesol.yeontan.network.QuestMessage;
import com.bandiyesol.yeontan.network.QuestPacketHandler;
import com.bandiyesol.yeontan.util.Helper;
import com.bandiyesol.yeontan.util.QuestHelper;
import com.example.iadd.comm.ComMoney;
import com.example.iadd.nick.NickTable;
import net.minecraft.command.CommandException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * 퀘스트 처리 서비스 클래스
 * 퀘스트 수락, 완료, 만료 등의 비즈니스 로직을 처리합니다.
 */
public class QuestService {

    // 퀘스트 제한 시간 (30초)
    private static final long LIMIT_TIME = 30 * 1000;

    /**
     * 퀘스트 수락 처리
     */
    public static void handleQuestAssignment(String teamName, EntityCustomNpc target) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        Quest quest = QuestManager.getRandomQuest();
        if (quest == null) return;

        NBTTagCompound extraData = target.getEntityData();
        NBTTagCompound questData = new NBTTagCompound();

        questData.setInteger("QuestID", quest.getId());
        questData.setString("OwnerTeam", Objects.requireNonNull(teamName));
        questData.setLong("ExpireTime", System.currentTimeMillis() + LIMIT_TIME);

        extraData.setTag("YeontanQuest", questData);
        
        // 퀘스트가 있는 NPC를 추적 목록에 추가
        int npcId = target.getEntityId();
        WorldServer world = target.world instanceof WorldServer ? (WorldServer) target.world : null;
        QuestStateManager.addActiveQuestNpc(npcId, world);

        QuestPacketHandler.getInstance().sendToAll(
                new QuestMessage(
                        npcId,
                        quest.getItemName(),
                        quest.getQuestTitle(),
                        true
                )
        );

        Helper.sendToTeam(server, teamName, "§a[수락] §f" + quest.getQuestTitle());
    }
    
    /**
     * 퀘스트 완료 처리
     */
    public static void handleQuestCompletion(EntityPlayerMP player, EntityCustomNpc target, String teamName, NBTTagCompound questData) throws CommandException {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        // 퀘스트 데이터 유효성 검사
        if (!questData.hasKey("QuestID")) {
            player.sendMessage(new TextComponentString("§c[BT2020] §f퀘스트 ID가 없습니다."));
            return;
        }
        
        Quest quest = QuestManager.getQuestById(questData.getInteger("QuestID"));
        if (quest == null) {
            player.sendMessage(new TextComponentString("§c[BT2020] §f존재하지 않는 퀘스트입니다."));
            return;
        }
        
        ItemStack heldItem = player.getHeldItemMainhand();

        if (!heldItem.isEmpty() && Objects.requireNonNull(heldItem.getItem().getRegistryName()).toString().equals(quest.getItemName())) {
            heldItem.shrink(1);

            ComMoney.giveCoin(player, quest.getRewardAmount());
            Helper.sendToTeam(server, teamName, "§a[완료] §f" + NickTable.getColorByName(player.getName()) + "님이 해결!");

            QuestPacketHandler.getInstance().sendToAll(
                    new QuestMessage(target.getEntityId(),
                            "",
                            "", false
                    )
            );

            // 추적 목록에서 제거
            QuestStateManager.removeActiveQuestNpc(target.getEntityId());
            
            QuestHelper.spawnQuestNpc(target);
            target.isDead = true;
            target.world.removeEntity(target);
        } else {
            player.sendMessage(new TextComponentString("§c[BT2020] §f퀘스트 아이템이 아닙니다."));
        }
    }
    
    /**
     * 퀘스트 만료 처리
     */
    public static void handleQuestExpiration(EntityCustomNpc target) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        
        int npcId = target.getEntityId();
        
        // 중복 처리 방지
        if (QuestStateManager.isProcessing(npcId)) {
            return;
        }
        
        QuestStateManager.addProcessing(npcId);
        
        try {
            // 퀘스트 정보 가져오기
            NBTTagCompound extraData = target.getEntityData();
            if (!extraData.hasKey("YeontanQuest")) {
                return;
            }
            
            NBTTagCompound questData = extraData.getCompoundTag("YeontanQuest");
            int questId = questData.getInteger("QuestID");
            String ownerTeam = questData.getString("OwnerTeam");
            
            Quest quest = QuestManager.getQuestById(questId);
            if (quest != null && !ownerTeam.isEmpty()) {
                // 팀 플레이어 캐시 사용 (모든 플레이어 검사 대신)
                List<EntityPlayerMP> teamPlayers = QuestStateManager.getTeamPlayers(server, ownerTeam);
                
                if (!teamPlayers.isEmpty()) {
                    // 팀원 중 랜덤으로 한 명 선택
                    Random random = new Random();
                    EntityPlayerMP selectedPlayer = teamPlayers.get(random.nextInt(teamPlayers.size()));
                    int penaltyAmount = -quest.getRewardAmount();

                    try {
                        ComMoney.giveCoin(selectedPlayer, penaltyAmount);
                        // 선택된 플레이어에게 개인 메시지
                        selectedPlayer.sendMessage(new TextComponentString("§c[실패] §f" + quest.getQuestTitle() + " 퀘스트가 만료되어 " + quest.getRewardAmount() + "코인이 차감되었습니다."));
                        // 팀에게 실패 메시지 전송 (누가 차감되었는지 포함)
                        Helper.sendToTeam(server, ownerTeam, "§c[실패] §f" + quest.getQuestTitle() + " 퀘스트가 만료되어 " + NickTable.getColorByName(selectedPlayer.getName()) + "님에게 " + quest.getRewardAmount() + "코인이 차감되었습니다.");
                    } catch (Exception e) {
                        System.err.println("[Yeontan] Failed to deduct coins from player " + NickTable.getColorByName(selectedPlayer.getName()) + ": " + e.getMessage());
                        // 에러 발생 시에도 팀에게 알림
                        Helper.sendToTeam(server, ownerTeam, "§c[실패] §f" + quest.getQuestTitle() + " 퀘스트가 만료되었습니다.");
                    }
                } else {
                    // 팀원이 없으면 팀에게만 메시지
                    Helper.sendToTeam(server, ownerTeam, "§c[실패] §f" + quest.getQuestTitle() + " 퀘스트가 만료되었습니다.");
                }
            }

            // 추적 목록에서 제거
            QuestStateManager.removeActiveQuestNpc(npcId);

            QuestHelper.spawnQuestNpc(target);
            target.isDead = true;
            target.world.removeEntity(target);
            QuestPacketHandler.getInstance().sendToAll(
                    new QuestMessage(target.getEntityId(),
                            "",
                            "",
                            false
                    )
            );
        } finally {
            // 처리 완료 후 플래그 제거 (예외 발생 시에도 보장)
            QuestStateManager.removeProcessing(npcId);
        }
    }
}

