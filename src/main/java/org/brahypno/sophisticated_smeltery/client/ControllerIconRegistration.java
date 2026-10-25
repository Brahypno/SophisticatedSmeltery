package org.brahypno.sophisticated_smeltery.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ControllerIconRegistration {
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        BackpackControllerModel.clearCache();
        ModelResourceLocation location = new ModelResourceLocation(SophisticatedSmeltery.id("melting_upgrade"), "inventory");
        BakedModel original = event.getModels().get(location);
        BakedModel brick = event.getModels().get(new ModelResourceLocation(TinkerSmeltery.searedBrick.getId(), "inventory"));
        if (original != null && brick != null) event.getModels().put(location, new ControllerIconModel(original, brick));
        BakedModel advanced = event.getModels().get(new ModelResourceLocation(
                new net.minecraft.resources.ResourceLocation("sophisticatedbackpacks", "advanced_magnet_upgrade"), "inventory"));
        for (String item : java.util.List.of("seared_casting_upgrade", "advanced_seared_casting_upgrade", "scorched_casting_upgrade", "advanced_scorched_casting_upgrade")) {
            var state = (item.contains("scorched") ? TinkerSmeltery.scorchedTable : TinkerSmeltery.searedTable).get().defaultBlockState();
            BakedModel table = event.getModels().get(BlockModelShaper.stateToModelLocation(state));
            var modelId = new ModelResourceLocation(SophisticatedSmeltery.id(item), "inventory");
            BakedModel plate = event.getModels().get(modelId);
            if (plate != null && table != null) event.getModels().put(modelId, new CastingIconModel(plate, table, state,
                    item.startsWith("advanced_") && advanced != null ? advanced.getParticleIcon() : null));
        }
    }
}
