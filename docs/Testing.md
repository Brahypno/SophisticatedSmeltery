# GameTest suites

The default `runGameTestServer` command retains the full 66-test suite. Use it
for release validation or changes outside the focused coverage below.

For tick-path performance changes, use the focused suite:

```powershell
./gradlew.bat runGameTestServer -PgameTestSuite=performance --offline
./gradlew.bat runGameTestServer -PgameTestSuite=performance -PwithApotheosis=true -PwithEstinker=true --offline
./gradlew.bat runGameTestServer -PgameTestSuite=performance -PwithBackpacks=false --offline
```

This matrix executes **25 + 25 + 9 = 59** tests with the current test set:

| Focused classes | Tests | Coverage |
| --- | ---: | --- |
| SpawnerUpgradeGameTests | 11 | Spawn batches, native rules and modifiers, adjacent support, movement, persistence and capture limits |
| FurnaceGameTests | 6 | Native controllers, input/reload lifecycle, fuel choice, paid-burn preservation, alloying and byproducts |
| MobMeltingGameTests | 5 | Damage, fluids, limits, immunity, drops, real hosts and optional transmute recipes |
| CastingUpgradeGameTests | 2 | Manual and automatic casting, heating independence, removal/reload and shared fluids |
| CastingRecipeGameTests | 1 | Tier recipe state preservation and network serialization |

Without Backpacks, both suite names run the 9 Storage compatibility tests,
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
