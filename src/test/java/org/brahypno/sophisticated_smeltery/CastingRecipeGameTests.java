package org.brahypno.sophisticated_smeltery;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import org.brahypno.sophisticated_smeltery.recipe.CastingTierRecipe;

/** Keep the custom tier-transfer contract; vanilla JSON pattern checks are not duplicated here. */
@PrefixGameTestTemplate(false)
public final class CastingRecipeGameTests {
    private static TransientCraftingContainer grid(ItemStack basic) {
        var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        }, 3, 3);
        grid.setItem(1, new ItemStack(Items.DIAMOND));
        grid.setItem(3, new ItemStack(Items.GOLD_INGOT));
        grid.setItem(4, basic);
        grid.setItem(5, new ItemStack(Items.GOLD_INGOT));
        for (int slot = 6; slot <= 8; slot++) grid.setItem(slot, new ItemStack(Items.REDSTONE));
        return grid;
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void tierUpgradePreservesIndependentStateAndClientPacket(GameTestHelper helper) {
        for (boolean scorched : new boolean[]{false, true}) {
            String prefix = scorched ? "advanced_scorched_" : "advanced_seared_";
            var recipe = (CastingTierRecipe)helper.getLevel().getRecipeManager()
                    .byKey(SophisticatedSmeltery.id(prefix + "casting_upgrade")).orElseThrow();
            ItemStack basic = new ItemStack(scorched ? SophisticatedSmeltery.SCORCHED_CASTING_UPGRADE.get() : SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get());
            var state = new CompoundTag();
            state.putBoolean("basin", true);
            state.putInt("timer", 17);
            state.put("inventory", new ItemStackHandler(2).serializeNBT());
            state.put("lockedCast", new ItemStack(Items.IRON_INGOT).save(new CompoundTag()));
            basic.getOrCreateTag().put("casting", state);
            basic.getOrCreateTag().putBoolean("castingDisabled", true);
            basic.getOrCreateTag().putBoolean("outputToStorage", true);
            var grid = grid(basic);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                SophisticatedSmeltery.CASTING_TIER.get().toNetwork(buffer, recipe);
                var decoded = SophisticatedSmeltery.CASTING_TIER.get().fromNetwork(recipe.getId(), buffer);
                helper.assertTrue(decoded.matches(grid, helper.getLevel()) && !decoded.isSpecial(), "Tier-up stays visible and valid after the client recipe packet");
                ItemStack advanced = decoded.assemble(grid, helper.getLevel().registryAccess());
                helper.assertTrue(advanced.is(scorched ? SophisticatedSmeltery.ADVANCED_SCORCHED_CASTING_UPGRADE.get() : SophisticatedSmeltery.ADVANCED_SEARED_CASTING_UPGRADE.get())
                        && basic.getTag().equals(advanced.getTag()), "Transfer all upgrade state to the matching advanced tier");
                advanced.getTag().getCompound("casting").putInt("timer", 99);
                advanced.getTag().getCompound("casting").remove("lockedCast");
                helper.assertTrue(basic.getTag().getCompound("casting").getInt("timer") == 17
                        && basic.getTag().getCompound("casting").contains("lockedCast"), "Crafted output owns its NBT independently");
                helper.assertTrue(decoded.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty), "Consume the basic upgrade without returning a duplicate");
            } finally { buffer.release(); }
        }
        helper.succeed();
    }
}
