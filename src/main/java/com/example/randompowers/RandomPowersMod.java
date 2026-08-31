package com.example.randompowers;

import com.example.randompowers.network.SyncPowerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

@Mod(RandomPowersMod.MOD_ID)
public class RandomPowersMod {

    public static final String MOD_ID = "randompowers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final List<Power> ALL_POWERS = List.of(Power.values());
    private static final Random RANDOM = new Random();

    private static final double BLINK_MAX_DISTANCE = 100.0;
    public static final long BLINK_COOLDOWN_TICKS = 30 * 20L; // 30 seconds

    public RandomPowersMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        RandomPowersNetworking.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Random Powers is loading — {} powers available", ALL_POWERS.size());
    }

    public static Power randomPower() {
        return ALL_POWERS.get(RANDOM.nextInt(ALL_POWERS.size()));
    }

    public static void sendPowerSync(ServerPlayer player, Power power) {
        RandomPowersNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPowerPacket(power == null ? "" : power.name()));
    }

    public static void performBlink(ServerPlayer player) {
        Vec3 start = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = start.add(look.scale(BLINK_MAX_DISTANCE));

        ClipContext context = new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player);
        BlockHitResult hit = player.level().clip(context);

        Vec3 destination = end;
        if (hit.getType() == HitResult.Type.BLOCK) {
            // Step back a little from the wall we hit so we don't end up inside it.
            destination = hit.getLocation().subtract(look.scale(0.5));
        }

        player.connection.teleport(destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
