package com.bandiyesol.yeontan.event;

import com.bandiyesol.yeontan.network.QuestMessage;
import com.bandiyesol.yeontan.network.QuestPacketHandler;
import com.bandiyesol.yeontan.entity.Quest;
import com.bandiyesol.yeontan.entity.QuestManager;
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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.*;

public class QuestEventHandler {

    private static final List<String> CLONE_NAMES = Arrays.asList(QuestHelper.CLONE_POOL);
    private static final long LIMIT_TIME = 30 * 1000;
    
    // 퀘스트가 있는 NPC ID를 추적하여 불필요한 검사 방지
    private static final Set<Integer> activeQuestNpcIds = new HashSet<>();
    
    // 처리 중인 NPC ID (중복 처리 방지)
    private static final Set<Integer> processingNpcIds = new HashSet<>();
    
    // 팀별 플레이어 캐싱 (5초마다 갱신)
    private static final Map<String, List<EntityPlayerMP>> teamPlayerCache = new HashMap<>();
    private static long lastCacheUpdate = 0;
    private static final long CACHE_UPDATE_INTERVAL = 5000; // 5초

    private int tickCounter = 0;


    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (++tickCounter >= 20) {
            tickCounter = 0;
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server == null) return;
            
            // 활성 퀘스트 NPC가 없으면 조기 종료
            if (activeQuestNpcIds.isEmpty()) return;

            long currentTime = System.currentTimeMillis();
            List<Integer> expiredNpcIds = new ArrayList<>();
            
            // 추적 중인 NPC만 검사 (모든 NPC 검사 대신)
            for (Integer npcId : activeQuestNpcIds) {
                EntityCustomNpc npc = findNpcById(server, npcId);
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
                            // 중복 처리 방지
                            if (!processingNpcIds.contains(npcId)) {
                                System.out.println("[QuestLog] Quest expired for NPC " + npcId);
                                handleQuestExpiration(npc);
                                expiredNpcIds.add(npcId);
                            }
                        }
                    }
                } else {
                    // 퀘스트 데이터가 없으면 추적 목록에서 제거
                    expiredNpcIds.add(npcId);
                }
            }
            
            // 만료되거나 제거된 NPC를 추적 목록에서 제거
            expiredNpcIds.forEach(activeQuestNpcIds::remove);
        }
    }

    // NPC ID로 NPC 찾기 (최적화된 검색)
    private EntityCustomNpc findNpcById(MinecraftServer server, int entityId) {
        for (WorldServer world : server.worlds) {
            EntityCustomNpc npc = (EntityCustomNpc) world.getEntityByID(entityId);
            if (npc != null && CLONE_NAMES.contains(npc.getName())) {
                return npc;
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onPlayerJoin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;

        // 접속한 플레이어에게 현재 월드에 있는 퀘스트 NPC의 정보만 전송 (최적화)
        // CLONE_NAMES에 해당하는 NPC만 검사
        for (EntityCustomNpc npc : player.getServerWorld().getEntities(EntityCustomNpc.class, 
                n -> n != null && CLONE_NAMES.contains(n.getName()))) {
            NBTTagCompound extraData = npc.getEntityData();
            if (extraData.hasKey("YeontanQuest")) {
                NBTTagCompound qData = extraData.getCompoundTag("YeontanQuest");
                Quest quest = QuestManager.getQuestById(qData.getInteger("QuestID"));

                if (quest != null) {
                    // 해당 플레이어에게만 패킷 전송
                    QuestPacketHandler.getInstance().sendTo(
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
        
        // 팀 가입 체크
        if (teamName == null || teamName.isEmpty()) {
            player.sendMessage(new TextComponentString("§c[BT2020] §f퀘스트를 받으려면 팀에 가입해야 합니다."));
            return;
        }

        NBTTagCompound extraData = target.getEntityData();
        System.out.println("NPC NBT Has Key: " + extraData.hasKey("YeontanQuest"));

        if (!extraData.hasKey("YeontanQuest")) {
            handleQuestAssignment(teamName, target);
        }

        else {
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
                // 완료 시에도 팀 멤버인지 재확인
                String currentTeam = Helper.getPlayerTeamName(player);
                if (currentTeam == null || !currentTeam.equals(teamName)) {
                    player.sendMessage(new TextComponentString("§c[BT2020] §f퀘스트를 완료하려면 해당 팀에 속해있어야 합니다."));
                    return;
                }
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
        
        // 퀘스트가 있는 NPC를 추적 목록에 추가
        activeQuestNpcIds.add(target.getEntityId());

        QuestPacketHandler.getInstance().sendToAll(
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
            Helper.sendToTeam(server, teamName, "§a[완료] §f" + NickTable.getColorByName(NickTable.getColorByName(player.getName())) + "님이 해결!");

            QuestPacketHandler.getInstance().sendToAll(
                    new QuestMessage(target.getEntityId(),
                            "",
                            "", false
                    )
            );

            // 추적 목록에서 제거
            activeQuestNpcIds.remove(target.getEntityId());
            
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
        
        int npcId = target.getEntityId();
        
        // 중복 처리 방지
        if (processingNpcIds.contains(npcId)) {
            System.out.println("[QuestLog] NPC " + npcId + " is already being processed, skipping...");
            return;
        }
        
        processingNpcIds.add(npcId);

        // 퀘스트 정보 가져오기
        NBTTagCompound extraData = target.getEntityData();
        if (!extraData.hasKey("YeontanQuest")) {
            processingNpcIds.remove(npcId);
            return;
        }
        
        NBTTagCompound questData = extraData.getCompoundTag("YeontanQuest");
        int questId = questData.getInteger("QuestID");
        String ownerTeam = questData.getString("OwnerTeam");
        
        Quest quest = QuestManager.getQuestById(questId);
        if (quest != null && !ownerTeam.isEmpty()) {
            // 팀 플레이어 캐시 사용 (모든 플레이어 검사 대신)
            List<EntityPlayerMP> teamPlayers = getTeamPlayers(server, ownerTeam);
            
            if (!teamPlayers.isEmpty()) {
                // 팀원 중 랜덤으로 한 명 선택
                Random random = new Random();
                EntityPlayerMP selectedPlayer = teamPlayers.get(random.nextInt(teamPlayers.size()));
                int penaltyAmount = -quest.getRewardAmount();

                String playerName = selectedPlayer.getName();
                NickTable.getNickByName(playerName);
                NickTable.getColorByName(playerName);

                try {
                    ComMoney.giveCoin(selectedPlayer, penaltyAmount);
                    // 선택된 플레이어에게 개인 메시지
                    selectedPlayer.sendMessage(new TextComponentString("§c[실패] §f" + quest.getQuestTitle() + " 퀘스트가 만료되어 " + quest.getRewardAmount() + "코인이 차감되었습니다."));
                    // 팀에게 실패 메시지 전송 (누가 차감되었는지 포함)
                    Helper.sendToTeam(server, ownerTeam, "§c[실패] §f" + quest.getQuestTitle() + " 퀘스트가 만료되어 " + playerName + "님에게 " + quest.getRewardAmount() + "코인이 차감되었습니다.");
                } catch (Exception e) {
                    System.err.println("[QuestLog] Failed to deduct coins from player " + playerName + ": " + e.getMessage());
                    // 에러 발생 시에도 팀에게 알림
                    Helper.sendToTeam(server, ownerTeam, "§c[실패] §f" + quest.getQuestTitle() + " 퀘스트가 만료되었습니다.");
                }
            } else {
                // 팀원이 없으면 팀에게만 메시지
                Helper.sendToTeam(server, ownerTeam, "§c[실패] §f" + quest.getQuestTitle() + " 퀘스트가 만료되었습니다.");
            }
        }

        // 추적 목록에서 제거
        activeQuestNpcIds.remove(npcId);
        
        // 처리 완료 후 플래그 제거
        processingNpcIds.remove(npcId);

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
    }
    
    // 팀 플레이어 캐시 관리 (최적화)
    private List<EntityPlayerMP> getTeamPlayers(MinecraftServer server, String teamName) {
        long currentTime = System.currentTimeMillis();
        
        // 캐시가 오래되었거나 해당 팀의 캐시가 없으면 갱신
        if (currentTime - lastCacheUpdate > CACHE_UPDATE_INTERVAL || !teamPlayerCache.containsKey(teamName)) {
            updateTeamPlayerCache(server);
            lastCacheUpdate = currentTime;
        }
        
        return teamPlayerCache.getOrDefault(teamName, Collections.emptyList());
    }
    
    private void updateTeamPlayerCache(MinecraftServer server) {
        teamPlayerCache.clear();
        
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            String teamName = Helper.getPlayerTeamName(player);
            if (teamName != null && !teamName.isEmpty()) {
                teamPlayerCache.computeIfAbsent(teamName, k -> new ArrayList<>()).add(player);
            }
        }
    }
}
