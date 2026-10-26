# Tick-path performance review

## Portable and world spawners

The portable wrapper previously encoded, copied and merged the complete spawner
NBT on every server tick, then serialized its item inventory for each changed
delay. Moving across a block boundary also recreated the registered block entity
and loaded the entire state again.

The wrapper now repositions its detached block entity within the same level.
Changing level or replacing the inventory item still creates a new entity through
the registered factory. No block entity placed in the world is repositioned.

For vanilla and the supported Apotheosis implementation, a tick starting with a
positive delay only changes that delay. After the first full snapshot, these
ticks update `Delay` in both the removable spawner and the upgrade's saved
inventory. The host save callback still runs for every changed delay, preserving
copy, extraction and unload behavior. Initialization and spawn attempts retain
full native serialization. Other registered spawner implementations retain full
serialization on every tick rather than assuming their countdown behavior.

World spawners resolve adjacent advanced support when native player activation
fails or when spawn rules are evaluated. A player-activated cooldown no longer
scans six neighboring blocks every tick. Results live only for the current
native tick, preserving immediate response to disabling or removing upgrades.
Upgrade lookup uses the host's existing type index.

Capture still updates storage for every birth so later births see occupied
slots. Viewer synchronization is deferred to the end of the native spawn tick
and sent once per affected storage, avoiding repeated player scans and growing
mob-list packets within one batch. Capture outside this context still syncs
immediately. Native collision, nearby-entity limits, redstone gating, modifiers
and spawn events remain in control.

## Casting, fuel and mob melting

- Automatic casting compares current fluids with its previous snapshots and
  copies those snapshots only after contents change. Full fluid NBT and amounts
  still participate, including when transfer limits hide changes from simulated
  drains.
- Fuel selection compares the current burn's temperature without copying its
  fluid NBT; a full paid-burn snapshot is created only when actually pausing it.
  Native ignition and temperature switching still decide consumption.
- Captured-mob melting checks the entity-type blacklist before constructing and
  loading a detached entity. It computes the melting-slot bound once per pulse
  and immediately skips empty pending-drop queues. Live damage, loot generation
  and entity state updates still use native entity behavior.

## Other reviewed paths

Melting inputs already rebind only when inventory or slot layout changes.
Recipe caches already invalidate on data-pack reload, and container transfers
already retry on fluid revisions. These mechanisms are retained. Alloying fuel
temperature demand still evaluates current input amounts and output capacity;
no cross-tick recipe-result cache is added over mutable tanks or custom recipes.
Controller model overlays already cache baked geometry by controller and facing;
their resource-reload invalidation is retained. Fluid capabilities continue to
read current tank contents instead of caching mutable handlers across upgrade
replacement.

These changes reduce identified encoding, allocation, lookup and packet work;
they do not establish a measured server TPS or latency improvement. Integration
tests cover exact countdown persistence during movement and reconstruction,
inactive spawners, spawn batches, all six support faces, capture limits and
optional native spawner modifiers.

Validation completed with `runGameTestServer build --offline` (66 required
tests), the same command with `-PwithApotheosis=true -PwithEstinker=true` (66),
and `runData runGameTestServer build -PwithBackpacks=false --offline` (9).
Storage-only data generation reproduced the same 106 parsed JSON resources.
`runClient -PspawnerSmoke --offline` also passed actual screen insertion and
extraction, server spawning and capture, client mob display, the disable switch,
and retained spawner NBT. The final build excludes data-generation hash caches
and test fixtures from the mod jar.
The optional-dependency runs log external resource errors: Apotheosis references
an absent `patchouli:guide_book`, and Storage advancements reference absent
Backpacks items in the Storage-only run. These are outside this mod's generated
resource namespaces; the required tests and builds pass.
