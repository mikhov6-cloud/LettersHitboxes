package com.letters.hitboxes.config;

/**
 * How a hitbox rule is applied.
 *
 * <p>OFF        - nothing is changed.
 * <p>DIMENSIONS - the real {@code EntityDimensions} (and therefore the AABB used for collisions,
 *                 movement, melee/projectile hit detection and rendering) is scaled. This is a
 *                 physical change, not a visual one.
 * <p>TARGETING  - only the "pick radius" is increased. The entity keeps its vanilla physics, but
 *                 the box used by ray casts (crosshair targeting, arrows, tridents...) grows.
 * <p>BOTH       - DIMENSIONS + TARGETING.
 */
public enum HitboxMode {
    OFF,
    DIMENSIONS,
    TARGETING,
    BOTH;

    public boolean affectsDimensions() {
        return this == DIMENSIONS || this == BOTH;
    }

    public boolean affectsTargeting() {
        return this == TARGETING || this == BOTH;
    }
}
