package com.bandiyesol.yeontan.util;

import com.bandiyesol.yeontan.entity.QuestEntity;
import com.bandiyesol.yeontan.entity.QuestEntityManager;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.*;

public class QuestHelper {

    private static final Random RANDOM = new Random();


    // --- [퀘스트 엔티티 부여 로직] ---
    public static void assignQuestToNpc(EntityCustomNpc npc, String questId) {
        if (npc == null) return;

        // NPC의 NBT에 퀘스트 정보 주입
        NBTTagCompound nbt = new NBTTagCompound();
        npc.writeEntityToNBT(nbt);

        NBTTagCompound yeontanData = nbt.getCompoundTag("YeontanData");
        yeontanData.setString("CurrentQuestID", questId);
        yeontanData.setInteger("QuestState", 0); // 0: 시작가능, 1: 진행중, 2: 완료가능

        nbt.setTag("YeontanData", yeontanData);
        npc.readEntityFromNBT(nbt);

        System.out.println(npc.getName() + "에게 퀘스트를 부여했습니다: " + questId);
    }

    // --- [퀘스트 엔티티 스폰 로직] ---
    public static void spawnQuestNpc(Entity origin) {
        List<UUID> templates = QuestEntityManager.getTemplateList();
        if (templates.isEmpty()) return;

        // 랜덤하게 UUID 하나 선택
        UUID templateUUID = templates.get(RANDOM.nextInt(templates.size()));

        // [변경] 엔티티 객체를 찾지 않고, 저장된 NBT 데이터를 가져옴
        NBTTagCompound savedNbt = QuestEntityManager.getTemplateNbt(templateUUID);

        if (savedNbt != null) {
            QuestEntity newQuestNpc = new QuestEntity(origin.world);

            // NBT 복제 (원본 보존을 위해 copy 사용)
            NBTTagCompound nbtCopy = savedNbt.copy();

            // 필수 태그 정리
            nbtCopy.removeTag("UUIDMost");
            nbtCopy.removeTag("UUIDLeast");
            nbtCopy.removeTag("Pos");

            newQuestNpc.readEntityFromNBT(nbtCopy);
            newQuestNpc.setPosition(origin.posX, origin.posY, origin.posZ);
            newQuestNpc.setTemplate(false);

            if (!origin.world.isRemote) {
                boolean success = origin.world.spawnEntity(newQuestNpc);
                if (success) {
                    System.out.println("성공: 데이터로부터 QuestEntity 소환 완료!");
                    newQuestNpc.updateClient();
                }
            }
        } else {
            System.out.println("실패: 해당 UUID의 NBT 데이터가 저장되어 있지 않습니다.");
        }
    }
}
