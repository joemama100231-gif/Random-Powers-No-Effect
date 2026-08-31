package com.example.randompowers;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * All server-side gameplay hooks. Note that unlike the Fabric version, none
 * of this needs Mixins — Forge exposes native events for damage modification
 * (LivingHurtEvent/LivingDamageEvent) and fall damage (LivingFallEvent) that
 * the Fabric API doesn't have equivalents for, which is why that version had
 * to reach for a Mixin into LivingEntity#damage.
 */
@Mod.EventBusSubscriber(modid = RandomPowersMod.MOD_ID)
public final class ServerEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        PowerState state = PowerState.get(server);

        if (!state.hasPower(player.getUUID())) {
            Power granted = RandomPowersMod.randomPower();
            state.setPower(player.getUUID(), granted);

            player.sendSystemMessage(Component.literal("\u00a76\u00a7lA power awakens within you: \u00a7e" + granted.getDisplayName()));
            player.sendSystemMessage(Component.literal("\u00a77" + granted.getMessage()));
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);

            RandomPowersMod.LOGGER.info("Granted power {} to player {}", granted, player.getGameProfile().getName());
        }

        RandomPowersMod.sendPowerSync(player, state.getPower(player.getUUID()));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PowerState state = PowerState.get(event.getServer());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            Power power = state.getPower(player.getUUID());
            if (power != null) {
                power.onTick(player);
            }
        }
    }

    // Featherfall: cancel fall damage entirely.
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Power power = PowerState.get(player.getServer()).getPower(player.getUUID());
        if (power == Power.FEATHER_FALL) {
            event.setCanceled(true);
        }
    }

    // Ragemode: the lower the attacking player's health, the harder they hit (up to 5x).
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof ServerPlayer attacker)) {
            return;
        }
        Power power = PowerState.get(attacker.getServer()).getPower(attacker.getUUID());
        if (power != Power.RAGE_MODE) {
            return;
        }
        float healthRatio = Mth.clamp(attacker.getHealth() / attacker.getMaxHealth(), 0.0f, 1.0f);
        float missingRatio = 1.0f - healthRatio;
        // 1.0x at full health, scaling up to 5.0x (500%) as health approaches zero.
        float multiplier = Math.min(1.0f + missingRatio * 4.0f, 5.0f);
        event.setAmount(event.getAmount() * multiplier);
    }

    // Ember Touch: anything the player hits catches fire for 10 seconds.
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof ServerPlayer attacker)) {
            return;
        }
        Power power = PowerState.get(attacker.getServer()).getPower(attacker.getUUID());
        if (power != Power.EMBER_TOUCH) {
            return;
        }
        Entity target = event.getEntity();
        if (target != attacker) {
            target.setSecondsOnFire(10);
        }
    }

    // Ore Sense: mining blocks builds toward a burst of ore detection.
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isCreative()) {
            return; // don't let creative-mode breaking farm charge
        }
        Power power = PowerState.get(player.getServer()).getPower(player.getUUID());
        if (power != Power.XRAY) {
            return;
        }
        long currentTick = player.level().getGameTime();
        boolean burstStarted = PowerRuntime.addOreSenseProgress(player.getUUID(), currentTick);
        if (burstStarted) {
            player.displayClientMessage(Component.literal("\u00a7dOre Sense flickers to life for a few seconds!"), true);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mypower")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (player == null) {
                        ctx.getSource().sendFailure(Component.literal("Only players have powers."));
                        return 0;
                    }
                    Power power = PowerState.get(ctx.getSource().getServer()).getPower(player.getUUID());
                    if (power == null) {
                        ctx.getSource().sendSuccess(() -> Component.literal("You haven't awakened a power yet."), false);
                    } else {
                        ctx.getSource().sendSuccess(() ->
                                Component.literal("Your power: \u00a7e" + power.getDisplayName() + " \u00a77\u2014 " + power.getMessage()), false);
                    }
                    return 1;
                }));
    }

    private ServerEventHandler() {
    }
}
