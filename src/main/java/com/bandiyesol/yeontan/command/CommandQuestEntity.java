package com.bandiyesol.yeontan.command;

import com.bandiyesol.yeontan.quest.QuestEntityManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;

import java.util.List;

public class CommandQuestEntity extends CommandBase {

    @Override
    public String getName() { return "questentity"; }

    @Override
    public String getUsage(ICommandSender sender) { return "/questentity <add|remove>"; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) sender;

        if (args.length < 1) return;

        Entity target = getMouseOverEntity(player, 3.0D);

        if (target != null) {
            if (args[0].equalsIgnoreCase("add")) {
                if (QuestEntityManager.isAuthorized(target.getUniqueID())) {
                    sender.sendMessage(new TextComponentString("이미 등록된 엔티티입니다."));
                    return;
                }

                QuestEntityManager.addEntity(target.getUniqueID());
                sender.sendMessage(new TextComponentString("조준한 엔티티가 등록되었습니다! (ID: " + target.getUniqueID() + ")"));
                QuestEntityManager.saveData();
            }

            else if (args[0].equalsIgnoreCase("remove")) {
                if (QuestEntityManager.isAuthorized(target.getUniqueID())) {
                    QuestEntityManager.removeEntity(target.getUniqueID());
                    sender.sendMessage(new TextComponentString("엔티티가 제거되었습니다."));
                    QuestEntityManager.saveData();
                }

                else {sender.sendMessage(new TextComponentString("등록되지 않은 엔티티입니다."));}
            }
        }

        else {
            sender.sendMessage(new TextComponentString("정확히 엔티티를 조준하고 명령어를 입력해주세요."));
        }
    }

    private Entity getMouseOverEntity(EntityPlayer player, double distance) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        Vec3d end = start.addVector(look.x * distance, look.y * distance, look.z * distance);

        Entity pointedEntity = null;
        List<Entity> list = player.world.getEntitiesWithinAABBExcludingEntity(player, player.getEntityBoundingBox().expand(look.x * distance, look.y * distance, look.z * distance).grow(1.0D, 1.0D, 1.0D));

        for (Entity entity : list) {
            if (entity.canBeCollidedWith()) {
                AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().grow((double)entity.getCollisionBorderSize());
                RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(start, end);

                if (axisalignedbb.contains(start)) {
                    if (distance >= 0.0D) {
                        pointedEntity = entity;
                        distance = 0.0D;
                    }
                } else if (raytraceresult != null) {
                    double d1 = start.distanceTo(raytraceresult.hitVec);
                    if (d1 < distance || distance == 0.0D) {
                        pointedEntity = entity;
                        distance = d1;
                    }
                }
            }
        }
        return pointedEntity;
    }
}
