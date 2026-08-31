package com.example.randompowers.client;

import com.example.randompowers.RandomPowersMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Two keybindings (Overdrive toggle, Voidstep blink) that just fire a tiny
 * packet at the server. The {@code value = Dist.CLIENT} on the annotation
 * is what keeps this class from ever being loaded on a dedicated server —
 * Forge doesn't need a separate client source set the way Fabric does.
 */
@Mod.EventBusSubscriber(modid = RandomPowersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModBusEvents {

    public static final KeyMapping TOGGLE_BOOST_KEY = new KeyMapping(
            "key.randompowers.toggle_boost",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            "key.categories.randompowers");

    public static final KeyMapping BLINK_KEY = new KeyMapping(
            "key.randompowers.blink",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            "key.categories.randompowers");

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_BOOST_KEY);
        event.register(BLINK_KEY);
    }

    private ClientModBusEvents() {
    }
}
