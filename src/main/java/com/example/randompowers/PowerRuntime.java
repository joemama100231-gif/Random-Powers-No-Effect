package com.example.randompowers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds short-lived, non-persistent state for powers that need a toggle
 * flag or a cooldown timer. This intentionally is NOT saved to disk —
 * losing a toggle state or an active cooldown on server restart is fine.
 */
public final class PowerRuntime {

    private static final Map<UUID, Boolean> OVERDRIVE_ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> BLINK_COOLDOWN_END_TICK = new HashMap<>();

    /** How many blocks a player with Ore Sense must mine to earn one burst of it. */
    public static final int ORE_SENSE_BLOCKS_REQUIRED = 200;
    /** How long each earned burst lasts, in ticks (3 seconds). */
    public static final long ORE_SENSE_ACTIVE_TICKS = 60L;

    private static final Map<UUID, Integer> ORE_SENSE_PROGRESS = new HashMap<>();
    private static final Map<UUID, Long> ORE_SENSE_ACTIVE_UNTIL_TICK = new HashMap<>();

    private PowerRuntime() {
    }

    public static boolean isOverdriveActive(UUID uuid) {
        return OVERDRIVE_ACTIVE.getOrDefault(uuid, false);
    }

    public static void toggleOverdrive(UUID uuid) {
        OVERDRIVE_ACTIVE.put(uuid, !isOverdriveActive(uuid));
    }

    public static boolean isBlinkReady(UUID uuid, long currentTick) {
        Long readyAt = BLINK_COOLDOWN_END_TICK.get(uuid);
        return readyAt == null || currentTick >= readyAt;
    }

    /** Returns remaining cooldown in ticks, or 0 if ready. */
    public static long getBlinkCooldownRemaining(UUID uuid, long currentTick) {
        Long readyAt = BLINK_COOLDOWN_END_TICK.get(uuid);
        if (readyAt == null) {
            return 0;
        }
        return Math.max(0, readyAt - currentTick);
    }

    public static void startBlinkCooldown(UUID uuid, long currentTick, long cooldownTicks) {
        BLINK_COOLDOWN_END_TICK.put(uuid, currentTick + cooldownTicks);
    }

    /**
     * Call once per block a player with Ore Sense mines. Once enough blocks
     * have accumulated, resets the counter and starts an active burst.
     * Returns true if this call was the one that triggered a new burst.
     */
    public static boolean addOreSenseProgress(UUID uuid, long currentTick) {
        int progress = ORE_SENSE_PROGRESS.getOrDefault(uuid, 0) + 1;
        if (progress >= ORE_SENSE_BLOCKS_REQUIRED) {
            ORE_SENSE_PROGRESS.put(uuid, 0);
            ORE_SENSE_ACTIVE_UNTIL_TICK.put(uuid, currentTick + ORE_SENSE_ACTIVE_TICKS);
            return true;
        }
        ORE_SENSE_PROGRESS.put(uuid, progress);
        return false;
    }

    public static int getOreSenseProgress(UUID uuid) {
        return ORE_SENSE_PROGRESS.getOrDefault(uuid, 0);
    }

    public static boolean isOreSenseActive(UUID uuid, long currentTick) {
        Long until = ORE_SENSE_ACTIVE_UNTIL_TICK.get(uuid);
        return until != null && currentTick < until;
    }
}
