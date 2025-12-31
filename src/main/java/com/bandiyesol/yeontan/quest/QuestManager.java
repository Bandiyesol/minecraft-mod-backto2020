package com.bandiyesol.yeontan.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class QuestManager {

    private static final List<Quest> ALL_QUESTS = new ArrayList<>();
    private static final Random RANDOM = new Random();
    public static int currentSeverLevel = 1;

    static {
        ALL_QUESTS.add(new Quest(1, "iadd:f_corn", "사과 가져오기", 1, "iadd:money", 1));
        ALL_QUESTS.add(new Quest(2, "iadd:f_sausage", "배고파요!", 1, "iadd:money", 1));
        ALL_QUESTS.add(new Quest(3, "iadd:f_cream", "귀한 보석", 2, "iadd:money", 1));
        ALL_QUESTS.add(new Quest(4, "iadd:f_ham_cheese_toast", "황금의 시대", 2, "iadd:money", 1));
    }

    public static Quest getRandomQuest() {
        List<Quest> pool = ALL_QUESTS.stream()
                .filter(quest -> quest.getLevel() <= currentSeverLevel)
                .collect(Collectors.toList());

        if (pool.isEmpty()) return null;
        return pool.get(RANDOM.nextInt(pool.size()));
    }
}