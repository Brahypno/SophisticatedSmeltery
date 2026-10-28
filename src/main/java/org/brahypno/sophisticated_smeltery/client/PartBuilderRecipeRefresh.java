package org.brahypno.sophisticated_smeltery.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.upgrade.PartBuilderUpgradeWrapper;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class PartBuilderRecipeRefresh {
    @SubscribeEvent public static void recipesUpdated(RecipesUpdatedEvent event) {
        PartBuilderUpgradeWrapper.invalidateRecipes();
        org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeWrapper.invalidateRecipes();
    }
}
