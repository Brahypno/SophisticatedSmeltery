package org.brahypno.sophisticated_smeltery.furnace;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock;

/** Dimension metadata only. Fuel presence comes from backpack handlers, without fake tank positions. */
final class PortableStructureData extends HeatingStructureMultiblock.StructureData {
    private final BooleanSupplier hasFuelSource;
    PortableStructureData(BlockPos max, boolean floor, boolean frame, boolean ceiling, BooleanSupplier hasFuelSource) {
        super(BlockPos.ZERO, max, Set.of(), floor, frame, ceiling, List.of());
        this.hasFuelSource = hasFuelSource;
    }
    @Override public boolean hasTanks() { return hasFuelSource.getAsBoolean(); }
}
