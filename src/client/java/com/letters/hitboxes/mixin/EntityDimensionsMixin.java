package com.letters.hitboxes.mixin;

import com.letters.hitboxes.HitboxEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Non-living entities (boats, minecarts, arrows, item entities...).
 * {@code LivingEntity} overrides {@code getDimensions} without calling super, so living entities are
 * handled by {@link LivingEntityDimensionsMixin} - no double scaling.
 */
@Mixin(Entity.class)
public abstract class EntityDimensionsMixin {

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void lettersHitboxes$scaleDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        EntityDimensions modified = HitboxEngine.modifyDimensions((Entity) (Object) this, cir.getReturnValue());
        if (modified != null) {
            cir.setReturnValue(modified);
        }
    }
}
