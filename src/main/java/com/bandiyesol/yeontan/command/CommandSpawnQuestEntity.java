package com.bandiyesol.yeontan.command;

import com.bandiyesol.yeontan.util.QuestHelper;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommandSpawnQuestEntity extends CommandBase {

    @Override
    public String getName() { return "questspawn"; }

    @Override
    public String getUsage(ICommandSender sender) { return "/questspawn <create|clear>"; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) sender;

        if (args.length < 1) return;

        if (args[0].equalsIgnoreCase("create")) {
            QuestHelper.spawnQuestNpc(player);
        }

        else if (args[0].equalsIgnoreCase("clear")) return;

    }
}
