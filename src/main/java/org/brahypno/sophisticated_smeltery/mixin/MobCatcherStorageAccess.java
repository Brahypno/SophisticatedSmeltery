package org.brahypno.sophisticated_smeltery.mixin;

import java.util.List;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Save health changes through the catcher's own persistence and slot-blocking path. */
@Mixin(value = MobCatcherStorage.class, remap = false)
public interface MobCatcherStorageAccess {
    @Invoker("saveCapturedMobs")
    static void sophisticatedsmeltery$save(IBackpackWrapper backpack, List<CapturedMob> mobs) {
        throw new AssertionError("Mixin invoker");
    }
}
