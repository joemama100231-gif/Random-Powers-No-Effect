package com.example.randompowers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores which power each player (by UUID) has been permanently assigned.
 * Saved to the overworld's level data, so it survives server restarts.
 */
public class PowerState extends SavedData {

    private static final String STORAGE_KEY = "randompowers_data";

    private final Map<UUID, String> powers = new HashMap<>();

    public static PowerState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(PowerState::load, PowerState::new, STORAGE_KEY);
    }

    public static PowerState load(CompoundTag nbt) {
        PowerState state = new PowerState();
        CompoundTag powersNbt = nbt.getCompound("powers");
        for (String key : powersNbt.getAllKeys()) {
            state.powers.put(UUID.fromString(key), powersNbt.getString(key));
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag powersNbt = new CompoundTag();
        for (Map.Entry<UUID, String> entry : powers.entrySet()) {
            powersNbt.putString(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("powers", powersNbt);
        return nbt;
    }

    public boolean hasPower(UUID uuid) {
        return powers.containsKey(uuid);
    }

    public Power getPower(UUID uuid) {
        String name = powers.get(uuid);
        if (name == null) {
            return null;
        }
        try {
            return Power.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setPower(UUID uuid, Power power) {
        powers.put(uuid, power.name());
        setDirty();
    }
}
