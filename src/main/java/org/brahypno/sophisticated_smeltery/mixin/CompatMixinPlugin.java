package org.brahypno.sophisticated_smeltery.mixin;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class CompatMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        String mod = mixin.endsWith("BackpackFluidUpgradeMixin") || mixin.endsWith("MobCatcherStorageAccess")
                || mixin.endsWith("BackpackFluidSideMixin") || mixin.endsWith("BackpackControllerModelMixin")
                ? "sophisticatedbackpacks"
                : mixin.endsWith("StorageFluidWrapperMixin") || mixin.endsWith("StorageFluidCapabilityMixin") || mixin.endsWith("StoragePackedFluidMixin")
                ? "sophisticatedstorage" : null;
        return mod == null || LoadingModList.get().getModFileById(mod) != null;
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
