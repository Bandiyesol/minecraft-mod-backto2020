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
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.viewerPosY + entity.height + 1.3;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.viewerPosZ;

        // OpenGL 상태 저장
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        
        // 엔티티 위치로 변환 및 카메라 회전 적용
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

        // 퀘스트 제목 렌더링
        GlStateManager.pushMatrix();
        float textScale = 0.018F;
        GlStateManager.scale(-textScale, -textScale, textScale);

        String title = data.getQuestTitle();
        if (title != null && !title.isEmpty()) {
            int width = fontRenderer.getStringWidth(title) / 2;
            fontRenderer.drawString(title, -width, 0, 0xFFFFFFFF, true);
        }

        GlStateManager.popMatrix();

        // 게이지 렌더링 (텍스트로, 제목 위에 배치)
        if (data.getExpireTime() > 0) {
            long currentTime = System.currentTimeMillis();
            long remainingTime = data.getExpireTime() - currentTime;
            
            if (remainingTime > 0) {
                int totalSlots = 10;
                long timePerSlot = 6000;
                int filledSlots = (int) Math.min(totalSlots, (remainingTime + timePerSlot - 1) / timePerSlot);
                
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0.2, 0);
                renderQuestGauge(fontRenderer, filledSlots, totalSlots);
                GlStateManager.popMatrix();
            }
        }

        // 아이템 아이콘 렌더링
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, -0.45, 0);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);

        String itemName = data.getItemName();
        Item item = (itemName != null && !itemName.isEmpty()) ? Item.getByNameOrId(itemName) : null;
        if (item != null) {
            RenderHelper.enableStandardItemLighting();
            Minecraft.getMinecraft().getRenderItem().renderItem(new ItemStack(item), ItemCameraTransforms.TransformType.FIXED);
            RenderHelper.disableStandardItemLighting();
        }

        GlStateManager.popMatrix();

        // OpenGL 상태 복원 (pushAttrib/popAttrib로 자동 복원됨)
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private void renderQuestGauge(FontRenderer fontRenderer, int filledSlots, int totalSlots) {
        float gaugeTextScale = 0.012F;
        GlStateManager.pushMatrix();
        GlStateManager.scale(-gaugeTextScale, -gaugeTextScale, gaugeTextScale);
        
        String gaugeColor;
        switch (filledSlots) {
            case 10:
            case 9:
                gaugeColor = "§2";
                break;
            case 8:
            case 7:
                gaugeColor = "§a";
                break;
            case 6:
            case 5:
                gaugeColor = "§e";
                break;
            case 4:
            case 3:
                gaugeColor = "§6";
                break;
            case 2:
            case 1:
                gaugeColor = "§c";
                break;
            default:
                gaugeColor = "§7";
                break;
        }
        
        StringBuilder gaugeContent = new StringBuilder();
        gaugeContent.append(gaugeColor);
        for (int i = 0; i < filledSlots; i++) {
            gaugeContent.append("█");
        }
        gaugeContent.append("§7");
        for (int i = filledSlots; i < totalSlots; i++) {
            gaugeContent.append("─");
        }
        
        String bracketLeft = "§0[";
        String bracketRight = "§0]";
        String gaugeContentStr = gaugeContent.toString();
        
        int bracketLeftWidth = fontRenderer.getStringWidth(bracketLeft);
        int gaugeContentWidth = fontRenderer.getStringWidth(gaugeContentStr);
        int bracketRightWidth = fontRenderer.getStringWidth(bracketRight);
        int totalWidth = bracketLeftWidth + gaugeContentWidth + bracketRightWidth;
        
        int startX = -totalWidth / 2;
        fontRenderer.drawString(bracketLeft, startX, 0, 0xFFFFFFFF, false);
        fontRenderer.drawString(bracketRight, startX + bracketLeftWidth + gaugeContentWidth, 0, 0xFFFFFFFF, false);
        fontRenderer.drawString(gaugeContentStr, startX + bracketLeftWidth, 0, 0xFFFFFFFF, false);
        
        GlStateManager.popMatrix();
    }

}