# Part builder upgrade

`part_builder_upgrade` and `storage_part_builder_upgrade` add a portable Tinkers'
part builder to Sophisticated Backpacks and Sophisticated Storage respectively.
Each host uses its own upgrade base and upgrade tag. One can be installed per
container, alongside melting, casting, alloying, and crafting upgrades.

The shaped recipe follows the host's crafting upgrade:

```text
 T 
IBI
 C 
```

- T: Tinkers' part builder (`tconstruct:part_builder`)
- I: iron ingot (`forge:ingots/iron`)
- B: the matching host's upgrade base
- C: chest (`forge:chests`)

The sidebar has a four-by-four pattern picker with a native draggable scrollbar,
pattern and material slots, a result slot, material value/cost, and the host crafting upgrade's Shift-click
destination icon. The picker background, scrollbar, slot backgrounds, pattern artwork,
and aligned slot hints use Tinkers' existing assets.
Taking a result crafts once; viewing a preview does not consume items. Shift-click
prefers either the player's inventory or the host's storage, as selected, and
crafts a batch from the supplied inputs up to one result stack.

The wrapper calls the registered `IPartBuilderRecipe` methods for matching,
assembly, costs and leftovers. Recipe-specific patterns, part recycling, reusable
patterns, and multi-item outputs retain native recipe behavior. Leftovers and
crafting containers return to the player. A partial Shift transfer returns the
rest of that recipe's output to the player instead of discarding it.

Inputs, selected pattern, and Shift destination are saved on the upgrade item.
Host conversion recipes preserve that data. The upgrade can be removed with its
inputs intact; its clearing recipe requires empty input slots. Server reloads and
client recipe updates invalidate cached pattern lists and result previews.

Verification:

```powershell
./gradlew.bat runData --offline
./gradlew.bat runGameTestServer build --offline
./gradlew.bat runGameTestServer -PwithBackpacks=false --offline
./gradlew.bat runClient -PpartBuilderSmoke --offline
```

The client smoke test clicks the real screen to insert both inputs, select a
pattern, take a part, change Shift destination, transfer a batch into
storage, and reopen the backpack. Screenshots are written under `build/`.
