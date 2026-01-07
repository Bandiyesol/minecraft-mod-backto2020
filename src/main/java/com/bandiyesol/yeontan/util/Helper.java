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

    // --- [플레이어의 팀 이름 불러오기 로직] ---
    public static String getPlayerTeamName(EntityPlayerMP player) {
        try {
            ScorePlayerTeam team = player.getWorldScoreboard().getPlayersTeam(player.getName());
            return (team != null) ? team.getName() : null;
        } catch (Exception e) { return null; }
    }

    // --- [팀에게 메세지 전송 로직] ---
    public static void sendToTeam(MinecraftServer server, String teamName, String message) {
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            String playerTeam = getPlayerTeamName(player);
            if (teamName.equals(playerTeam)) {
                player.sendMessage(new TextComponentString(message));
            }
        }
    }
}
