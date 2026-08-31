package com.example.randompowers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Every possible power a player can be randomly granted.
 * Add new constants here and they will automatically be included
 * in the random pool.
 */
public enum Power {

    STRENGTH("Strength", "Your muscles surge with unnatural power. (+3 attack damage, permanent, no potion icon)") {
        private final java.util.UUID DAMAGE_MODIFIER_ID = java.util.UUID.fromString("a1b2c3d4-1111-4a2b-8c3d-1a2b3c4d5e6f");

        @Override
        public void onTick(ServerPlayer player) {
            // Strength I is +3 flat attack damage; applying it as a genuine attribute
            // modifier (like the game's own base stats) means it never shows up as a
            // potion icon, isn't cleared by milk, and doesn't fight with real Strength potions.
            setAttributeModifierPresence(player, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER_ID,
                    "randompowers_strength", 3.0, AttributeModifier.Operation.ADDITION, true);
        }
    },

    SPEED("Swiftness", "Your legs feel lighter than air. (+20% movement speed, permanent, no potion icon)") {
        private final java.util.UUID SPEED_MODIFIER_ID = java.util.UUID.fromString("a1b2c3d4-2222-4a2b-8c3d-1a2b3c4d5e6f");

        @Override
        public void onTick(ServerPlayer player) {
            setAttributeModifierPresence(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID,
                    "randompowers_speed", 0.2, AttributeModifier.Operation.MULTIPLY_TOTAL, true);
        }
    },

    REGENERATION("Regeneration", "Your wounds knit themselves closed. (Passive healing, forever, no potion icon)") {
        @Override
        public void onTick(ServerPlayer player) {
            maintainHiddenEffect(player, MobEffects.REGENERATION, 0);
        }
    },

    FIRE_IMMUNITY("Cinderborn", "Flames part around you like water. (Immune to fire, forever, no potion icon)") {
        @Override
        public void onTick(ServerPlayer player) {
            maintainHiddenEffect(player, MobEffects.FIRE_RESISTANCE, 0);
        }
    },

    NIGHT_VISION("Nightsight", "The dark holds no secrets from you anymore. (See in the dark, forever, no potion icon)") {
        @Override
        public void onTick(ServerPlayer player) {
            maintainHiddenEffect(player, MobEffects.NIGHT_VISION, 0);
        }
    },

    WATER_BREATHING("Deep Lungs", "The ocean feels like home now. (Never run out of air, forever, no potion icon)") {
        @Override
        public void onTick(ServerPlayer player) {
            maintainHiddenEffect(player, MobEffects.WATER_BREATHING, 0);
        }
    },

    JUMP_BOOST("Springheel", "Your legs coil like springs. (Higher jumps, forever, no potion icon)") {
        @Override
        public void onTick(ServerPlayer player) {
            maintainHiddenEffect(player, MobEffects.JUMP, 1);
        }
    },

    FEATHER_FALL("Featherfall", "Falling no longer hurts you. (Immune to fall damage, forever)") {
        @Override
        public void onTick(ServerPlayer player) {
            // Handled by a LivingFallEvent listener in ServerEventHandler, nothing to tick.
        }
    },

    MAGNET("Lodestone", "Nearby loose items drift toward your hands. (Item magnet, forever)") {
        @Override
        public void onTick(ServerPlayer player) {
            Level world = player.level();
            double radius = 6.0;
            AABB box = player.getBoundingBox().inflate(radius);
            Predicate<Entity> notHeldByOther = e -> true;
            List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, box, notHeldByOther);
            for (ItemEntity item : items) {
                if (item.isRemoved()) {
                    continue;
                }
                Vec3 toPlayer = player.position().add(0, 0.6, 0).subtract(item.position());
                double distance = toPlayer.length();
                if (distance < 0.3) {
                    continue;
                }
                double pullStrength = 0.18;
                Vec3 pull = toPlayer.normalize().scale(pullStrength);
                item.setDeltaMovement(item.getDeltaMovement().add(pull));
                item.hurtMarked = true;
            }
        }
    },

    /*
     * NERFED from the Fabric version, twice over. First: radius cut from
     * 15 -> 8 blocks, the ore list trimmed down to genuinely rare finds
     * (iron/gold/redstone/lapis/copper removed since those aren't worth
     * detecting), and capped to the 6 nearest matches per scan so it can't
     * instantly reveal an entire cave system at once. Second: it's no longer
     * always-on. Mining ORE_SENSE_BLOCKS_REQUIRED blocks (see PowerRuntime)
     * earns one ORE_SENSE_ACTIVE_TICKS-long burst of detection — the actual
     * mining/counting happens in ServerEventHandler's BlockEvent.BreakEvent
     * listener, since that's where Forge tells us a block was broken.
     */
    XRAY("Ore Sense", "Mine 200 blocks to earn a 3-second burst of ore detection within 8 blocks, for your eyes only. (Ore Sense, forever)") {
        private static final int RADIUS = 8;
        private static final int MAX_REVEALED_PER_SCAN = 6;

        @Override
        public void onTick(ServerPlayer player) {
            long currentTick = player.level().getGameTime();
            if (!PowerRuntime.isOreSenseActive(player.getUUID(), currentTick)) {
                return; // hasn't mined enough yet to have an active burst
            }
            if (player.tickCount % 10 != 0) {
                return; // scan every half-second during the burst
            }
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            BlockPos center = player.blockPosition();
            List<BlockPos> found = new ArrayList<>();
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                    for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                        if (dx * dx + dy * dy + dz * dz > RADIUS * RADIUS) {
                            continue;
                        }
                        BlockPos pos = center.offset(dx, dy, dz);
                        Block block = serverLevel.getBlockState(pos).getBlock();
                        if (isValuableOre(block)) {
                            found.add(pos.immutable());
                        }
                    }
                }
            }
            found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
            int limit = Math.min(found.size(), MAX_REVEALED_PER_SCAN);
            for (int i = 0; i < limit; i++) {
                BlockPos pos = found.get(i);
                serverLevel.sendParticles(player, ParticleTypes.END_ROD, false,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        private boolean isValuableOre(Block block) {
            return block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                    || block == Blocks.ANCIENT_DEBRIS
                    || block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE;
        }
    },

    HASTE("Quarryman", "Your pickaxe practically swings itself. (Mining speed roughly doubled, forever)") {
        @Override
        public void onTick(ServerPlayer player) {
            // No vanilla attribute governs mining speed, so this one still has to run
            // through the effect system under the hood — but it's fully hidden (no
            // icon, no particles, no HUD indicator), so nothing shows up on screen.
            maintainHiddenEffect(player, MobEffects.DIG_SPEED, 4); // Haste V ~ +100% mining speed
        }
    },

    OVERDRIVE("Overdrive", "Press your bound key to kick into 200% walk speed on demand. (Toggle boost, no potion icon)") {
        private final java.util.UUID OVERDRIVE_MODIFIER_ID = java.util.UUID.fromString("a1b2c3d4-3333-4a2b-8c3d-1a2b3c4d5e6f");

        @Override
        public void onTick(ServerPlayer player) {
            boolean active = PowerRuntime.isOverdriveActive(player.getUUID());
            setAttributeModifierPresence(player, Attributes.MOVEMENT_SPEED, OVERDRIVE_MODIFIER_ID,
                    "randompowers_overdrive", 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL, active);
        }
    },

    NOCTURNE("Nocturne", "The night belongs to you: automatic bonus speed after dark, and you can see in the dark too. (forever)") {
        private final java.util.UUID NOCTURNE_SPEED_MODIFIER_ID = java.util.UUID.fromString("a1b2c3d4-4444-4a2b-8c3d-1a2b3c4d5e6f");

        @Override
        public void onTick(ServerPlayer player) {
            Level world = player.level();
            boolean isNight = world.getDayTime() % 24000 >= 13000 && world.getDayTime() % 24000 <= 23000;
            setAttributeModifierPresence(player, Attributes.MOVEMENT_SPEED, NOCTURNE_SPEED_MODIFIER_ID,
                    "randompowers_nocturne_speed", 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL, isNight);
            // No vanilla attribute exists for night vision, so that half still has to
            // ride on the effect system — hidden, so it's invisible on screen either way.
            if (isNight) {
                maintainHiddenEffect(player, MobEffects.NIGHT_VISION, 0);
            }
        }
    },

    BLINK("Voidstep", "Press your bound key to blink up to 100 blocks forward. (30s cooldown, forever)") {
        @Override
        public void onTick(ServerPlayer player) {
            // Entirely packet driven; see BlinkPacket / RandomPowersMod#performBlink.
        }
    },

    RAGE_MODE("Ragemode", "500% max health, and the lower your health drops, the harder you hit (up to 500% damage). (forever)") {
        private final java.util.UUID HEALTH_MODIFIER_ID = java.util.UUID.fromString("7d1a4b2e-5a3e-4b8f-9c7a-1f2e3d4c5b6a");

        @Override
        public void onTick(ServerPlayer player) {
            AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null && maxHealth.getModifier(HEALTH_MODIFIER_ID) == null) {
                maxHealth.addPermanentModifier(new AttributeModifier(
                        HEALTH_MODIFIER_ID, "randompowers_ragemode_health", 4.0,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
                player.setHealth(player.getMaxHealth());
            }
            // Outgoing damage scaling is handled by the LivingHurtEvent listener in ServerEventHandler.
        }
    },

    CAVE_DWELLER("Cave Dweller", "You can no longer read nametags, and caves are pitch black unless you're holding a lantern. (forever)") {
        @Override
        public void onTick(ServerPlayer player) {
            Level world = player.level();
            boolean holdingLantern = player.getMainHandItem().is(Items.LANTERN)
                    || player.getOffhandItem().is(Items.LANTERN)
                    || player.getMainHandItem().is(Items.SOUL_LANTERN)
                    || player.getOffhandItem().is(Items.SOUL_LANTERN);
            boolean seesSky = world.canSeeSky(player.blockPosition());
            if (!seesSky && !holdingLantern) {
                maintainHiddenEffect(player, MobEffects.BLINDNESS, 0);
            }
            // Nametag hiding is handled client-side for the owning player via a synced flag.
        }
    },

    EMBER_TOUCH("Ember Touch", "Enemies you strike burst into flame for 10 seconds. (forever)") {
        @Override
        public void onTick(ServerPlayer player) {
            // Passive; the ignite-on-hit effect is handled by the LivingDamageEvent listener in ServerEventHandler.
        }
    };

    private final String displayName;
    private final String message;

    Power(String displayName, String message) {
        this.displayName = displayName;
        this.message = message;
    }

    public abstract void onTick(ServerPlayer player);

    public String getDisplayName() {
        return displayName;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Re-applies an infinite status effect, completely invisible to the player
     * (no particles, no HUD icon, doesn't appear on the inventory effects screen),
     * if they don't currently have it (or have a weaker version / one about to run
     * out). Used only for the handful of powers that have no vanilla attribute
     * equivalent (regeneration, fire resistance, night vision, water breathing,
     * jump strength, mining speed, blindness) — everything else uses a real
     * attribute modifier instead, via {@link #setAttributeModifierPresence}.
     * This still survives milk buckets, death, dimension changes, and
     * effect-clearing commands from other mods, same as before.
     */
    private static void maintainHiddenEffect(ServerPlayer player, net.minecraft.world.effect.MobEffect effect, int amplifier) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing == null || existing.getAmplifier() < amplifier || existing.getDuration() < 100) {
            // ambient=true, showParticles=false, showIcon=false: completely invisible.
            player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, false, false));
        }
    }

    /**
     * Ensures a named attribute modifier is present (or absent) on the player,
     * adding/removing it only when its current state doesn't match. This is how
     * "innate" stat boosts like Strength and Speed are granted now — as genuine
     * attribute modifiers, exactly like the game's own equipment and base stats,
     * rather than status effects. They never show a potion icon or particles,
     * they aren't cleared by milk buckets or effect-clearing commands, and they
     * stack cleanly alongside any real potion effects of the same kind the
     * player might drink.
     */
    private static void setAttributeModifierPresence(ServerPlayer player, Attribute attributeType,
            java.util.UUID modifierId, String modifierName, double amount,
            AttributeModifier.Operation operation, boolean shouldBePresent) {
        AttributeInstance instance = player.getAttribute(attributeType);
        if (instance == null) {
            return;
        }
        boolean present = instance.getModifier(modifierId) != null;
        if (shouldBePresent && !present) {
            instance.addTransientModifier(new AttributeModifier(modifierId, modifierName, amount, operation));
        } else if (!shouldBePresent && present) {
            instance.removeModifier(modifierId);
        }
    }
}
