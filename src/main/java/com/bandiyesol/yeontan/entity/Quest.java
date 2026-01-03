package com.bandiyesol.yeontan.entity;

public class Quest {
    private final int id;
    private final String itemName;
    private final String questTitle;
    private final int level;
    private final int rewardAmount;

    public Quest(int id, String itemName, String questTitle, int level, int rewardAmount) {
        this.id = id;
        this.itemName = itemName;
        this.questTitle = questTitle;
        this.level = level;
        this.rewardAmount = rewardAmount;
    }

    public int getId() { return id; }
    public String getItemName() { return itemName; }
    public String getQuestTitle() { return questTitle; }
    public int getLevel() { return level; }
    public int getRewardAmount() { return rewardAmount; }
}