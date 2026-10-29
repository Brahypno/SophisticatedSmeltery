package org.brahypno.sophisticated_smeltery.data;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/** Translation source; generated JSON is checked in under src/generated/resources. */
final class SmelteryLanguageProvider extends LanguageProvider {
    private final String locale;

    SmelteryLanguageProvider(PackOutput output, String locale) {
        super(output, "sophisticated_smeltery", locale);
        this.locale = locale;
    }

    @Override protected void addTranslations() {
        if (locale.equals("zh_cn")) chinese();
        else english();
    }

    private void workstationTranslations(boolean chinese) {
        add("item.sophisticated_smeltery.workstation_appearance", chinese ? "外观：%s" : "Appearance: %s");
        add("item.sophisticated_smeltery.workstation_appearance_hint", chinese
                ? "与同类工作台或工匠砧合成以更换外观，并返还原台子。"
                : "Craft with a matching workstation to change its appearance and return the previous one.");
        for (String prefix : java.util.List.of("", "storage_")) {
            add("item.sophisticated_smeltery." + prefix + "modifier_worktable_upgrade", chinese ? "强化调配台升级" : "Modifier Worktable Upgrade");
            add("item.sophisticated_smeltery." + prefix + "modifier_worktable_upgrade.tooltip", chinese
                    ? (prefix.isEmpty() ? "在背包提供强化调配台" : "在存储提供强化调配台")
                    : (prefix.isEmpty() ? "Provides a Modifier Worktable in your backpack." : "Provides a Modifier Worktable in your storage."));
            add("item.sophisticated_smeltery." + prefix + "tinker_station_upgrade", chinese ? "工匠台升级" : "Tinker Station Upgrade");
            add("item.sophisticated_smeltery." + prefix + "tinkers_anvil_upgrade", chinese ? "工匠砧升级" : "Tinkers' Anvil Upgrade");
            add("item.sophisticated_smeltery." + prefix + "tinker_station_upgrade.tooltip", chinese
                    ? "使用匠魂原生配方组装、修复、强化和替换工具部件，提供三个材料槽。"
                    : "Builds, repairs, modifies and swaps tool parts using native Tinkers' recipes, with three input slots.");
            add("item.sophisticated_smeltery." + prefix + "tinkers_anvil_upgrade.tooltip", chinese
                    ? "工匠台升级的高级版，提供五个材料槽，支持大型工具与更多材料的强化配方。"
                    : "An advanced Tinker Station with five input slots for large tools and recipes requiring more ingredients.");
        }
        add("gui.sophisticated_smeltery.tinker_station", chinese ? "工匠台" : "Tinker Station");
        add("gui.sophisticated_smeltery.tinkers_anvil", chinese ? "工匠砧" : "Tinkers' Anvil");
        add("gui.sophisticated_smeltery.tinker_station_name", chinese ? "工具名称" : "Tool name");
        add("gui.sophisticated_smeltery.tinker_station_tool", chinese ? "放入工具" : "Insert a tool");
        add("gui.sophisticated_smeltery.tinker_station_input", chinese ? "材料槽 %s" : "Ingredient %s");
        add("gui.sophisticated_smeltery.tinker_station_hint", chinese ? "取出产物制作" : "Take result to craft");
        add("gui.sophisticated_smeltery.tinker_station_page_craft", chinese ? "制作" : "Craft");
        add("gui.sophisticated_smeltery.tinker_station_page_stats", chinese ? "数值" : "Stats");
        add("gui.sophisticated_smeltery.tinker_station_page_modifiers", chinese ? "强化" : "Mods");
        add("gui.sophisticated_smeltery.tinker_station_information", chinese ? "工具数据" : "Tool information");
        add("gui.sophisticated_smeltery.tinker_station_drag_hint", chinese ? "按住标题栏拖动数据面板。" : "Drag the title bar to move the information panel.");
        add("gui.sophisticated_smeltery.tinker_station_info_empty", chinese ? "放入工具或部件后，可查看工具数值和强化数据。" : "Insert a tool or parts to inspect its stats and modifiers.");
        add("gui.sophisticated_smeltery.tinker_station_no_modifiers", chinese ? "没有可显示的强化或特性。" : "No visible modifiers or traits.");
        add("gui.sophisticated_smeltery.tinker_station_scroll_hint", chinese ? "滚动鼠标滚轮，或点击上下按钮翻页。" : "Scroll or use the arrows to page through the information.");
        add("gui.sophisticated_smeltery.tinker_station_modifiers_hint", chinese ? "悬停查看说明；Shift 仅显示强化，Ctrl 仅显示材料特性。可滚动或拖动滚动条。" : "Hover for descriptions. Shift shows upgrades; Ctrl shows traits. Scroll or drag the scrollbar.");
        add("gui.sophisticated_smeltery.tinker_station_help", chinese
                ? "选择工具布局并按槽位提示放入部件以组装工具。选择修复与强化布局，放入工具及材料以修复、强化或替换部件。滚动查看其他布局。"
                : "Select a tool layout and insert the indicated parts to build it. Use the repair layout with a tool and ingredients to repair, modify or swap parts. Scroll for more layouts.");
    }
    private void english() {
        workstationTranslations(false);
        add("item.sophisticated_smeltery.part_builder_upgrade", "Part Builder Upgrade");
        add("item.sophisticated_smeltery.part_builder_upgrade.tooltip", "Crafts parts with Tinkers' part builder recipes. Select a pattern, insert materials, and take the result.");
        add("item.sophisticated_smeltery.storage_part_builder_upgrade", "Part Builder Upgrade");
        add("item.sophisticated_smeltery.storage_part_builder_upgrade.tooltip", "Crafts parts with Tinkers' part builder recipes. Select a pattern, insert materials, and take the result.");
        add("gui.sophisticated_smeltery.part_builder", "Part Builder");
        add("gui.sophisticated_smeltery.part_builder_pattern", "Pattern");
        add("gui.sophisticated_smeltery.part_builder_material", "Material");
        add("gui.sophisticated_smeltery.part_builder_cost", "Cost: %s / %s");
        add("gui.sophisticated_smeltery.part_builder_select", "Insert a pattern and select a part.");
        add("gui.sophisticated_smeltery.part_builder_scroll", "Scroll to see more parts.");
        add("item.sophisticated_smeltery.spawner_upgrade", "Spawner Upgrade");
        add("item.sophisticated_smeltery.advanced_spawner_upgrade", "Advanced Spawner Upgrade");
        add("item.sophisticated_smeltery.spawner_upgrade.tooltip", "Runs one spawner using its native rules. With a Mob Catcher, spawns prefer backpack storage.");
        add("item.sophisticated_smeltery.advanced_spawner_upgrade.tooltip", "Ignores player distance and natural spawn rules for the internal spawner and spawners directly beside a placed backpack, even when no spawner is installed inside. Space, nearby limits, delay and redstone control still apply.");
        add("gui.sophisticated_smeltery.spawner_slot", "Place one spawner here. The upgrade switch pauses spawning.");
        add("gui.sophisticated_smeltery.spawner", "Spawner");
        add("item.sophisticated_smeltery.scorched_alloyer_upgrade", "Scorched Alloyer Upgrade");
        add("item.sophisticated_smeltery.scorched_alloyer_upgrade.tooltip", "Alloys fluids independently alongside melting upgrades. Includes an output tank and a 3x3 component grid for eight input tanks and one fuel slot.");
        add("error.sophisticated_smeltery.empty_alloyer", "Empty the output, ingredient and fuel tanks before removing this upgrade.");
        add("gui.sophisticated_smeltery.alloyer_switch_hint", "Pause or resume alloying and fuel consumption. Remaining paid burn time is preserved.");
        add("gui.sophisticated_smeltery.alloyer_output", "Output tank");
        add("gui.sophisticated_smeltery.alloyer_input", "Input tank %s");
        add("gui.sophisticated_smeltery.alloyer_target_hint", "Click to select the container target: output or inputs 1–8. You can also use containers directly on each fluid display.");
        add("gui.sophisticated_smeltery.alloyer_input_hint", "Input tank %s: install one tank accepted by the alloyer tank tag.");
        add("gui.sophisticated_smeltery.alloyer_fuel_hint", "Fuel slot (ninth slot): insert solid fuel or a fuel tank. Backpack tanks can also supply liquid fuel.");
        add("itemGroup.sophisticated_smeltery", "Sophisticated Smeltery");
        add("item.sophisticated_smeltery.advanced_transmute_upgrade", "Blank Advanced Transmute Upgrade");
        add("item.sophisticated_smeltery.advanced_transmute_upgrade.bound", "Advanced %s Upgrade");
        add("item.sophisticated_smeltery.advanced_transmute_upgrade.tooltip", "Switch between Transmute, Smeltery and Foundry modes and toggle alloying independently.");
        add("gui.sophisticated_smeltery.transmute_mode_0", "Melting mode: Transmute");
        add("gui.sophisticated_smeltery.transmute_mode_1", "Melting mode: Smeltery");
        add("gui.sophisticated_smeltery.transmute_mode_2", "Melting mode: Foundry");
        add("gui.sophisticated_smeltery.transmute_mode_hint", "Click to cycle melting modes.");
        add("gui.sophisticated_smeltery.transmute_alloy_on", "Alloying: on");
        add("gui.sophisticated_smeltery.transmute_alloy_off", "Alloying: off");
        add("gui.sophisticated_smeltery.transmute_alloy_hint", "Click to toggle. Alloying requires heating to be enabled.");
        add("item.sophisticated_smeltery.seared_casting_upgrade", "Seared Casting Upgrade");
        add("item.sophisticated_smeltery.advanced_seared_casting_upgrade", "Advanced Seared Casting Upgrade");
        add("item.sophisticated_smeltery.advanced_seared_casting_upgrade.tooltip", "Pours manually or automatically, preferring the furnace's bottom fluid before tank upgrades.");
        add("item.sophisticated_smeltery.seared_casting_upgrade.tooltip", "Uses Tinkers' casting recipes, preferring the furnace's bottom fluid before tank upgrades.");
        add("item.sophisticated_smeltery.scorched_casting_upgrade", "Scorched Casting Upgrade");
        add("item.sophisticated_smeltery.advanced_scorched_casting_upgrade", "Advanced Scorched Casting Upgrade");
        add("item.sophisticated_smeltery.scorched_casting_upgrade.tooltip", "Casts using scorched table or basin rules. Requires a cast; lock it to refill from storage.");
        add("item.sophisticated_smeltery.advanced_scorched_casting_upgrade.tooltip", "Pours manually or automatically using scorched casting rules. Lock a cast to refill from storage.");
        add("error.sophisticated_smeltery.empty_casting", "Return remaining casting fluid or finish cooling before removing this upgrade.");
        add("gui.sophisticated_smeltery.casting", "Seared Casting");
        add("gui.sophisticated_smeltery.advanced_casting", "Advanced Seared");
        add("gui.sophisticated_smeltery.scorched_casting", "Scorched Casting");
        add("gui.sophisticated_smeltery.advanced_scorched_casting", "Advanced Scorched");
        add("gui.sophisticated_smeltery.cast_pour", "Pour");
        add("gui.sophisticated_smeltery.cast_return", "Return fluid");
        add("gui.sophisticated_smeltery.cast_automatic_on", "Auto: ON");
        add("gui.sophisticated_smeltery.cast_automatic_off", "Auto: OFF");
        add("gui.sophisticated_smeltery.cast_table", "Casting table");
        add("gui.sophisticated_smeltery.cast_basin", "Casting basin");
        add("gui.sophisticated_smeltery.cast_mode_locked", "Return the fluid or finish cooling before switching modes.");
        add("gui.sophisticated_smeltery.cast_cooling", "Cooling: %s / %s ticks");
        add("gui.sophisticated_smeltery.cast_waiting", "Waiting for enough fluid");
        add("gui.sophisticated_smeltery.cast_preview", "Recipe preview");
        add("gui.sophisticated_smeltery.cast_input", "Input");
        add("gui.sophisticated_smeltery.cast_output", "Output");
        add("gui.sophisticated_smeltery.cast_no_controller", "Install a melting controller or tank upgrade to pour or return fluid.");
        add("gui.sophisticated_smeltery.cast_enabled", "Casting: ON");
        add("gui.sophisticated_smeltery.cast_disabled", "Casting: OFF (existing casts keep cooling)");
        add("gui.sophisticated_smeltery.cast_output_storage", "Output: storage first");
        add("gui.sophisticated_smeltery.cast_output_slot", "Output: stack in slot");
        add("gui.sophisticated_smeltery.cast_output_hint", "Storage prefers non-melting slots. The output slot stacks identical items and data, waiting when full.");
        add("gui.sophisticated_smeltery.cast_lock_off", "Lock current cast");
        add("gui.sophisticated_smeltery.cast_lock_on", "Cast locked");
        add("gui.sophisticated_smeltery.cast_lock_hint", "Remembers this cast's item and data; refills it from storage after use. Click again to unlock.");
        add("item.sophisticated_smeltery.melting_upgrade", "Blank Melting Upgrade");
        add("item.sophisticated_smeltery.melting_upgrade.tooltip", "Melts backpack items using the bound controller's rules.");
        add("item.sophisticated_smeltery.melting_upgrade.bound", "%s Upgrade");
        add("error.sophisticated_smeltery.one_controller", "Only one melting controller can be installed.");
        add("error.sophisticated_smeltery.bind_controller", "Bind a supported controller before installing this upgrade.");
        add("error.sophisticated_smeltery.empty_fluids", "Empty the furnace and Tinkers fuel tanks before removing the controller.");
        add("gui.sophisticated_smeltery.materials", "Materials");
        add("gui.sophisticated_smeltery.fuel_tank", "Tinkers Fuel Tank");
        add("gui.sophisticated_smeltery.heating_on", "Heat: ON");
        add("gui.sophisticated_smeltery.heating_off", "Heat: OFF");
        add("gui.sophisticated_smeltery.temperature", "Temperature: %s C");
        add("gui.sophisticated_smeltery.empty", "Empty");
        add("gui.sophisticated_smeltery.fluid", "%s / %s mB");
        add("gui.sophisticated_smeltery.backpack_tank", "Backpack tank %s");
        add("gui.sophisticated_smeltery.tank_button", "Tank %s");
        add("gui.sophisticated_smeltery.transfer_hint", "Left: tank to furnace. Right: furnace to tank.");
        add("item.sophisticated_smeltery.storage_spawner_upgrade", "Spawner Upgrade");
        add("item.sophisticated_smeltery.storage_advanced_spawner_upgrade", "Advanced Spawner Upgrade");
        add("item.sophisticated_smeltery.storage_spawner_upgrade.tooltip", "Runs one spawner at the storage position using its native rules.");
        add("item.sophisticated_smeltery.storage_advanced_spawner_upgrade.tooltip", "Ignores player distance and natural spawn rules. Space, nearby limits, delay and redstone control still apply.");
        add("item.sophisticated_smeltery.storage_scorched_alloyer_upgrade", "Scorched Alloyer Upgrade");
        add("item.sophisticated_smeltery.storage_scorched_alloyer_upgrade.tooltip", "Alloys fluids independently alongside melting upgrades. Includes an output tank and a 3x3 component grid for eight input tanks and one fuel slot.");
        add("item.sophisticated_smeltery.storage_advanced_transmute_upgrade", "Blank Advanced Transmute Upgrade");
        add("item.sophisticated_smeltery.storage_advanced_transmute_upgrade.bound", "Advanced %s Upgrade");
        add("item.sophisticated_smeltery.storage_advanced_transmute_upgrade.tooltip", "Switch between Transmute, Smeltery and Foundry modes and toggle alloying independently.");
        add("item.sophisticated_smeltery.storage_seared_casting_upgrade", "Seared Casting Upgrade");
        add("item.sophisticated_smeltery.storage_advanced_seared_casting_upgrade", "Advanced Seared Casting Upgrade");
        add("item.sophisticated_smeltery.storage_advanced_seared_casting_upgrade.tooltip", "Pours manually or automatically, preferring the furnace's bottom fluid before tank upgrades.");
        add("item.sophisticated_smeltery.storage_seared_casting_upgrade.tooltip", "Uses Tinkers' casting recipes, preferring the furnace's bottom fluid before tank upgrades.");
        add("item.sophisticated_smeltery.storage_scorched_casting_upgrade", "Scorched Casting Upgrade");
        add("item.sophisticated_smeltery.storage_advanced_scorched_casting_upgrade", "Advanced Scorched Casting Upgrade");
        add("item.sophisticated_smeltery.storage_scorched_casting_upgrade.tooltip", "Casts using scorched table or basin rules. Requires a cast; lock it to refill from storage.");
        add("item.sophisticated_smeltery.storage_advanced_scorched_casting_upgrade.tooltip", "Pours manually or automatically using scorched casting rules. Lock a cast to refill from storage.");
        add("item.sophisticated_smeltery.storage_melting_upgrade", "Blank Melting Upgrade");
        add("item.sophisticated_smeltery.storage_melting_upgrade.tooltip", "Melts storage items using the bound controller's rules.");
        add("item.sophisticated_smeltery.storage_melting_upgrade.bound", "%s Upgrade");
    }

    private void chinese() {
        workstationTranslations(true);
        add("item.sophisticated_smeltery.part_builder_upgrade", "部件制作台升级");
        add("item.sophisticated_smeltery.part_builder_upgrade.tooltip", "使用匠魂部件制作台配方制作部件。放入模具与材料，选择部件后取出产物。");
        add("item.sophisticated_smeltery.storage_part_builder_upgrade", "部件制作台升级");
        add("item.sophisticated_smeltery.storage_part_builder_upgrade.tooltip", "使用匠魂部件制作台配方制作部件。放入模具与材料，选择部件后取出产物。");
        add("gui.sophisticated_smeltery.part_builder", "部件制作台");
        add("gui.sophisticated_smeltery.part_builder_pattern", "模具");
        add("gui.sophisticated_smeltery.part_builder_material", "材料");
        add("gui.sophisticated_smeltery.part_builder_cost", "材料：%s / %s");
        add("gui.sophisticated_smeltery.part_builder_select", "放入模具并选择部件。");
        add("gui.sophisticated_smeltery.part_builder_scroll", "滚动查看其他部件。");
        add("item.sophisticated_smeltery.spawner_upgrade", "刷怪笼升级");
        add("item.sophisticated_smeltery.advanced_spawner_upgrade", "高级刷怪笼升级");
        add("item.sophisticated_smeltery.spawner_upgrade.tooltip", "按原生条件运行一个刷怪笼。安装生物捕捉升级时，优先生成到背包内。");
        add("item.sophisticated_smeltery.advanced_spawner_upgrade.tooltip", "忽略内部及放置背包六面紧邻刷怪笼的玩家距离和自然生成规则，内部刷怪笼槽为空时也生效。保留空间、附近数量、刷怪间隔及红石控制。");
        add("gui.sophisticated_smeltery.spawner_slot", "放入一个刷怪笼；升级模块开关控制刷怪。");
        add("gui.sophisticated_smeltery.spawner", "刷怪笼");
        add("itemGroup.sophisticated_smeltery", "精妙匠魂");
        add("item.sophisticated_smeltery.advanced_transmute_upgrade", "空白高级嬗变炉升级");
        add("item.sophisticated_smeltery.advanced_transmute_upgrade.bound", "高级%s升级");
        add("item.sophisticated_smeltery.advanced_transmute_upgrade.tooltip", "支持嬗变、冶炼炉、熔铸炉三种模式，并可独立开关合金。");
        add("gui.sophisticated_smeltery.transmute_mode_0", "熔炼模式：嬗变炉");
        add("gui.sophisticated_smeltery.transmute_mode_1", "熔炼模式：冶炼炉");
        add("gui.sophisticated_smeltery.transmute_mode_2", "熔炼模式：熔铸炉");
        add("gui.sophisticated_smeltery.transmute_mode_hint", "点击切换熔炼模式。");
        add("gui.sophisticated_smeltery.transmute_alloy_on", "合金：开");
        add("gui.sophisticated_smeltery.transmute_alloy_off", "合金：关");
        add("gui.sophisticated_smeltery.transmute_alloy_hint", "点击切换；合金仍需总加热开启。");
        add("item.sophisticated_smeltery.seared_casting_upgrade", "焦黑浇筑升级");
        add("item.sophisticated_smeltery.advanced_seared_casting_upgrade", "高级焦黑浇筑升级");
        add("item.sophisticated_smeltery.advanced_seared_casting_upgrade.tooltip", "优先使用炉内底层流体，也可使用储罐升级；支持手动或自动浇筑。");
        add("item.sophisticated_smeltery.seared_casting_upgrade.tooltip", "使用匠魂浇筑配方，优先使用炉内底层流体，也可使用储罐升级。");
        add("item.sophisticated_smeltery.scorched_casting_upgrade", "焦褐浇筑升级");
        add("item.sophisticated_smeltery.advanced_scorched_casting_upgrade", "高级焦褐浇筑升级");
        add("item.sophisticated_smeltery.scorched_casting_upgrade.tooltip", "按焦褐铸件台或铸造盆的规则浇筑，需要模具；可锁定模具并从背包补充。");
        add("item.sophisticated_smeltery.advanced_scorched_casting_upgrade.tooltip", "支持手动或自动焦褐浇筑，需要模具；可锁定模具并从背包补充。");
        add("error.sophisticated_smeltery.empty_casting", "退回浇筑残液或等待冷却完成后，才能移除此升级。");
        add("gui.sophisticated_smeltery.casting", "焦黑浇筑");
        add("gui.sophisticated_smeltery.advanced_casting", "高级焦黑浇筑");
        add("gui.sophisticated_smeltery.scorched_casting", "焦褐浇筑");
        add("gui.sophisticated_smeltery.advanced_scorched_casting", "高级焦褐浇筑");
        add("gui.sophisticated_smeltery.cast_pour", "浇筑");
        add("gui.sophisticated_smeltery.cast_return", "退液");
        add("gui.sophisticated_smeltery.cast_automatic_on", "自动：开");
        add("gui.sophisticated_smeltery.cast_automatic_off", "自动：关");
        add("gui.sophisticated_smeltery.cast_table", "浇筑台");
        add("gui.sophisticated_smeltery.cast_basin", "浇筑盆");
        add("gui.sophisticated_smeltery.cast_mode_locked", "退回残液或等待冷却完成后，才能切换模式。");
        add("gui.sophisticated_smeltery.cast_cooling", "冷却：%s / %s 刻");
        add("gui.sophisticated_smeltery.cast_waiting", "等待足够的流体");
        add("gui.sophisticated_smeltery.cast_preview", "配方预览");
        add("gui.sophisticated_smeltery.cast_input", "输入");
        add("gui.sophisticated_smeltery.cast_output", "输出");
        add("gui.sophisticated_smeltery.cast_no_controller", "安装熔炼控制器或储罐升级后，即可浇筑或退液。");
        add("gui.sophisticated_smeltery.cast_enabled", "浇筑：开");
        add("gui.sophisticated_smeltery.cast_disabled", "浇筑：关（已开始的冷却继续）");
        add("gui.sophisticated_smeltery.cast_output_storage", "产物：优先包内");
        add("gui.sophisticated_smeltery.cast_output_slot", "产物：输出槽堆积");
        add("gui.sophisticated_smeltery.cast_output_hint", "包内优先使用非熔炼槽；输出槽仅堆积相同物品及数据，放不下时等待。");
        add("gui.sophisticated_smeltery.cast_lock_off", "锁定当前模具");
        add("gui.sophisticated_smeltery.cast_lock_on", "模具已锁定");
        add("gui.sophisticated_smeltery.cast_lock_hint", "记住模具的物品及数据；消耗后从背包自动补充。再次点击解除锁定。");
        add("item.sophisticated_smeltery.melting_upgrade", "空白熔炼升级");
        add("item.sophisticated_smeltery.melting_upgrade.tooltip", "让背包按绑定控制器的规则熔炼物品。");
        add("item.sophisticated_smeltery.melting_upgrade.bound", "%s升级");
        add("error.sophisticated_smeltery.one_controller", "只能安装一个熔炼控制器。");
        add("error.sophisticated_smeltery.bind_controller", "安装前请先绑定受支持的控制器。");
        add("error.sophisticated_smeltery.empty_fluids", "移除控制器前，必须排空炉液和匠魂燃料储罐。");
        add("item.sophisticated_smeltery.scorched_alloyer_upgrade", "焦褐合金炉升级");
        add("item.sophisticated_smeltery.scorched_alloyer_upgrade.tooltip", "独立进行流体合金，可与熔炼升级共存。自带产物罐，九格组件槽支持八个原料储罐和一个燃料位。");
        add("error.sophisticated_smeltery.empty_alloyer", "排空产物罐及内部原料、燃料储罐后，才能移除此升级。");
        add("gui.sophisticated_smeltery.alloyer_switch_hint", "暂停或恢复合金及燃烧；已付费的剩余燃烧量会保留。");
        add("gui.sophisticated_smeltery.alloyer_output", "产物罐");
        add("gui.sophisticated_smeltery.alloyer_input", "原料罐 %s");
        add("gui.sophisticated_smeltery.alloyer_target_hint", "点击切换桶槽目标：产物罐或原料罐 1–8。液体显示区域也可持桶直接存取。");
        add("gui.sophisticated_smeltery.alloyer_input_hint", "原料储罐 %s：安装一个符合合金炉标签的储罐。");
        add("gui.sophisticated_smeltery.alloyer_fuel_hint", "燃料位（第九格）：可放固体燃料或燃料储罐。也可由背包储罐供给液体燃料。");
        add("gui.sophisticated_smeltery.materials", "结构材料");
        add("gui.sophisticated_smeltery.fuel_tank", "匠魂燃料储罐");
        add("gui.sophisticated_smeltery.heating_on", "加热：开");
        add("gui.sophisticated_smeltery.heating_off", "加热：关");
        add("gui.sophisticated_smeltery.temperature", "温度：%s ℃");
        add("gui.sophisticated_smeltery.empty", "空");
        add("gui.sophisticated_smeltery.fluid", "%s / %s mB");
        add("gui.sophisticated_smeltery.backpack_tank", "背包储罐 %s");
        add("gui.sophisticated_smeltery.tank_button", "储罐 %s");
        add("gui.sophisticated_smeltery.transfer_hint", "左键：储罐注入炉内；右键：炉液转入储罐。");
        add("item.sophisticated_smeltery.storage_spawner_upgrade", "刷怪笼升级");
        add("item.sophisticated_smeltery.storage_advanced_spawner_upgrade", "高级刷怪笼升级");
        add("item.sophisticated_smeltery.storage_spawner_upgrade.tooltip", "在存储位置按原生规则运行一个刷怪笼。");
        add("item.sophisticated_smeltery.storage_advanced_spawner_upgrade.tooltip", "忽略玩家距离和自然生成规则，保留空间、附近数量、刷怪间隔及红石控制。");
        add("item.sophisticated_smeltery.storage_advanced_transmute_upgrade", "空白高级嬗变炉升级");
        add("item.sophisticated_smeltery.storage_advanced_transmute_upgrade.bound", "高级%s升级");
        add("item.sophisticated_smeltery.storage_advanced_transmute_upgrade.tooltip", "支持嬗变、冶炼炉、熔铸炉三种模式，并可独立开关合金。");
        add("item.sophisticated_smeltery.storage_seared_casting_upgrade", "焦黑浇筑升级");
        add("item.sophisticated_smeltery.storage_advanced_seared_casting_upgrade", "高级焦黑浇筑升级");
        add("item.sophisticated_smeltery.storage_advanced_seared_casting_upgrade.tooltip", "优先使用炉内底层流体，也可使用储罐升级；支持手动或自动浇筑。");
        add("item.sophisticated_smeltery.storage_seared_casting_upgrade.tooltip", "使用匠魂浇筑配方，优先使用炉内底层流体，也可使用储罐升级。");
        add("item.sophisticated_smeltery.storage_scorched_casting_upgrade", "焦褐浇筑升级");
        add("item.sophisticated_smeltery.storage_advanced_scorched_casting_upgrade", "高级焦褐浇筑升级");
        add("item.sophisticated_smeltery.storage_scorched_casting_upgrade.tooltip", "按焦褐铸件台或铸造盆的规则浇筑，需要模具；可锁定模具并从存储补充。");
        add("item.sophisticated_smeltery.storage_advanced_scorched_casting_upgrade.tooltip", "支持手动或自动焦褐浇筑，需要模具；可锁定模具并从存储补充。");
        add("item.sophisticated_smeltery.storage_melting_upgrade", "空白熔炼升级");
        add("item.sophisticated_smeltery.storage_melting_upgrade.tooltip", "让存储按绑定控制器的规则熔炼物品。");
        add("item.sophisticated_smeltery.storage_melting_upgrade.bound", "%s升级");
        add("item.sophisticated_smeltery.storage_scorched_alloyer_upgrade", "焦褐合金炉升级");
        add("item.sophisticated_smeltery.storage_scorched_alloyer_upgrade.tooltip", "独立进行流体合金，可与熔炼升级共存。自带产物罐，九格组件槽支持八个原料储罐和一个燃料位。");
    }
}
