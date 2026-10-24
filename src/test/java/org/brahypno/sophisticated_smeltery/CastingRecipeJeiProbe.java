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
}
