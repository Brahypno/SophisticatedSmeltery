package org.brahypno.sophisticated_smeltery.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.util.ModifierTooltip;
import slimeknights.tconstruct.library.tools.item.ITinkerStationDisplay;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.TinkerTooltipFlags;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeWrapper;

/** Uses the same stat and modifier display hooks as Tinkers' ToolTableScreen. */
public final class TinkerStationToolInfo {
    public record Entry(Component text, @Nullable Component tooltip) {
        public Entry(Component text) { this(text, null); }
    }
    private TinkerStationToolInfo() {}

    public static ItemStack displayedTool(TinkerStationUpgradeWrapper wrapper) {
        ItemStack preview = wrapper.result();
        return preview.isEmpty() ? wrapper.getTinkerableStack() : preview;
    }
    public static List<Entry> stats(ItemStack stack, Player player) {
        List<Component> text = new ArrayList<>();
        if (stack.getItem() instanceof ITinkerStationDisplay display && stack.is(TinkerTags.Items.MODIFIABLE)) {
            text = display.getStatInformation(ToolStack.from(stack), player, text,
                    SafeClientAccess.getTooltipKey(), TinkerTooltipFlags.TINKER_STATION);
        } else if (!stack.isEmpty()) {
            stack.getItem().appendHoverText(stack, Minecraft.getInstance().level, text, TooltipFlag.Default.NORMAL);
        }
        return text.stream().map(Entry::new).toList();
    }
    public static List<Entry> modifiers(ItemStack stack, Player player, boolean upgradesOnly, boolean traitsOnly) {
        if (!stack.is(TinkerTags.Items.MODIFIABLE)) return List.of();
        ToolStack tool = ToolStack.from(stack);
        List<ModifierEntry> modifiers;
        if (traitsOnly) {
            Map<Modifier, Integer> upgrades = tool.getUpgrades().getModifiers().stream()
                    .collect(Collectors.toMap(ModifierEntry::getModifier, ModifierEntry::getLevel, Integer::sum));
            modifiers = new ArrayList<>();
            for (var entry : tool.getModifierList()) {
                int level = entry.getLevel() - upgrades.getOrDefault(entry.getModifier(), 0);
                if (level > 0) modifiers.add(new ModifierEntry(entry.getModifier(), level));
            }
        } else modifiers = upgradesOnly ? tool.getUpgrades().getModifiers() : tool.getModifierList();
        List<Entry> lines = new ArrayList<>();
        for (var entry : modifiers) {
            var modifier = entry.getModifier();
            if (modifier.shouldDisplay(ModifierTooltip.TINKER_STATION)) lines.add(new Entry(
                    modifier.getDisplayName(tool, entry, player.level().registryAccess()), modifier.getDescription(tool, entry)));
        }
        return lines;
    }
}