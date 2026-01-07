package com.bandiyesol.yeontan.command;

import com.bandiyesol.yeontan.service.QuestService;
import com.bandiyesol.yeontan.util.QuestNpcLocationManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandQuest extends CommandBase {

    @Override
    @Nonnull
    public String getName() { 
        return "quest"; 
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender) { 
        return "/quest <level|play|location> [args...]"; 
    }

    @Override
    @Nonnull
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    @Nonnull
    public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "level", "play", "location");
        
        if (args.length == 2) {
            switch (args[0]) {
                case "level":
                    return getListOfStringsMatchingLastWord(args, "1", "2");
                case "play":
                    return getListOfStringsMatchingLastWord(args, "start", "stop");
                case "location":
                    return getListOfStringsMatchingLastWord(args, "add", "remove", "list", "clear");
            }
        }
        
        if (args.length == 3 && args[0].equals("location") && args[1].equals("remove")) {
            List<String> indices = new ArrayList<>();
            int count = QuestNpcLocationManager.getLocations().size();
            for (int i = 0; i < count; i++) {
                indices.add(String.valueOf(i));
            } return getListOfStringsMatchingLastWord(args, indices.toArray(new String[0]));
        } return Collections.emptyList();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP 권한 레벨 (2 = OP)
    }
    
    @Override
    public boolean checkPermission(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender) { 
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            com.mojang.authlib.GameProfile profile = player.getGameProfile();
            server.getPlayerList().getOppedPlayers().getEntry(profile);
            return true;
        }
        return true;
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
        if (!checkPermission(server, sender)) {
            sender.sendMessage(new TextComponentString("§c[BT2020] §f이 명령어를 사용하려면 OP 권한이 필요합니다."));
            throw new CommandException("commands.generic.permission");
        }
        
        if (args.length < 1) {
            sender.sendMessage(new TextComponentString("§c사용법: /quest <level|play|location> [args...]"));
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "level":
                QuestService.handleLevel(server, sender, args);
                break;
            case "play":
                QuestService.handlePlay(server, sender, args);
                break;
            case "location":
                QuestService.handleLocation(server, sender, args);
                break;
            default:
                sender.sendMessage(new TextComponentString("§c알 수 없는 서브커맨드: " + subCommand));
                sender.sendMessage(new TextComponentString("§c사용법: /quest <level|play|location> [args...]"));
        }
    }
}

