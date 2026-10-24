package org.brahypno.sophisticated_smeltery.furnace;

import java.lang.reflect.Method;
import javax.annotation.Nullable;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;

/** Optional world-independent initialization; reflection is resolved once per controller. */
record NativeHeatPolicy(boolean automaticAlloying, @Nullable Method clearWorldComponents) {
    static NativeHeatPolicy find(HeatingStructureBlockEntity controller) {
        Class<?> heatOwner = null;
        Method reset = null;
        for (Class<?> type = controller.getClass(); type != HeatingStructureBlockEntity.class; type = type.getSuperclass()) {
            if (heatOwner == null) {
                try { type.getDeclaredMethod("heat"); heatOwner = type; }
                catch (NoSuchMethodException ignored) { }
            }
            if (type.getName().equals("org.brahypno.esotericismtinker.smeltery.block.entity.controller.TransmuteBlockEntity")) {
                try { reset = type.getDeclaredMethod("checkHeatBuff"); reset.setAccessible(true); }
                catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported Transmute heat-component API", e); }
            }
        }
        return new NativeHeatPolicy(heatOwner == SmelteryBlockEntity.class, reset);
    }

    void initializeWithoutWorld(HeatingStructureBlockEntity controller) {
        if (clearWorldComponents == null) return;
        if (controller.getLevel() != null) throw new IllegalStateException("Component initialization must not scan a world");
        try { clearWorldComponents.invoke(controller); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot initialize native heat components", e); }
    }
}
