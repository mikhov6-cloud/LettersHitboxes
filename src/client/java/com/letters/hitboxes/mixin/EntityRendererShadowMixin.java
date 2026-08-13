package com.letters.hitboxes.mixin;

import com.letters.hitboxes.HitboxEngine;
import com.letters.hitboxes.config.ConfigManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes the drop shadow follow the (visually) enlarged model. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererShadowMixin {

    @Inject(method = "getShadowRadius", at = @At("RETURN"), cancellable = true)
    private void lettersHitboxes$scaleShadow(Entity entity, CallbackInfoReturnable<Float> cir) {
        if (!ConfigManager.get().active().visual.scaleShadow) return;
        float radius = cir.getReturnValueF();
        if (radius <= 0.0F) return;
        float[] scale = HitboxEngine.modelScale(entity);
        if (scale != null) {
            cir.setReturnValue(radius * scale[0]);
        }
    }
}
