package com.letters.hitboxes.mixin;

import com.letters.hitboxes.HitboxEngine;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TARGETING mode. {@code Entity#getPickRadius} is what {@code ProjectileUtil#getEntityHitResult}
 * inflates the candidate AABB with, so raising it grows the box used by crosshair targeting and by
 * client-simulated projectiles WITHOUT touching physics/collisions.
 */
@Mixin(Entity.class)
public abstract class EntityPickRadiusMixin {

    @Inject(method = "getPickRadius", at = @At("RETURN"), cancellable = true)
    private void lettersHitboxes$expandPickRadius(CallbackInfoReturnable<Float> cir) {
        float bonus = HitboxEngine.extraPickRadius((Entity) (Object) this);
        if (bonus > 0.0F) {
            cir.setReturnValue(cir.getReturnValueF() + bonus);
        }
    }
}
