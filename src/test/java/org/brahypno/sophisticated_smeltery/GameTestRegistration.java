package org.brahypno.sophisticated_smeltery;

import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Select registration at runtime, keeping helper classes available and the full suite discoverable. */
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GameTestRegistration {
    private static final Set<String> PERFORMANCE_CLASSES = Set.of(
            "SpawnerUpgradeGameTests", "FurnaceGameTests", "MobMeltingGameTests",
            "CastingUpgradeGameTests", "CastingRecipeGameTests");

    @SubscribeEvent public static void register(RegisterGameTestsEvent event) {
        boolean focused = System.getProperty("sophisticatedsmeltery.gameTestSuite", "full").equals("performance")
                && ModList.get().isLoaded("sophisticatedbackpacks");
        String testPackage = GameTestRegistration.class.getPackageName() + ".";
        // Forge already scans method annotations. Discover the full set instead of maintaining a second class list.
        var classes = ModList.get().getAllScanData().stream().flatMap(scan -> scan.getAnnotations().stream())
                .filter(annotation -> annotation.annotationType().getClassName().equals(GameTest.class.getName()))
                .map(annotation -> annotation.clazz().getClassName())
                .filter(name -> name.startsWith(testPackage))
                .distinct().sorted()
                .filter(name -> !focused || PERFORMANCE_CLASSES.contains(name.substring(testPackage.length())))
                .toList();
        if (classes.isEmpty()) throw new IllegalStateException("No GameTests discovered; check this run's compiled test classes");
        classes.forEach(name -> {
                    try {
                        Class<?> tests = Class.forName(name);
                        event.register(tests);
                        SophisticatedSmeltery.LOGGER.info("Registering {} GameTests: {}", focused ? "performance" : "full", tests.getSimpleName());
                    } catch (ClassNotFoundException exception) {
                        throw new IllegalStateException("Scanned GameTest class is missing: " + name, exception);
                    }
                });
    }
}
