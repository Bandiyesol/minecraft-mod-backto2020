package com.bandiyesol.yeontan.network;

public class QuestDisplayData {
    private final String itemName;
    private final String questTitle;
    private long expireTime;

    public QuestDisplayData(String itemName, String questTitle) {
        this.itemName = itemName;
        this.questTitle = questTitle;
        this.expireTime = 0;
    }

    public String getItemName() { return itemName; }
    public String getQuestTitle() { return questTitle; }
    public long getExpireTime() { return expireTime; }
    public void setExpireTime(long expireTime) { this.expireTime = expireTime; }
}