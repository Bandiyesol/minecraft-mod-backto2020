package com.bandiyesol.yeontan.command;

import com.bandiyesol.yeontan.entity.QuestEntity;
import com.bandiyesol.yeontan.entity.QuestEntityManager;
import com.bandiyesol.yeontan.util.Helper;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.entity.EntityCustomNpc;

import javax.annotation.Nonnull;


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

        Entity target = Helper.getMouseOverEntity(player, 5.0D);
        if (target instanceof EntityCustomNpc) {
            EntityCustomNpc npc = (EntityCustomNpc) target;

            if (args[0].equalsIgnoreCase("add")) {
                if (QuestEntityManager.isTemplate(npc.getUniqueID())) {
                    sender.sendMessage(new TextComponentString("§c이미 등록된 원본 엔티티입니다."));
                    return;
                }

                QuestEntityManager.addTemplate(npc.getUniqueID(), npc);
                if (npc instanceof QuestEntity) {
                    ((QuestEntity) npc).setTemplate(true);
                }

                sender.sendMessage(new TextComponentString("§a[BT2020] §f조준한 엔티티를 원본 템플릿으로 등록했습니다!"));
            }

            else if (args[0].equalsIgnoreCase("remove")) {
                if (QuestEntityManager.isTemplate(npc.getUniqueID())) {
                    if (npc instanceof QuestEntity) {
                        ((QuestEntity) npc).setTemplate(false);
                    }

                    QuestEntityManager.removeTemplate(npc.getUniqueID());
                    sender.sendMessage(new TextComponentString("§a[BT2020] §f조준한 엔티티가 원본 목록에서 제거되었습니다."));
                }

                else { sender.sendMessage(new TextComponentString("§c등록되지 않은 엔티티입니다.")); }
            }
        }
        else { sender.sendMessage(new TextComponentString("§6[QuestEntity] §fCustomNPC를 정확히 조준해주세요.")); }
    }
}
