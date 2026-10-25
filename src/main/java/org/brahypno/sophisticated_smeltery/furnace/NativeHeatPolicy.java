package org.brahypno.sophisticated_smeltery.furnace;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import javax.annotation.Nullable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;

/** Optional world-independent initialization; reflection is resolved once per controller. */
record NativeHeatPolicy(boolean automaticAlloying, @Nullable Method clearWorldComponents,
                        @Nullable TransmuteComponents components) {
    static NativeHeatPolicy find(HeatingStructureBlockEntity controller) {
        Class<?> heatOwner = null;
        Method reset = null;
        TransmuteComponents components = null;
        for (Class<?> type = controller.getClass(); type != HeatingStructureBlockEntity.class; type = type.getSuperclass()) {
            if (heatOwner == null) {
                try { type.getDeclaredMethod("heat"); heatOwner = type; }
                catch (NoSuchMethodException ignored) { }
            }
            if (type.getName().equals("org.brahypno.esotericismtinker.smeltery.block.entity.controller.TransmuteBlockEntity")) {
                try {
                    reset = type.getDeclaredMethod("checkHeatBuff");
                    reset.setAccessible(true);
                    components = new TransmuteComponents(type);
                }
                catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported Transmute heat-component API", e); }
            }
        }
        return new NativeHeatPolicy(heatOwner == SmelteryBlockEntity.class, reset, components);
    }

    void updateComponents(HeatingStructureBlockEntity controller, IItemHandler inventory, boolean formed) {
        if (components == null) return;
        int heaters = 0, accelerators = 0;
        try {
            if (formed) for (int slot = 0; slot < inventory.getSlots(); slot++) {
                var stack = inventory.getStackInSlot(slot);
                if (stack.getItem() instanceof BlockItem item) {
                    if ((boolean)components.isHeater.invoke(controller, item.getBlock())) heaters += stack.getCount();
                    if ((boolean)components.isAccelerator.invoke(controller, item.getBlock())) accelerators += stack.getCount();
                }
            }
            components.heater.setInt(controller, heaters);
            components.accelerator.setInt(controller, accelerators);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot update Transmute heat components", e); }
    }

    int temperatureBonus(HeatingStructureBlockEntity controller) {
        if (components == null) return 0;
        try { return (int)components.temperature.invoke(controller); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot read Transmute heating temperature", e); }
    }

    void updateControls(HeatingStructureBlockEntity controller, int mode, boolean alloying) {
        if (components == null) return;
        try {
            components.meltType.invoke(controller.getMeltingInventory(), components.modes[mode]);
            components.allowAlloying.setBoolean(controller, alloying);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot update Transmute switches", e); }
    }

    /** Resolve the optional API once; its own methods retain tags and configured effect values. */
    private record TransmuteComponents(Field heater, Field accelerator, Method isHeater, Method isAccelerator, Method temperature,
                                      Field allowAlloying, Method meltType, Object[] modes) {
        TransmuteComponents(Class<?> type) throws ReflectiveOperationException {
            this(type.getDeclaredField("heater"), type.getDeclaredField("accelerator"),
                    type.getDeclaredMethod("isHeaterBlock", Block.class), type.getDeclaredMethod("isAccelBlock", Block.class),
                    type.getDeclaredMethod("getHeatTemperatureBuff"), type.getDeclaredField("allowAlloying"),
                    Class.forName("org.brahypno.esotericismtinker.smeltery.block.entity.module.SuperByproductMeltingModuleInventory").getMethod("updateMeltType",
                            Class.forName("org.brahypno.esotericismtinker.smeltery.block.entity.module.SuperByproductMeltingModuleInventory$MeltType")),
                    Class.forName("org.brahypno.esotericismtinker.smeltery.block.entity.module.SuperByproductMeltingModuleInventory$MeltType").getEnumConstants());
            heater.setAccessible(true);
            accelerator.setAccessible(true);
            isHeater.setAccessible(true);
            isAccelerator.setAccessible(true);
            temperature.setAccessible(true);
            allowAlloying.setAccessible(true);
        }
    }

    void initializeWithoutWorld(HeatingStructureBlockEntity controller) {
        if (clearWorldComponents == null) return;
        if (controller.getLevel() != null) throw new IllegalStateException("Component initialization must not scan a world");
        try { clearWorldComponents.invoke(controller); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot initialize native heat components", e); }
    }
}
