package org.brahypno.sophisticated_smeltery.mixin;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import org.brahypno.sophisticated_smeltery.furnace.FurnaceFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The backpack's upgrade wrapper API does not carry the queried block face. */
@Mixin(value = BackpackBlockEntity.class, remap = false)
public abstract class BackpackFluidSideMixin {
    @Unique private final Map<Direction, LazyOptional<IFluidHandler>> sophisticatedsmeltery$fluidSides = new EnumMap<>(Direction.class);
    @Unique @Nullable private LazyOptional<IFluidHandler> sophisticatedsmeltery$fluidSource;

    @Inject(method = "getCapability", at = @At("RETURN"), cancellable = true)
    private <T> void sophisticatedsmeltery$routeSide(Capability<T> capability, @Nullable Direction side,
                                                    CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (capability != ForgeCapabilities.FLUID_HANDLER || side == null) return;
        LazyOptional<IFluidHandler> source = cir.getReturnValue().cast();
        if (!(source.orElse(null) instanceof FurnaceFluidHandler handler)) return;
        if (sophisticatedsmeltery$fluidSource != source) {
            sophisticatedsmeltery$clearFluidSides();
            sophisticatedsmeltery$fluidSource = source;
            source.addListener(ignored -> sophisticatedsmeltery$clearFluidSides());
        }
        LazyOptional<IFluidHandler> sided = sophisticatedsmeltery$fluidSides.computeIfAbsent(side, direction ->
                LazyOptional.of(() -> new FurnaceFluidHandler(handler.furnace(), handler.next(), handler.container(), () -> {
                    var state = ((BackpackBlockEntity)(Object)this).getBlockState();
                    return state.hasProperty(BackpackBlock.FACING) && direction.getAxis().isHorizontal()
                            && direction.getAxis() != state.getValue(BackpackBlock.FACING).getAxis();
                })));
        cir.setReturnValue(sided.cast());
    }

    @Unique private void sophisticatedsmeltery$clearFluidSides() {
        sophisticatedsmeltery$fluidSides.values().forEach(LazyOptional::invalidate);
        sophisticatedsmeltery$fluidSides.clear();
        sophisticatedsmeltery$fluidSource = null;
    }
}
