package com.bandiyesol.yeontan.command;

import com.bandiyesol.yeontan.entity.QuestManager;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nonnull;

public class CommandSetQuestLevel extends CommandBase {

    @Override
    @MethodsReturnNonnullByDefault
    public String getName() { return "quest"; }

    @Override
    @MethodsReturnNonnullByDefault
    public String getUsage(@Nonnull ICommandSender sender) { return "/quest <1/2>"; }

    @Override
    public int getRequiredPermissionLevel() { return 2; }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) return;
        QuestManager.currentSeverLevel = Integer.parseInt(args[0]);
        sender.sendMessage(new TextComponentString("퀘스트 레벨이 " + args[0] + "단계로 설정되었습니다."));
    }
}
