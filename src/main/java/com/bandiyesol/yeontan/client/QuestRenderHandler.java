package com.bandiyesol.yeontan.client;

import com.bandiyesol.yeontan.network.QuestDisplayData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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
            int entityId = entry.getKey();
            QuestDisplayData data = entry.getValue();
            Entity entity = mc.world.getEntityByID(entityId);

            if (entity != null && !entity.isDead && mc.player.getDistanceSq(entity) < 1024) { renderQuestFloatingDisplay(entity, data, partialTicks); }
            else if (entity == null || entity.isDead) { activeRenderQuests.remove(entityId); }
        }
    }

    private void renderQuestFloatingDisplay(Entity entity, QuestDisplayData data, float partialTicks) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderManager.viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.viewerPosY + entity.height + 1.2;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.viewerPosZ;

        // OpenGL 상태 저장 (각 엔티티 렌더링 전 상태 명확히 저장)
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        
        // 엔티티 위치로 변환 및 카메라 회전 적용
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

        // Depth 테스트 설정 (일관되게 관리)
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(519); // GL_LEQUAL
        GlStateManager.depthMask(true); // 기본값 설정

        // 게이지 렌더링
        if (data.getExpireTime() > 0) {
            long currentTime = System.currentTimeMillis();
            long remainingTime = data.getExpireTime() - currentTime;
            
            if (remainingTime > 0) {
                int totalSlots = 5;
                long timePerSlot = 12000; 
                int filledSlots = (int) Math.min(totalSlots, (remainingTime + timePerSlot - 1) / timePerSlot);
                
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0.2, 0);
                renderQuestGauge(filledSlots, totalSlots);
                GlStateManager.popMatrix();
            }
        }

        // 퀘스트 제목 렌더링
        GlStateManager.pushMatrix();
        float textScale = 0.018F;
        GlStateManager.scale(-textScale, -textScale, textScale);

        String title = data.getQuestTitle();
        if (title != null && !title.isEmpty()) {
            int width = fontRenderer.getStringWidth(title) / 2;
            fontRenderer.drawString(title, -width, 0, 0xFFFFFFFF);
        }

        GlStateManager.popMatrix();

        // 아이템 아이콘 렌더링
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, -0.3, 0);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);

        String itemName = data.getItemName();
        Item item = (itemName != null && !itemName.isEmpty()) ? Item.getByNameOrId(itemName) : null;
        if (item != null) {
            RenderHelper.enableStandardItemLighting();
            Minecraft.getMinecraft().getRenderItem().renderItem(new ItemStack(item), ItemCameraTransforms.TransformType.FIXED);
            RenderHelper.disableStandardItemLighting();
        }

        GlStateManager.popMatrix();

        // OpenGL 상태 복원 (각 엔티티 렌더링 후 상태 명확히 복원)
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private void renderQuestGauge( int filledSlots, int totalSlots) {
        float gaugeScale = 0.012F;
        GlStateManager.pushMatrix();
        GlStateManager.scale(-gaugeScale, -gaugeScale, gaugeScale);
        
        int slotWidth = 8;
        int slotHeight = 2;
        int spacing = 2;
        int totalWidth = (slotWidth + spacing) * totalSlots - spacing;
        int startX = -totalWidth / 2;
        
        // 배경 렌더링
        drawRect(startX - 1, -1, startX + totalWidth + 1, slotHeight + 1, 0x80000000);

        float colorRatio = (float)filledSlots / totalSlots;
        
        // 각 슬롯 렌더링
        for (int i = 0; i < totalSlots; i++) {
            int slotX = startX + i * (slotWidth + spacing);
            if (i < filledSlots) {
                int red = (int)(255 * (1.0F - colorRatio));
                int green = (int)(255 * colorRatio);
                int color = 0xFF000000 | (red << 16) | (green << 8);
                
                drawRect(slotX, 0, slotX + slotWidth, slotHeight, color);
            } else {
                drawRect(slotX, 0, slotX + slotWidth, slotHeight, 0xFF808080);
            }
        }
        
        GlStateManager.popMatrix();
    }

    private void drawRect(int left, int top, int right, int bottom, int color) {
        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        // OpenGL 상태 설정 (depth test는 유지, depth mask만 false로 설정)
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
        // depth test는 유지하되, depth buffer에 기록하지 않도록 depth mask를 false로 설정
        // 이렇게 하면 여러 게이지가 서로를 가리지 않으면서도 월드 요소와의 depth 관계는 유지됨
        GlStateManager.depthMask(false);
        GlStateManager.color(red, green, blue, alpha);

        // Tessellator를 사용한 렌더링 (Minecraft 표준 방식)
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(left, bottom, 0.0D).color(red, green, blue, alpha).endVertex();
        buffer.pos(right, bottom, 0.0D).color(red, green, blue, alpha).endVertex();
        buffer.pos(right, top, 0.0D).color(red, green, blue, alpha).endVertex();
        buffer.pos(left, top, 0.0D).color(red, green, blue, alpha).endVertex();
        tessellator.draw();

        // OpenGL 상태 복원 (depth mask를 true로 복원)
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}