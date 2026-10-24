package org.brahypno.sophisticated_smeltery;

import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import org.brahypno.sophisticated_smeltery.recipe.CastingTierRecipe;

@GameTestHolder("sophisticatedsmeltery")
@PrefixGameTestTemplate(false)
public final class CastingRecipeGameTests {
    private static TransientCraftingContainer grid() {
        return new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        }, 3, 3);
    }
    private static ItemStack item(String name) { return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("tconstruct", name))); }
    private static TransientCraftingContainer advancedGrid(ItemStack basic) {
        var grid = grid();
        grid.setItem(1, new ItemStack(Items.DIAMOND));
        grid.setItem(3, new ItemStack(Items.GOLD_INGOT));
        grid.setItem(4, basic);
        grid.setItem(5, new ItemStack(Items.GOLD_INGOT));
        for (int slot = 6; slot <= 8; slot++) grid.setItem(slot, new ItemStack(Items.REDSTONE));
        return grid;
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void basicRecipeMatchesTheNineSlotReference(GameTestHelper helper) {
        var recipe = (CraftingRecipe)helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id("casting_upgrade")).orElseThrow();
        var grid = grid();
        grid.setItem(0, item("seared_basin"));
        grid.setItem(1, item("seared_faucet"));
        grid.setItem(2, item("seared_table"));
        grid.setItem(3, item("seared_channel"));
        grid.setItem(4, new ItemStack(ModItems.UPGRADE_BASE.get()));
        grid.setItem(5, item("seared_channel"));
        grid.setItem(6, item("seared_faucet"));
        grid.setItem(7, item("seared_channel"));
        grid.setItem(8, item("seared_faucet"));
        helper.assertTrue(recipe.matches(grid, helper.getLevel()) && recipe.assemble(grid, helper.getLevel().registryAccess()).is(SophisticatedSmeltery.CASTING_UPGRADE.get()),
                "The exact pictured basin/faucet/table/channel/upgrade-base pattern produces the basic upgrade");
        helper.assertTrue(!recipe.isSpecial() && recipe.getIngredients().size() == 9, "Expose normal recipe-book and JEI ingredients");
        grid.setItem(0, ItemStack.EMPTY);
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "The pictured casting basin is a required ingredient");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void advancedRecipePreservesIndependentUpgradeNbt(GameTestHelper helper) {
        var recipe = (CraftingRecipe)helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id("advanced_casting_upgrade")).orElseThrow();
        ItemStack basic = new ItemStack(SophisticatedSmeltery.CASTING_UPGRADE.get());
        CompoundTag state = new CompoundTag();
        state.putBoolean("basin", true);
        state.putInt("timer", 17);
        state.put("inventory", new net.minecraftforge.items.ItemStackHandler(2).serializeNBT());
        state.putString("testMarker", "Keep casting state");
        basic.getOrCreateTag().put("casting", state);
        var grid = advancedGrid(basic);
        helper.assertTrue(recipe.matches(grid, helper.getLevel()) && !recipe.isSpecial(), "Standard diamond/gold/redstone recipe stays visible in the recipe book");
        ItemStack advanced = recipe.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(advanced.is(SophisticatedSmeltery.ADVANCED_CASTING_UPGRADE.get()) && basic.getTag().equals(advanced.getTag()),
                "Native tier-up semantics preserve every stored field");
        advanced.getTag().getCompound("casting").putInt("timer", 99);
        helper.assertTrue(basic.getTag().getCompound("casting").getInt("timer") == 17, "Crafted output must own an independent NBT copy");
        helper.assertTrue(recipe.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty), "Tier-up consumes the basic item instead of returning a duplicate");
        grid.setItem(1, new ItemStack(Items.GOLD_INGOT));
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "The top diamond cannot be replaced by a gold ingot");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void advancedRecipeSurvivesTheClientRecipePacket(GameTestHelper helper) {
        var recipe = (CastingTierRecipe)helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id("advanced_casting_upgrade")).orElseThrow();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SophisticatedSmeltery.CASTING_TIER.get().toNetwork(buffer, recipe);
            var decoded = SophisticatedSmeltery.CASTING_TIER.get().fromNetwork(recipe.getId(), buffer);
            var grid = advancedGrid(new ItemStack(SophisticatedSmeltery.CASTING_UPGRADE.get()));
            helper.assertTrue(decoded.matches(grid, helper.getLevel()) && !decoded.isSpecial()
                            && decoded.getResultItem(helper.getLevel().registryAccess()).is(SophisticatedSmeltery.ADVANCED_CASTING_UPGRADE.get()),
                    "The actual network serializer preserves the displayed pattern and advanced output");
            helper.assertTrue(decoded instanceof ShapedRecipe, "Keep the normal shaped recipe contract for client recipe displays");
        } finally { buffer.release(); }
        helper.succeed();
    }
}
