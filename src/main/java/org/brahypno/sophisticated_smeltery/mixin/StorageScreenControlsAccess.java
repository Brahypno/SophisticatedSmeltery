package org.brahypno.sophisticated_smeltery.mixin;

import java.util.Map;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryControlBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = StorageScreenBase.class, remap = false)
public interface StorageScreenControlsAccess {
    @Accessor("storageInventoryControls")
    Map<UpgradeType<?>, UpgradeInventoryControlBase> sophisticatedsmeltery$getStorageControls();
}
