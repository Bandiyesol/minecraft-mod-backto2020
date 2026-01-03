package com.bandiyesol.yeontan.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import noppes.npcs.entity.EntityCustomNpc;

import javax.annotation.Nonnull;
import java.io.IOException;

public class QuestEntity extends EntityCustomNpc implements IEntityAdditionalSpawnData {

    private Quest currentQuest;
    private String ownerTeam = "";
    private long expireTime;
    private boolean isTemplate = false;

    public QuestEntity(World world) {
        super(world);
        if (this.display == null) { this.display = new noppes.npcs.entity.data.DataDisplay(this); }
        if (this.stats == null) { this.stats = new noppes.npcs.entity.data.DataStats(this); }
        if (this.ais == null) { this.ais = new noppes.npcs.entity.data.DataAI(this); }
    }

    public Quest getCurrentQuest() { return currentQuest; }
    public String getOwnerTeam() { return ownerTeam; }
    public long getExpireTime() { return expireTime; }
    public boolean isExpired() { return System.currentTimeMillis() > expireTime; }
    public void setTemplate(boolean isTemplate) { this.isTemplate = isTemplate; }
    public boolean isTemplate() { return isTemplate; }

    public void assignQuest(Quest quest,String teamName, long duration) {
        this.currentQuest = quest;
        this.ownerTeam = teamName;
        this.expireTime = System.currentTimeMillis() + duration;
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        NBTTagCompound compound = new NBTTagCompound();
        this.writeEntityToNBT(compound);
        new PacketBuffer(buffer).writeCompoundTag(compound);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        try {
            NBTTagCompound compound = new PacketBuffer(buffer).readCompoundTag();
            if (compound != null) { this.readEntityFromNBT(compound);}
        }

        catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void writeEntityToNBT(@Nonnull NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("QuestID", currentQuest != null ? currentQuest.getId() : -1);
        compound.setString("OwnerTeam", ownerTeam);
        compound.setLong("ExpireTime", expireTime);
        compound.setBoolean("IsTemplate", isTemplate);
    }

    @Override
    public void readEntityFromNBT(@Nonnull NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (this.display == null) this.display = new noppes.npcs.entity.data.DataDisplay(this);
        this.display.readToNBT(compound);
        if (this.stats == null) this.stats = new noppes.npcs.entity.data.DataStats(this);
        this.stats.readToNBT(compound);

        int questId = compound.getInteger("QuestID");
        if (questId != -1) { this.currentQuest = QuestManager.getQuestById(questId); }
        this.ownerTeam = compound.getString("OwnerTeam");
        this.expireTime = compound.getLong("ExpireTime");
        this.isTemplate = compound.getBoolean("IsTemplate");
    }

    @Override
    public String getName() { return (this.display != null && this.display.getName() != null) ? this.display.getName() : "Quest NPC"; }

    @Override
    public boolean getAlwaysRenderNameTagForRender() { return false; }

    @Override
    public boolean hasCustomName() { return false; }

    @Override
    public void setCustomNameTag(@Nonnull String name) { super.setCustomNameTag(name); }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {}

    @Override
    public void setSwingingArms(boolean swingingArms) {}
}
