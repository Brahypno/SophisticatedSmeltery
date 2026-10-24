package org.brahypno.sophisticated_smeltery;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.sophisticated_smeltery.casting.PortableCasting;
import org.brahypno.sophisticated_smeltery.casting.PortableCasting.Mode;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

@GameTestHolder("sophisticatedsmeltery")
@PrefixGameTestTemplate(false)
public final class CastingGameTests {
    private static ItemStack cast(String name) {
        ItemStack item = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("tconstruct", name)));
        if (item.isEmpty()) throw new IllegalStateException("Missing native cast " + name);
        return item;
    }

    private static PortableCasting table(ItemStack cast) {
        var engine = new PortableCasting(new CompoundTag(), () -> {});
        engine.inventory().setStackInSlot(0, cast);
        return engine;
    }

    private static FluidTank iron(int amount) {
        var source = new FluidTank(10000);
        source.fill(new FluidStack(TinkerFluids.moltenIron.get(), amount), FluidAction.EXECUTE);
        return source;
    }

    private static void compareNative(GameTestHelper helper, Mode mode, ItemStack input, FluidStack fluid) {
        var block = mode == Mode.TABLE ? TinkerSmeltery.searedTable : TinkerSmeltery.searedBasin;
        helper.setBlock(BlockPos.ZERO, block.get());
        var nativeTable = (CastingBlockEntity)helper.getBlockEntity(BlockPos.ZERO);
        nativeTable.setItem(0, input.copy());
        var portable = table(input.copy());
        portable.setMode(mode);
        FluidTank source = new FluidTank(10000);
        source.fill(fluid, FluidAction.EXECUTE);
        int nativeAmount = nativeTable.getTank().fill(fluid, FluidAction.EXECUTE);
        int portableAmount = portable.pour(helper.getLevel(), source);
        helper.assertTrue(nativeAmount > 0 && nativeAmount == portableAmount, "Use the native recipe's exact fluid amount");
        helper.assertTrue(nativeTable.getCoolingTime() == portable.coolingTime(), "Preserve native cooling time");
        helper.assertTrue(source.getFluidAmount() + portable.fluid().getAmount() == fluid.getAmount(), "Pouring must conserve fluid");
        int ticks = Math.max(1, nativeTable.getCoolingTime());
        for (int tick = 0; tick < ticks; tick++) {
            CastingBlockEntity.SERVER_TICKER.tick(helper.getLevel(), nativeTable.getBlockPos(), nativeTable.getBlockState(), nativeTable);
            portable.tick(helper.getLevel());
            if (tick < ticks - 1) helper.assertTrue(portable.inventory().getStackInSlot(1).isEmpty(), "Never finish before native cooling completes");
        }
        for (int slot = 0; slot < 2; slot++) helper.assertTrue(ItemStack.matches(nativeTable.getItem(slot), portable.inventory().getStackInSlot(slot)),
                "Preserve native output, cast consumption, switchSlots and all result NBT at slot " + slot);
        helper.assertTrue(portable.fluid().isEmpty() && nativeTable.getTank().isEmpty(), "Exactly one recipe consumes its casting fluid");
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void reusableCastMatchesNativeTable(GameTestHelper helper) {
        compareNative(helper, Mode.TABLE, cast("ingot_cast"), new FluidStack(TinkerFluids.moltenIron.get(), 180));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void consumedCastMatchesNativeTable(GameTestHelper helper) {
        compareNative(helper, Mode.TABLE, cast("ingot_sand_cast"), new FluidStack(TinkerFluids.moltenIron.get(), 180));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void basinBlockMatchesNativeCooling(GameTestHelper helper) {
        compareNative(helper, Mode.BASIN, ItemStack.EMPTY, new FluidStack(TinkerFluids.moltenIron.get(), 1000));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void dynamicTankFillingMatchesNativeNbtAndSlotSwitch(GameTestHelper helper) {
        var tank = new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.INGOT_TANK));
        tank.getOrCreateTag().putInt("retained", 1729);
        compareNative(helper, Mode.BASIN, tank, new FluidStack(TinkerFluids.moltenIron.get(), 1000));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void partialCastingReturnsOnlyAcceptedFluid(GameTestHelper helper) {
        var casting = table(cast("ingot_cast"));
        var source = iron(45);
        helper.assertTrue(casting.pour(helper.getLevel(), source) == 45 && source.isEmpty(), "Allow a real half-full recipe");
        for (int tick = 0; tick < 100; tick++) casting.tick(helper.getLevel());
        helper.assertTrue(!casting.isCooling() && casting.timer() == 0 && casting.inventory().getStackInSlot(1).isEmpty(),
                "Insufficient fluid must not start cooling or produce an item");
        helper.assertTrue(!casting.setMode(Mode.BASIN) && casting.inventory().extractItem(0, 1, false).isEmpty(),
                "Mode and cast stay fixed while liquid remains");
        FluidTank small = new FluidTank(20);
        helper.assertTrue(casting.returnFluid(helper.getLevel(), small) == 20 && casting.fluid().getAmount() == 25, "Return only the capacity accepted by the destination");
        helper.assertTrue(casting.returnFluid(helper.getLevel(), small) == 0 && casting.fluid().getAmount() == 25, "Full destination cannot void the remaining liquid");
        FluidTank remaining = new FluidTank(1000);
        helper.assertTrue(casting.returnFluid(helper.getLevel(), remaining) == 25 && casting.fluid().isEmpty(), "Return the rest without losses");
        helper.assertTrue(small.getFluidAmount() + remaining.getFluidAmount() == 45 && casting.inventory().getStackInSlot(0).is(cast("ingot_cast").getItem()),
                "Returning partial liquid cannot consume or duplicate the cast");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void coolingAndPartialProgressSurviveNbtReload(GameTestHelper helper) {
        var casting = table(cast("ingot_cast"));
        var source = iron(180);
        helper.assertTrue(casting.pour(helper.getLevel(), source) == 90, "Take one recipe, leaving the other ingot in the furnace");
        for (int tick = 0; tick < 20; tick++) casting.tick(helper.getLevel());
        var restored = new PortableCasting(casting.save(), () -> {});
        // A GUI action can arrive before the first host tick has rebound a loaded recipe.
        helper.assertTrue(restored.returnFluid(helper.getLevel(), source) == 0
                        && restored.timer() == 20 && restored.fluid().getAmount() == 90,
                "Keep exact cooldown and refuse to cancel an executing recipe");
        int remaining = restored.coolingTime() - restored.timer();
        for (int tick = 0; tick < remaining; tick++) restored.tick(helper.getLevel());
        helper.assertTrue(restored.inventory().getStackInSlot(1).is(Items.IRON_INGOT) && restored.inventory().getStackInSlot(1).getCount() == 1,
                "Finish exactly once after reload");
        helper.assertTrue(restored.pour(helper.getLevel(), source) == 0 && source.getFluidAmount() == 90,
                "Occupied independent output blocks another pour and cannot be remelted");
        restored.inventory().extractItem(1, 1, false);
        source.drain(45, FluidAction.EXECUTE);
        restored.pour(helper.getLevel(), source);
        var partial = new PortableCasting(restored.save(), () -> {});
        helper.assertTrue(partial.fluid().getAmount() == 45 && partial.returnFluid(helper.getLevel(), source) == 45,
                "A half-filled saved cast remains recoverable after reload");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery", batch = "casting_reload")
    public static void recipePreviewTracksModeCastAndDatapackChanges(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        var casting = table(cast("ingot_cast"));
        FluidStack iron = new FluidStack(TinkerFluids.moltenIron.get(), 90);
        helper.assertTrue(casting.preview(helper.getLevel(), iron).is(Items.IRON_INGOT), "Preview the actual table recipe without consuming anything");
        casting.setMode(Mode.BASIN);
        casting.inventory().setStackInSlot(0, ItemStack.EMPTY);
        helper.assertTrue(casting.preview(helper.getLevel(), iron).is(Items.IRON_BLOCK), "Changing mode and cast must invalidate the preview");
        try {
            manager.replaceRecipes(List.of());
            PortableCasting.invalidateRecipes();
            helper.assertTrue(casting.preview(helper.getLevel(), iron).isEmpty(), "Data reload must remove a cached preview even when its input is unchanged");
            helper.assertTrue(casting.fluid().isEmpty() && casting.inventory().getStackInSlot(1).isEmpty(), "Previews cannot hold fluid or create actual output");
        } finally { manager.replaceRecipes(original); PortableCasting.invalidateRecipes(); }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery", batch = "casting_reload")
    public static void removedCastingRecipeCannotProduceOrLoseFluid(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        var casting = table(cast("ingot_cast"));
        var source = iron(90);
        casting.pour(helper.getLevel(), source);
        for (int tick = 0; tick < 20; tick++) casting.tick(helper.getLevel());
        try {
            manager.replaceRecipes(List.of());
            PortableCasting.invalidateRecipes();
            for (int tick = 0; tick < 100; tick++) casting.tick(helper.getLevel());
            helper.assertTrue(casting.inventory().getStackInSlot(1).isEmpty() && casting.fluid().getAmount() == 90,
                    "Removed recipe cannot finish from cache or void the paid casting liquid");
            helper.assertTrue(casting.returnFluid(helper.getLevel(), source) == 90 && source.getFluidAmount() == 90,
                    "A recipe invalidated by reload can return all its liquid");
        } finally { manager.replaceRecipes(original); PortableCasting.invalidateRecipes(); }
        helper.succeed();
    }
}
