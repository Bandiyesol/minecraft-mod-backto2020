package com.bandiyesol.yeontan.command;

import com.bandiyesol.yeontan.entity.QuestManager;
import com.bandiyesol.yeontan.util.QuestHelper;
import com.bandiyesol.yeontan.util.QuestNpcLocationManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "level", "play", "location");
        }
        
        if (args.length == 2) {
            if (args[0].equals("level")) {
                return getListOfStringsMatchingLastWord(args, "1", "2");
            } else if (args[0].equals("play")) {
                return getListOfStringsMatchingLastWord(args, "start", "stop");
            } else if (args[0].equals("location")) {
                return getListOfStringsMatchingLastWord(args, "add", "remove", "list", "clear");
            }
        }
        
        if (args.length == 3 && args[0].equals("location") && args[1].equals("remove")) {
            List<String> indices = new ArrayList<>();
            int count = QuestNpcLocationManager.getLocations().size();
            for (int i = 0; i < count; i++) {
                indices.add(String.valueOf(i));
            }
            return getListOfStringsMatchingLastWord(args, indices.toArray(new String[0]));
        }
        
        return Collections.emptyList();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP 권한 레벨 (2 = OP)
    }
    
    @Override
    public boolean checkPermission(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender) { 
        // OP 권한 체크
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            com.mojang.authlib.GameProfile profile = player.getGameProfile();
            if (profile != null) {
                return server.getPlayerList().getOppedPlayers().getEntry(profile) != null;
            }
            return false;
        }
        // 콘솔이나 명령 블록은 항상 허용
        return true;
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
        // OP 권한 재확인 (명령어 실행 시)
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
                handleLevel(server, sender, args);
                break;
            case "play":
                handlePlay(server, sender, args);
                break;
            case "location":
                handleLocation(server, sender, args);
                break;
            default:
                sender.sendMessage(new TextComponentString("§c알 수 없는 서브커맨드: " + subCommand));
                sender.sendMessage(new TextComponentString("§c사용법: /quest <level|play|location> [args...]"));
        }
    }
	
	// --- [퀘스트 레벨 설정] ---
    private void handleLevel(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString("§c사용법: /quest level <1/2>"));
            return;
        }
        
        try {
            int level = Integer.parseInt(args[1]);
            if (level < 1 || level > 2) {
                sender.sendMessage(new TextComponentString("§c레벨은 1 또는 2만 가능합니다."));
                return;
            }
            
            QuestManager.currentSeverLevel = level;
            sender.sendMessage(new TextComponentString("§a퀘스트 레벨이 " + level + "단계로 설정되었습니다."));
        } catch (NumberFormatException e) {
            sender.sendMessage(new TextComponentString("§c잘못된 숫자 형식입니다."));
        }
    }
    
	// --- [퀘스트 게임 시작/종료] ---
    private void handlePlay(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString("§c사용법: /quest play <start|stop>"));
            return;
        }
        
        String action = args[1].toLowerCase();
        
        if (action.equals("start")) {
            List<QuestNpcLocationManager.SpawnLocation> locations = QuestNpcLocationManager.getLocations();
            
            if (locations.isEmpty()) {
                sender.sendMessage(new TextComponentString("§c스폰 위치가 설정되지 않았습니다."));
                sender.sendMessage(new TextComponentString("§e/quest location add 명령어로 현재 위치를 추가하세요."));
                return;
            }
            
            sender.sendMessage(new TextComponentString("§a게임 시작! " + locations.size() + "개 위치에 NPC를 스폰합니다..."));
            
            for (QuestNpcLocationManager.SpawnLocation location : locations) {
                QuestHelper.spawnQuestNpcAtLocation(location);
            }
            
            sender.sendMessage(new TextComponentString("§a모든 NPC가 스폰되었습니다."));
            
        } else if (action.equals("stop")) {
            sender.sendMessage(new TextComponentString("§a게임 종료! 모든 NPC를 제거합니다..."));
            QuestHelper.removeAllActiveNpcs();
            sender.sendMessage(new TextComponentString("§a모든 NPC가 제거되었습니다."));
            
        } else {
            sender.sendMessage(new TextComponentString("§c사용법: /quest play <start|stop>"));
        }
    }
    
	// --- [퀘스트 NPC 스폰 위치 관리] ---
    private void handleLocation(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString("§c사용법: /quest location <add|remove|list|clear>"));
            return;
        }
        
        String action = args[1].toLowerCase();
        World world = sender.getEntityWorld();
        BlockPos pos = sender.getPosition();
        
        switch (action) {
            case "add":
                QuestNpcLocationManager.addLocation(world, pos);
                String addMessage = Objects.requireNonNull(String.format(
                    "§a위치 추가됨: Dimension=%d, X=%.1f, Y=%.1f, Z=%.1f",
                    world.provider.getDimension(), (double)pos.getX(), (double)pos.getY(), (double)pos.getZ()
                ));
                sender.sendMessage(new TextComponentString(addMessage));
                break;
                
            case "remove":
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentString("§c사용법: /quest location remove <인덱스>"));
                    return;
                }
                try {
                    int index = Integer.parseInt(args[2]);
                    if (QuestNpcLocationManager.removeLocation(index)) {
                        sender.sendMessage(new TextComponentString("§a위치 " + index + "번이 제거되었습니다."));
                    } else {
                        sender.sendMessage(new TextComponentString("§c잘못된 인덱스입니다."));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(new TextComponentString("§c잘못된 숫자 형식입니다."));
                }
                break;
                
            case "list":
                List<QuestNpcLocationManager.SpawnLocation> locations = QuestNpcLocationManager.getLocations();
                if (locations.isEmpty()) {
                    sender.sendMessage(new TextComponentString("§e등록된 위치가 없습니다."));
                } else {
                    sender.sendMessage(new TextComponentString("§a등록된 위치 목록 (" + locations.size() + "개):"));
                    for (int i = 0; i < locations.size(); i++) {
                        QuestNpcLocationManager.SpawnLocation loc = locations.get(i);
                        String listMessage = Objects.requireNonNull(String.format(
                            "  §7[%d] Dimension=%d, X=%.1f, Y=%.1f, Z=%.1f",
                            i, loc.dimension, loc.x, loc.y, loc.z
                        ));
                        sender.sendMessage(new TextComponentString(listMessage));
                    }
                }
                break;
                
            case "clear":
                QuestNpcLocationManager.clearLocations();
                sender.sendMessage(new TextComponentString("§a모든 위치가 제거되었습니다."));
                break;
                
            default:
                sender.sendMessage(new TextComponentString("§c사용법: /quest location <add|remove|list|clear>"));
        }
    }
}

