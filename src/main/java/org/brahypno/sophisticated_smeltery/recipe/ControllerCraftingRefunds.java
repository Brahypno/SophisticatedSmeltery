package org.brahypno.sophisticated_smeltery.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeWrapper;

/** Extra refunds can exceed the crafting grid's one-remainder-per-slot capacity. */
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID)
public final class ControllerCraftingRefunds {
    private ControllerCraftingRefunds() {}

    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getCrafting().getItem() instanceof MeltingUpgradeItem
                || event.getCrafting().getItem() instanceof AlloyerUpgradeItem)) return;
        for (int slot = 0; slot < event.getInventory().getContainerSize(); slot++) {
            ItemStack input = event.getInventory().getItem(slot);
            if (!(input.getItem() instanceof MeltingUpgradeItem || input.getItem() instanceof AlloyerUpgradeItem)) continue;
            // Tier crafting carries the inventories forward instead of returning them.
            if (java.util.Objects.equals(input.getTag(), event.getCrafting().getTag())) continue;
            for (ItemStack returned : input.getItem() instanceof AlloyerUpgradeItem ? AlloyerUpgradeWrapper.storedItems(input) : ControllerBinding.getStoredItems(input)) {
                event.getEntity().getInventory().placeItemBackInInventory(returned);
            }
        }
    }
}
