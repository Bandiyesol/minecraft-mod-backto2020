package com.bandiyesol.yeontan.event;

import com.bandiyesol.yeontan.entity.QuestEntity;
import com.bandiyesol.yeontan.network.QuestMessage;
import com.bandiyesol.yeontan.network.QuestPacketHandler;
import com.bandiyesol.yeontan.entity.Quest;
import com.bandiyesol.yeontan.entity.QuestManager;
import com.bandiyesol.yeontan.util.Helper;
import com.bandiyesol.yeontan.util.QuestHelper;
import net.minecraft.command.CommandException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import com.example.iadd.comm.ComMoney;

import java.util.*;

public class QuestEventHandler {

    private static final Map<String, List<QuestEntity>> teamQuests = new HashMap<>();
    private static final long LIMIT_TIME = 2 * 60 * 1000;

    private int tickCounter;


    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;

            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server == null) return;

            for (WorldServer world : server.worlds) {
                List<QuestEntity> questEntities = world.getEntities(QuestEntity.class, entity -> true);

                for (QuestEntity npc : questEntities) {
                    if (!npc.isTemplate() && npc.getCurrentQuest() != null && npc.isExpired()) {
                        handleQuestExpiration(npc);
                    }
                }
            }
        }
    }


    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) throws CommandException {
        if (event.getWorld().isRemote || !(event.getTarget() instanceof QuestEntity)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        QuestEntity target = (QuestEntity) event.getTarget();
        String teamName = Helper.getPlayerTeamName(player);

        if (target.isTemplate()) {
            player.sendMessage(new TextComponentString("§c[BT2020] §fQuestNPC 원본입니다."));
            return;
        }

        if (teamName == null) {
            player.sendMessage(new TextComponentString("§c[BT2020] §f팀에 소속되어 있어야 합니다."));
            return;
        }

        Quest quest = target.getCurrentQuest();
        if (quest == null) {
            handleQuestAssignment(player, target, teamName);
        }

        else {
            if (target.getOwnerTeam().equals(teamName)) {
                handleQuestCompletion(player, target, teamName, quest);            }

            else {
                player.sendMessage(new TextComponentString("§c[BT2020] §f다른 팀이 이미 수행 중인 NPC입니다."));
            }
        }
    }

    // --- [퀘스트 수락 로직] ---
    private void handleQuestAssignment(EntityPlayerMP player, QuestEntity target, String teamName) {
        Quest quest = QuestManager.getRandomQuest();
        if (quest != null) {
            target.assignQuest(quest, teamName, LIMIT_TIME);
            teamQuests.computeIfAbsent(teamName, key -> new ArrayList<>()).add(target);

            QuestPacketHandler.INSTANCE.sendTo(
                    new QuestMessage(
                            target.getEntityId(),
                            quest.getItemName(),
                            quest.getQuestTitle(),
                            true
                    ),
                    player
            );
        }
    }

    // --- [퀘스트 완료 로직] ---
    private void handleQuestCompletion(EntityPlayerMP player, QuestEntity target, String teamName, Quest quest) throws CommandException {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        ItemStack heldItem = player.getHeldItemMainhand();

        if (!heldItem.isEmpty() && Objects.requireNonNull(heldItem.getItem().getRegistryName()).toString().equals(quest.getItemName())) {
            heldItem.shrink(1);
            ComMoney.giveCoin(player, quest.getRewardAmount());
            String message = String.format("§a[퀘스트 완료] %s님이 %s를 완료했습니다!", player.getName(), quest.getQuestTitle());
            Helper.sendToTeam(Objects.requireNonNull(player.world.getMinecraftServer()), teamName, message);
            QuestPacketHandler.INSTANCE.sendToAll(
                    new QuestMessage(
                            target.getEntityId(),
                            "",
                            "",
                            false
                    )
            );

            QuestHelper.spawnQuestNpc(target);
            target.isDead = true;
            target.world.removeEntity(target);
            teamQuests.get(teamName).remove(target);
        }
    }

    // --- [만료된 퀘스트 엔티티 교체 로직] ---
    private void handleQuestExpiration(QuestEntity target) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        String ownerTeam = target.getOwnerTeam();
        if (!ownerTeam.isEmpty() && teamQuests.containsKey(ownerTeam)) {
            teamQuests.get(ownerTeam).remove(target);
            Helper.sendToTeam(server, ownerTeam, "§c[퀘스트 실패] 제한 시간이 초과되어 NPC가 교체되었습니다.");
        }

        QuestPacketHandler.INSTANCE.sendToAll(
                new QuestMessage(
                        target.getEntityId(),
                        "",
                        "",
                        false
                )
        );

        QuestHelper.spawnQuestNpc(target);
        target.isDead = true;
        target.world.removeEntity(target);
    }
}
