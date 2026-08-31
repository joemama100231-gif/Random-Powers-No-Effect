package com.example.randompowers.client;

import com.example.randompowers.RandomPowersMod;
import com.example.randompowers.RandomPowersNetworking;
import com.example.randompowers.network.BlinkPacket;
import com.example.randompowers.network.ToggleBoostPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cave Dweller: cancels nametag rendering entirely for the local client that
 * owns that power. This only affects what that player sees on their own
 * screen — it does not hide anything from other players. Forge has a
 * dedicated event for this (RenderNameTagEvent), so unlike the Fabric
 * version no Mixin into the entity renderer is needed.
 */
@Mod.EventBusSubscriber(modid = RandomPowersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeBusEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        while (ClientModBusEvents.TOGGLE_BOOST_KEY.consumeClick()) {
            RandomPowersNetworking.CHANNEL.sendToServer(new ToggleBoostPacket());
        }
        while (ClientModBusEvents.BLINK_KEY.consumeClick()) {
            RandomPowersNetworking.CHANNEL.sendToServer(new BlinkPacket());
        }
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if ("CAVE_DWELLER".equals(RandomPowersClientState.currentPower)) {
            event.setCanceled(true);
        }
    }

    private ClientForgeBusEvents() {
    }
}
