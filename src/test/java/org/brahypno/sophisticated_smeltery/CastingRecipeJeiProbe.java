package org.brahypno.sophisticated_smeltery;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/** Development-only probe of JEI's visible crafting list, not a production integration. */
@JeiPlugin
public final class CastingRecipeJeiProbe implements IModPlugin {
    private static IJeiRuntime runtime;
    @Override public ResourceLocation getPluginUid() { return SophisticatedSmeltery.id("casting_recipe_test"); }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) { CastingRecipeJeiProbe.runtime = runtime; }
    @Override public void onRuntimeUnavailable() { runtime = null; }
    static boolean visible(ResourceLocation id) {
        return runtime != null && runtime.getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).get()
                .anyMatch(recipe -> recipe.getId().equals(id));
    }
    static void verifyUniqueTierRecipes() {
        if (runtime == null) throw new IllegalStateException("JEI runtime is unavailable");
        for (var item : java.util.List.of(SophisticatedSmeltery.ADVANCED_SEARED_CASTING_UPGRADE.get(),
                SophisticatedSmeltery.ADVANCED_SCORCHED_CASTING_UPGRADE.get())) {
            long count = runtime.getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).get()
                    .filter(recipe -> recipe.getResultItem(net.minecraft.client.Minecraft.getInstance().level.registryAccess()).is(item)).count();
            if (count != 1) throw new IllegalStateException("Expected one JEI tier recipe for " + item + ", got " + count);
        }
        SophisticatedSmeltery.LOGGER.info("JEI TIER RECIPES PASSED: one recipe per advanced casting upgrade");
    }
}
