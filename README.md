# Random Powers (Forge mod, Minecraft 1.20.1)

The first time a player joins the world, they're randomly given one permanent
power. It sticks with them forever — surviving death, milk buckets, dimension
travel, and server restarts.

This is a **Forge 1.20.1 port** of the original Fabric mod. Gameplay is the
same, with one change: **Ore Sense has been toned down** (see below). Under
the hood it's a full rewrite — Forge uses different mappings (Mojang's
official names instead of Yarn) and different APIs, and it turns out Forge
has native events for things the Fabric version needed Mixins for, so there
are no Mixins in this version at all.

## What changed from the Fabric version

**Ore Sense nerf** — the numbers to retune are the `RADIUS` and
`MAX_REVEALED_PER_SCAN` constants and the `isValuableOre` list inside the
`XRAY` constant in `Power.java`, plus `ORE_SENSE_BLOCKS_REQUIRED` and
`ORE_SENSE_ACTIVE_TICKS` in `PowerRuntime.java`:
- Detection radius: 15 blocks → **8 blocks**
- Ore list: dropped iron, gold, redstone, lapis, and copper — only
  **diamond, emerald, and ancient debris** show up now
- Capped to the **6 nearest** matches per scan, so it can no longer light up
  an entire cave system at once
- **No longer always-on.** It now has to be earned: mine **200 blocks**
  (creative-mode breaking doesn't count) and you get a **3-second burst** of
  detection, scanning twice a second during that window. The counter and
  active-burst timer live in `PowerRuntime` (not saved to disk, same as the
  Overdrive toggle and Voidstep cooldown — losing progress on a restart is
  fine), and the actual block-mining hook is a `BlockEvent.BreakEvent`
  listener in `ServerEventHandler.java`.

**No Mixins** — Ragemode/Ember Touch's damage hooks now use Forge's
`LivingHurtEvent`/`LivingDamageEvent`, Featherfall uses `LivingFallEvent`,
and Cave Dweller's nametag-hiding uses `RenderNameTagEvent`. Fabric doesn't
expose plain API events for any of these, which is why the original needed
Mixins into `LivingEntity#damage` and the entity renderer.

**Single source set** — Forge lets client-only code (keybindings, the
nametag event) live in the normal `src/main/java` tree, guarded by
`@Mod.EventBusSubscriber(..., value = Dist.CLIENT)`, instead of needing a
separate `src/client` source set like Fabric does.

## Powers included

Same 17 powers as before — Strength, Swiftness, Regeneration, Cinderborn,
Nightsight, Deep Lungs, Springheel, Featherfall, Lodestone, Ore Sense
(nerfed, see above), Quarryman, Overdrive, Nocturne, Voidstep, Ragemode,
Cave Dweller, and Ember Touch. Check `Power.java` for exact numbers, or run
`/mypower` in-game to see what you got.

### Default keybinds

- **G** — toggle Overdrive's speed boost (if you have that power)
- **V** — Voidstep blink (if you have that power)

Both are rebindable in Minecraft's Controls menu under "Random Powers".

## How to build

You need a JDK 17+ installed. From this folder:

```bash
./gradlew build
```

(On Windows use `gradlew.bat build`.) The compiled mod jar will be at
`build/libs/randompowers-1.0.0.jar`.

> Note: the `gradlew`/`gradlew.bat` wrapper scripts and the `gradle/wrapper`
> folder aren't included in this package — Gradle will offer to generate
> them on first run, or install Gradle and run `gradle wrapper` once, then
> re-run the build command above. The first build will also take a while,
> since Forge's toolchain needs to download and process Minecraft/Forge
> itself.

## How to install

1. Install [Minecraft Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
   for **Minecraft 1.20.1** on your client (via the Forge installer, which
   adds a new profile to the vanilla launcher) and/or your server (using
   the installer's "Install server" option).
2. Drop `randompowers-1.0.0.jar` into the `mods` folder — this is
   `.minecraft/mods` for a client install, or the `mods` folder next to
   your server jar for a dedicated server.
3. Launch the game / start the server.

This mod needs to be installed on **both** the client and the server (or
just the client, for singleplayer) — Overdrive, Voidstep, and Cave
Dweller's nametag-hiding are client-driven features, same as in the
original Fabric version.

## Versions used

This project targets Minecraft 1.20.1 with Forge `47.3.0`. If that build is
no longer current, check
https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html
and bump `forge_version` in `gradle.properties` — any 1.20.1 build in the
47.x line should be a drop-in replacement.

## A note on balance

Ore Sense aside, Ragemode's 500% HP/damage and the always-on speed/haste
powers are still intentionally strong — this is a "big, fun, chaotic"
power mod rather than a balanced PvP one. If you want to tone anything else
down, the numbers to tweak are the `amplifier` values passed to
`maintainEffect(...)` and the multiplier constants in `Power.java` and
`ServerEventHandler.java`.
