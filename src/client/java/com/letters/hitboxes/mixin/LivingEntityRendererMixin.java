package com.letters.hitboxes.mixin;

import com.letters.hitboxes.HitboxEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional visual part: scales the model so it LOOKS the size of the (already physical) hitbox.
 * Runs after vanilla's own scaling (baby mobs, slimes...), so those effects are preserved.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "scale", at = @At("TAIL"))
    private void lettersHitboxes$scaleModel(LivingEntity entity, PoseStack poseStack, float partialTick,
                                           CallbackInfo ci) {
        float[] scale = HitboxEngine.modelScale(entity);
        if (scale != null) {
            poseStack.scale(scale[0], scale[1], scale[0]);
        }
    }
}
