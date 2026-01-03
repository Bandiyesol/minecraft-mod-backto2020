package com.bandiyesol.yeontan.proxy;


import com.bandiyesol.yeontan.client.QuestRenderHandler;
import com.bandiyesol.yeontan.entity.QuestEntity;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import noppes.npcs.client.model.ModelPlayerAlt;
import noppes.npcs.client.renderer.RenderCustomNpc;

public class ClientProxy extends CommonProxy{
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        RenderingRegistry.registerEntityRenderingHandler(QuestEntity.class, manager -> {
            // 1. 모델 생성
            noppes.npcs.client.model.ModelPlayerAlt questModel = new noppes.npcs.client.model.ModelPlayerAlt(0.0F, false);

            // 2. 익명 클래스 생성 시 제네릭 타입을 명시적으로 지정
            return new noppes.npcs.client.renderer.RenderCustomNpc<QuestEntity>(questModel) {

                // 인자 타입을 QuestEntity가 아니라 상위 타입으로 써야 오버라이드가 인식됩니다.
                @Override
                public void renderName(QuestEntity entity, double x, double y, double z) {
                    // 빈 공간으로 두어 기본 이름표 출력을 차단
                }

                @Override
                public void doRender(QuestEntity entity, double x, double y, double z, float entityYaw, float partialTicks) {
                    // 데이터가 완전히 동기화되지 않았을 때 부모 로직이 터지는 것 방지
                    if (entity.display != null) {
                        try {
                            super.doRender(entity, x, y, z, entityYaw, partialTicks);
                        } catch (Exception e) {
                            // 렌더링 중 발생하는 예상치 못한 NPE 무시
                        }
                    }
                }
            };
        });

        MinecraftForge.EVENT_BUS.register(new QuestRenderHandler());
    }
}
