package com.bandiyesol.yeontan.client;

import com.bandiyesol.yeontan.entity.QuestEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.entity.EntityCustomNpc;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public class QuestRenderHandler {
    public static Map<Integer, String> activeQuests = new HashMap<>();
    public static Map<Integer, String> activeQuestTitles = new HashMap<>();


    @SubscribeEvent
    public void onRenderTick(RenderWorldLastEvent event) {
        for (Entity entity : Minecraft.getMinecraft().world.loadedEntityList) {
            if (entity instanceof EntityCustomNpc) {
                EntityCustomNpc npc = (EntityCustomNpc) entity;
                NBTTagCompound nbt = new NBTTagCompound();
                npc.writeEntityToNBT(nbt);

                if (nbt.hasKey("YeontanData")) {
                    int state = nbt.getCompoundTag("YeontanData").getInteger("QuestState");
                    // state 값에 따라 ! 또는 ? 아이콘을 띄움
                    renderFloatingIcon(npc, state, event.getPartialTicks());
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        float partialTicks = event.getPartialTicks();

        for (Map.Entry<Integer, String> entry : activeQuests.entrySet()) {
            Entity entity = player.world.getEntityByID(entry.getKey());

            if (entity instanceof QuestEntity && player.getDistanceSq(entity) < 1024) {
                String title = activeQuestTitles.getOrDefault(entry.getKey(), "퀘스트 진행 중");
                renderQuestFloatingDisplay(entity, entry.getValue(), title, partialTicks);
            }
        }
    }

    private void renderQuestFloatingDisplay(Entity entity, String itemName, String title, float partialTicks) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderManager.viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.viewerPosY + entity.height + 1.2;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.viewerPosZ;

        // 위치 계산
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

        // 1. 텍스트(Quest Title) 렌더링
        GlStateManager.pushMatrix();
        float textScale = 0.025F;
        GlStateManager.scale(-textScale, -textScale, textScale);

        // 검은색 반투명 배경판
        int width = fontRenderer.getStringWidth(title) / 2;
        drawRect(-width - 2, -2, width + 2, 9, 0x80000000);
        fontRenderer.drawString(title, -width, 0, 0xFFFFFFFF);
        GlStateManager.popMatrix();

        // 2. 아이템 렌더링
        GlStateManager.translate(0, -0.6, 0); // 텍스트 아래로 0.6블록 이동
        GlStateManager.scale(0.6F, 0.6F, 0.6F);

        Item item = Item.getByNameOrId(itemName);
        if (item != null) {
            RenderHelper.enableStandardItemLighting();
            Minecraft.getMinecraft().getRenderItem().renderItem(new ItemStack(item), ItemCameraTransforms.TransformType.FIXED);
            RenderHelper.disableStandardItemLighting();
        }

        GlStateManager.popMatrix();
    }

    // 간단한 사각형 그리기 함수 (텍스트 배경용)
    private void drawRect(int left, int top, int right, int bottom, int color) {
        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(f, f1, f2, f3);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}