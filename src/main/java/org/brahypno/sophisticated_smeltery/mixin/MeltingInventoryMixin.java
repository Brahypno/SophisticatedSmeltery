package org.brahypno.sophisticated_smeltery.mixin;

import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import org.brahypno.sophisticated_smeltery.furnace.PortableController;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;

@Mixin(value = MeltingModuleInventory.class, remap = false)
public abstract class MeltingInventoryMixin {
    @Shadow @Final private MantleBlockEntity parent;

    // Native capacity setters resize to the world volume. Portable concurrency is
    // bounded by selected backpack slots; those unused modules must not be allocated.
    @Inject(method = "resize", at = @At("HEAD"), cancellable = true)
    private void sophisticatedsmeltery$dimensionsOnly(int size, Consumer<ItemStack> overflow, CallbackInfo ci) {
        if (parent instanceof PortableController portable && portable.sophisticatedsmeltery$isSizing()) ci.cancel();
    }
}
