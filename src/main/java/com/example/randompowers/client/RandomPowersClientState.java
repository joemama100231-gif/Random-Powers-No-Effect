package com.example.randompowers.client;

/** The power name the server most recently told us we have. Empty string = none yet. */
public final class RandomPowersClientState {

    public static volatile String currentPower = "";

    public static void setCurrentPower(String power) {
        currentPower = power;
    }

    private RandomPowersClientState() {
    }
}
