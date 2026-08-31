package com.example.randompowers.network;

import com.example.randompowers.Power;
import com.example.randompowers.PowerRuntime;
import com.example.randompowers.PowerState;
import com.example.randompowers.RandomPowersMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: sent when the player presses the Blink key. */
public class BlinkPacket {

    public BlinkPacket() {
    }

    public static void encode(BlinkPacket pkt, FriendlyByteBuf buf) {
    }

    public static BlinkPacket decode(FriendlyByteBuf buf) {
        return new BlinkPacket();
    }

    public static void handle(BlinkPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            Power power = PowerState.get(player.getServer()).getPower(player.getUUID());
            if (power != Power.BLINK) {
                return;
            }
            long currentTick = player.getServer().overworld().getGameTime();
            if (!PowerRuntime.isBlinkReady(player.getUUID(), currentTick)) {
                long remainingSeconds = PowerRuntime.getBlinkCooldownRemaining(player.getUUID(), currentTick) / 20L + 1;
                player.displayClientMessage(Component.literal("\u00a77Voidstep is on cooldown (" + remainingSeconds + "s left)."), true);
                return;
            }
            RandomPowersMod.performBlink(player);
            PowerRuntime.startBlinkCooldown(player.getUUID(), currentTick, RandomPowersMod.BLINK_COOLDOWN_TICKS);
        });
        ctx.setPacketHandled(true);
    }
}
