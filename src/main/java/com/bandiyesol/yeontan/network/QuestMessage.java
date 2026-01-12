package com.bandiyesol.yeontan.network;

import com.bandiyesol.yeontan.client.QuestRenderHandler;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class QuestMessage implements IMessage {

    private int entityId;
    private String itemStackName;
    private String questTitle;
    private boolean isVisible;

    public QuestMessage() {}

    public QuestMessage(int entityId, String itemStackName, String questTitle, boolean isVisible) {
        this.entityId = entityId;
        this.itemStackName = itemStackName;
        this.questTitle = questTitle;
        this.isVisible = isVisible;
    }

    public int getEntityId() { return entityId; }
    public String getItemStackName() { return itemStackName; }
    public String getQuestTitle() { return questTitle; }
    public boolean isVisible() { return isVisible; }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf == null) return;
        this.entityId = buf.readInt();
        this.itemStackName = ByteBufUtils.readUTF8String(buf);
        this.questTitle = ByteBufUtils.readUTF8String(buf);
        this.isVisible = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        if (buf == null) return;
        buf.writeInt(entityId);
        ByteBufUtils.writeUTF8String(buf, itemStackName != null ? itemStackName : "");
        ByteBufUtils.writeUTF8String(buf, questTitle != null ? questTitle : "");
        buf.writeBoolean(isVisible);
    }

    public static class Handler implements IMessageHandler<QuestMessage, IMessage> {

        @Override
        public IMessage onMessage(QuestMessage message, MessageContext context) {
            if (context.side.isClient()) { handleClientMessage(message); }
            return null;
        }

        private void handleClientMessage(QuestMessage message) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                if (message.getEntityId() == -1) {
                    QuestRenderHandler.activeRenderQuests.clear();
                    return;
                }

                if (message.isVisible()) {
                    QuestDisplayData existingData = QuestRenderHandler.activeRenderQuests.get(message.getEntityId());
                    if (existingData != null) {
                        long existingExpireTime = existingData.getExpireTime();
                        QuestRenderHandler.activeRenderQuests.put(
                                message.getEntityId(),
                                new QuestDisplayData(
                                        message.getItemStackName(),
                                        message.getQuestTitle()
                                )
                        );
                        QuestRenderHandler.activeRenderQuests.get(message.getEntityId()).setExpireTime(existingExpireTime);
                    } else {
                        QuestRenderHandler.activeRenderQuests.put(
                                message.getEntityId(),
                                new QuestDisplayData(
                                        message.getItemStackName(),
                                        message.getQuestTitle()
                                )
                        );
                    }
                } else {
                    QuestRenderHandler.activeRenderQuests.remove(message.getEntityId());
                }
            });
        }
    }
}
