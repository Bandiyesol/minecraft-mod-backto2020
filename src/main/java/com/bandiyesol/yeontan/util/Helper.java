package com.bandiyesol.yeontan.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;

import java.util.List;

public class Helper {

    // --- [플레이어의 시선의 엔티티 불러오기 로직] ---
    public static Entity getMouseOverEntity(EntityPlayer player, double distance) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        Vec3d end = start.add(look.x * distance, look.y * distance, look.z * distance);

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

    // --- [플레이어의 팀 이름 불러오기 로직] ---
    public static String getPlayerTeamName(EntityPlayerMP player) {
        try {
            ScorePlayerTeam team = player.getWorldScoreboard().getPlayersTeam(player.getName());
            return (team != null) ? team.getName() : null;
        }

        catch (Exception e) {
            return null;
        }
    }

    // --- [팀에게 메세지 전송 로직] ---
    public static void sendToTeam(MinecraftServer server, String teamName, String message) {
        // 최적화: 팀 이름으로 필터링하여 불필요한 반복 줄이기
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            String playerTeam = getPlayerTeamName(player);
            if (teamName.equals(playerTeam)) {
                player.sendMessage(new TextComponentString(message));
            }
        }
    }
}
