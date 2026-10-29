package org.brahypno.sophisticated_smeltery.data;

import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SmelteryData {
    // IDs are used deliberately: optional host mods and Esotericism Tinker need not be installed to generate their resources.
    static final java.util.List<String> UPGRADES = java.util.List.of(
            "melting_upgrade", "part_builder_upgrade", "tinker_station_upgrade", "tinkers_anvil_upgrade", "modifier_worktable_upgrade", "scorched_alloyer_upgrade", "spawner_upgrade", "advanced_spawner_upgrade",
            "advanced_transmute_upgrade", "seared_casting_upgrade", "advanced_seared_casting_upgrade",
            "scorched_casting_upgrade", "advanced_scorched_casting_upgrade");

    @SubscribeEvent public static void gather(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        generator.addProvider(event.includeServer(), new SmelteryRecipeProvider(output));
        generator.addProvider(event.includeClient(), new SmelteryAssetProvider(output, true));
        generator.addProvider(event.includeServer(), new SmelteryAssetProvider(output, false));
        generator.addProvider(event.includeClient(), new SmelteryLanguageProvider(output, "en_us"));
        generator.addProvider(event.includeClient(), new SmelteryLanguageProvider(output, "zh_cn"));
    }
}
