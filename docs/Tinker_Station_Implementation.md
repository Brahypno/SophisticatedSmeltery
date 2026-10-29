# Tinker station and anvil upgrades

`tinker_station_upgrade` provides Tinkers' three ingredient slots and one tool
slot. `tinkers_anvil_upgrade` is its advanced tier, with five ingredient slots.
Both also have a result slot. Storage variants use the `storage_` prefix.
Each host accepts its own upgrade plate; both tiers can coexist with other
crafting, part builder, melting, casting and alloying upgrades.

The anvil tier accepts both `tconstruct:tinkers_anvil` and
`tconstruct:scorched_anvil`. Crafting saves the complete native workstation stack
under `tinkerStationBlock`, retaining wood retexturing, metal material variants,
and anvil block skins. Existing upgrades can be crafted with a matching-tier
workstation to replace their appearance and return the previously bound stack;
tools, ingredients and settings remain intact. Clearing an empty upgrade also
returns its explicitly bound workstation. Legacy and creative stacks use the
original default appearance until one is bound. Host conversions preserve it.
The native model's own item overrides supply textures and colors. Anvils use a
side view; stations expose their top and legs because only the legs are
retextured by the native model. Appearance support adds no Mixin.

The wrapper uses registered `ITinkerStationRecipe` instances for matching,
validated results, tool-slot consumption and ingredient updates. Tool building,
repair, modifiers, part swapping, recipe errors and crafting remainders follow
native recipe behavior. Large tools requiring more than three ingredients need
the anvil tier. Previewing never consumes items. Taking a result crafts once;
Shift-click follows the selected player/storage destination.

The sidebar is 125 GUI pixels wide. Client config
`config/sophisticated_smeltery-client.toml` exposes
`[tinkerStation] toolSelectionRows` (1–5, default 3), shared by both tiers and
hosts. Each row shows five native layouts, with wheel scrolling for more.
Changing the row count moves the ingredient slots, retained-item row, destination
button, result and rename field, and resizes the sidebar. Heights for 1–5 rows
are 170, 170, 188, 206 and 224 pixels; the shortest layouts reserve space for
all five equipment slots and keep the result beneath them. Reopen the storage
screen to apply a changed value. The default is 36 pixels shorter than five rows.
It uses native
station layouts, icons, ingredient hints and slot filters. Layout buttons reuse
the native `icons.png` wood style for the station and metal style for the anvil,
including selected and hovered states. Slots and the unbordered rename field use
`tinker.png`. Switching layouts keeps existing items accessible in a separate row
immediately below the picker, above the active ingredient layout. The Shift-click
destination button sits to the left of this row. The rename field stays at the bottom.
Four player armor slots occupy the former arrow-button column; the player
offhand slot sits beneath them. These are live player inventory slots in both
tiers and hosts, using Tinkers' native `ArmorSlot` filters, stack limit and
Binding Curse rules. Closing the tab hides them; closing the storage leaves
equipped items on the player. The open host item is locked against removal
through these equipment aliases.
The rename field appears when a result is available, with no placeholder caption;
hovering it explains its purpose. The text box names the result;
the name clears after crafting, as in the native station. Hover over the
result slot for the complete recipe error or usage instructions.


The selected layout caption is omitted to keep the sidebar compact.
Tool information is a separate 166 by 244 GUI pixel
window, initially placed to the left of the storage and its upgrade controls.
Its two panels are always visible together: tool stats above and modifiers below.
Before a tool exists, they show the native layout instructions and required
components instead, marking missing parts red. Drag either caption to move both
panels within the screen. Both remain available alongside the crafting slots.
Closing the workstation tab hides the window, and reopening
that tab keeps its position for the current screen. A newly opened screen starts
on the left again. Each panel scrolls independently with its wheel area and
draggable scrollbar. The panels use the native `panel.png` dark background, wood/metal
border and scrollbar, plus the native underlined captions, light body text and
the original 8/9 and 7/9 text scales for stats and modifiers respectively.
Long lines wrap with their native colors. Floating-window input is
consumed before inventory and recipe-viewer input, including when it overlaps slots.

Equipment is registered after the result slot, outside the recipe inventory.
The storage core has no public hook for its automatic upgrade-input range or
its upgrade-slot cleanup predicate. `TinkerStationEquipmentMixin` limits recipe
Shift transfers to the crafting slots and excludes these player-owned slots
from upgrade-inventory cleanup. Both hooks apply only to this workstation.

Stats use `ITinkerStationDisplay.getStatInformation` with the same tooltip key
and `TINKER_STATION` flag as the native workstations. Modifier names, levels,
visibility and hover descriptions use the native modifier hooks. Shift filters
to applied upgrades; Ctrl filters to material traits. Information prefers the
validated output preview, falling back to the tool slot when no preview exists.

Basic recipe:

```text
 T
IBI
 C
```

T is `tconstruct:tinker_station`, I is an iron ingot, B is the matching host's
upgrade base, and C is a chest.

Advanced recipe:

```text
 A
GUG
RRR
```

A is `tconstruct:tinkers_anvil`, G is a gold ingot, U is the matching basic
station upgrade, and R is redstone dust. The recipe preserves the complete
upgrade NBT and expands the restored inventory from four to six slots.
Both tiers have the existing bidirectional host conversion recipes.

Inputs, layout, pending name and Shift destination persist on the item. Removing
the upgrade preserves its contents. Clearing requires every input and tool slot
to be empty. Server and client recipe reloads invalidate open result previews.

Checks:

```powershell
./gradlew.bat runData compileTestJava --offline
./gradlew.bat runGameTestServer build --offline
./gradlew.bat runGameTestServer -PwithBackpacks=false --offline
./gradlew.bat runClient -PtinkerStationSmoke --offline
```

Five `TinkerStationUpgradeGameTests` cover both installed hosts and both tiers,
with native tool and material-consumption comparisons, upgrade-state transfer,
new anvil slots, clearing and recipe reloads. Equipment coverage uses real host
menus to check armor filters, Binding Curse, offhand clicks, recipe transfer
exclusion, host-item locking and equipment preservation on close.
The client check uses the smallest
leather backpack at 1280 by 720 with GUI scale 2, checks the sidebar bounds, and
clicks through layout selection, input placement, naming, crafting, Shift output
and reopening for both tiers. It selects layouts in the added picker rows,
scrolls over the fifth row, and checks that every visible crafting slot fits
between the picker and bottom controls. It also compares preview/input stats and modifier
data with native display hooks, checks simultaneous crafting and both stacked
panels without input changes, and exercises independent wheel scrolling and
dragging the lower scrollbar with a long modifier list. Required-component
labels are checked before a tool exists and after inserting a matching part.
Title dragging checks both panels move together, all four screen edges, release,
and clicks over inventory slots. Screenshots are written under `build/`.
Verified on 2026-10-27:

- Data generation and test compilation passed (`build/tinker-station-datagen.log`).
- The full 78-test suite and build passed (`build/tinker-station-tests.log`).
- Both tiers passed the real small-backpack client interaction check and the
  client task completed successfully (`build/tinker-station-client.log`).
- GUI screenshots: `build/tinker-station-small-selected.png` and
  `build/tinkers-anvil-small-selected.png`; empty and reopened states are also
  saved beside them.
- Storage-only: all 14 tests and the final build passed (`build/tinker-station-storage-only.log`).
- Floating information: all 78 required tests, build and client interactions
  passed (`build/tinker-floating-final.log`). Dragging covers all screen edges
  and slot click-through; both crafting tiers remain usable beside the window.
- Native artwork: build and both-tier client interactions passed after applying
  native button styles, panel borders/background, slots and rename field
  (`build/tinker-native-ui-final.log`). The final anvil preview is captured in
  `build/tinkers-anvil-preview-stats.png`.

- Stacked panels: build and both-tier client checks passed
  (`build/tinker-dual-panels.log`). Both panels display native preview/input data
  simultaneously, scroll independently, and move together. The lower panel
  includes native modifier hover descriptions, a draggable scrollbar, and
  required-component labels that update their colors as parts are inserted.
  Screenshots: `build/tinkers-anvil-input-panels.png` and
  `build/tinker-station-required-components.png`.
- Five-row picker: build and both-tier client checks passed
  (`build/tinker-five-row-picker.log`), including selection from the extra rows,
  wheel scrolling, slot bounds, naming, crafting and reopening. The sidebar
  remains 125 by 224 pixels. Screenshot: `build/tinkers-anvil-preview-panels.png`.
- Player equipment and five columns: all 79 full-suite tests passed
  (`build/tinker-equipment-tests.log`), and all 15 Storage-only tests passed
  (`build/tinker-equipment-storage-only.log`). Final build and both-tier client
  clicks passed with five tool columns, four armor slots, offhand, the relocated
  destination button and wheel-only layout browsing
  (`build/tinker-five-columns-equipment.log`). The client equips all five slots,
  verifies reopening, and switches layouts while retaining a tool. Screenshots:
  `build/tinkers-anvil-small-selected.png`, `build/tinker-station-equipment.png`,
  and `build/tinkers-anvil-equipped-layout-switch.png`.
- Native appearances: build and all 80 full-suite tests
  passed (`build/workstation-appearance-verified.log`). Both anvil sources craft
  large tools, retain material NBT, and preserve inventory/settings on rebinding,
  host conversion and network synchronization. Rebinding and empty clearing
  return the exact previously bound workstation. The real-world client verifies
  eight distinct wood/material/block-skin models per host, including side views,
  native host plates and advanced frames. Screenshot:
  `build/storage-compat/workstation-appearances-both.png`.
  All 16 Storage-only tests also passed
  (`build/mixin-reduction-appearance-storage-only.log`).
- Commit validation: the workstation feature compiles independently from its
  staged sources (`build/commit-groups/foundation-compile.log`). The final build
  and all 80 required tests passed (`build/commit-groups/final-validation.log`).
- Configurable picker rows: build and real-client checks passed
  (`build/tinker-configurable-rows.log`). Both tiers verify all five row settings,
  scrolling, layout selection, panel height and slot bounds without overlaps.
  The default three-row layout also passes naming, crafting, Shift-click output,
  equipment, reopening and floating-panel interactions. Screenshots:
  `build/tinkers-anvil-small-selected.png` and `build/tinkers-anvil-rows-1.png`
  through `build/tinkers-anvil-rows-5.png` (station equivalents are also saved).
