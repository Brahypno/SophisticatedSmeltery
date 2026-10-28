package org.brahypno.sophisticated_smeltery.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.tables.TinkerTables;

/** Keeps the native workstation stack, including material variants and retexturing data. */
public final class TinkerStationAppearance {
    private static final String SOURCE = "tinkerStationBlock";
    private TinkerStationAppearance() { }

    public static boolean accepts(ItemStack upgrade, ItemStack workstation) {
        if (!(upgrade.getItem() instanceof TinkerStationUpgradeItem item)) return false;
        return item.isAdvanced()
                ? workstation.is(TinkerTables.tinkersAnvil.asItem()) || workstation.is(TinkerTables.scorchedAnvil.asItem())
                : workstation.is(TinkerTables.tinkerStation.asItem());
    }

    public static ItemStack bound(ItemStack upgrade) {
        if (!upgrade.hasTag() || !upgrade.getTag().contains(SOURCE)) return ItemStack.EMPTY;
        ItemStack source = ItemStack.of(upgrade.getTag().getCompound(SOURCE).copy());
        return accepts(upgrade, source) ? source.copyWithCount(1) : ItemStack.EMPTY;
    }

    public static ItemStack source(ItemStack upgrade) {
        ItemStack source = bound(upgrade);
        if (!source.isEmpty()) return source;
        return new ItemStack(((TinkerStationUpgradeItem)upgrade.getItem()).isAdvanced()
                ? TinkerTables.tinkersAnvil.asItem() : TinkerTables.tinkerStation.asItem());
    }

    public static ItemStack bind(ItemStack upgrade, ItemStack workstation) {
        if (!accepts(upgrade, workstation)) return ItemStack.EMPTY;
        ItemStack result = upgrade.copyWithCount(1);
        result.getOrCreateTag().put(SOURCE, workstation.copyWithCount(1).save(new CompoundTag()));
        return result;
    }
}
