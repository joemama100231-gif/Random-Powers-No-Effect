package com.example.randompowers.network;

import com.example.randompowers.Power;
import com.example.randompowers.PowerRuntime;
import com.example.randompowers.PowerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: sent when the player presses the Overdrive toggle key. */
public class ToggleBoostPacket {

    public ToggleBoostPacket() {
    }

    public static void encode(ToggleBoostPacket pkt, FriendlyByteBuf buf) {
    }

    public static ToggleBoostPacket decode(FriendlyByteBuf buf) {
        return new ToggleBoostPacket();
    }

    public static void handle(ToggleBoostPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            Power power = PowerState.get(player.getServer()).getPower(player.getUUID());
            if (power == Power.OVERDRIVE) {
                PowerRuntime.toggleOverdrive(player.getUUID());
                boolean nowActive = PowerRuntime.isOverdriveActive(player.getUUID());
                player.displayClientMessage(Component.literal(nowActive
                        ? "\u00a7bOverdrive engaged \u2014 200% speed!"
                        : "\u00a77Overdrive disengaged."), true);
            }
        });
        ctx.setPacketHandled(true);
    }
}
