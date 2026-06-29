package com.iamkaf.konfig.api.v1;

/**
 * Describes when a config value change fully takes effect.
 */
public enum RestartRequirement {
    /**
     * The value can apply without restarting.
     */
    NONE,
    /**
     * The value requires reloading or reopening the current world.
     */
    WORLD,
    /**
     * The value requires restarting the game.
     */
    GAME
}
