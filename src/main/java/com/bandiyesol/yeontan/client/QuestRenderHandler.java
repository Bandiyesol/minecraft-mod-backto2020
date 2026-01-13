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

        // 게이지 렌더링 (텍스트로, 제목 위에 배치)
        if (data.getExpireTime() > 0) {
            long currentTime = System.currentTimeMillis();
            long remainingTime = data.getExpireTime() - currentTime;
            
            if (remainingTime > 0) {
                int totalSlots = 5;
                long timePerSlot = 12000; 
                int filledSlots = (int) Math.min(totalSlots, (remainingTime + timePerSlot - 1) / timePerSlot);
                
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0.15, 0); // 제목 위에 배치
                renderQuestGauge(fontRenderer, filledSlots, totalSlots);
                GlStateManager.popMatrix();
            }
        }

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

    private void renderQuestGauge(FontRenderer fontRenderer, int filledSlots, int totalSlots) {
        // 텍스트 스케일 설정 (제목과 동일한 스케일 사용)
        float textScale = 0.018F;
        GlStateManager.pushMatrix();
        GlStateManager.scale(-textScale, -textScale, textScale);
        
        // 시간에 따른 색상 결정 (녹색 → 노란색 → 빨간색)
        float colorRatio = (float)filledSlots / totalSlots;
        String colorCode;
        if (colorRatio > 0.6F) {
            colorCode = "§a"; // 녹색 (60% 이상)
        } else if (colorRatio > 0.3F) {
            colorCode = "§e"; // 노란색 (30-60%)
        } else {
            colorCode = "§c"; // 빨간색 (30% 미만)
        }
        
        // 게이지 텍스트 생성 (유니코드 블록 문자 사용)
        StringBuilder gaugeText = new StringBuilder();
        gaugeText.append(colorCode);
        
        // 채워진 칸
        for (int i = 0; i < filledSlots; i++) {
            gaugeText.append("█");
        }
        
        // 빈 칸
        gaugeText.append("§7"); // 회색
        for (int i = filledSlots; i < totalSlots; i++) {
            gaugeText.append("░");
        }
        
        // 텍스트 렌더링 (제목과 동일한 방식)
        String gaugeString = gaugeText.toString();
        int width = fontRenderer.getStringWidth(gaugeString) / 2;
        fontRenderer.drawString(gaugeString, -width, 0, 0xFFFFFFFF);
        
        GlStateManager.popMatrix();
    }
}