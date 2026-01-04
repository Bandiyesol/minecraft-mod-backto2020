package com.bandiyesol.yeontan.util;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QuestHelper {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    public static final String[] CLONE_POOL = {"아경", "재경", "연경", "보경", "경빈", "예빈", "채빈"};
    private static final Random RANDOM = new Random();


    // --- [퀘스트 엔티티 스폰 로직] ---
    public static void spawnQuestNpc(Entity entity) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        // 1. 랜덤하게 클론 선택
        String selectedClone = CLONE_POOL[RANDOM.nextInt(CLONE_POOL.length)];

        // 2. 소환 위치 설정
        double x = entity.posX;
        double y = entity.posY;
        double z = entity.posZ;

        // 3. 명령어 구성: /noppes clone spawn <이름> <탭> <좌표>
        // 좌표 형식을 명확히 하기 위해 String.format 사용
        scheduler.schedule(() -> {
            server.addScheduledTask(() -> {
                String command = String.format("noppes clone spawn %s 0 %s %.2f %.2f %.2f", selectedClone, selectedClone, x, y, z);
                System.out.println("[QuestLog] Executing Command: " + command);
                server.getCommandManager().executeCommand(entity, command);
            });
        },3, TimeUnit.SECONDS);
    }
}
