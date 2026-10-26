package org.brahypno.sophisticated_smeltery.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeItem;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;

public final class StorageCompat {
    private StorageCompat() {}

    public static ItemStack creativeTabIcon() {
        if (SophisticatedSmeltery.MELTING_UPGRADE.isPresent()) return new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get());
        if (SophisticatedSmeltery.STORAGE_MELTING_UPGRADE.isPresent()) return new ItemStack(SophisticatedSmeltery.STORAGE_MELTING_UPGRADE.get());
        return ItemStack.EMPTY;
    }

    public static Item preferredItem(String path) {
        for (String mod : new String[]{"sophisticatedbackpacks", "sophisticatedstorage"}) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(mod, path));
            if (item != null && item != Items.AIR) return item;
        }
        return Items.AIR;
    }

    public static double tankRatio(IStorageWrapper storage) {
        // Storage has no tank upgrade/config of its own; use the core default ratio of 1.
        if (!storage.getStorageType().equals("backpack")) return 1;
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("sophisticatedbackpacks", "tank_upgrade"));
        return item instanceof TankUpgradeItem tank ? tank.getTankUpgradeConfig().stackMultiplierRatio.get() : 1;
    }

    public static double tankMultiplier(IStorageWrapper storage) {
        return 1 + tankRatio(storage) * (storage.getInventoryHandler().getStackSizeMultiplier() - 1);
    }
}
