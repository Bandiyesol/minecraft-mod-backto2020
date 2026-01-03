package com.bandiyesol.yeontan.event;

import com.bandiyesol.yeontan.network.QuestMessage;
import com.bandiyesol.yeontan.network.QuestPacketHandler;
import com.bandiyesol.yeontan.entity.Quest;
import com.bandiyesol.yeontan.entity.QuestManager;
import com.bandiyesol.yeontan.util.Helper;
import com.bandiyesol.yeontan.util.QuestHelper;
import com.example.iadd.comm.ComMoney;
import net.minecraft.command.CommandException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.*;

public class QuestEventHandler {

    private static final List<String> CLONE_NAMES = Arrays.asList(QuestHelper.CLONE_POOL);
    private static final long LIMIT_TIME = 2 * 60 * 1000;

    private int tickCounter = 0;


    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (++tickCounter >= 20) {
            tickCounter = 0;
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server == null) return;

            for (WorldServer world : server.worlds) {
                for (EntityCustomNpc npc : world.getEntities(EntityCustomNpc.class, n -> CLONE_NAMES.contains(n.getName()))) {
                    NBTTagCompound nbt = npc.writeToNBT(new NBTTagCompound());
                    if (nbt.hasKey("YeontanQuest")) {
                        long expireTime = nbt.getCompoundTag("YeontanQuest").getLong("ExpireTime");
                        if (System.currentTimeMillis() > expireTime) {
                            handleQuestExpiration(npc);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;

        // 접속한 플레이어에게 현재 월드에 있는 모든 퀘스트 NPC의 정보를 다시 전송
        for (EntityCustomNpc npc : player.getServerWorld().getEntities(EntityCustomNpc.class, n -> true)) {
            NBTTagCompound extraData = npc.getEntityData();
            if (extraData.hasKey("YeontanQuest")) {
                NBTTagCompound qData = extraData.getCompoundTag("YeontanQuest");
                Quest quest = QuestManager.getQuestById(qData.getInteger("QuestID"));

                if (quest != null) {
                    // 해당 플레이어에게만 패킷 전송
                    QuestPacketHandler.INSTANCE.sendTo(
                            new QuestMessage(npc.getEntityId(), quest.getItemName(), quest.getQuestTitle(), true),
                            player
                    );
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
        if (teamName == null) return;

        NBTTagCompound extraData = target.getEntityData();
        System.out.println("NPC NBT Has Key: " + extraData.hasKey("YeontanQuest"));

        if (!extraData.hasKey("YeontanQuest")) {
            handleQuestAssignment(teamName, target);
        }

        else {
            NBTTagCompound questData = extraData.getCompoundTag("YeontanQuest");

            if (questData.getString("OwnerTeam").equals(teamName)) {
                handleQuestCompletion(player, target, teamName, questData);
            }

            else {
                player.sendMessage(new TextComponentString("§c[BT2020] §f타 팀이 수행 중입니다."));
            }
        }
    }

    // --- [퀘스트 수락 로직] ---
    private void handleQuestAssignment(String teamName, EntityCustomNpc target) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        Quest quest = QuestManager.getRandomQuest();
        if (quest == null) return;

        NBTTagCompound extraData = target.getEntityData();
        NBTTagCompound questData = new NBTTagCompound();

        questData.setInteger("QuestID", quest.getId());
        questData.setString("OwnerTeam", teamName);
        questData.setLong("ExpireTime", System.currentTimeMillis() + LIMIT_TIME);

        extraData.setTag("YeontanQuest", questData);

        QuestPacketHandler.INSTANCE.sendToAll(
                new QuestMessage(
                        target.getEntityId(),
                        quest.getItemName(),
                        quest.getQuestTitle(),
                        true
                )
        );

        Helper.sendToTeam(server, teamName, "§a[수락] §f" + quest.getQuestTitle());
    }

    // --- [퀘스트 완료 로직] ---
    private void handleQuestCompletion(EntityPlayerMP player, EntityCustomNpc target, String teamName, NBTTagCompound questData) throws CommandException {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        Quest quest = QuestManager.getQuestById(questData.getInteger("QuestID"));
        ItemStack heldItem = player.getHeldItemMainhand();

        if (quest != null && !heldItem.isEmpty() && Objects.requireNonNull(heldItem.getItem().getRegistryName()).toString().equals(quest.getItemName())) {
            heldItem.shrink(1);

            ComMoney.giveCoin(player, quest.getRewardAmount());
            Helper.sendToTeam(server, teamName, "§a[완료] §f" + player.getName() + "님이 해결!");

            QuestPacketHandler.INSTANCE.sendToAll(
                    new QuestMessage(target.getEntityId(),
                            "",
                            "", false
                    )
            );

            QuestHelper.spawnQuestNpc(target);
            target.isDead = true;
            target.world.removeEntity(target);
        }

        else {
            player.sendMessage(new TextComponentString("§c[BT2020] §f아이템이 부족합니다."));
        }
    }

    // --- [만료된 퀘스트 엔티티 교체 로직] ---
    private void handleQuestExpiration(EntityCustomNpc target) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        QuestHelper.spawnQuestNpc(target);
        target.setDead();
        QuestPacketHandler.INSTANCE.sendToAll(
                new QuestMessage(target.getEntityId(),
                        "",
                        "",
                        false
                )
        );
    }
}
