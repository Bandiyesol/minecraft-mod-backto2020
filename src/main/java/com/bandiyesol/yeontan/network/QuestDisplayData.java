package com.bandiyesol.yeontan.network;

public class QuestDisplayData {
    private final String itemName;
    private final String questTitle;

    public QuestDisplayData(String itemName, String questTitle) {
        this.itemName = itemName;
        this.questTitle = questTitle;
    }

    public String getItemName() { return itemName; }
    public String getQuestTitle() { return questTitle; }
}