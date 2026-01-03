package com.bandiyesol.yeontan.entity;

import com.bandiyesol.yeontan.Yeontan;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

@Mod.EventBusSubscriber(modid = "yeontan")
public class QuestRegisterHandler {

    @SubscribeEvent
    public static void onRegisterEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(
                EntityEntryBuilder.create()
                        .entity(QuestEntity.class)
                        .id(new ResourceLocation(Yeontan.MODID, "quest_entity"), 100)
                        .name("quest_entity")
                        .tracker(64, 3, true)
                        .factory(QuestEntity::new)
                        .build()
        );
    }
}
