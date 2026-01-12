package com.bandiyesol.yeontan.network;

import com.bandiyesol.yeontan.client.QuestRenderHandler;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class QuestTimerMessage implements IMessage {

    private int entityId;
    private long expireTime;

    public QuestTimerMessage() {}

    public QuestTimerMessage(int entityId, long expireTime) {
        this.entityId = entityId;
        this.expireTime = expireTime;
    }

    public int getEntityId() { return entityId; }
    public long getExpireTime() { return expireTime; }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf == null) return;
        this.entityId = buf.readInt();
        this.expireTime = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        if (buf == null) return;
        buf.writeInt(entityId);
        buf.writeLong(expireTime);
    }

    public static class Handler implements IMessageHandler<QuestTimerMessage, IMessage> {

        @Override
        public IMessage onMessage(QuestTimerMessage message, MessageContext context) {
            if (context.side.isClient()) {
                handleClientMessage(message);
            }
            return null;
        }

        private void handleClientMessage(QuestTimerMessage message) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                QuestDisplayData data = QuestRenderHandler.activeRenderQuests.get(message.getEntityId());
                if (data != null) {
                    data.setExpireTime(message.getExpireTime());
                } else {
                    QuestRenderHandler.activeRenderQuests.put(
                            message.getEntityId(),
                            new QuestDisplayData("", "")
                    );
                    QuestRenderHandler.activeRenderQuests.get(message.getEntityId()).setExpireTime(message.getExpireTime());
                }
            });
        }
    }
}

