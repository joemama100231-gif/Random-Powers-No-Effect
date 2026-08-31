package com.example.randompowers;

import com.example.randompowers.network.BlinkPacket;
import com.example.randompowers.network.SyncPowerPacket;
import com.example.randompowers.network.ToggleBoostPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Channel + packet registration for the small amount of custom networking
 * this mod needs (Overdrive toggle, Voidstep blink, and the server telling
 * the client which power it has, so client-only effects can react).
 */
public final class RandomPowersNetworking {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RandomPowersMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(nextId++, ToggleBoostPacket.class,
                ToggleBoostPacket::encode, ToggleBoostPacket::decode, ToggleBoostPacket::handle);
        CHANNEL.registerMessage(nextId++, BlinkPacket.class,
                BlinkPacket::encode, BlinkPacket::decode, BlinkPacket::handle);
        CHANNEL.registerMessage(nextId++, SyncPowerPacket.class,
                SyncPowerPacket::encode, SyncPowerPacket::decode, SyncPowerPacket::handle);
    }

    private RandomPowersNetworking() {
    }
}
