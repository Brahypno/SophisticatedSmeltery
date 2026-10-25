package org.brahypno.sophisticated_smeltery.compat;

import java.util.ArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.Config;
import org.brahypno.esotericismtinker.smeltery.block.entity.module.ByproductEntityMeltingModule;
import org.brahypno.esotericismtinker.smeltery.block.entity.controller.TransmuteBlockEntity;
import org.brahypno.esotericismtinker.smeltery.recipe.entitymelting.ByproductEntityMeltingRecipeCache;
import org.brahypno.sophisticated_smeltery.furnace.PortableMobMelting.Melt;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipeCache;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;

/** Loaded only for the optional Transmute controller. Recipes and multipliers remain native. */
public final class TransmuteMobMelting {
    private TransmuteMobMelting() {}

    public static boolean supports(HeatingStructureBlockEntity controller) { return controller instanceof TransmuteBlockEntity; }

    public static Melt recipe(ServerLevel level, LivingEntity entity) {
        var custom = ByproductEntityMeltingRecipeCache.findRecipe(level.getRecipeManager(), entity.getType());
        if (custom != null) {
            var fluids = new ArrayList<>(custom.getByproducts(entity));
            fluids.add(0, custom.getOutput(entity));
            return new Melt(custom.getDamage(), fluids);
        }
        var recipe = EntityMeltingRecipeCache.findRecipe(level.getRecipeManager(), entity.getType());
        var fluid = (recipe == null ? ByproductEntityMeltingModule.getDefaultFluid() : recipe.getOutput(entity)).copy();
        fluid.setAmount((int)Math.min(Integer.MAX_VALUE, (long)fluid.getAmount() * Config.BYPRODUCT_ENTITY_MELTING_MULTIPLIER.get()));
        return new Melt(recipe == null ? 2 : recipe.getDamage(), java.util.List.of(fluid));
    }
}
