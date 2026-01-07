package com.bandiyesol.yeontan.event;

import com.bandiyesol.yeontan.entity.Quest;
import com.bandiyesol.yeontan.entity.QuestManager;
import com.bandiyesol.yeontan.network.QuestMessage;
import com.bandiyesol.yeontan.network.QuestPacketHandler;
import com.bandiyesol.yeontan.network.QuestTimerMessage;
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

    private static final Set<String> CLONE_NAMES = QuestHelper.getCloneNamesSet();

    private int tickCounter = 0;


    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (++tickCounter >= 20) {
            tickCounter = 0;
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server == null) return;
            
            Set<Integer> activeQuestNpcIds = QuestStateManager.getActiveQuestNpcIds();
            if (activeQuestNpcIds.isEmpty()) return;

            long currentTime = System.currentTimeMillis();
            List<Integer> expiredNpcIds = new ArrayList<>();
            Set<Integer> npcIdsCopy = new HashSet<>(activeQuestNpcIds);
            
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
                        } else {
                            // 만료되지 않은 퀘스트의 남은 시간을 모든 플레이어에게 전송
                            QuestTimerMessage timerMessage = new QuestTimerMessage(npcId, expireTime);
                            QuestPacketHandler.getInstance().sendToAll(timerMessage);
                        }
                    }
                } else { expiredNpcIds.add(npcId); }
            } for (Integer npcId : expiredNpcIds) { QuestStateManager.removeActiveQuestNpc(npcId); }
        }
    }


    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote || !(event.getTarget() instanceof EntityCustomNpc)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        EntityCustomNpc target = (EntityCustomNpc) event.getTarget();
        String teamName = Helper.getPlayerTeamName(player);

        if (!CLONE_NAMES.contains(target.getName())) return;
        if (teamName == null || teamName.isEmpty()) {
            player.sendMessage(new TextComponentString("§c[BT2020] §f퀘스트를 받으려면 팀에 가입해야 합니다."));
            return;
        }

        NBTTagCompound extraData = target.getEntityData();
        if (!extraData.hasKey("YeontanQuest")) { QuestService.handleQuestAssignment(teamName, target); }
        else {
            NBTTagCompound questData = extraData.getCompoundTag("YeontanQuest");
            if (!questData.hasKey("OwnerTeam")) {
                player.sendMessage(new TextComponentString("§c[BT2020] §f퀘스트 데이터가 손상되었습니다."));
                return;
            }

            String ownerTeam = questData.getString("OwnerTeam");
            if (ownerTeam.isEmpty()) {
                player.sendMessage(new TextComponentString("§c[BT2020] §f퀘스트 소유 팀 정보가 없습니다."));
                return;
            }
            
            if (ownerTeam.equals(teamName)) {
                try { QuestService.handleQuestCompletion(player, target, teamName, questData); }
                catch (CommandException e) { /* CommandException은 QuestService에서 처리됨 */ }
            } else { player.sendMessage(new TextComponentString("§c[BT2020] §f타 팀이 수행 중입니다.")); }
        }
    }


    @SubscribeEvent
    public void onPlayerJoin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        QuestPacketHandler.getInstance().sendTo(
                new QuestMessage(
                        -1,
                        "",
                        "",
                        false),
                player
        );

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
                        long expireTime = qData.hasKey("ExpireTime") ? qData.getLong("ExpireTime") : 0;
                        QuestPacketHandler.getInstance().sendTo(
                                new QuestMessage(
                                        npc.getEntityId(),
                                        quest.getItemName(),
                                        quest.getQuestTitle(),
                                        true),
                                player
                        );
                        if (expireTime > 0) {
                            QuestTimerMessage timerMessage = new QuestTimerMessage(npc.getEntityId(), expireTime);
                            QuestPacketHandler.getInstance().sendTo(timerMessage, player);
                        }
                    }
                }
            }
        }
    }


    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() instanceof WorldServer) {
            WorldServer world = (WorldServer) event.getWorld();
            QuestStateManager.onWorldUnload(world);
        }
    }
}
