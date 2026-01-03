package com.bandiyesol.yeontan.command;

import com.bandiyesol.yeontan.entity.QuestManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CommandSetQuestLevel extends CommandBase {

    @Override
    @Nonnull
    public String getName() { 
        return "quest"; 
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender) { 
        return "/quest <1/2>"; 
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "1", "2");
        }
        return Collections.emptyList();
    }

    @Override
    public boolean checkPermission(@Nonnull MinecraftServer server, ICommandSender sender) { return sender.canUseCommand(this.getRequiredPermissionLevel(), this.getName()); }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) return;
        QuestManager.currentSeverLevel = Integer.parseInt(args[0]);
        sender.sendMessage(new TextComponentString("퀘스트 레벨이 " + args[0] + "단계로 설정되었습니다."));
    }
}
