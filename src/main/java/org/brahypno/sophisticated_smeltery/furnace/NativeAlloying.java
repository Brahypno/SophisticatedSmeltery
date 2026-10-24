package org.brahypno.sophisticated_smeltery.furnace;

import java.lang.reflect.Field;
import javax.annotation.Nullable;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.alloying.MultiAlloyingModule;
import slimeknights.tconstruct.smeltery.block.entity.module.alloying.SmelteryAlloyTank;

/** Discovers the existing native alloy components once, never on the ticking path. */
public record NativeAlloying(MultiAlloyingModule module, SmelteryAlloyTank tank) {
    @Nullable
    public static NativeAlloying find(HeatingStructureBlockEntity controller) {
        MultiAlloyingModule module = null;
        SmelteryAlloyTank tank = null;
        for (Class<?> type = controller.getClass(); type != HeatingStructureBlockEntity.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    if (MultiAlloyingModule.class.isAssignableFrom(field.getType()) && field.trySetAccessible()) {
                        if (module != null) return null; // More than one module needs a dedicated adapter.
                        module = (MultiAlloyingModule)field.get(controller);
                    } else if (SmelteryAlloyTank.class.isAssignableFrom(field.getType()) && field.trySetAccessible()) {
                        if (tank != null) return null;
                        tank = (SmelteryAlloyTank)field.get(controller);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot access native alloy component", e);
                }
            }
        }
        return module != null && tank != null ? new NativeAlloying(module, tank) : null;
    }
}
