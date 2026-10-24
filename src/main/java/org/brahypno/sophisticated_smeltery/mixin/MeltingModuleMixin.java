package org.brahypno.sophisticated_smeltery.mixin;

import java.util.function.Predicate;
import org.brahypno.sophisticated_smeltery.furnace.BackpackSlotInput;
import org.brahypno.sophisticated_smeltery.furnace.BoundMeltingModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModule;

@Mixin(value = MeltingModule.class, remap = false)
public abstract class MeltingModuleMixin implements BoundMeltingModule {
    @Shadow @Final private int slotIndex;
    @Shadow private IMeltingRecipe lastRecipe;
    @Unique private BackpackSlotInput sophisticatedsmeltery$input;
    public void sophisticatedsmeltery$bind(BackpackSlotInput input) { sophisticatedsmeltery$input = input; }
    public void sophisticatedsmeltery$invalidateRecipe() { lastRecipe = null; }

    @Redirect(method = "setStack", at = @At(value = "FIELD", target = "Lslimeknights/tconstruct/smeltery/block/entity/module/MeltingModule;slotIndex:I"))
    private int sophisticatedsmeltery$noWorldPacket(MeltingModule self) {
        return sophisticatedsmeltery$input == null ? slotIndex : -1;
    }

    @Redirect(method = "onItemFinishedHeating", at = @At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))
    private boolean sophisticatedsmeltery$consumeBackpackInput(Predicate<IMeltingRecipe> output, Object recipe) {
        return sophisticatedsmeltery$input == null ? output.test((IMeltingRecipe)recipe)
                : sophisticatedsmeltery$input.finish(output, (IMeltingRecipe)recipe);
    }
}
