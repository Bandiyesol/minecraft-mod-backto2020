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
    boolean isVisible;

    public QuestMessage() {}

    public QuestMessage(int entityId, String itemStackName, String questTitle, boolean isVisible) {
        this.entityId = entityId;
        this.itemStackName = itemStackName;
        this.questTitle = questTitle;
        this.isVisible = isVisible;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.itemStackName = ByteBufUtils.readUTF8String(buf);
        this.questTitle = ByteBufUtils.readUTF8String(buf);
        this.isVisible = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        ByteBufUtils.writeUTF8String(buf, itemStackName);
        ByteBufUtils.writeUTF8String(buf, this.questTitle);
        buf.writeBoolean(isVisible);
    }

    public static class Handler implements IMessageHandler<QuestMessage, IMessage> {
        @Override
        public IMessage onMessage(QuestMessage message, MessageContext context) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();

            mc.addScheduledTask(() -> {
                if (message.isVisible) {
                    QuestRenderHandler.activeQuests.put(message.entityId, message.itemStackName);
                    QuestRenderHandler.activeQuestTitles.put(message.entityId, message.questTitle);
                }

                else {
                    QuestRenderHandler.activeQuests.remove(message.entityId);
                    QuestRenderHandler.activeQuestTitles.remove(message.entityId);
                }
            });

            return null;
        }
    }
}
