package org.brahypno.sophisticated_smeltery.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ControllerIconRegistration {
    @SubscribeEvent public static void loaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("upgrade_base", (net.minecraftforge.client.model.geometry.IGeometryLoader<UpgradeBaseGeometry>)
                (json, context) -> new UpgradeBaseGeometry(net.minecraft.util.GsonHelper.getAsString(json, "host")));
    }

    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        BackpackControllerModel.clearCache();
        for (String prefix : java.util.List.of("", "storage_")) {
            ModelResourceLocation location = new ModelResourceLocation(SophisticatedSmeltery.id(prefix + "melting_upgrade"), "inventory");
            BakedModel original = event.getModels().get(location);
            BakedModel brick = event.getModels().get(new ModelResourceLocation(TinkerSmeltery.searedBrick.getId(), "inventory"));
            if (original != null && brick != null) event.getModels().put(location, new ControllerIconModel(original, brick, null));
            BakedModel advanced = event.getModels().get(new ModelResourceLocation(
                    new net.minecraft.resources.ResourceLocation(prefix.isEmpty() ? "sophisticatedbackpacks" : "sophisticatedstorage", "advanced_magnet_upgrade"), "inventory"));
            var transmuteId = new ModelResourceLocation(SophisticatedSmeltery.id(prefix + "advanced_transmute_upgrade"), "inventory");
            BakedModel transmutePlate = event.getModels().get(transmuteId);
            if (transmutePlate != null && brick != null && advanced != null)
                event.getModels().put(transmuteId, new ControllerIconModel(transmutePlate, brick, advanced.getParticleIcon()));
            for (String item : java.util.List.of("seared_casting_upgrade", "advanced_seared_casting_upgrade", "scorched_casting_upgrade", "advanced_scorched_casting_upgrade")) {
                var state = (item.contains("scorched") ? TinkerSmeltery.scorchedTable : TinkerSmeltery.searedTable).get().defaultBlockState();
                BakedModel table = event.getModels().get(BlockModelShaper.stateToModelLocation(state));
                var modelId = new ModelResourceLocation(SophisticatedSmeltery.id(prefix + item), "inventory");
                BakedModel plate = event.getModels().get(modelId);
                if (plate != null && table != null) event.getModels().put(modelId, new CastingIconModel(plate, table, state,
                        item.startsWith("advanced_") && advanced != null ? advanced.getParticleIcon() : null));
            }
            var partBuilderState = slimeknights.tconstruct.tables.TinkerTables.partBuilder.get().defaultBlockState();
            var worktableState = slimeknights.tconstruct.tables.TinkerTables.modifierWorktable.get().defaultBlockState();
            var worktableModel = event.getModels().get(BlockModelShaper.stateToModelLocation(worktableState));
            var worktableId = new ModelResourceLocation(SophisticatedSmeltery.id(prefix + "modifier_worktable_upgrade"), "inventory");
            var worktablePlate = event.getModels().get(worktableId);
            if (worktablePlate != null && worktableModel != null)
                event.getModels().put(worktableId, new TinkerStationIconModel(
                        new CastingIconModel(worktablePlate, worktableModel, worktableState, null, false, true), worktablePlate, null, false));
            for (String item : java.util.List.of("tinker_station_upgrade", "tinkers_anvil_upgrade")) {
                boolean anvil = item.equals("tinkers_anvil_upgrade");
                var state = (anvil ? slimeknights.tconstruct.tables.TinkerTables.tinkersAnvil
                        : slimeknights.tconstruct.tables.TinkerTables.tinkerStation).get().defaultBlockState();
                if (anvil) state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                        net.minecraft.core.Direction.NORTH);
                var table = event.getModels().get(BlockModelShaper.stateToModelLocation(state));
                var id = new ModelResourceLocation(SophisticatedSmeltery.id(prefix + item), "inventory");
                var plate = event.getModels().get(id);
                if (plate != null && table != null)
                    event.getModels().put(id, new TinkerStationIconModel(new CastingIconModel(plate, table, state,
                            anvil && advanced != null ? advanced.getParticleIcon() : null, anvil, !anvil), plate,
                            anvil && advanced != null ? advanced.getParticleIcon() : null, anvil));
            }
            var partBuilderModel = event.getModels().get(BlockModelShaper.stateToModelLocation(partBuilderState));
            var partBuilderId = new ModelResourceLocation(SophisticatedSmeltery.id(prefix + "part_builder_upgrade"), "inventory");
            var partBuilderPlate = event.getModels().get(partBuilderId);
            if (partBuilderPlate != null && partBuilderModel != null)
                event.getModels().put(partBuilderId, new CastingIconModel(partBuilderPlate, partBuilderModel, partBuilderState, null));
            var alloyerState = TinkerSmeltery.scorchedAlloyer.get().defaultBlockState();
            var alloyerModel = event.getModels().get(BlockModelShaper.stateToModelLocation(alloyerState));
            var alloyerId = new ModelResourceLocation(SophisticatedSmeltery.id(prefix + "scorched_alloyer_upgrade"), "inventory");
            var alloyerPlate = event.getModels().get(alloyerId);
            if (alloyerPlate != null && alloyerModel != null)
                event.getModels().put(alloyerId, new CastingIconModel(alloyerPlate, alloyerModel, alloyerState, null));
            var spawnerState = net.minecraft.world.level.block.Blocks.SPAWNER.defaultBlockState();
            var spawnerModel = event.getModels().get(BlockModelShaper.stateToModelLocation(spawnerState));
            for (String item : java.util.List.of("spawner_upgrade", "advanced_spawner_upgrade")) {
                var id = new ModelResourceLocation(SophisticatedSmeltery.id(prefix + item), "inventory");
                var plate = event.getModels().get(id);
                if (plate != null && spawnerModel != null)
                    event.getModels().put(id, new CastingIconModel(plate, spawnerModel, spawnerState,
                            item.startsWith("advanced_") && advanced != null ? advanced.getParticleIcon() : null));
            }
        }
    }
}
