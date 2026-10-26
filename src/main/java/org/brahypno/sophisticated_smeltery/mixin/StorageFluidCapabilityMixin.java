package org.brahypno.sophisticated_smeltery.mixin;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.ContentsFilteredFluidHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import org.brahypno.sophisticated_smeltery.furnace.FurnaceFluidHandler;
import org.brahypno.sophisticated_smeltery.compat.StorageFluidInvalidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StorageBlockEntity.class, remap = false)
public abstract class StorageFluidCapabilityMixin implements StorageFluidInvalidator {
    @Shadow private boolean chunkBeingUnloaded;
    @Shadow private void invalidateStorageCap() { throw new AssertionError(); }
    @Unique private final Map<Direction, LazyOptional<IFluidHandler>> sophisticatedsmeltery$sides = new HashMap<>();
    @Unique private IFluidHandler sophisticatedsmeltery$source;

    @Override public void sophisticatedsmeltery$invalidateFluids() { invalidateStorageCap(); }

    @Inject(method = "onChunkUnloaded", at = @At("RETURN"))
    private void sophisticatedsmeltery$unload(CallbackInfo ci) { invalidateStorageCap(); }

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true)
    private <T> void sophisticatedsmeltery$capability(Capability<T> cap, @Nullable Direction side,
                                                      CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (cap != ForgeCapabilities.FLUID_HANDLER) return;
        var block = (StorageBlockEntity)(Object)this;
        if (block.isRemoved() || chunkBeingUnloaded || block.getLevel() == null || block instanceof WoodStorageBlockEntity wood && wood.isPacked()
                || block.getLinkedStorageEndpointData() != null) return;
        if (!(block.getStorageWrapper().getFluidHandler().orElse(null) instanceof FurnaceFluidHandler handler)) return;
        if (sophisticatedsmeltery$source != handler) {
            sophisticatedsmeltery$clear();
            sophisticatedsmeltery$source = handler;
        }
        var sided = sophisticatedsmeltery$sides.computeIfAbsent(side, direction -> LazyOptional.of(() -> {
            var fluid = handler.withFuelPreference(() -> {
                if (direction == null || !direction.getAxis().isHorizontal()) return false;
                var state = block.getBlockState();
                Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                        ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                        : state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : Direction.NORTH;
                return facing.getAxis().isHorizontal() && direction.getAxis() != facing.getAxis();
            });
            return new ContentsFilteredFluidHandler(fluid, block::isLocked);
        }));
        cir.setReturnValue(sided.cast());
    }

    @Inject(method = "invalidateStorageCap", at = @At("HEAD"))
    private void sophisticatedsmeltery$invalidate(CallbackInfo ci) { sophisticatedsmeltery$clear(); }

    @Unique private void sophisticatedsmeltery$clear() {
        sophisticatedsmeltery$sides.values().forEach(LazyOptional::invalidate);
        sophisticatedsmeltery$sides.clear();
        sophisticatedsmeltery$source = null;
    }
}
