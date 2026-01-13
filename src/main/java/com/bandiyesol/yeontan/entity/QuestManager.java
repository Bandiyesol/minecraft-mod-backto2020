package com.bandiyesol.yeontan.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class QuestManager {

    private static final List<Quest> ALL_QUESTS = new ArrayList<>();
    private static final Random RANDOM = new Random();
    public static int currentServerLevel = 1;

    static {
		ALL_QUESTS.add(new Quest(1, "iadd:f_ham_toast", "햄 토스트 주세요", 1, 5500));
        ALL_QUESTS.add(new Quest(2, "iadd:f_ham_cheese_toast", "햄치즈 토스트 주세요", 1, 13000));
        ALL_QUESTS.add(new Quest(3, "iadd:f_bacon_toast", "베이컨 토스트 주세요", 1, 27500));
        ALL_QUESTS.add(new Quest(4, "iadd:f_dmb_toast", "더블 모짜 베이컨 토스트 주세요", 2, 56500));
        ALL_QUESTS.add(new Quest(5, "iadd:f_esb_toast", "에그 소시지 베이컨 토스트 주세요", 2, 115000));
        ALL_QUESTS.add(new Quest(6, "iadd:f_psph_toast", "포테이토 고구마 햄 토스트 주세요", 2, 240000));
        ALL_QUESTS.add(new Quest(7, "iadd:f_sbh_toast", "핫 베이컨 햄 토스트 주세요", 2, 470000));
		
		ALL_QUESTS.add(new Quest(8, "iadd:f_redbean_bun", "팥 호빵 주세요", 1, 850));
        ALL_QUESTS.add(new Quest(9, "iadd:f_vegetable_bun", "야채 호빵 주세요", 1, 3500));
        ALL_QUESTS.add(new Quest(10, "iadd:f_pizza_bun", "피자 호빵 주세요", 1, 8300));
        ALL_QUESTS.add(new Quest(11, "iadd:f_sweetpotato_bun", "고구마 호빵 주세요", 2, 18000));
		ALL_QUESTS.add(new Quest(12, "iadd:f_corn_bun", "옥수수 호빵 주세요", 2, 38100));
        ALL_QUESTS.add(new Quest(13, "iadd:f_goc_bun", "대파 크림치즈 호빵 주세요", 2, 78000));
        ALL_QUESTS.add(new Quest(14, "iadd:f_angbutter_bun", "앙버터 호빵 주세요", 2, 160000));

        ALL_QUESTS.add(new Quest(15, "iadd:f_m_hotdog", "모짜 핫도그 주세요", 1, 1200));
        ALL_QUESTS.add(new Quest(16, "iadd:f_squid_m_hotdog", "오징어 모짜 핫도그 주세요", 1, 3500));
        ALL_QUESTS.add(new Quest(17, "iadd:f_spicy_m_hotdog", "모짜 HOT도그 주세요", 1, 9500));
        ALL_QUESTS.add(new Quest(18, "iadd:f_mssp_hotdog", "모짜 소시지 고구마 핫도그 주세요", 2, 24000));
        ALL_QUESTS.add(new Quest(19, "iadd:f_spicy_sp_hotdog", "고구마 HOT도그 주세요", 2, 49000));
        ALL_QUESTS.add(new Quest(20, "iadd:f_dssm_hotdog", "더블 소시지 고구마 모짜 핫도그 주세요", 2, 100000));
        ALL_QUESTS.add(new Quest(21, "iadd:f_goblin_hotdog", "도깨비 핫도그 주세요", 2, 200000));
        ALL_QUESTS.add(new Quest(22, "iadd:f_m_tomato_hotdog", "모짜 토마토 핫도그 주세요", 2, 400000));

        ALL_QUESTS.add(new Quest(23, "iadd:f_peanut_bread", "땅콩빵 주세요", 2, 4000));
        ALL_QUESTS.add(new Quest(24, "iadd:f_corn_bread", "옥수수빵 주세요", 2, 4400));
        ALL_QUESTS.add(new Quest(25, "iadd:f_egg_bread", "계란빵 주세요", 2, 3900));
        ALL_QUESTS.add(new Quest(26, "iadd:f_cream_cheese_bread", "크림치즈빵 주세요", 2, 4300));
    }


    public static Quest getQuestById(int id) {
        return ALL_QUESTS.stream()
                .filter(quest -> quest.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public static Quest getRandomQuest() {
        List<Quest> pool = ALL_QUESTS.stream()
                .filter(quest -> quest.getLevel() <= currentServerLevel)
                .collect(Collectors.toList());

        if (pool.isEmpty()) return null;
        return pool.get(RANDOM.nextInt(pool.size()));
    }
}