# Resource data generation

Run `./gradlew runData` (Windows: `./gradlew.bat runData`) from the project root.
Commit both the Java provider changes and the resulting files under
`src/generated/resources`. Normal builds package these checked-in resources and
do not launch Minecraft data generation.
Data-generator hash caches are ignored by Git and excluded from packaged
resources.

The providers in `org.brahypno.sophisticated_smeltery.data` own:

- Backpack and storage upgrade recipes, including tier upgrades, controller
  binding variants and conversions between hosts.
- Their advancement IDs, inventory criteria, requirements and optional-mod
  conditions.
- Both hosts' optional upgrade tags and item models.
- English and Simplified Chinese translations.

Host variants and shared crafting patterns are generated together. The providers
use resource IDs instead of accessing optional item registrations, so generating
data without a host or Esotericism Tinker still produces its conditional
resources. `--client` and `--server` select the corresponding providers.

Keep simple one-off JSON handwritten: the Mixin bootstrap configuration, the
`bind_controller` and `upgrade_clear` special-recipe entry points, and the
test-only entity melting blacklist. These do not have repeated variants that
benefit from another generator. Do not duplicate a generated path in
`src/main/resources`, as Gradle rejects duplicate packaged resources.

The migration compared all 106 generated JSON files with the original parsed
objects. Whitespace and object-key order were ignored; values, arrays, IDs,
conditions and advancement requirements were preserved.
