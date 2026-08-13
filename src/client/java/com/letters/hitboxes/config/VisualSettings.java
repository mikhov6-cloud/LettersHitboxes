package com.letters.hitboxes.config;

/** Visual (model) scaling so mobs LOOK the size of their physical hitbox. */
public class VisualSettings {

    /** Scale entity models to match the modified hitbox. */
    public boolean scaleModels = true;
    /** Also scale the local player's own model (third person / F5). */
    public boolean scaleSelfModel = true;
    /** Extra multiplier on top of the hitbox ratio (1.0 = exactly follow the hitbox). */
    public float modelWidthFactor = 1.0F;
    public float modelHeightFactor = 1.0F;
    /** Safety clamp for the model scale. */
    public float maxModelScale = 6.0F;
    /** Scale the drop shadow together with the model. */
    public boolean scaleShadow = true;

    public void sanitize() {
        if (modelWidthFactor <= 0.0F || Float.isNaN(modelWidthFactor)) modelWidthFactor = 1.0F;
        if (modelHeightFactor <= 0.0F || Float.isNaN(modelHeightFactor)) modelHeightFactor = 1.0F;
        if (maxModelScale <= 0.0F || Float.isNaN(maxModelScale)) maxModelScale = 6.0F;
        maxModelScale = Math.min(maxModelScale, 64.0F);
    }
}
