package com.example.randompowers.network;

import com.example.randompowers.client.RandomPowersClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: tells the client which power (if any) it currently has, so
 * client-only effects (like hiding nametags for Cave Dweller) can react.
 */
public class SyncPowerPacket {

    private final String power;

    public SyncPowerPacket(String power) {
        this.power = power;
    }

    public static void encode(SyncPowerPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.power);
    }

    public static SyncPowerPacket decode(FriendlyByteBuf buf) {
        return new SyncPowerPacket(buf.readUtf());
    }

    public static void handle(SyncPowerPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> RandomPowersClientState.setCurrentPower(pkt.power)));
        ctx.setPacketHandled(true);
    }
}
