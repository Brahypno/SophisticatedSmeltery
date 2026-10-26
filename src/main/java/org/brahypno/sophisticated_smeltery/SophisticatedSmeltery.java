package org.brahypno.sophisticated_smeltery;

import com.mojang.logging.LogUtils;
import java.util.function.Consumer;
import java.util.List;
import java.util.stream.Stream;
import net.minecraftforge.fml.ModList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import org.brahypno.sophisticated_smeltery.recipe.ControllerBindingRecipe;
import org.brahypno.sophisticated_smeltery.recipe.VisibleControllerBindingRecipe;
import org.brahypno.sophisticated_smeltery.recipe.CastingTierRecipe;
import org.brahypno.sophisticated_smeltery.recipe.UpgradeClearingRecipe;
import org.brahypno.sophisticated_smeltery.furnace.NativeFurnace;
import org.brahypno.sophisticated_smeltery.furnace.PortableAlloyer;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.casting.PortableCasting;
import org.brahypno.sophisticated_smeltery.compat.StorageCompat;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.SpawnerUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.SpawnerUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.SpawnerUpgradeContainer;
import org.slf4j.Logger;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

@Mod(SophisticatedSmeltery.MODID)
public class SophisticatedSmeltery {
    public static final String MODID = "sophisticated_smeltery";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<MeltingUpgradeItem> MELTING_UPGRADE = ITEMS.register("melting_upgrade", MeltingUpgradeItem::new);
    public static final RegistryObject<AlloyerUpgradeItem> ALLOYER_UPGRADE = ITEMS.register("scorched_alloyer_upgrade", AlloyerUpgradeItem::new);
    public static final RegistryObject<SpawnerUpgradeItem> SPAWNER_UPGRADE = ITEMS.register("spawner_upgrade", () -> new SpawnerUpgradeItem(false));
    public static final RegistryObject<SpawnerUpgradeItem> ADVANCED_SPAWNER_UPGRADE = ITEMS.register("advanced_spawner_upgrade", () -> new SpawnerUpgradeItem(true));
    public static final RegistryObject<MeltingUpgradeItem> ADVANCED_TRANSMUTE_UPGRADE = registerTransmute(ITEMS, "advanced_transmute_upgrade");
    public static final RegistryObject<CastingUpgradeItem> SEARED_CASTING_UPGRADE = ITEMS.register("seared_casting_upgrade", CastingUpgradeItem::new);
    public static final RegistryObject<CastingUpgradeItem> ADVANCED_SEARED_CASTING_UPGRADE = ITEMS.register("advanced_seared_casting_upgrade", () -> new CastingUpgradeItem(true));
    public static final RegistryObject<CastingUpgradeItem> SCORCHED_CASTING_UPGRADE = ITEMS.register("scorched_casting_upgrade", () -> new CastingUpgradeItem(false, true));
    public static final RegistryObject<CastingUpgradeItem> ADVANCED_SCORCHED_CASTING_UPGRADE = ITEMS.register("advanced_scorched_casting_upgrade", () -> new CastingUpgradeItem(true, true));
    public static final DeferredRegister<Item> STORAGE_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<MeltingUpgradeItem> STORAGE_MELTING_UPGRADE = STORAGE_ITEMS.register("storage_melting_upgrade", MeltingUpgradeItem::new);
    public static final RegistryObject<AlloyerUpgradeItem> STORAGE_ALLOYER_UPGRADE = STORAGE_ITEMS.register("storage_scorched_alloyer_upgrade", AlloyerUpgradeItem::new);
    public static final RegistryObject<SpawnerUpgradeItem> STORAGE_SPAWNER_UPGRADE = STORAGE_ITEMS.register("storage_spawner_upgrade", () -> new SpawnerUpgradeItem(false));
    public static final RegistryObject<SpawnerUpgradeItem> STORAGE_ADVANCED_SPAWNER_UPGRADE = STORAGE_ITEMS.register("storage_advanced_spawner_upgrade", () -> new SpawnerUpgradeItem(true));
    public static final RegistryObject<MeltingUpgradeItem> STORAGE_ADVANCED_TRANSMUTE_UPGRADE = registerTransmute(STORAGE_ITEMS, "storage_advanced_transmute_upgrade");
    public static final RegistryObject<CastingUpgradeItem> STORAGE_SEARED_CASTING_UPGRADE = STORAGE_ITEMS.register("storage_seared_casting_upgrade", CastingUpgradeItem::new);
    public static final RegistryObject<CastingUpgradeItem> STORAGE_ADVANCED_SEARED_CASTING_UPGRADE = STORAGE_ITEMS.register("storage_advanced_seared_casting_upgrade", () -> new CastingUpgradeItem(true));
    public static final RegistryObject<CastingUpgradeItem> STORAGE_SCORCHED_CASTING_UPGRADE = STORAGE_ITEMS.register("storage_scorched_casting_upgrade", () -> new CastingUpgradeItem(false, true));
    public static final RegistryObject<CastingUpgradeItem> STORAGE_ADVANCED_SCORCHED_CASTING_UPGRADE = STORAGE_ITEMS.register("storage_advanced_scorched_casting_upgrade", () -> new CastingUpgradeItem(true, true));
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final RegistryObject<CreativeModeTab> CREATIVE_TAB = CREATIVE_TABS.register(MODID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sophisticated_smeltery"))
            .icon(StorageCompat::creativeTabIcon)
            .displayItems((parameters, output) -> addCreativeUpgrades(output::accept)).build());
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final RegistryObject<RecipeSerializer<ControllerBindingRecipe>> BIND_CONTROLLER = RECIPES.register(
            "bind_controller", () -> new SimpleCraftingRecipeSerializer<>(ControllerBindingRecipe::new));
    public static final RegistryObject<RecipeSerializer<VisibleControllerBindingRecipe>> VISIBLE_BINDING = RECIPES.register(
            "controller_binding", VisibleControllerBindingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<CastingTierRecipe>> CASTING_TIER = RECIPES.register(
            "casting_upgrade_tier", CastingTierRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<org.brahypno.sophisticated_smeltery.recipe.TransmuteTierRecipe>> TRANSMUTE_TIER = RECIPES.register(
            "transmute_upgrade_tier", org.brahypno.sophisticated_smeltery.recipe.TransmuteTierRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<UpgradeClearingRecipe>> UPGRADE_CLEAR = RECIPES.register(
            "upgrade_clear", () -> new SimpleCraftingRecipeSerializer<>(UpgradeClearingRecipe::new));
    public static final UpgradeContainerType<MeltingUpgradeWrapper, MeltingUpgradeContainer> MELTING_CONTAINER =
            new UpgradeContainerType<>(MeltingUpgradeContainer::new);
    public static final UpgradeContainerType<CastingUpgradeWrapper, CastingUpgradeContainer> CASTING_CONTAINER =
            new UpgradeContainerType<>(CastingUpgradeContainer::new);
    public static final UpgradeContainerType<AlloyerUpgradeWrapper, AlloyerUpgradeContainer> ALLOYER_CONTAINER =
            new UpgradeContainerType<>(AlloyerUpgradeContainer::new);
    public static final UpgradeContainerType<SpawnerUpgradeWrapper, SpawnerUpgradeContainer> SPAWNER_CONTAINER =
            new UpgradeContainerType<>(SpawnerUpgradeContainer::new);

    public SophisticatedSmeltery() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        if (ModList.get().isLoaded("sophisticatedbackpacks")) ITEMS.register(bus);
        if (ModList.get().isLoaded("sophisticatedstorage")) STORAGE_ITEMS.register(bus);
        CREATIVE_TABS.register(bus);
        RECIPES.register(bus);
        if (net.minecraftforge.fml.ModList.get().isLoaded("sophisticatedbackpacks")) {
            MinecraftForge.EVENT_BUS.register(org.brahypno.sophisticated_smeltery.furnace.BackpackFluidInteraction.class);
            MinecraftForge.EVENT_BUS.register(org.brahypno.sophisticated_smeltery.furnace.PortableMobMelting.class);
        }
        bus.addListener(this::setup);
        bus.addListener(this::creativeItems);
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) ->
                event.addListener((ResourceManagerReloadListener)resources -> {
                    NativeFurnace.invalidateRecipes();
                    PortableAlloyer.invalidateRecipes();
                    PortableCasting.invalidateRecipes();
                }));
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Stream.concat(ITEMS.getEntries().stream(), STORAGE_ITEMS.getEntries().stream())
                    .filter(RegistryObject::isPresent).forEach(item -> {
                        if (item.get() instanceof MeltingUpgradeItem) UpgradeContainerRegistry.register(item.getId(), MELTING_CONTAINER);
                        else if (item.get() instanceof AlloyerUpgradeItem) UpgradeContainerRegistry.register(item.getId(), ALLOYER_CONTAINER);
                        else if (item.get() instanceof CastingUpgradeItem) UpgradeContainerRegistry.register(item.getId(), CASTING_CONTAINER);
                        else if (item.get() instanceof SpawnerUpgradeItem) UpgradeContainerRegistry.register(item.getId(), SPAWNER_CONTAINER);
                    });
        });
    }

    private void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) addCreativeUpgrades(event::accept);
    }

    private static void addCreativeUpgrades(Consumer<ItemStack> output) {
        for (String prefix : List.of("", "storage_")) {
            var melting = prefix.isEmpty() ? MELTING_UPGRADE : STORAGE_MELTING_UPGRADE;
            if (!melting.isPresent()) continue;
            for (String name : List.of("melting_upgrade", "scorched_alloyer_upgrade", "spawner_upgrade", "advanced_spawner_upgrade"))
                output.accept(new ItemStack(ForgeRegistries.ITEMS.getValue(id(prefix + name))));
            addBoundFurnaces(output, melting.get(), prefix.isEmpty() ? ADVANCED_TRANSMUTE_UPGRADE : STORAGE_ADVANCED_TRANSMUTE_UPGRADE);
            for (String name : List.of("seared_casting_upgrade", "advanced_seared_casting_upgrade", "scorched_casting_upgrade", "advanced_scorched_casting_upgrade"))
                output.accept(new ItemStack(ForgeRegistries.ITEMS.getValue(id(prefix + name))));
        }
    }

    private static void addBoundFurnaces(Consumer<ItemStack> output, MeltingUpgradeItem melting, RegistryObject<MeltingUpgradeItem> advanced) {
        ItemStack upgrade = new ItemStack(melting);
        output.accept(ControllerBinding.bind(upgrade, new ItemStack(TinkerSmeltery.smelteryController)));
        output.accept(ControllerBinding.bind(upgrade, new ItemStack(TinkerSmeltery.foundryController)));
        ResourceLocation transmuteController = new ResourceLocation("esotericism_tinker", "transmute_controller");
        if (ForgeRegistries.ITEMS.containsKey(transmuteController)) {
            output.accept(ControllerBinding.bind(upgrade, new ItemStack(ForgeRegistries.ITEMS.getValue(transmuteController))));
            output.accept(ControllerBinding.bind(new ItemStack(advanced.get()), new ItemStack(ForgeRegistries.ITEMS.getValue(transmuteController))));
        }
    }

    private static RegistryObject<MeltingUpgradeItem> registerTransmute(DeferredRegister<Item> items, String path) {
        return ModList.get().isLoaded("esotericism_tinker") ? items.register(path, () -> new MeltingUpgradeItem(true))
                : RegistryObject.createOptional(id(path), Registries.ITEM, MODID);
    }

    public static ResourceLocation id(String path) { return new ResourceLocation(MODID, path); }
}
