package com.bandiyesol.yeontan.service;

import com.bandiyesol.yeontan.entity.Quest;
import com.bandiyesol.yeontan.entity.QuestManager;
import com.bandiyesol.yeontan.network.QuestMessage;
import com.bandiyesol.yeontan.network.QuestPacketHandler;
import com.bandiyesol.yeontan.network.QuestTimerMessage;
import com.bandiyesol.yeontan.util.Helper;
import com.bandiyesol.yeontan.util.QuestHelper;
import com.bandiyesol.yeontan.util.QuestNpcLocationManager;
import com.example.iadd.comm.ComMoney;
import com.example.iadd.nick.NickTable;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.List;
import java.util.Objects;


public class QuestService {

    private static final long LIMIT_TIME = 60 * 1000;


    // --- [퀘스트 레벨 설정] ---
    public static void handleLevel(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString("§c사용법: /quest level <1/2>"));
            return;
        }

        try {
            int level = Integer.parseInt(args[1]);
            if (level < 1 || level > 2) {
                sender.sendMessage(new TextComponentString("§c레벨은 1 또는 2만 가능합니다."));
                return;
            }

            QuestManager.currentServerLevel = level;
            sender.sendMessage(new TextComponentString("§a퀘스트 레벨이 " + level + "단계로 설정되었습니다."));
        } catch (NumberFormatException e) { sender.sendMessage(new TextComponentString("§c잘못된 숫자 형식입니다.")); }
    }


    // --- [퀘스트 게임 시작/종료] ---
    public static void handlePlay(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString("§c사용법: /quest play <start|stop>"));
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("start")) {
            List<QuestNpcLocationManager.SpawnLocation> locations = QuestNpcLocationManager.getLocations();
            if (locations.isEmpty()) {
                sender.sendMessage(new TextComponentString("§c스폰 위치가 설정되지 않았습니다."));
                sender.sendMessage(new TextComponentString("§e/quest location add 명령어로 현재 위치를 추가하세요."));
                return;
            }

            sender.sendMessage(new TextComponentString("§a게임 시작! " + locations.size() + "개 위치에 NPC를 스폰합니다..."));
            for (QuestNpcLocationManager.SpawnLocation location : locations) { QuestHelper.spawnQuestNpcAtLocation(location, sender); }
            sender.sendMessage(new TextComponentString("§a모든 NPC가 스폰되었습니다."));

        }

        else if (action.equals("stop")) {
            sender.sendMessage(new TextComponentString("§a게임 종료! 모든 NPC를 제거합니다..."));
            QuestHelper.removeAllActiveNpcs();
            sender.sendMessage(new TextComponentString("§a모든 NPC가 제거되었습니다."));

        }

        else { sender.sendMessage(new TextComponentString("§c사용법: /quest play <start|stop>")); }
    }


    // --- [퀘스트 NPC 스폰 위치 관리] ---
    public static void handleLocation(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString("§c사용법: /quest location <add|remove|list|clear>"));
            return;
        }

        String action = args[1].toLowerCase();
        World world = sender.getEntityWorld();
        BlockPos pos = sender.getPosition();

        switch (action) {
            case "add":
                QuestNpcLocationManager.addLocation(world, pos);
                String addMessage = Objects.requireNonNull(String.format(
                        "§a위치 추가됨: Dimension=%d, X=%.1f, Y=%.1f, Z=%.1f",
                        world.provider.getDimension(), (double)pos.getX(), (double)pos.getY(), (double)pos.getZ()
                ));
                sender.sendMessage(new TextComponentString(addMessage));
                break;

            case "remove":
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentString("§c사용법: /quest location remove <인덱스>"));
                    return;
                }

                try {
                    int index = Integer.parseInt(args[2]);
                    if (QuestNpcLocationManager.removeLocation(index)) sender.sendMessage(new TextComponentString("§a위치 " + index + "번이 제거되었습니다."));
                    else { sender.sendMessage(new TextComponentString("§c잘못된 인덱스입니다.")); }
                } catch (NumberFormatException e) { sender.sendMessage(new TextComponentString("§c잘못된 숫자 형식입니다.")); }
                break;

            case "list":
                List<QuestNpcLocationManager.SpawnLocation> locations = QuestNpcLocationManager.getLocations();
                if (locations.isEmpty()) sender.sendMessage(new TextComponentString("§e등록된 위치가 없습니다."));
                else {
                    sender.sendMessage(new TextComponentString("§a등록된 위치 목록 (" + locations.size() + "개):"));
                    for (int i = 0; i < locations.size(); i++) {
                        QuestNpcLocationManager.SpawnLocation loc = locations.get(i);
                        String listMessage = Objects.requireNonNull(String.format(
                                "  §7[%d] Dimension=%d, X=%.1f, Y=%.1f, Z=%.1f",
                                i, loc.dimension, loc.x, loc.y, loc.z
                        ));
                        sender.sendMessage(new TextComponentString(listMessage));
                    }
                }
                break;

            case "clear":
                QuestNpcLocationManager.clearLocations();
                sender.sendMessage(new TextComponentString("§a모든 위치가 제거되었습니다."));
                break;

            default:
                sender.sendMessage(new TextComponentString("§c사용법: /quest location <add|remove|list|clear>"));
        }
    }


    // --- [퀘스트 수락 처리] ---
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
        
        int npcId = target.getEntityId();
        WorldServer world = target.world instanceof WorldServer ? (WorldServer) target.world : null;
        QuestStateManager.addActiveQuestNpc(npcId, world);

        long expireTime = questData.getLong("ExpireTime");
        
        QuestPacketHandler.getInstance().sendToAll(
                new QuestMessage(
                        npcId,
                        quest.getItemName(),
                        quest.getQuestTitle(),
                        true
                )
        );
        
        // 만료 시간 전송
        QuestTimerMessage timerMessage = new QuestTimerMessage(npcId, expireTime);
        QuestPacketHandler.getInstance().sendToAll(timerMessage);

        Helper.sendToTeam(server, teamName, "§a[수락] §f" + quest.getQuestTitle());
    }



    // --- [퀘스트 만료 처리] ---
    public static void handleQuestCompletion(EntityPlayerMP player, EntityCustomNpc target, String teamName, NBTTagCompound questData) throws CommandException {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

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
        String itemName = Objects.requireNonNull(heldItem.getItem().getRegistryName()).toString();
        if (!heldItem.isEmpty() && itemName.equals(quest.getItemName())) {
            heldItem.shrink(1);
            ComMoney.giveCoin(player, quest.getRewardAmount());

            QuestStateManager.removeActiveQuestNpc(target.getEntityId());
            QuestHelper.spawnQuestNpc(target);
            target.isDead = true;
            target.world.removeEntity(target);

            QuestPacketHandler.getInstance().sendToAll(
                    new QuestMessage(target.getEntityId(),
                            "",
                            "", false
                    )
            );

            Helper.sendToTeam(server, teamName, "§a[완료] §f" + NickTable.getColorByName(NickTable.getNickByName(player.getName())) + "님이 해결!");
        } else { player.sendMessage(new TextComponentString("§c[BT2020] §f퀘스트 아이템이 아닙니다.")); }
    }


    // --- [퀘스트 만료 처리] ---
    public static void handleQuestExpiration(EntityCustomNpc target) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        
        int npcId = target.getEntityId();
        if (QuestStateManager.isProcessing(npcId)) return;
        
        QuestStateManager.addProcessing(npcId);
        try {
            NBTTagCompound extraData = target.getEntityData();
            if (!extraData.hasKey("YeontanQuest")) return;
            
            NBTTagCompound questData = extraData.getCompoundTag("YeontanQuest");
            int questId = questData.getInteger("QuestID");
            String ownerTeam = questData.getString("OwnerTeam");

            Quest quest = QuestManager.getQuestById(questId);
            Helper.sendToTeam(server, ownerTeam, "§c[실패] §f" + quest.getQuestTitle() + " 퀘스트가 만료되었습니다.");

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
        } finally { QuestStateManager.removeProcessing(npcId); }
    }
}

