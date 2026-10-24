package org.brahypno.sophisticated_smeltery.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ControllerIconRegistration {
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation location = new ModelResourceLocation(SophisticatedSmeltery.id("melting_upgrade"), "inventory");
        BakedModel original = event.getModels().get(location);
        if (original != null) event.getModels().put(location, new ControllerIconModel(original));
    }
}
