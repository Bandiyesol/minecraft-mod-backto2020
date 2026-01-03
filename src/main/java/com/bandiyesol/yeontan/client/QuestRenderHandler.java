package com.bandiyesol.yeontan.client;

import com.bandiyesol.yeontan.network.QuestDisplayData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuestRenderHandler {

    public static final Map<Integer, QuestDisplayData> activeRenderQuests = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || activeRenderQuests.isEmpty()) return;

        float partialTicks = event.getPartialTicks();

        for (Map.Entry<Integer, QuestDisplayData> entry : activeRenderQuests.entrySet()) {
            Entity entity = mc.world.getEntityByID(entry.getKey());

            if (entity != null && !entity.isDead && mc.player.getDistanceSq(entity) < 1024) {
                renderQuestFloatingDisplay(entity, entry.getValue(), partialTicks);
            }
        }
    }

    private void renderQuestFloatingDisplay(Entity entity, QuestDisplayData data, float partialTicks) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderManager.viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.viewerPosY + entity.height + 1.2;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

        GlStateManager.pushMatrix();
        float textScale = 0.025F;
        GlStateManager.scale(-textScale, -textScale, textScale);

        String title = data.getQuestTitle();
        int width = fontRenderer.getStringWidth(title) / 2;
        drawRect(-width - 2, -2, width + 2, 9, 0x80000000);
        fontRenderer.drawString(title, -width, 0, 0xFFFFFFFF);
        GlStateManager.popMatrix();

        GlStateManager.translate(0, -0.6, 0);
        GlStateManager.scale(0.6F, 0.6F, 0.6F);

        Item item = Item.getByNameOrId(data.getItemName());
        if (item != null) {
            RenderHelper.enableStandardItemLighting();
            Minecraft.getMinecraft().getRenderItem().renderItem(new ItemStack(item), ItemCameraTransforms.TransformType.FIXED);
            RenderHelper.disableStandardItemLighting();
        }

        GlStateManager.popMatrix();
    }

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