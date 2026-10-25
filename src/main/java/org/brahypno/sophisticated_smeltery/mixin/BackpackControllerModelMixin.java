package org.brahypno.sophisticated_smeltery.mixin;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackDynamicModel;
import org.brahypno.sophisticated_smeltery.client.BackpackControllerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BackpackDynamicModel.BackpackBakedModel.class, remap = false)
public abstract class BackpackControllerModelMixin {
    @Unique @Nullable private BlockState sophisticatedsmeltery$controller;

    @Inject(method = "getRenderPasses", at = @At("HEAD"))
    private void sophisticatedsmeltery$controllerItem(ItemStack stack, boolean fabulous,
                                                      CallbackInfoReturnable<List<BakedModel>> cir) {
        sophisticatedsmeltery$controller = BackpackControllerModel.controller(new BackpackWrapper(stack).getRenderInfo());
    }

    @Inject(method = "getModelData", at = @At("RETURN"), cancellable = true)
    private void sophisticatedsmeltery$controllerData(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                                      ModelData data, CallbackInfoReturnable<ModelData> cir) {
        BlockState controller = level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack
                ? BackpackControllerModel.controller(backpack.getBackpackWrapper().getRenderInfo()) : null;
        cir.setReturnValue(cir.getReturnValue().derive().with(BackpackControllerModel.CONTROLLER, controller).build());
    }

    @Inject(method = "getQuads", at = @At("RETURN"), cancellable = true)
    private void sophisticatedsmeltery$controllerQuads(@Nullable BlockState state, @Nullable Direction side,
            RandomSource random, ModelData data, @Nullable RenderType renderType, CallbackInfoReturnable<List<BakedQuad>> cir) {
        if (side != null || renderType == RenderType.translucent()) return;
        BlockState controller = state == null ? sophisticatedsmeltery$controller : data.get(BackpackControllerModel.CONTROLLER);
        if (controller == null) return;
        Direction facing = state == null ? Direction.NORTH : state.getValue(BackpackBlock.FACING);
        List<BakedQuad> quads = new ArrayList<>(cir.getReturnValue());
        quads.addAll(BackpackControllerModel.quads(controller, facing));
        cir.setReturnValue(quads);
    }
}
