package com.letters.hitboxes.config;

/** Fully resolved, cached rule for one entity type. */
public record ResolvedRule(
        HitboxMode mode,
        float width,
        float height,
        float eyeHeight,
        float pickRadiusBonus,
        float widthBonus,
        float heightBonus,
        boolean scaleFixedDimensions
) {
    public static final ResolvedRule NONE =
            new ResolvedRule(HitboxMode.OFF, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, false);

    public boolean changesDimensions() {
        return mode.affectsDimensions()
                && (width != 1.0F || height != 1.0F || eyeHeight != 1.0F || widthBonus != 0.0F || heightBonus != 0.0F);
    }

    public boolean changesTargeting() {
        return mode.affectsTargeting() && pickRadiusBonus > 0.0F;
    }

    public boolean isNoop() {
        return !changesDimensions() && !changesTargeting();
    }
}
