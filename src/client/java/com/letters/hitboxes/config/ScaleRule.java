package com.letters.hitboxes.config;

/**
 * A partial rule. Every field is nullable: {@code null} means "inherit from the parent rule"
 * (entity type override -> category -> defaults). That makes the config file very flexible -
 * you can write {@code {"width": 2.0}} and everything else is inherited.
 */
public class ScaleRule {
    public HitboxMode mode;
    /** Width multiplier (X/Z). 1.0 = vanilla. */
    public Float width;
    /** Height multiplier (Y). 1.0 = vanilla. */
    public Float height;
    /** Extra multiplier applied to the eye height only (after the height scaling). */
    public Float eyeHeight;
    /** Blocks added to the ray cast box (crosshair, arrows) when mode uses TARGETING. */
    public Float pickRadiusBonus;
    /** Flat blocks added to width/height after multiplying. Can be negative. */
    public Float widthBonus;
    public Float heightBonus;
    /** Some entities (item frames, shulkers, ...) have "fixed" dimensions that vanilla refuses to
     *  scale. Set this to true to scale them anyway. */
    public Boolean scaleFixedDimensions;

    public ScaleRule() {
    }

    public ScaleRule(HitboxMode mode, Float width, Float height) {
        this.mode = mode;
        this.width = width;
        this.height = height;
    }

    /** Returns a new rule where every {@code null} field of {@code child} falls back to {@code parent}. */
    public static ScaleRule inherit(ScaleRule parent, ScaleRule child) {
        if (child == null) return parent;
        if (parent == null) return child;
        ScaleRule out = new ScaleRule();
        out.mode = child.mode != null ? child.mode : parent.mode;
        out.width = child.width != null ? child.width : parent.width;
        out.height = child.height != null ? child.height : parent.height;
        out.eyeHeight = child.eyeHeight != null ? child.eyeHeight : parent.eyeHeight;
        out.pickRadiusBonus = child.pickRadiusBonus != null ? child.pickRadiusBonus : parent.pickRadiusBonus;
        out.widthBonus = child.widthBonus != null ? child.widthBonus : parent.widthBonus;
        out.heightBonus = child.heightBonus != null ? child.heightBonus : parent.heightBonus;
        out.scaleFixedDimensions = child.scaleFixedDimensions != null ? child.scaleFixedDimensions : parent.scaleFixedDimensions;
        return out;
    }

    /** Collapses the rule into a fully populated, immutable snapshot. */
    public ResolvedRule resolve(float maxScale) {
        HitboxMode m = mode != null ? mode : HitboxMode.DIMENSIONS;
        float w = clamp(width, maxScale);
        float h = clamp(height, maxScale);
        float eye = clamp(eyeHeight, maxScale);
        float pick = pickRadiusBonus != null ? Math.max(0.0F, Math.min(pickRadiusBonus, maxScale)) : 0.0F;
        float wb = widthBonus != null ? widthBonus : 0.0F;
        float hb = heightBonus != null ? heightBonus : 0.0F;
        boolean fixed = scaleFixedDimensions != null && scaleFixedDimensions;
        return new ResolvedRule(m, w, h, eye, pick, wb, hb, fixed);
    }

    private static float clamp(Float value, float maxScale) {
        if (value == null) return 1.0F;
        return Math.max(0.05F, Math.min(value, maxScale));
    }

    @Override
    public String toString() {
        return "mode=" + mode + ", width=" + width + ", height=" + height
                + ", eyeHeight=" + eyeHeight + ", pickRadiusBonus=" + pickRadiusBonus
                + ", widthBonus=" + widthBonus + ", heightBonus=" + heightBonus
                + ", scaleFixedDimensions=" + scaleFixedDimensions;
    }
}
