package org.brahypno.sophisticated_smeltery.furnace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;

public final class ControllerBinding {
    public static final String CONTROLLER = "controller";
    public static final String STATE = "furnace";
    private static final Map<Block, Boolean> SUPPORTED = new ConcurrentHashMap<>();
    private static final Map<Block, Component> NAMES = new ConcurrentHashMap<>();
    private ControllerBinding() {}

    public static ItemStack getController(ItemStack upgrade) {
        CompoundTag tag = upgrade.getTag();
        return tag == null ? ItemStack.EMPTY : ItemStack.of(tag.getCompound(CONTROLLER));
    }

    public static boolean supports(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) return false;
        return SUPPORTED.computeIfAbsent(item.getBlock(), block -> create(stack) != null);
    }

    public static Component getControllerName(ItemStack stack) {
        if (stack.hasCustomHoverName() || !(stack.getItem() instanceof BlockItem item)) return stack.getHoverName();
        // Cache the translation component, never a resolved string: language/resource-pack changes still apply.
        return NAMES.computeIfAbsent(item.getBlock(), block -> {
            HeatingStructureBlockEntity controller = create(stack);
            return controller == null ? stack.getHoverName() : controller.getDisplayName();
        });
    }

    @Nullable
    public static HeatingStructureBlockEntity create(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof EntityBlock entityBlock)) return null;
        BlockEntity entity = entityBlock.newBlockEntity(BlockPos.ZERO, item.getBlock().defaultBlockState());
        return entity instanceof HeatingStructureBlockEntity controller && controller.getMeltingInventory() != null ? controller : null;
    }

    public static ItemStack bind(ItemStack upgrade, ItemStack controller) {
        ItemStack result = upgrade.copyWithCount(1);
        result.getOrCreateTag().put(CONTROLLER, controller.copyWithCount(1).save(new CompoundTag()));
        return result;
    }

    public static boolean cleanForBinding(ItemStack upgrade) {
        CompoundTag tag = upgrade.getTag();
        if (tag == null) return true;
        CompoundTag state = tag.getCompound(STATE);
        if (!state.getCompound("tank").getList("fluids", Tag.TAG_COMPOUND).isEmpty()
                || !state.getCompound("components").getList("Items", Tag.TAG_COMPOUND).isEmpty()
                || !state.getCompound("containers").getList("Items", Tag.TAG_COMPOUND).isEmpty()
                || state.getCompound("fuel").getInt("fuel") > 0) return false;
        for (Tag entry : state.getCompound("fuel").getList("pausedBurns", Tag.TAG_COMPOUND)) {
            if (((CompoundTag)entry).getInt("fuel") > 0) return false;
        }
        for (Tag entry : state.getList("progress", Tag.TAG_COMPOUND)) {
            int time = ((CompoundTag)entry).getInt("time");
            if (time != 0) return false;
        }
        return true;
    }
}
