package com.letters.hitboxes.mixin;

import com.letters.hitboxes.HitboxEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * All mobs and players. This is the real, physical hitbox: the returned dimensions are used to build
 * the AABB, so collisions, movement, melee/projectile hit detection and rendering all follow.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDimensionsMixin {

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void lettersHitboxes$scaleLivingDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        EntityDimensions modified = HitboxEngine.modifyDimensions((Entity) (Object) this, cir.getReturnValue());
        if (modified != null) {
            cir.setReturnValue(modified);
        }
    }
}
