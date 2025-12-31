package com.bandiyesol.yeontan.event;

import com.bandiyesol.yeontan.network.QuestMessage;
import com.bandiyesol.yeontan.network.QuestPacketHandler;
import com.bandiyesol.yeontan.quest.Quest;
import com.bandiyesol.yeontan.quest.QuestEntityManager;
import com.bandiyesol.yeontan.quest.QuestManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.*;

public class QuestEventHandler {

    private int tickCounter = 0;

    private static final Map<UUID, Integer> playerTargetEntity = new HashMap<>();
    private static final Map<UUID, Quest> playerActiveQuest = new HashMap<>();
    private static final Map<UUID, Long> playerQuestTimeout = new HashMap<>();

    private static final Set<Integer> busyEntities = new HashSet<>();



    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;

        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            processTimeouts(event.world.getMinecraftServer());
        }
    }


    // --- 수락/완료 로직 ---
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote || !(event.getEntityPlayer() instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        Entity target = event.getTarget();
        UUID uuid = player.getUniqueID();

        if (!(target instanceof EntityCustomNpc) || !QuestEntityManager.isAuthorized(target.getUniqueID())) return;

        if (playerTargetEntity.getOrDefault(uuid, -1) == target.getEntityId()) {
            handleQuestCompletion(player, target);
            return;
        }

        handleQuestAssignment(player, target);
    }


    private void processTimeouts(net.minecraft.server.MinecraftServer server) {
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Long> entry : playerQuestTimeout.entrySet()) {
            if (now > entry.getValue()) {
                UUID uuid = entry.getKey();
                int entityId = playerTargetEntity.getOrDefault(uuid, -1);
                EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(uuid);

                player.sendMessage(new TextComponentString("§6[퀘스트] 제한 시간이 초과되었습니다."));
                clearQuest(player, entityId);
            }
        }
    }


    // --- [완료 로직] ---
    private void handleQuestCompletion(EntityPlayerMP player, Entity target) {
        Quest quest = playerActiveQuest.get(player.getUniqueID());
        if (quest == null) return;

        ItemStack heldItem = player.getHeldItemMainhand();

        if (heldItem.isEmpty()) {
            player.sendMessage(new TextComponentString("§c아이템을 들고 우클릭해주세요!"));
            return;
        }

        String heldItemName = Objects.requireNonNull(heldItem.getItem().getRegistryName()).toString();

        if (heldItemName.equals(quest.getItemName())) {
            heldItem.shrink(1);
            giveReward(player, quest);
            player.sendMessage(new TextComponentString("§a[퀘스트] '" + quest.getQuestTitle() + "' 완료!"));
            clearQuest(player, target.getEntityId());
        }

        else {
            player.sendMessage(new TextComponentString("§c잘못된 아이템입니다! §7(요구: " + quest.getItemName() + ")"));
        }
    }


    // --- [수락 로직] ---
    private void handleQuestAssignment(EntityPlayerMP player, Entity target) {
        UUID uuid = player.getUniqueID();
        long currentTime = System.currentTimeMillis();

        if (playerActiveQuest.containsKey(uuid)) {
            player.sendMessage(new TextComponentString("§e이미 진행 중인 퀘스트가 있습니다."));
            return;
        }

        if (busyEntities.contains(target.getEntityId())) {
            player.sendMessage(new TextComponentString("§c이 NPC는 현재 바쁩니다."));
            return;
        }

        Quest selectedQuest = QuestManager.getRandomQuest();

        if (selectedQuest != null) {
            playerActiveQuest.put(uuid, selectedQuest);
            playerTargetEntity.put(uuid, target.getEntityId());
            playerQuestTimeout.put(uuid, currentTime + (5 * 60 * 1000));
            busyEntities.add(target.getEntityId());

            QuestPacketHandler.INSTANCE.sendTo(
                    new QuestMessage(
                        target.getEntityId(),
                        selectedQuest.getItemName(),
                        selectedQuest.getQuestTitle(),
                        true
                    ),
                    player
            );

            player.sendMessage(new TextComponentString("§b[퀘스트] " + selectedQuest.getQuestTitle() + " 미션을 시작합니다!"));
        }
    }


    private void giveReward(EntityPlayerMP player, Quest quest) {
        Item rewardItem = Item.getByNameOrId(quest.getRewardItem());
        if (rewardItem != null) {
            ItemStack rewardStack = new ItemStack(rewardItem, quest.getRewardAmount());

            if (player.inventory.addItemStackToInventory(rewardStack)) {
                player.dropItem(rewardStack, false);
            }
        }

    }


    private void clearQuest(EntityPlayerMP player, int entityId) {
        UUID playerUuid = player.getUniqueID();
        playerActiveQuest.remove(playerUuid);
        playerTargetEntity.remove(playerUuid);
        playerQuestTimeout.remove(playerUuid);

        if (entityId != -1) {
            busyEntities.remove(entityId);
            QuestPacketHandler.INSTANCE.sendTo(new QuestMessage(entityId, "", "", false),  player);
        }
    }
}
