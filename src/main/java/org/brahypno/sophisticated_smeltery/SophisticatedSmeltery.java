package org.brahypno.sophisticated_smeltery;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
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
import org.brahypno.sophisticated_smeltery.furnace.NativeFurnace;
import org.brahypno.sophisticated_smeltery.casting.PortableCasting;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeContainer;
import org.slf4j.Logger;

@Mod(SophisticatedSmeltery.MODID)
public class SophisticatedSmeltery {
    public static final String MODID = "sophisticatedsmeltery";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<MeltingUpgradeItem> MELTING_UPGRADE = ITEMS.register("melting_upgrade", MeltingUpgradeItem::new);
    public static final RegistryObject<CastingUpgradeItem> SEARED_CASTING_UPGRADE = ITEMS.register("seared_casting_upgrade", CastingUpgradeItem::new);
    public static final RegistryObject<CastingUpgradeItem> ADVANCED_SEARED_CASTING_UPGRADE = ITEMS.register("advanced_seared_casting_upgrade", () -> new CastingUpgradeItem(true));
    public static final RegistryObject<CastingUpgradeItem> SCORCHED_CASTING_UPGRADE = ITEMS.register("scorched_casting_upgrade", () -> new CastingUpgradeItem(false, true));
    public static final RegistryObject<CastingUpgradeItem> ADVANCED_SCORCHED_CASTING_UPGRADE = ITEMS.register("advanced_scorched_casting_upgrade", () -> new CastingUpgradeItem(true, true));
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final RegistryObject<RecipeSerializer<ControllerBindingRecipe>> BIND_CONTROLLER = RECIPES.register(
            "bind_controller", () -> new SimpleCraftingRecipeSerializer<>(ControllerBindingRecipe::new));
    public static final RegistryObject<RecipeSerializer<VisibleControllerBindingRecipe>> VISIBLE_BINDING = RECIPES.register(
            "controller_binding", VisibleControllerBindingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<CastingTierRecipe>> CASTING_TIER = RECIPES.register(
            "casting_upgrade_tier", CastingTierRecipe.Serializer::new);
    public static final UpgradeContainerType<MeltingUpgradeWrapper, MeltingUpgradeContainer> MELTING_CONTAINER =
            new UpgradeContainerType<>(MeltingUpgradeContainer::new);
    public static final UpgradeContainerType<CastingUpgradeWrapper, CastingUpgradeContainer> CASTING_CONTAINER =
            new UpgradeContainerType<>(CastingUpgradeContainer::new);

    public SophisticatedSmeltery() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        RECIPES.register(bus);
        bus.addListener(this::setup);
        bus.addListener(this::creativeItems);
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) ->
                event.addListener((ResourceManagerReloadListener)resources -> {
                    NativeFurnace.invalidateRecipes();
                    PortableCasting.invalidateRecipes();
                }));
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            UpgradeContainerRegistry.register(MELTING_UPGRADE.getId(), MELTING_CONTAINER);
            UpgradeContainerRegistry.register(SEARED_CASTING_UPGRADE.getId(), CASTING_CONTAINER);
            UpgradeContainerRegistry.register(ADVANCED_SEARED_CASTING_UPGRADE.getId(), CASTING_CONTAINER);
            UpgradeContainerRegistry.register(SCORCHED_CASTING_UPGRADE.getId(), CASTING_CONTAINER);
            UpgradeContainerRegistry.register(ADVANCED_SCORCHED_CASTING_UPGRADE.getId(), CASTING_CONTAINER);
        });
    }

    private void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MELTING_UPGRADE.get());
            event.accept(SEARED_CASTING_UPGRADE.get());
            event.accept(ADVANCED_SEARED_CASTING_UPGRADE.get());
            event.accept(SCORCHED_CASTING_UPGRADE.get());
            event.accept(ADVANCED_SCORCHED_CASTING_UPGRADE.get());
        }
    }

    public static ResourceLocation id(String path) { return new ResourceLocation(MODID, path); }
}
