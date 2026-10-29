# GameTest suites

The default `runGameTestServer` command runs the full 86-test suite. Use it
for release validation or changes outside the focused coverage below. This
includes three slot-transfer algorithm tests: 128 seeded differential layouts,
blocked-path operation counts and immediate recovery, and native void callbacks.
Slot-transfer changes require the full suite because the focused suite does not
select these tests or `CastingExpansionGameTests`.

Controller appearance is covered by `controllerAppearanceTracksIgnitionAndHeatingSwitch`:
ignition, heating off/on with remaining fuel, exhaustion, and render updates only
when the active state changes. Run `./gradlew.bat runClient -PbackpackControllerSmoke --offline`
for an idle/burning comparison of both controller types in inventory and worn views.
It also checks the lower-half UV crop, pouch coverage and all four placed orientations;
the preview is saved to `build/backpack-controller-smoke.png`.
Add `-PwithEstinker=true` to include idle and heater-only active Transmute controllers.
`heaterOnlyControllerAppearanceTracksHeatAndSwitch` checks both Transmute upgrade
tiers with no fuel, including heating off/on and removal of the heater components.

For tick-path performance changes, use the focused suite:

```powershell
./gradlew.bat runGameTestServer -PgameTestSuite=performance --offline
./gradlew.bat runGameTestServer -PgameTestSuite=performance -PwithApotheosis=true -PwithEstinker=true --offline
./gradlew.bat runGameTestServer -PgameTestSuite=performance -PwithBackpacks=false --offline
```

This matrix executes **25 + 25 + 16 = 66** tests with the current test set:

| Focused classes | Tests | Coverage |
| --- | ---: | --- |
| SpawnerUpgradeGameTests | 11 | Spawn batches, native rules and modifiers, adjacent support, movement, persistence and capture limits |
| FurnaceGameTests | 6 | Native controllers, input/reload lifecycle, fuel choice, paid-burn preservation, alloying and byproducts |
| MobMeltingGameTests | 5 | Damage, fluids, limits, immunity, drops, real hosts and optional transmute recipes |
| CastingUpgradeGameTests | 2 | Manual and automatic casting, heating independence, removal/reload and shared fluids |
| CastingRecipeGameTests | 1 | Tier recipe state preservation and network serialization |

Without Backpacks, both suite names run the 16 Storage/workstation tests,
covering container capabilities, linked storage, spawning, optional recipes and
host conversions. These test classes compile independently of Backpacks.

`GameTestRegistration` uses Forge's scanned `@GameTest` methods and the standard
registration event. All test bodies and assertions are retained. The focused
class selection is applied only during registration, so shared test helpers
remain available. New test classes are discovered by the full suite without
adding them to another class list. Counts grow if tests are added to selected
classes; 25 is the current coverage count, not a limit.
New test classes use `@GameTest` methods with explicit template namespaces and
omit `@GameTestHolder`, since registration is centralized here.
Run host configurations sequentially because they share compiled test outputs.
Registration fails when no test classes are discovered, avoiding Forge's
otherwise successful result for zero executed tests.

The focused suite does not cover every bucket interaction, rendering behavior,
controller control, or linked-storage scenario available in the full suite.
Choose additional tests when those areas change. Data-generation validation and
client smoke checks are separate from these server counts. Minecraft/Forge
startup still contributes to elapsed time, so reducing the test count does not
imply the same percentage reduction in wall-clock time.


Part builder changes are covered by four `PartBuilderUpgradeGameTests`, the
Storage-only part builder test, and host conversion/registration checks in
`StorageCompatibilityGameTests`. The client interaction test runs with
`./gradlew.bat runClient -PpartBuilderSmoke --offline`; it also captures screenshots.
See [Part_Builder_Implementation.md](Part_Builder_Implementation.md) for behavior
and recipe details.


Tinker station changes add six tests, including native recipe comparisons, player equipment rules and appearance preservation, also included in Storage-only runs. The real small-backpack UI check is `./gradlew.bat runClient -PtinkerStationSmoke --offline`; native material and skin rendering is checked with `./gradlew.bat runClient -PstorageCompatSmoke -PworkstationAppearanceSmoke --offline`. See [Tinker_Station_Implementation.md](Tinker_Station_Implementation.md).
The workstation UI check sweeps client `toolSelectionRows` values 1–5 for both
tiers, checking selection, scrolling and slot bounds, then restores the original
setting and runs the crafting, naming, equipment and reopen interaction checks.
