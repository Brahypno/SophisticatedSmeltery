package org.brahypno.sophisticated_smeltery.compat.jei;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationRecipeTransfer;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeContainer;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;

@JeiPlugin
public final class SmelteryJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() {
        return SophisticatedSmeltery.id("jei");
    }

    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        if (ModList.get().isLoaded("sophisticatedbackpacks")) register(registration, BackpackContainer.class);
        if (ModList.get().isLoaded("sophisticatedstorage")) register(registration, StorageContainerMenu.class);
    }

    private static <C extends StorageContainerMenuBase<?>> void register(IRecipeTransferRegistration registration, Class<C> menu) {
        var helper = registration.getTransferHelper();
        registration.addRecipeTransferHandler(new TransferHandler<>(menu, TConstructJEIConstants.TOOL_BUILDING, helper),
                TConstructJEIConstants.TOOL_BUILDING);
        registration.addRecipeTransferHandler(new TransferHandler<>(menu, TConstructJEIConstants.MODIFIERS, helper),
                TConstructJEIConstants.MODIFIERS);
    }

    private record TransferHandler<C extends StorageContainerMenuBase<?>, R>(Class<C> menuClass, RecipeType<R> recipeType,
            IRecipeTransferHandlerHelper helper) implements IRecipeTransferHandler<C, R> {
        @Override public Class<C> getContainerClass() { return menuClass; }
        @Override public Optional<MenuType<C>> getMenuType() { return Optional.empty(); }
        @Override public RecipeType<R> getRecipeType() { return recipeType; }

        @Override @Nullable public IRecipeTransferError transferRecipe(C menu, R recipe, IRecipeSlotsView slots,
                Player player, boolean maxTransfer, boolean doTransfer) {
            if (!(menu.getOpenContainer().orElse(null) instanceof TinkerStationUpgradeContainer station)) {
                if (menu.getUpgradeContainers().values().stream().noneMatch(TinkerStationUpgradeContainer.class::isInstance)) {
                    return helper.createInternalError();
                }
                return error("gui.sophisticated_smeltery.tinker_transfer_open");
            }

            ResourceLocation id;
            int materialPositions;
            boolean modifier = recipe instanceof IDisplayModifierRecipe;
            if (recipe instanceof ToolBuildingRecipe building) {
                id = building.getId();
                materialPositions = building.getToolParts().size() + building.getExtraRequirements().size();
            } else if (recipe instanceof IDisplayModifierRecipe display) {
                id = display.getRecipeId();
                // The modifier category emits five material positions before the custom slot-type ingredient.
                materialPositions = 5;
            } else {
                return helper.createInternalError();
            }
            if (id == null) return helper.createInternalError();

            // Keep empty positions. Tool building also has hidden lookup ingredients after the material slots.
            List<List<ItemStack>> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT).stream().limit(materialPositions)
                    .map(slot -> slot.getItemStacks().filter(stack -> !stack.isEmpty()).toList()).toList();
            var result = TinkerStationRecipeTransfer.preview(menu, station, inputs, maxTransfer);
            if (result.error() != null) return helper.createUserErrorWithTooltip(result.error().message());
            if (doTransfer) TinkerStationRecipeTransfer.request(menu, station, id, modifier, result.plan());
            return null;
        }

        private IRecipeTransferError error(String key) {
            return helper.createUserErrorWithTooltip(Component.translatable(key));
        }
    }
}
