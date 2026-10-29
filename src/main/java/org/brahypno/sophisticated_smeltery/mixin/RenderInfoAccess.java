package org.brahypno.sophisticated_smeltery.mixin;

import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = RenderInfo.class, remap = false)
public interface RenderInfoAccess {
    @Invoker("save")
    void sophisticatedsmeltery$save(boolean triggerRenderUpdateListener);
}
